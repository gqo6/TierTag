package com.gqo6.tiertag;

import com.gqo6.tiertag.config.TierConfig;
import com.gqo6.tiertag.data.McpvpClient;
import com.gqo6.tiertag.data.PlayerTiers;
import com.gqo6.tiertag.data.Tier;
import com.gqo6.tiertag.render.TierService;
import com.gqo6.tiertag.screen.TierConfigScreen;
import com.gqo6.tiertag.util.UpdateChecker;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class TierTagMod implements ClientModInitializer {
    public static final String MOD_ID = "tiertag";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static TierConfig config;
    private static TierService service;

    public static TierConfig config() {
        return config;
    }

    public static TierService service() {
        return service;
    }

    public static MinecraftClient client() {
        return MinecraftClient.getInstance();
    }

    public static String version() {
        Optional<String> version = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString());
        return version.orElse("1.0.0");
    }

    public static void reloadConfig() {
        config = TierConfig.load();
        if (service != null) {
            service.clearCache();
        }
    }

    @Override
    public void onInitializeClient() {
        config = TierConfig.load();
        service = new TierService(config);
        registerCommands();
        registerUpdateCheck();
        LOGGER.info("[TierTag] v{} initialized. Data source: {}<username> (HTML profile)",
                version(), McpvpClient.BASE_URL);
    }

    private void registerUpdateCheck() {
        if (config.checkUpdates) {
            UpdateChecker.checkAsync(config.updateCheckRepo, false);
        }
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> UpdateChecker.announceIfNeeded());
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("tiertag")
                        .then(ClientCommandManager.literal("toggle").executes(context -> {
                            config.enabled = !config.enabled;
                            config.save();
                            feedback("Tier tags " + (config.enabled ? "enabled" : "disabled"));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("tab").executes(context -> {
                            config.showInTabList = !config.showInTabList;
                            config.save();
                            feedback("Tab list tiers " + (config.showInTabList ? "enabled" : "disabled"));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("icons").executes(context -> {
                            config.showIcons = !config.showIcons;
                            config.save();
                            feedback("Icons " + (config.showIcons ? "enabled" : "disabled"));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("reload").executes(context -> {
                            reloadConfig();
                            feedback("Config reloaded");
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("update").executes(context -> {
                            if (config.checkUpdates) {
                                feedback("Checking GitHub for updates...");
                                UpdateChecker.checkAsync(config.updateCheckRepo, true);
                            } else {
                                feedback("Update checks are disabled in the config");
                            }
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("clearcache").executes(context -> {
                            if (service != null) {
                                service.clearCache();
                            }
                            feedback("Tier cache cleared");
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("lookup")
                                .then(ClientCommandManager.argument("player", StringArgumentType.word())
                                        .executes(context -> {
                                            String username = StringArgumentType.getString(context, "player");
                                            feedback("Looking up " + username + " on MCPVP...");
                                            if (service != null) {
                                                service.lookup(username).whenComplete((tiers, error) ->
                                                        MinecraftClient.getInstance().execute(() ->
                                                                reportLookup(username, tiers, error)));
                                            }
                                            return 1;
                                        })))));
    }

    private static void feedback(String message) {
        MinecraftClient.getInstance().player.sendMessage(
                Text.literal("[TierTagger] ").formatted(Formatting.GOLD).append(Text.literal(message).formatted(Formatting.WHITE)),
                false);
    }

    private static void reportLookup(String username, PlayerTiers tiers, Throwable error) {
        if (error != null) {
            feedback("Lookup failed for " + username + ": " + error.getMessage());
            return;
        }
        if (tiers == null || !tiers.found()) {
            feedback("No MCPVP profile or verified tiers for " + username);
            return;
        }
        Tier best = tiers.bestVerifiedTier().map(Map.Entry::getValue).orElse(null);
        String bestKit = tiers.bestVerifiedTier().map(Map.Entry::getKey).orElse(null);
        if (best != null) {
            Text bestLine = Text.literal("[TierTagger] ").formatted(Formatting.GOLD)
                    .append(Text.literal("Best: ").formatted(Formatting.WHITE))
                    .append(Text.literal(com.gqo6.tiertag.data.Kit.glyphFor(bestKit) + " " + best.name())
                            .styled(style -> style.withColor(config.colorFor(best))))
                    .append(Text.literal(" (" + com.gqo6.tiertag.data.Kit.displayNameFor(bestKit) + ")")
                            .formatted(Formatting.GRAY));
            MinecraftClient.getInstance().player.sendMessage(bestLine, false);
        }
        Text header = Text.literal("[TierTagger] ").formatted(Formatting.GOLD)
                .append(Text.literal(tiers.username() + " — overall: ").formatted(Formatting.WHITE))
                .append(Text.literal(tiers.overallTierCode() == null ? "unranked" : tiers.overallTierCode())
                        .styled(style -> style.withColor(tiers.overallTier()
                                .map(t -> config.colorFor(t)).orElse(0xFFFFFF))));
        MinecraftClient.getInstance().player.sendMessage(header, false);
        for (Map.Entry<String, String> entry : tiers.kitTiers().entrySet()) {
            String displayName = com.gqo6.tiertag.data.Kit.displayNameFor(entry.getKey());
            Text line = Text.literal("  " + displayName + ": ").formatted(Formatting.GRAY)
                    .append(Text.literal(entry.getValue()).formatted(Formatting.WHITE));
            MinecraftClient.getInstance().player.sendMessage(line, false);
        }
    }
}
