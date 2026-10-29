package com.gqo6.tiertag.render;

import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Makes server-supplied player-name text readable. Some servers (and players)
 * send display names packed with decorative Unicode (mathematical bold/italic
 * letters like "{@literal 𝓕𝓻𝓮𝓪𝓷𝓴}"), private-use-area "sticker" characters, or a
 * custom entry {@code font} style. Those resolve to tiny colorful custom glyphs
 * inside resource-pack font providers and look like corrupted letters in the
 * Tab list. This rebuilds the name text as plain letters:
 *
 * <ul>
 *   <li>private-use (U+E000-U+F8FF) characters removed</li>
 *   <li>zero-width / direction marks and variation selectors removed</li>
 *   <li>fancy Unicode letters normalized to ASCII via NFKC</li>
 *   <li>the custom {@code font} style reference cleared (colors kept!)</li>
 * </ul>
 */
public final class TierTextSanitizer {
    private static final Pattern STRIPPED = Pattern.compile(
            "[\\uE000-\\uF8FF\\u200B-\\u200F\\u2060-\\u2064\\uFE00-\\uFE0F]");

    private TierTextSanitizer() {
    }

    /** @return a readable version of {@code text}, or {@code text} itself when clean. */
    public static Text sanitize(Text text) {
        if (text == null) {
            return null;
        }
        MutableText transformed = transform(text);
        return transformed == null ? text : transformed;
    }

    /** @return a sanitized clone, or {@code null} when this subtree needed no change. */
    private static MutableText transform(Text node) {
        // Fast path: nothing a resource pack could decorate on this subtree.
        if (node.getStyle() == Style.EMPTY
                && node.getContent() instanceof PlainTextContent.Literal lit
                && isPlainAscii(lit.string())
                && node.getSiblings().isEmpty()) {
            return null;
        }

        PlainTextContent.Literal literal = node.getContent() instanceof PlainTextContent.Literal l
                ? l : null;
        String cleanedString = literal == null ? null : clean(literal.string());
        Style own = node.getStyle();
        Style cleanedStyle = own.withFont(null);
        boolean ownChanged = (literal != null && !cleanedString.equals(literal.string()))
                || !cleanedStyle.equals(own);

        List<Text> siblings = node.getSiblings();
        java.util.List<MutableText> childResults = new java.util.ArrayList<>(siblings.size());
        boolean anyChildChanged = false;
        for (Text child : siblings) {
            MutableText transformedChild = transform(child);
            if (transformedChild != null) {
                anyChildChanged = true;
            }
            childResults.add(transformedChild);
        }

        if (!ownChanged && !anyChildChanged) {
            return null;
        }

        MutableText out = rebuild(node, cleanedStyle, cleanedString);
        for (int i = 0; i < siblings.size(); i++) {
            MutableText transformedChild = childResults.get(i);
            out.append(transformedChild != null ? transformedChild : siblings.get(i));
        }
        return out;
    }

    private static MutableText rebuild(Text node, Style cleanedStyle, String literalOverride) {
        TextContent content = node.getContent();
        if (literalOverride != null) {
            content = PlainTextContent.of(literalOverride);
        }
        return MutableText.of(content).setStyle(cleanedStyle);
    }

    private static boolean isPlainAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 0x7F || s.charAt(i) < 0x20) {
                return false;
            }
        }
        return true;
    }

    private static String clean(String s) {
        String stripped = STRIPPED.matcher(s).replaceAll("");
        return Normalizer.normalize(stripped, Normalizer.Form.NFKC);
    }
}
