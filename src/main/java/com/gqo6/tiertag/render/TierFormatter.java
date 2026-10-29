package com.gqo6.tiertag.render;

import com.gqo6.tiertag.TierTagMod;
import com.gqo6.tiertag.config.TierConfig;
import com.gqo6.tiertag.data.Kit;
import com.gqo6.tiertag.data.PlayerTiers;
import com.gqo6.tiertag.data.Tier;
import net.minecraft.text.MutableText;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;

public final class TierFormatter {
    private TierFormatter() {
    }

    /**
     * Dedicated icon font in the mod's own namespace. Unlike the injected
     * properties of the shared "default" font, this font cannot be shadowed by
     * stale jars, other mods, or server resource packs - which is what caused
     * the recurring "white dot" for the End Game/Crystal glyph.
     */
    private static final StyleSpriteSource ICON_FONT = buildIconFont();

    private static StyleSpriteSource buildIconFont() {
        try {
            return new StyleSpriteSource.Font(Identifier.of("tiertag", "tier_icons"));
        } catch (Throwable legacyLinkage) {
            return null;
        }
    }

    private static MutableText styledGlyph(char glyph) {
        MutableText t = Text.literal(String.valueOf(glyph));
        if (ICON_FONT == null) {
            return t;
        }
        try {
            return t.styled(style -> style.withFont(ICON_FONT));
        } catch (Throwable legacyLinkage) {
            return t;
        }
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
            // Neutral parent: the glyph child carries the icon font while the
            // tier text keeps the DEFAULT font (children inherit a parent's
            // unset style fields, so styling the parent would tofu the text).
            prefix = Text.empty().append(styledGlyph(glyph)).append(tierText);
        } else {
            prefix = tierText;
        }

        String separator = config.tagSeparator == null ? "" : config.tagSeparator.trim();
        if (!separator.isEmpty()) {
            prefix.append(Text.literal(" " + separator + " ").formatted(Formatting.GRAY));
        } else {
            prefix.append(Text.literal(" "));
        }
        return prefix.append(config.plainNames
                ? TierTextSanitizer.sanitize(original)
                : original);
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
