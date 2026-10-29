package com.gqo6.tiertag.data;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileParserTest {
    private static String fixture(String name) {
        try {
            return Files.readString(Path.of("src/test/resources/" + name));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void parsesSingleKitProfile() {
        PlayerTiers tiers = ProfileParser.parse(fixture("profile-gqo6.html"), "gqo6", 1000L);
        assertTrue(tiers.found());
        assertEquals("gqo6", tiers.username());
        assertTrue(tiers.overallTier().isPresent());
        assertEquals(Tier.LT6, tiers.overallTier().get());
        assertTrue(tiers.tierCodeFor("mace").isPresent());
        assertEquals("LT6", tiers.tierCodeFor("mace").get());
        assertEquals("mace", tiers.topKitSlug());
        assertTrue(tiers.tierCodeFor("sword").isEmpty());
        assertTrue(tiers.tierFor("crystal").isEmpty());
    }

    @Test
    void parsesMultiKitProfileAndSkipsUnverifiedTiers() {
        PlayerTiers tiers = ProfileParser.parse(fixture("profile-JokingDK.html"), "JokingDK", 1000L);
        assertTrue(tiers.found());
        assertTrue(tiers.overallTier().isPresent());
        assertEquals(Tier.HT2, tiers.overallTier().get());
        assertTrue(tiers.tierFor("smp").isPresent());
        assertEquals(Tier.HT1, tiers.tierFor("smp").get());
        assertTrue(tiers.tierFor("shield").isPresent());
        assertEquals(Tier.LT1, tiers.tierFor("shield").get());
        assertTrue(tiers.tierFor("mace").isEmpty());
        assertTrue(tiers.tierCodeFor("cart").isEmpty());
        assertTrue(tiers.bestVerifiedTier().isPresent());
        assertEquals(Tier.HT1, tiers.bestVerifiedTier().get().getValue());
    }

    @Test
    void returnsNotFoundForEmptyOrForeignHtml() {
        assertFalse(ProfileParser.parse("", "someone", 1L).found());
        assertFalse(ProfileParser.parse("<html><body>Login page</body></html>", "someone", 1L).found());
        assertFalse(ProfileParser.parse(null, "someone", 1L).found());
    }

    @Test
    void ignoresInvalidTierCodes() {
        String html = "<html><body>"
                + "<button class=\"ranking-kit\" data-kit=\"mace\" data-label=\"Mace\">"
                + "<span class=\"kit-rank\" data-tier-code=\"???\">???</span></button>"
                + "<span class=\"overall-tier\" data-tier-code=\"HT2\">HT2</span>"
                + "</body></html>";
        PlayerTiers tiers = ProfileParser.parse(html, "x", 1L);
        assertTrue(tiers.found());
        assertTrue(tiers.tierCodeFor("mace").isEmpty());
        assertEquals(Tier.HT2, tiers.overallTier().get());
    }

    @Test
    void tierGrammarAndOrdering() {
        assertTrue(Tier.parse("ht1").isPresent());
        assertTrue(Tier.parse(" ST6 ").isPresent());
        assertTrue(Tier.parse("HT7").isEmpty());
        assertTrue(Tier.parse("UNRANKED").isEmpty());
        assertTrue(Tier.parse("???").isEmpty());
        assertTrue(Tier.parse(null).isEmpty());
        assertTrue(Tier.HT1.isBetterThan(Tier.ST6));
        assertTrue(Tier.LT6.isBetterThan(null));
        assertFalse(Tier.ST6.isBetterThan(Tier.LT1));
        assertEquals(400.0, Tier.HT1.minPoints(), 1e-9);
        assertEquals(0.125, Tier.LT6.minPoints(), 1e-9);
    }
}
