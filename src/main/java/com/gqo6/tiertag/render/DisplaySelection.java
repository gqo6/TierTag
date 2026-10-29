package com.gqo6.tiertag.render;

import com.gqo6.tiertag.config.TierConfig;
import com.gqo6.tiertag.data.PlayerTiers;
import com.gqo6.tiertag.data.Tier;

import java.util.Map;
import java.util.Optional;

public record DisplaySelection(String kitSlug, Tier tier, String tierCode) {
    public static DisplaySelection select(PlayerTiers tiers, TierConfig config) {
        if ("HIGHEST".equalsIgnoreCase(config.displayMode)) {
            Optional<Map.Entry<String, Tier>> best = tiers.bestVerifiedTier();
            if (best.isPresent()) {
                return new DisplaySelection(best.get().getKey(), best.get().getValue(), best.get().getValue().name());
            }
            Optional<Tier> overall = tiers.overallTier();
            if (overall.isPresent()) {
                String slug = tiers.topKitSlug() != null
                        ? tiers.topKitSlug()
                        : tiers.bestVerifiedTier().map(Map.Entry::getKey).orElse(null);
                return new DisplaySelection(slug, overall.get(), overall.get().name());
            }
            return null;
        }
        String slug = config.displayMode == null ? "" : config.displayMode.toLowerCase();
        Optional<String> code = tiers.tierCodeFor(slug);
        if (code.isEmpty()) {
            return null;
        }
        Optional<Tier> tier = Tier.parse(code.get());
        return tier.map(value -> new DisplaySelection(slug, value, value.name())).orElse(null);
    }
}
