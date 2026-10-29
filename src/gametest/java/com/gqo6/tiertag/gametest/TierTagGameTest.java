package com.gqo6.tiertag.gametest;

import com.gqo6.tiertag.TierTagMod;
import com.gqo6.tiertag.config.TierConfig;
import com.gqo6.tiertag.render.TierFormatter;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.text.Text;

import java.nio.file.Path;

/**
 * Deterministic visual harness: renders the EXACT text the mod prepends to a
 * player name through Minecraft's real font stack (same provider used above
 * heads, in the Tab list and chat) as the action-bar overlay, then screenshots
 * it. Does not depend on key-press timing like the Tab list did.
 */
public class TierTagGameTest implements FabricClientGameTest {
    private static final String[] KITS = {"end-game", "crystal", "mace", "smp", "pot", "spear"};

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            TierConfig config = TierTagMod.config();
            config.enabled = true;
            config.showIcons = true;
            config.showAboveHead = true;
            config.showInTabList = true;
            config.displayMode = "HIGHEST";
        });
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            for (String kit : KITS) {
                final String slug = kit;
                context.runOnClient(client -> {
                    TierConfig config = TierTagMod.config();
                    config.debugTestTag = true;
                    config.debugTestKit = slug;
                    Text tag = TierFormatter.appendTier(
                            client.player.getUuid(), "Dev", Text.literal("Dev"));
                    client.inGameHud.setOverlayMessage(tag.copy(), false);
                });
                context.waitTicks(10);
                Path shot = context.takeScreenshot("tier_tag_" + slug);
                System.out.println("[TierTagGameTest] screenshot " + slug + ": " + shot);
            }
            context.runOnClient(client -> {
                TierConfig config = TierTagMod.config();
                config.debugTestKit = null;
                client.inGameHud.setOverlayMessage(Text.empty(), false);
            });
        }
    }
}
