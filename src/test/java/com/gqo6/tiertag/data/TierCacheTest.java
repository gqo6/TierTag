package com.gqo6.tiertag.data;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TierCacheTest {
    @Test
    void cachesFoundTiersWithTtl() {
        TierCache cache = new TierCache();
        PlayerTiers tiers = new PlayerTiers(null, "A", Map.of("mace", "LT6"), "LT6", "mace", 100L, true);
        cache.put(tiers, 60_000L, 100L);
        assertEquals(tiers, cache.getIfFresh("a", 60_000L, 200L));
        assertEquals(null, cache.getIfFresh("a", 60_000L, 60_001L + 100L));
    }

    @Test
    void negativeCacheExpiresFaster() {
        TierCache cache = new TierCache();
        PlayerTiers tiers = PlayerTiers.notFound("B", 100L);
        cache.put(tiers, 300_000L, 100L);
        assertEquals(tiers, cache.getIfFresh("b", 300_000L, 100L + 55_000L));
        assertEquals(null, cache.getIfFresh("b", 300_000L, 100L + 61_000L));
    }

    @Test
    void backoffEscalatesAndClearsOnSuccess() {
        TierCache cache = new TierCache();
        cache.recordFailure("C", 0L);
        assertTrue(cache.isBackingOff("c", 1000L));
        cache.recordFailure("C", 1000L);
        assertTrue(cache.isBackingOff("c", 12_000L));
        PlayerTiers tiers = new PlayerTiers(null, "C", Map.of("sword", "HT2"), "HT2", "sword", 20_000L, true);
        cache.put(tiers, 60_000L, 20_000L);
        assertFalse(cache.isBackingOff("c", 20_001L));
        assertEquals(0, cache.computeIfAbsent("C").consecutiveFailures);
    }

    @Test
    void caseInsensitiveKeys() {
        TierCache cache = new TierCache();
        PlayerTiers tiers = new PlayerTiers(null, "MiXeD", Map.of(), "HT1", null, 1L, true);
        cache.put(tiers, 60_000L, 1L);
        assertEquals(tiers, cache.getIfFresh("mixed", 60_000L, 2L));
    }
}
