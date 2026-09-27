package com.skyblockutils.features.chat;

import com.skyblockutils.ModKeyBindings;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class SeparatedChat {
    private static final List<SystemChat> systemChats = new ArrayList<>();
    private static final List<ParsedMessage> history = new ArrayList<>();
    private static final int MESSAGE_TIME_TICK = 100;
    private static final int MAX_HISTORY = 220;
    private static final Pattern USER_SENT_MESSAGE_PATTERN = Pattern.compile("^\\[\\d{1,4}] .+?: .*$");
    private static final Pattern USER_SHOW_PATTERN = Pattern.compile(".+? (?:is holding|is wearing|is friends with a|has) \\[.+]$");
    private static final Pattern LEVEL_PREFIX_PATTERN = Pattern.compile("^\\[\\d{1,4}] .+");
    private static final List<String> USER_CHANNEL_PREFIXES = List.of("Party > ", "Guild > ", "Officer > ", "Co-op > ", "From ", "To ");
    private static final String SEPARATOR_CHARS = "-=▬━─_";
    private static final int MIN_SEPARATOR_LENGTH = 5;
    private static final int TOP_MARGIN = 0;
    private static final int PADDING = 1;
    private static final int LINE_HEIGHT = 9;
    private static final int HISTORY_VISIBLE_LINES = 10;
    private static final int SCROLL_STEP = 3;
    private static final int SCROLLBAR_WIDTH = 2;

    private static int scrollOffset = 0;

    private enum LineType { NORMAL, CENTERED, SEPARATOR }

    private record Segment(Style style, String text) {}

    private record ParsedLine(LineType type, Component component, Style style, String codes, char separatorChar) {}

    private record ParsedMessage(List<ParsedLine> lines) {}

    private record RenderLine(FormattedCharSequence text, boolean centered) {}

    private static class SystemChat {
        private final ParsedMessage message;
        private int tickTime = 0;

        SystemChat(ParsedMessage message) {
            this.message = message;
        }

        void tick() {
            tickTime++;
        }

        boolean expired() {
            return tickTime > MESSAGE_TIME_TICK;
        }
    }

    public static boolean handleMessage(Component message, boolean overlay) {
        if (overlay) return true;
        if (!ModConfig.INSTANCE.separateMessage) return true;
        String cleanMessage = message.getString().replaceAll("§.", "").trim();
        if (isUserMessage(cleanMessage)) return true;
        if (cleanMessage.matches("You are now in the .* channel") || cleanMessage.matches("Friend > .* (left|joined)\\.")) return true;

        ParsedMessage parsed = parse(message);
        systemChats.add(new SystemChat(parsed));
        history.add(parsed);
        if (history.size() > MAX_HISTORY) history.removeFirst();
        if (isHistoryOpen() && scrollOffset > 0) {
            int wrap = wrapWidth();
            scrollOffset += buildLines(parsed, wrap, wrap, Minecraft.getInstance().font).size();
        }

        return false;
    }

    private static boolean isUserMessage(String clean) {
        if (USER_SENT_MESSAGE_PATTERN.matcher(clean).matches()) return true;
        if (LEVEL_PREFIX_PATTERN.matcher(clean).matches() && USER_SHOW_PATTERN.matcher(clean).matches()) return true;

        for (String prefix : USER_CHANNEL_PREFIXES) {
            if (!clean.startsWith(prefix)) continue;
            if (clean.contains(": ")) return true;
            if (USER_SHOW_PATTERN.matcher(clean.substring(prefix.length())).matches()) return true;
        }
        return false;
    }

    private static float chatScale() {
        return Minecraft.getInstance().options.chatScale().get().floatValue();
    }

    private static int wrapWidth() {
        float scale = chatScale();
        if (scale <= 0) return 1;
        int chatWidth = ChatComponent.getWidth(Minecraft.getInstance().options.chatWidth().get());
        return Math.max(1, Mth.floor(chatWidth / scale));
    }

    private static ParsedMessage parse(Component message) {
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

    private static int contentWidth(ParsedMessage message, int wrap, Font font) {
        int width = 0;
        for (ParsedLine line : message.lines()) {
            if (line.type() == LineType.SEPARATOR) continue;
            for (FormattedCharSequence seq : font.split(line.component(), wrap)) {
                width = Math.max(width, font.width(seq));
            }
        }
        return width;
    }

    private static List<RenderLine> buildLines(ParsedMessage message, int width, int wrap, Font font) {
        List<RenderLine> lines = new ArrayList<>();
        for (ParsedLine line : message.lines()) {
            if (line.type() == LineType.SEPARATOR) {
                lines.add(new RenderLine(separator(line, width, font), false));
                continue;
            }

            List<FormattedCharSequence> wrapped = font.split(line.component(), wrap);
            if (wrapped.isEmpty()) {
                lines.add(new RenderLine(FormattedCharSequence.EMPTY, false));
                continue;
            }

            boolean centered = line.type() == LineType.CENTERED;
            for (FormattedCharSequence seq : wrapped) lines.add(new RenderLine(seq, centered));
        }
        return lines;
    }

    private static FormattedCharSequence separator(ParsedLine line, int width, Font font) {
        String unit = String.valueOf(line.separatorChar());
        int unitWidth = font.width(Component.literal(line.codes() + unit).setStyle(line.style()));
        if (unitWidth <= 0) unitWidth = 4;
        int count = Math.max(1, width / unitWidth);
        return Component.literal(line.codes() + unit.repeat(count)).setStyle(line.style()).getVisualOrderText();
    }

    public static void tickAllMessages() {
        if (!ModConfig.INSTANCE.separateMessage) {
            systemChats.clear();
            return;
        }

        Iterator<SystemChat> it = systemChats.iterator();
        while (it.hasNext()) {
            SystemChat chat = it.next();
            chat.tick();
            if (chat.expired()) it.remove();
        }
    }

    private static boolean isHistoryOpen() {
        return StrayersSkyblockUtilsClient.isInSkyblock
                && ModConfig.INSTANCE.separateMessage
                && ModKeyBindings.SYSTEM_CHAT_HISTORY_KEY.isDown()
                && Minecraft.getInstance().gui.screen() == null;
    }

    public static boolean onScroll(double vertical) {
        if (!isHistoryOpen() || vertical == 0) return false;
        scrollOffset += (int) Math.signum(vertical) * SCROLL_STEP;
        if (scrollOffset < 0) scrollOffset = 0;
        return true;
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (!ModConfig.INSTANCE.separateMessage) return;

        float scale = chatScale();
        if (scale <= 0) return;

        if (isHistoryOpen()) {
            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);
            renderHistory(graphics, scale);
            graphics.pose().popMatrix();
            return;
        }

        scrollOffset = 0;

        if (systemChats.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int wrap = wrapWidth();

        int textWidth = 0;
        for (SystemChat chat : systemChats) textWidth = Math.max(textWidth, contentWidth(chat.message, wrap, font));
        if (textWidth == 0) textWidth = wrap;

        List<RenderLine> lines = new ArrayList<>();
        for (SystemChat chat : systemChats) lines.addAll(buildLines(chat.message, textWidth, wrap, font));
        if (lines.isEmpty()) return;

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);

        int right = Mth.ceil(graphics.guiWidth() / scale);
        int left = right - textWidth - PADDING * 2;
        int top = Mth.floor(TOP_MARGIN / scale);
        int bottom = top + lines.size() * LINE_HEIGHT + PADDING * 2;

        int backgroundColor = backgroundColor(mc);
        if ((backgroundColor >>> 24) > 0) graphics.fill(left, top, right, bottom, backgroundColor);

        int textColor = textColor(mc);
        int lineY = top + PADDING;
        for (RenderLine line : lines) {
            int x = left + PADDING;
            if (line.centered()) x += (textWidth - font.width(line.text())) / 2;
            graphics.text(font, line.text(), x, lineY, textColor, true);
            lineY += LINE_HEIGHT;
        }

        graphics.pose().popMatrix();
    }

    private static void renderHistory(GuiGraphicsExtractor graphics, float scale) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int wrap = wrapWidth();

        List<RenderLine> lines = new ArrayList<>();
        for (ParsedMessage message : history) lines.addAll(buildLines(message, wrap, wrap, font));

        int maxOffset = Math.max(0, lines.size() - HISTORY_VISIBLE_LINES);
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;

        int panelWidth = wrap + PADDING * 3 + SCROLLBAR_WIDTH;
        int panelHeight = HISTORY_VISIBLE_LINES * LINE_HEIGHT + PADDING * 2;
        int right = Mth.ceil(graphics.guiWidth() / scale);
        int left = right - panelWidth;
        int top = Mth.floor(TOP_MARGIN / scale);
        int bottom = top + panelHeight;

        graphics.fill(left, top, right, bottom, Math.max(backgroundColor(mc) >>> 24, 0x80) << 24);

        int textColor = textColor(mc);

        if (lines.isEmpty()) {
            graphics.text(font, "No system messages yet", left + PADDING, top + PADDING, 0xFFAAAAAA, true);
            return;
        }

        int end = lines.size() - scrollOffset;
        int start = Math.max(0, end - HISTORY_VISIBLE_LINES);
        int lineY = bottom - PADDING - (end - start) * LINE_HEIGHT;

        for (int i = start; i < end; i++) {
            RenderLine line = lines.get(i);
            int x = left + PADDING;
            if (line.centered()) x += (wrap - font.width(line.text())) / 2;
            graphics.text(font, line.text(), x, lineY, textColor, true);
            lineY += LINE_HEIGHT;
        }

        if (maxOffset > 0) {
            int trackTop = top + PADDING;
            int trackHeight = panelHeight - PADDING * 2;
            int thumbHeight = Math.max(8, trackHeight * HISTORY_VISIBLE_LINES / lines.size());
            int thumbTop = trackTop + (trackHeight - thumbHeight) * (maxOffset - scrollOffset) / maxOffset;
            int barRight = right - PADDING;
            int barLeft = barRight - SCROLLBAR_WIDTH;

            graphics.fill(barLeft, trackTop, barRight, trackTop + trackHeight, 0x40FFFFFF);
            graphics.fill(barLeft, thumbTop, barRight, thumbTop + thumbHeight, 0xC0FFFFFF);
        }
    }

    private static int textColor(Minecraft mc) {
        int alpha = (int) (255.0 * (mc.options.chatOpacity().get() * 0.9 + 0.1));
        return (alpha << 24) | 0xFFFFFF;
    }

    private static int backgroundColor(Minecraft mc) {
        int alpha = (int) (255.0 * mc.options.textBackgroundOpacity().get());
        return alpha << 24;
    }
}