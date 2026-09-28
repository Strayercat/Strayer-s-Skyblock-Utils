package com.skyblockutils.features.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ChatLayout {
    private static final String SEPARATOR_CHARS = "-=▬━─_";
    private static final int MIN_SEPARATOR_LENGTH = 5;

    private ChatLayout() {}

    enum LineType { NORMAL, CENTERED, SEPARATOR }

    record Segment(Style style, String text) {}

    record ParsedLine(LineType type, Component component, Style style, String codes, char separatorChar) {}

    record ParsedMessage(List<ParsedLine> lines) {
        boolean hasLayoutLines() {
            for (ParsedLine line : lines) {
                if (line.type() != LineType.NORMAL) return true;
            }
            return false;
        }
    }

    static float chatScale() {
        return Minecraft.getInstance().options.chatScale().get().floatValue();
    }

    static int wrapWidth() {
        float scale = chatScale();
        if (scale <= 0) return 1;
        int chatWidth = ChatComponent.getWidth(Minecraft.getInstance().options.chatWidth().get());
        return Math.max(1, Mth.floor(chatWidth / scale));
    }

    static ParsedMessage parse(Component message) {
        List<List<Segment>> rawLines = new ArrayList<>();
        rawLines.add(new ArrayList<>());

        message.visit((style, text) -> {
            String[] parts = text.split("\n", -1);
            String codes = "";
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) rawLines.add(new ArrayList<>());
                String part = (i > 0 ? codes : "") + parts[i];
                codes = activeCodes(codes, parts[i]);
                if (!part.isEmpty()) rawLines.getLast().add(new Segment(style, part));
            }
            return Optional.<Void>empty();
        }, Style.EMPTY);

        List<ParsedLine> lines = new ArrayList<>();
        for (List<Segment> segments : rawLines) lines.add(classify(segments));
        return new ParsedMessage(lines);
    }

    static int contentWidth(ParsedMessage message, int wrap, Font font) {
        int width = 0;
        for (ParsedLine line : message.lines()) {
            if (line.type() == LineType.SEPARATOR) continue;
            for (FormattedCharSequence seq : font.split(line.component(), wrap)) {
                width = Math.max(width, font.width(seq));
            }
        }
        return width;
    }

    static Component separator(ParsedLine line, int width, Font font) {
        String unit = String.valueOf(line.separatorChar());
        int unitWidth = font.width(Component.literal(line.codes() + unit).setStyle(line.style()));
        if (unitWidth <= 0) unitWidth = 4;
        int count = Math.max(1, width / unitWidth);
        return Component.literal(line.codes() + unit.repeat(count)).setStyle(line.style());
    }

    static Component toChatComponent(ParsedMessage message, int width, Font font) {
        MutableComponent out = Component.empty();
        int spaceWidth = Math.max(1, font.width(" "));
        List<ParsedLine> lines = message.lines();

        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) out.append("\n");
            ParsedLine line = lines.get(i);

            switch (line.type()) {
                case SEPARATOR -> out.append(separator(line, width, font));
                case CENTERED -> {
                    int pad = Math.max(0, (width - font.width(line.component())) / 2);
                    int spaces = pad / spaceWidth;
                    if (spaces > 0) out.append(" ".repeat(spaces));
                    out.append(line.component());
                }
                case NORMAL -> out.append(line.component());
            }
        }

        return out;
    }

    private static ParsedLine classify(List<Segment> segments) {
        StringBuilder raw = new StringBuilder();
        for (Segment segment : segments) raw.append(segment.text());
        String plain = raw.toString().replaceAll("§.", "");
        String trimmed = plain.trim();

        Segment first = null;
        for (Segment segment : segments) {
            if (!segment.text().replaceAll("§.", "").isEmpty()) {
                first = segment;
                break;
            }
        }

        if (first != null && isSeparator(plain, trimmed, segments, raw.toString())) {
            char c = trimmed.isEmpty() ? ' ' : trimmed.charAt(0);
            return new ParsedLine(LineType.SEPARATOR, null, first.style(), activeCodes("", first.text()), c);
        }

        if (plain.startsWith("  ") && !trimmed.isEmpty()) {
            return new ParsedLine(LineType.CENTERED, rebuild(segments, true), null, null, ' ');
        }

        return new ParsedLine(LineType.NORMAL, rebuild(segments, false), null, null, ' ');
    }

    private static boolean isSeparator(String plain, String trimmed, List<Segment> segments, String raw) {
        if (trimmed.length() >= MIN_SEPARATOR_LENGTH && SEPARATOR_CHARS.indexOf(trimmed.charAt(0)) >= 0) {
            char c = trimmed.charAt(0);
            for (int i = 0; i < trimmed.length(); i++) {
                if (trimmed.charAt(i) != c) return false;
            }
            return true;
        }

        if (trimmed.isEmpty() && plain.length() >= MIN_SEPARATOR_LENGTH) {
            if (raw.contains("§m")) return true;
            for (Segment segment : segments) {
                if (segment.style().isStrikethrough()) return true;
            }
        }

        return false;
    }

    private static String activeCodes(String current, String text) {
        StringBuilder codes = new StringBuilder(current);
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) != '§') continue;
            char code = Character.toLowerCase(text.charAt(i + 1));
            if ("0123456789abcdefr".indexOf(code) >= 0) codes.setLength(0);
            codes.append('§').append(code);
            i++;
        }
        return codes.toString();
    }

    private static Component rebuild(List<Segment> segments, boolean trim) {
        List<Segment> result = new ArrayList<>(segments);

        if (trim) {
            for (int i = 0; i < result.size(); i++) {
                Segment segment = result.get(i);
                String stripped = stripLeadingSpaces(segment.text());
                result.set(i, new Segment(segment.style(), stripped));
                if (!stripped.replaceAll("§.", "").isEmpty()) break;
            }

            for (int i = result.size() - 1; i >= 0; i--) {
                Segment segment = result.get(i);
                String stripped = stripTrailingSpaces(segment.text());
                result.set(i, new Segment(segment.style(), stripped));
                if (!stripped.replaceAll("§.", "").isEmpty()) break;
            }
        }

        MutableComponent component = Component.empty();
        for (Segment segment : result) {
            if (!segment.text().isEmpty()) component.append(Component.literal(segment.text()).setStyle(segment.style()));
        }
        return component;
    }

    private static String stripLeadingSpaces(String text) {
        StringBuilder codes = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                codes.append(c).append(text.charAt(i + 1));
                i += 2;
            } else if (c == ' ') {
                i++;
            } else {
                break;
            }
        }
        return codes + text.substring(i);
    }

    private static String stripTrailingSpaces(String text) {
        int end = text.length();
        while (end > 0) {
            if (text.charAt(end - 1) == ' ') {
                end--;
            } else if (end >= 2 && text.charAt(end - 2) == '§') {
                end -= 2;
            } else {
                break;
            }
        }
        return text.substring(0, end);
    }
}