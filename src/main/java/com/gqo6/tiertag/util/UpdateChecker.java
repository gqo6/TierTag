package com.gqo6.tiertag.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.gqo6.tiertag.TierTagMod;
import com.mojang.logging.LogUtils;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class UpdateChecker {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static volatile Text pendingMessage;

    private UpdateChecker() {
    }

    public static void checkAsync(String repo) {
        checkAsync(repo, false);
    }

    /**
     * @param direct when true (manual /tiertag update), print the result immediately
     *               in chat if a player is online instead of waiting for the next join.
     */
    public static void checkAsync(String repo, boolean direct) {
        if (repo == null || repo.isBlank() || !repo.contains("/")) {
            return;
        }
        String url = "https://api.github.com/repos/" + repo.trim() + "/releases/latest";
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "TierTag update checker (Minecraft Fabric mod)")
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();
        http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .completeOnTimeout(null, 10, TimeUnit.SECONDS)
                .thenAccept(response -> {
                    if (response == null || response.statusCode() != 200) {
                        LOGGER.debug("[TierTag] update check: no release found for {}", repo);
                        return;
                    }
                    JsonObject release = GSON.fromJson(response.body(), JsonObject.class);
                    String tagName = release.has("tag_name") && !release.get("tag_name").isJsonNull()
                            ? release.get("tag_name").getAsString() : null;
                    String htmlUrl = release.has("html_url") && !release.get("html_url").isJsonNull()
                            ? release.get("html_url").getAsString() : null;
                    if (tagName == null || htmlUrl == null) {
                        return;
                    }
                    String latest = normalize(tagName);
                    String current = normalize(TierTagMod.version());
                    if (latest.isEmpty() || !isNewer(latest, current)) {
                        LOGGER.info("[TierTag] up to date ({} vs {})", current, latest);
                        if (direct && TierTagMod.client() != null && TierTagMod.client().player != null) {
                            // Diagnostic: user can always see WHICH jar actually runs
                            TierTagMod.client().player.sendMessage(Text.literal("[TierTag] ")
                                    .formatted(Formatting.GOLD)
                                    .append(Text.literal("running v" + current + " - up to date")
                                            .formatted(Formatting.GRAY)), false);
                        }
                        return;
                    }
                    LOGGER.info("[TierTag] update available: {} -> {}", current, latest);
                    Text msg = buildUpdateMessage(latest, current, htmlUrl);
                    if (direct && TierTagMod.client() != null && TierTagMod.client().player != null) {
                        TierTagMod.client().player.sendMessage(msg, false);
                    } else {
                        pendingMessage = msg;
                    }
                })
                .exceptionally(error -> {
                    LOGGER.debug("[TierTag] update check failed: {}", error.toString());
                    return null;
                });
    }

    public static void announceIfNeeded() {
        Text message = pendingMessage;
        if (message != null) {
            pendingMessage = null;
            if (TierTagMod.client() != null && TierTagMod.client().player != null) {
                TierTagMod.client().player.sendMessage(message.copy(), false);
            }
        }
    }

    /**
     * Version-aware update message. This build compiles against the sealed
     * {@code ClickEvent.OpenUrl(URI)} record (1.21.2+). On older releases where
     * that nested record does not exist the link construction throws a linkage
     * error, which is caught here so 1.21.0/1.21.1 degrade to plain text
     * instead of crashing.
     */
    private static Text buildUpdateMessage(String latest, String current, String htmlUrl) {
        MutableText base = Text.literal("[TierTag] ").formatted(Formatting.GOLD)
                .append(Text.literal("Update available: ").formatted(Formatting.YELLOW))
                .append(Text.literal(latest).styled(style -> style.withColor(TextColor.fromRgb(0x55FFFF))))
                .append(Text.literal(" (current: " + current + ") ").formatted(Formatting.GRAY));
        try {
            return base.append(Text.literal("[Download]")
                    .styled(style -> style.withColor(TextColor.fromRgb(0x55FF55))
                            .withUnderline(true)
                            .withClickEvent(new ClickEvent.OpenUrl(URI.create(htmlUrl)))));
        } catch (Throwable legacyLinkage) {
            return base.append(Text.literal(htmlUrl).formatted(Formatting.GRAY));
        }
    }

    private static String normalize(String version) {
        String v = version.trim().toLowerCase();
        if (v.startsWith("v")) {
            v = v.substring(1);
        }
        StringBuilder sb = new StringBuilder();
        for (char c : v.toCharArray()) {
            if (Character.isDigit(c) || c == '.') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean isNewer(String latest, String current) {
        String[] a = latest.split("\\.");
        String[] b = current.split("\\.");
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int x = i < a.length && !a[i].isEmpty() ? Integer.parseInt(a[i]) : 0;
            int y = i < b.length && !b[i].isEmpty() ? Integer.parseInt(b[i]) : 0;
            if (x != y) {
                return x > y;
            }
        }
        return false;
    }
}
