package com.skyblockutils.features.chat.emoji;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmojiRegistry {
    public record Emoji(String name, List<String> aliases, int codepoint) {
        public List<String> names() {
            List<String> names = new ArrayList<>(aliases.size() + 1);
            names.add(name);
            names.addAll(aliases);
            return names;
        }

        public String text() {
            return Character.toString(codepoint);
        }

        public String shortcode() {
            return ":" + name + ":";
        }
    }

    private static final String INDEX_PATH = "/assets/skyblockutils/emojis/index.json";
    private static final Pattern SHORTCODE = Pattern.compile(":([a-z0-9_+\\-]+):");
    private static final Pattern NAME = Pattern.compile("[a-z0-9_+\\-]+");
    private static final int GLYPH_MIN = 0xE800;
    private static final int GLYPH_MAX = 0xF8FF;

    private static List<Emoji> custom = List.of();
    private static List<Emoji> normal = List.of();
    private static final Map<String, Emoji> byName = new HashMap<>();
    private static final Map<Integer, Emoji> byCodepoint = new HashMap<>();
    private static boolean loaded = false;

    private EmojiRegistry() {}

    public static List<Emoji> custom() {
        load();
        return custom;
    }

    public static List<Emoji> normal() {
        load();
        return normal;
    }

    public static String toShortcodes(String text) {
        return toShortcodes(text, Integer.MAX_VALUE);
    }

    public static String toShortcodes(String text, int maxLength) {
        if (hasGlyph(text)) return text.length() > maxLength ? trimCodepoints(text, maxLength) : text;
        load();
        StringBuilder out = new StringBuilder(text.length() + 16);
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            Emoji emoji = byCodepoint.get(cp);
            String piece = emoji != null ? emoji.shortcode() : Character.toString(cp);
            if (out.length() + piece.length() > maxLength) break;
            out.append(piece);
            i += Character.charCount(cp);
        }
        return out.toString();
    }

    private static String trimCodepoints(String text, int maxLength) {
        int end = maxLength;
        if (end > 0 && Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return text.substring(0, end);
    }

    public static int extraLength(String text) {
        if (hasGlyph(text)) return 0;
        load();
        int extra = 0;
        for (int i = 0; i < text.length(); i++) {
            Emoji emoji = byCodepoint.get((int) text.charAt(i));
            if (emoji != null) extra += emoji.name().length() + 1;
        }
        return extra;
    }

    public static Emoji shortcodeEndingAt(String text, int end) {
        if (end < 3 || end > text.length() || text.charAt(end - 1) != ':') return null;
        int start = text.lastIndexOf(':', end - 2);
        if (start < 0) return null;
        String name = text.substring(start + 1, end - 1);
        if (!NAME.matcher(name).matches()) return null;
        load();
        return byName.get(name);
    }

    public static Component withEmojis(Component message) {
        if (message.getString().indexOf(':') < 0) return message;
        load();

        MutableComponent out = Component.empty();
        boolean[] changed = {false};

        message.visit((style, text) -> {
            Matcher matcher = SHORTCODE.matcher(text);
            int last = 0;
            int searchFrom = 0;

            while (searchFrom < text.length() && matcher.find(searchFrom)) {
                Emoji emoji = byName.get(matcher.group(1));
                if (emoji == null) {
                    searchFrom = matcher.end() - 1;
                    continue;
                }
                if (matcher.start() > last) out.append(Component.literal(text.substring(last, matcher.start())).setStyle(style));
                out.append(Component.literal(emoji.text()).setStyle(emojiStyle(style)));
                last = matcher.end();
                searchFrom = last;
                changed[0] = true;
            }

            if (last < text.length()) out.append(Component.literal(text.substring(last)).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);

        return changed[0] ? out : message;
    }

    public static FormattedCharSequence formatInput(String text) {
        if (hasGlyph(text)) return null;
        load();
        Style emoji = emojiStyle(Style.EMPTY);
        return sink -> {
            int i = 0;
            while (i < text.length()) {
                int cp = text.codePointAt(i);
                if (!sink.accept(i, byCodepoint.containsKey(cp) ? emoji : Style.EMPTY, cp)) return false;
                i += Character.charCount(cp);
            }
            return true;
        };
    }

    private static Style emojiStyle(Style style) {
        return style.withColor(0xFFFFFF)
                .withBold(false)
                .withItalic(false)
                .withUnderlined(false)
                .withStrikethrough(false)
                .withObfuscated(false)
                .withShadowColor(0);
    }

    private static boolean hasGlyph(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= GLYPH_MIN && c <= GLYPH_MAX) return false;
        }
        return true;
    }

    private static void load() {
        if (loaded) return;
        loaded = true;

        try (InputStream in = EmojiRegistry.class.getResourceAsStream(INDEX_PATH)) {
            if (in == null) return;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            normal = parse(root.getAsJsonArray("normal"));
            custom = parse(root.getAsJsonArray("custom"));
        } catch (IOException | RuntimeException e) {
            System.err.println("[SSU] Failed to load emoji index: " + e.getMessage());
        }
    }

    private static List<Emoji> parse(JsonArray array) {
        List<Emoji> list = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            List<String> aliases = new ArrayList<>();
            if (obj.has("aliases")) obj.getAsJsonArray("aliases").forEach(alias -> aliases.add(alias.getAsString()));

            Emoji emoji = new Emoji(obj.get("name").getAsString(), List.copyOf(aliases), obj.get("cp").getAsInt());
            list.add(emoji);
            byName.put(emoji.name(), emoji);
            for (String alias : emoji.aliases()) byName.putIfAbsent(alias, emoji);
            byCodepoint.put(emoji.codepoint(), emoji);
        }
        return List.copyOf(list);
    }
}
