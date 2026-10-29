package com.gqo6.tiertag.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gqo6.tiertag.data.Tier;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TierConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public boolean showIcons = true;
    public boolean showAboveHead = true;
    public boolean showInTabList = true;

    public String displayMode = "HIGHEST";

    public String tagSeparator = "|";
    public int maxRenderDistance = 64;
    public boolean debugTestTag = false;

    public int cacheDurationSeconds = 600;
    public int requestIntervalMs = 500;
    public int negativeCacheSeconds = 120;

    public boolean checkUpdates = true;
    public String updateCheckRepo = "gqo6/TierTag";

    public Map<String, String> tierColors = defaultTierColors();

    public static Map<String, String> defaultTierColors() {
        Map<String, String> colors = new LinkedHashMap<>();
        for (Tier tier : Tier.values()) {
            colors.put(tier.name(), String.format("#%06X", tier.rgb()));
        }
        return colors;
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("tiertag.json");
    }

    public static TierConfig load() {
        TierConfig config = new TierConfig();
        Path path = configPath();
        if (Files.exists(path)) {
            try {
                TierConfig loaded = GSON.fromJson(Files.readString(path), TierConfig.class);
                if (loaded != null) {
                    config = loaded;
                    if (config.tierColors == null) {
                        config.tierColors = defaultTierColors();
                    }
                }
            } catch (IOException | RuntimeException ignored) {
            }
        }
        return config;
    }

    public void save() {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException ignored) {
        }
    }

    public long cacheTtlMillis() {
        return Math.max(30L, cacheDurationSeconds) * 1000L;
    }

    public long negativeTtlMillis() {
        return Math.max(30L, negativeCacheSeconds) * 1000L;
    }

    public long requestIntervalMillis() {
        return Math.max(0, requestIntervalMs);
    }

    public int colorFor(Tier tier) {
        if (tier == null) {
            return 0xFFFFFF;
        }
        String hex = tierColors.get(tier.name());
        if (hex != null) {
            try {
                String cleaned = hex.trim().replace("#", "");
                if (cleaned.length() == 6) {
                    return Integer.parseInt(cleaned, 16);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return tier.rgb();
    }
}
