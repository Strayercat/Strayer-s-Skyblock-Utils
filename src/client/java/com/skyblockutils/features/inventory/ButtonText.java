package com.skyblockutils.features.inventory;

import com.skyblockutils.utils.ModStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;
import java.util.Locale;

public final class ButtonText {
    public record Token(String code, String name, int rgb) {
    }

    public static final String GRADIENT = "title";
    private static final String LEGACY = "0123456789abcdef";
    private static final int[] RGB = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    private ButtonText() {
    }

    public static List<Token> ssuTokens() {
        return List.of(
                new Token("main", "SSU Main", ModStyle.main() & 0xFFFFFF),
                new Token("text", "SSU Text", ModStyle.text() & 0xFFFFFF),
                new Token("start", "SSU Title Start", ModStyle.titleStart() & 0xFFFFFF),
                new Token("end", "SSU Title End", ModStyle.titleEnd() & 0xFFFFFF),
                new Token(GRADIENT, "SSU Title Gradient", ModStyle.titleStart() & 0xFFFFFF));
    }

    private static Integer tokenColor(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "main" -> ModStyle.main() & 0xFFFFFF;
            case "text" -> ModStyle.text() & 0xFFFFFF;
            case "start" -> ModStyle.titleStart() & 0xFFFFFF;
            case "end" -> ModStyle.titleEnd() & 0xFFFFFF;
            default -> null;
        };
    }

    public static Component parse(String raw) {
        MutableComponent out = Component.empty();
        if (raw == null || raw.isEmpty()) return out;

        Style style = Style.EMPTY;
        boolean gradient = false;
        StringBuilder buffer = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < raw.length()) {
                char next = raw.charAt(i + 1);
                if (next == '#' && i + 8 <= raw.length() && raw.substring(i + 2, i + 8).matches("[0-9a-fA-F]{6}")) {
                    flush(out, buffer, style, gradient);
                    style = Style.EMPTY.withColor(TextColor.fromRgb(Integer.parseInt(raw.substring(i + 2, i + 8), 16)));
                    gradient = false;
                    i += 8;
                    continue;
                }
                if (next == '{') {
                    int close = raw.indexOf('}', i + 2);
                    if (close > 0) {
                        String token = raw.substring(i + 2, close);
                        Integer color = tokenColor(token);
                        if (color != null || token.equalsIgnoreCase(GRADIENT)) {
                            flush(out, buffer, style, gradient);
                            gradient = color == null;
                            style = gradient ? Style.EMPTY : Style.EMPTY.withColor(TextColor.fromRgb(color));
                            i = close + 1;
                            continue;
                        }
                    }
                }
                char code = Character.toLowerCase(next);
                int colorIndex = LEGACY.indexOf(code);
                if (colorIndex >= 0 || "klmnor".indexOf(code) >= 0) {
                    flush(out, buffer, style, gradient);
                    if (colorIndex >= 0) {
                        style = Style.EMPTY.withColor(TextColor.fromRgb(RGB[colorIndex]));
                        gradient = false;
                    } else {
                        style = switch (code) {
                            case 'k' -> style.withObfuscated(true);
                            case 'l' -> style.withBold(true);
                            case 'm' -> style.withStrikethrough(true);
                            case 'n' -> style.withUnderlined(true);
                            case 'o' -> style.withItalic(true);
                            default -> {
                                gradient = false;
                                yield Style.EMPTY;
                            }
                        };
                    }
                    i += 2;
                    continue;
                }
            }
            buffer.append(c);
            i++;
        }
        flush(out, buffer, style, gradient);
        return out;
    }

    private static void flush(MutableComponent out, StringBuilder buffer, Style style, boolean gradient) {
        if (buffer.isEmpty()) return;
        String text = buffer.toString();
        buffer.setLength(0);
        if (!gradient) {
            out.append(Component.literal(text).withStyle(style));
            return;
        }
        int start = ModStyle.titleStart() & 0xFFFFFF;
        int end = ModStyle.titleEnd() & 0xFFFFFF;
        int[] points = text.codePoints().toArray();
        for (int i = 0; i < points.length; i++) {
            float t = points.length == 1 ? 0 : (float) i / (points.length - 1);
            out.append(Component.literal(Character.toString(points[i])).withStyle(style.withColor(TextColor.fromRgb(lerp(start, end, t)))));
        }
    }

    private static int lerp(int a, int b, float t) {
        int r = Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return r << 16 | g << 8 | bl;
    }
}
