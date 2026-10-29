package com.gqo6.tiertag.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerTiers {
    private final UUID uuid;
    private final String username;
    private final Map<String, String> kitTiers;
    private final String overallTier;
    private final String topKitSlug;
    private final long fetchedAt;
    private final boolean found;

    public PlayerTiers(UUID uuid, String username, Map<String, String> kitTiers, String overallTier,
                       String topKitSlug, long fetchedAt, boolean found) {
        this.uuid = uuid;
        this.username = username;
        this.kitTiers = kitTiers == null ? Map.of() : Map.copyOf(kitTiers);
        this.overallTier = overallTier;
        this.topKitSlug = topKitSlug;
        this.fetchedAt = fetchedAt;
        this.found = found;
    }

    public UUID uuid() {
        return uuid;
    }

    public String username() {
        return username;
    }

    public Map<String, String> kitTiers() {
        return kitTiers;
    }

    public String overallTierCode() {
        return overallTier;
    }

    public Optional<Tier> overallTier() {
        return Tier.parse(overallTier);
    }

    public String topKitSlug() {
        return topKitSlug;
    }

    public long fetchedAt() {
        return fetchedAt;
    }

    public boolean found() {
        return found;
    }

    public Optional<Tier> tierFor(String kitSlug) {
        String code = kitTiers.get(kitSlug);
        return Tier.parse(code);
    }

    public Optional<String> tierCodeFor(String kitSlug) {
        String code = kitTiers.get(kitSlug);
        return code == null ? Optional.empty() : Optional.of(code);
    }

    public Optional<Map.Entry<String, Tier>> bestVerifiedTier() {
        Map.Entry<String, Tier> best = null;
        for (Map.Entry<String, String> entry : kitTiers.entrySet()) {
            Optional<Tier> tier = Tier.parse(entry.getValue());
            if (tier.isPresent() && (best == null || tier.get().isBetterThan(best.getValue()))) {
                best = Map.entry(entry.getKey(), tier.get());
            }
        }
        return Optional.ofNullable(best);
    }

    public Map<String, Object> describe() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("username", username);
        out.put("overall", overallTier == null ? "unranked" : overallTier);
        kitTiers.forEach((slug, code) -> out.put(Kit.displayNameFor(slug), code));
        return out;
    }

    public static PlayerTiers notFound(String username, long now) {
        return new PlayerTiers(null, username, Map.of(), null, null, now, false);
    }
}
