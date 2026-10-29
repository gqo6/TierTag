package com.gqo6.tiertag.data;

import java.util.Optional;

public enum Kit {
    SWORD("sword", "Sword", '\uEB02'),
    MACE("mace", "Mace", '\uEB01'),
    SPEAR("spear", "Spear", '\uEB09'),
    SHIELD("shield", "Shield", '\uEB0A'),
    POT("pot", "Pot", '\uEB04'),
    EARLY_GAME("early-game", "Early Game", '\uEB0C'),
    END_GAME("end-game", "End Game", '\uEB0F'),
    CRYSTAL("crystal", "Crystal", '\uEB0F'),
    LATE_GAME("late-game", "Late Game", '\uEB07'),
    DIAMOND_SMP("diamond-smp", "Diamond SMP", '\uEB06'),
    NETHERITE_POT("netherite-pot", "Netherite Pot", '\uEB05'),
    CREEPER("creeper", "Creeper", '\uEB0D'),
    CART("cart", "Cart", '\uEB0E'),
    SMP("smp", "SMP", '\uEB06'),
    BOW("bow", "Bow", '\uEB0B'),
    UHC("uhc", "UHC", '\uEB08');

    private final String slug;
    private final String displayName;
    private final char glyph;

    Kit(String slug, String displayName, char glyph) {
        this.slug = slug;
        this.displayName = displayName;
        this.glyph = glyph;
    }

    public String slug() {
        return slug;
    }

    public String displayName() {
        return displayName;
    }

    public char glyph() {
        return glyph;
    }

    public static Optional<Kit> fromSlug(String slug) {
        if (slug == null) {
            return Optional.empty();
        }
        for (Kit kit : values()) {
            if (kit.slug.equalsIgnoreCase(slug.trim())) {
                return Optional.of(kit);
            }
        }
        return Optional.empty();
    }

    public static char glyphFor(String slug) {
        return fromSlug(slug).map(Kit::glyph).orElse('•');
    }

    public static String displayNameFor(String slug) {
        return fromSlug(slug).map(Kit::displayName).orElseGet(() -> prettySlug(slug));
    }

    private static String prettySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return "Unknown";
        }
        String[] parts = slug.trim().toLowerCase().split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.isEmpty() ? "Unknown" : sb.toString();
    }
}
