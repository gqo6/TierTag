package com.gqo6.tiertag.render;

import com.gqo6.tiertag.TierTagMod;
import com.gqo6.tiertag.config.TierConfig;
import com.gqo6.tiertag.data.Kit;
import com.gqo6.tiertag.data.PlayerTiers;
import com.gqo6.tiertag.data.Tier;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.UUID;

public final class TierFormatter {
    private TierFormatter() {
    }

    public static Text appendTier(UUID uuid, String username, Text original) {
        TierConfig config = TierTagMod.config();
        if (config == null || !config.enabled || original == null) {
            return original;
        }
        TierService service = TierTagMod.service();
        if (service == null) {
            return original;
        }
        PlayerTiers tiers = config.debugTestTag
                ? debugTiers(uuid, username)
                : service.getOrRequest(uuid, username);
        if (tiers == null) {
            return original;
        }
        DisplaySelection selection = DisplaySelection.select(tiers, config);
        if (selection == null) {
            return original;
        }

        MutableText tierText = Text.literal(selection.tierCode())
                .styled(style -> style.withColor(TextColor.fromRgb(config.colorFor(selection.tier()))));
        MutableText prefix;
        if (config.showIcons && selection.kitSlug() != null) {
            char glyph = Kit.glyphFor(selection.kitSlug());
            prefix = Text.literal(String.valueOf(glyph)).append(tierText);
        } else {
            prefix = tierText;
        }

        String separator = config.tagSeparator == null ? "" : config.tagSeparator.trim();
        if (!separator.isEmpty()) {
            prefix.append(Text.literal(" " + separator + " ").formatted(Formatting.GRAY));
        } else {
            prefix.append(Text.literal(" "));
        }
        return prefix.append(original);
    }

    private static PlayerTiers debugTiers(UUID uuid, String username) {
        String name = username == null ? "Dev" : username;
        TierConfig config = TierTagMod.config();
        String kit = (config != null && config.debugTestKit != null && !config.debugTestKit.isBlank())
                ? config.debugTestKit.trim() : "mace";
        return new PlayerTiers(uuid, name,
                Map.of(kit, Tier.HT1.name()),
                Tier.HT1.name(), kit, System.currentTimeMillis(), true);
    }
}
