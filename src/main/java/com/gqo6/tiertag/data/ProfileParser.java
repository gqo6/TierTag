package com.gqo6.tiertag.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProfileParser {
    private static final Pattern KIT_BUTTON = Pattern.compile(
            "<button[^>]*class=\"[^\"]*ranking-kit[^\"]*\"[^>]*>(.*?)</button>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern KIT_SLUG = Pattern.compile(
            "data-kit=\"([A-Za-z0-9_-]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern KIT_TIER = Pattern.compile(
            "data-tier-code=\"([A-Za-z0-9]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern OVERALL_TIER = Pattern.compile(
            "<span[^>]*class=\"[^\"]*overall-tier[^\"]*\"[^>]*data-tier-code=\"([A-Za-z0-9]*)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern PROFILE_NAME = Pattern.compile(
            "<title>\\s*MCPVP \\|\\s*([^<|]+?)\\s*(?:\\||</title>)", Pattern.CASE_INSENSITIVE);

    private ProfileParser() {
    }

    public static PlayerTiers parse(String html, String requestedUsername, long now) {
        if (html == null || html.isBlank()) {
            return PlayerTiers.notFound(requestedUsername, now);
        }

        Map<String, String> kitTiers = new LinkedHashMap<>();
        String topKitSlug = null;

        Matcher buttonMatcher = KIT_BUTTON.matcher(html);
        while (buttonMatcher.find()) {
            String buttonHtml = buttonMatcher.group(0);
            Matcher slugMatcher = KIT_SLUG.matcher(buttonHtml);
            if (!slugMatcher.find()) {
                continue;
            }
            String slug = slugMatcher.group(1).toLowerCase();
            if (kitTiers.containsKey(slug)) {
                continue;
            }
            Matcher tierMatcher = KIT_TIER.matcher(buttonHtml);
            if (!tierMatcher.find()) {
                continue;
            }
            String code = tierMatcher.group(1).trim().toUpperCase();
            if (Tier.parse(code).isPresent()) {
                if (topKitSlug == null) {
                    topKitSlug = slug;
                }
                kitTiers.put(slug, code);
            }
        }

        String overall = null;
        Matcher overallMatcher = OVERALL_TIER.matcher(html);
        if (overallMatcher.find()) {
            String code = overallMatcher.group(1).trim().toUpperCase();
            if (Tier.parse(code).isPresent()) {
                overall = code;
            }
        }

        String resolvedName = requestedUsername;
        Matcher nameMatcher = PROFILE_NAME.matcher(html);
        if (nameMatcher.find()) {
            String fromTitle = nameMatcher.group(1).trim();
            if (!fromTitle.isBlank()) {
                resolvedName = fromTitle;
            }
        }

        boolean found = overall != null || !kitTiers.isEmpty();
        if (!found) {
            return PlayerTiers.notFound(requestedUsername, now);
        }
        return new PlayerTiers(null, resolvedName, kitTiers, overall, topKitSlug, now, true);
    }
}
