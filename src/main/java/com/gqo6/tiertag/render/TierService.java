package com.gqo6.tiertag.render;

import com.gqo6.tiertag.config.TierConfig;
import com.gqo6.tiertag.data.McpvpClient;
import com.gqo6.tiertag.data.PlayerTiers;
import com.gqo6.tiertag.data.TierCache;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TierService {
    private final TierConfig config;
    private final TierCache cache = new TierCache();
    private final McpvpClient client;

    public TierService(TierConfig config) {
        this.config = config;
        this.client = new McpvpClient(cache, config);
    }

    public PlayerTiers getOrRequest(UUID uuid, String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        long now = System.currentTimeMillis();
        PlayerTiers cached = client.lookupCached(uuid, username, now);
        if (cached != null && cached.found()) {
            return cached;
        }
        client.request(uuid, username, now);
        TierCache.Entry entry = cache.get(username);
        if (entry != null && entry.tiers != null && entry.tiers.found()) {
            return entry.tiers;
        }
        return null;
    }

    public CompletableFuture<PlayerTiers> lookup(String username) {
        return client.lookupNow(null, username);
    }

    public void clearCache() {
        client.clearCache();
    }
}
