package com.gqo6.tiertag.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TierCache {
    public static final long NEVER = -1L;

    public static final class Entry {
        public volatile PlayerTiers tiers;
        public volatile long expiresAt;
        public volatile long notFoundUntil;
        public volatile int consecutiveFailures;
        public volatile long backoffUntil;
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public Entry get(String username) {
        return entries.get(key(username));
    }

    public Entry computeIfAbsent(String username) {
        return entries.computeIfAbsent(key(username), k -> new Entry());
    }

    public boolean isFresh(String username, long ttlMillis, long now) {
        Entry entry = get(username);
        if (entry == null || entry.tiers == null || entry.tiers.found()) {
            return false;
        }
        return now < entry.expiresAt;
    }

    public PlayerTiers getIfFresh(String username, long ttlMillis, long now) {
        Entry entry = get(username);
        if (entry == null || entry.tiers == null) {
            return null;
        }
        long expiry = entry.tiers.found() ? entry.expiresAt : entry.notFoundUntil;
        return now < expiry ? entry.tiers : null;
    }

    public void put(PlayerTiers tiers, long ttlMillis, long now) {
        Entry entry = computeIfAbsent(tiers.username());
        entry.tiers = tiers;
        if (tiers.found()) {
            entry.expiresAt = now + ttlMillis;
            entry.notFoundUntil = 0;
            entry.consecutiveFailures = 0;
            entry.backoffUntil = 0;
        } else {
            entry.notFoundUntil = now + Math.max(ttlMillis / 5, 30_000L);
        }
    }

    public void recordFailure(String username, long now) {
        Entry entry = computeIfAbsent(username);
        entry.consecutiveFailures++;
        long delay = Math.min(60_000L, 5_000L * (1L << Math.min(entry.consecutiveFailures, 4)));
        entry.backoffUntil = now + delay;
    }

    public boolean isBackingOff(String username, long now) {
        Entry entry = get(username);
        return entry != null && now < entry.backoffUntil;
    }

    public void clear() {
        entries.clear();
    }

    private static String key(String username) {
        return username == null ? "" : username.toLowerCase();
    }
}
