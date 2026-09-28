package com.skyblockutils.features.chat;

import com.skyblockutils.ModKeyBindings;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class SeparatedChat {
    private static final List<SystemChat> systemChats = new ArrayList<>();
    private static final List<ChatLayout.ParsedMessage> history = new ArrayList<>();
    private static final int MESSAGE_TIME_TICK = 100;
    private static final int MAX_HISTORY = 200;
    private static final Pattern USER_SENT_MESSAGE_PATTERN = Pattern.compile("^\\[\\d{1,4}] .+?: .*$");
    private static final Pattern USER_SHOW_PATTERN = Pattern.compile(".+? (?:is holding|is wearing|is friends with a|has) \\[.+]$");
    private static final Pattern LEVEL_PREFIX_PATTERN = Pattern.compile("^\\[\\d{1,4}] .+");
    private static final List<String> USER_CHANNEL_PREFIXES = List.of("Party > ", "Guild > ", "Officer > ", "Co-op > ", "From ", "To ");
    private static final int TOP_MARGIN = 0;
    private static final int PADDING = 1;
    private static final int LINE_HEIGHT = 9;
    private static final int HISTORY_VISIBLE_LINES = 10;
    private static final int SCROLL_STEP = 3;
    private static final int SCROLLBAR_WIDTH = 2;
    private static final int TAP_TICKS = 4;

    private static int scrollOffset = 0;
    private static int heldTicks = 0;
    private static boolean ignoreUntilRelease = false;

    private record RenderLine(FormattedCharSequence text, boolean centered) {}

    private record HistoryView(List<RenderLine> lines, int start, int end, int left, int top, int right, int bottom, int wrap, int maxOffset) {}

    private static class SystemChat {
        private final ChatLayout.ParsedMessage message;
        private int tickTime = 0;

        SystemChat(ChatLayout.ParsedMessage message) {
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

        ChatLayout.ParsedMessage parsed = ChatLayout.parse(message);
        systemChats.add(new SystemChat(parsed));
        history.add(parsed);
        if (history.size() > MAX_HISTORY) history.removeFirst();
        if ((isHistoryOpen() || isPinned()) && scrollOffset > 0) {
            int wrap = ChatLayout.wrapWidth();
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

    private static List<RenderLine> buildLines(ChatLayout.ParsedMessage message, int width, int wrap, Font font) {
        List<RenderLine> lines = new ArrayList<>();
        for (ChatLayout.ParsedLine line : message.lines()) {
            if (line.type() == ChatLayout.LineType.SEPARATOR) {
                lines.add(new RenderLine(ChatLayout.separator(line, width, font).getVisualOrderText(), false));
                continue;
            }

            List<FormattedCharSequence> wrapped = font.split(line.component(), wrap);
            if (wrapped.isEmpty()) {
                lines.add(new RenderLine(FormattedCharSequence.EMPTY, false));
                continue;
            }

            boolean centered = line.type() == ChatLayout.LineType.CENTERED;
            for (FormattedCharSequence seq : wrapped) lines.add(new RenderLine(seq, centered));
        }
        return lines;
    }

    public static void tickAllMessages() {
        tickHistoryKey();

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

    private static void tickHistoryKey() {
        Minecraft mc = Minecraft.getInstance();
        boolean keyDown = ModKeyBindings.SYSTEM_CHAT_HISTORY_KEY.isDown();

        if (ignoreUntilRelease) {
            if (!keyDown) ignoreUntilRelease = false;
            heldTicks = 0;
            return;
        }

        if (keyDown && mc.gui.screen() == null) {
            heldTicks++;
            return;
        }

        if (!keyDown && heldTicks > 0 && heldTicks <= TAP_TICKS && canPin()) {
            heldTicks = 0;
            mc.gui.setScreen(new SystemChatScreen());
            return;
        }

        heldTicks = 0;
    }

    private static boolean canPin() {
        return StrayersSkyblockUtilsClient.isInSkyblock
                && ModConfig.INSTANCE.separateMessage
                && Minecraft.getInstance().gui.screen() == null;
    }

    public static void ignoreKeyUntilRelease() {
        ignoreUntilRelease = true;
        heldTicks = 0;
    }

    private static boolean isPinned() {
        return Minecraft.getInstance().gui.screen() instanceof SystemChatScreen;
    }

    private static boolean isHistoryOpen() {
        return StrayersSkyblockUtilsClient.isInSkyblock
                && ModConfig.INSTANCE.separateMessage
                && ModKeyBindings.SYSTEM_CHAT_HISTORY_KEY.isDown()
                && Minecraft.getInstance().gui.screen() == null;
    }

    public static boolean onScroll(double vertical) {
        if (!isHistoryOpen() || vertical == 0) return false;
        scrollHistory(vertical);
        return true;
    }

    public static void scrollHistory(double vertical) {
        if (vertical == 0) return;
        scrollOffset += (int) Math.signum(vertical) * SCROLL_STEP;
        if (scrollOffset < 0) scrollOffset = 0;
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (!ModConfig.INSTANCE.separateMessage) return;

        float scale = ChatLayout.chatScale();
        if (scale <= 0) return;

        if (isPinned()) return;

        if (isHistoryOpen()) {
            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);
            renderHistory(graphics, scale, false);
            graphics.pose().popMatrix();
            return;
        }

        scrollOffset = 0;

        if (systemChats.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int wrap = ChatLayout.wrapWidth();

        int textWidth = 0;
        for (SystemChat chat : systemChats) textWidth = Math.max(textWidth, ChatLayout.contentWidth(chat.message, wrap, font));
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

    public static void renderPinned(GuiGraphicsExtractor graphics) {
        float scale = ChatLayout.chatScale();
        if (scale <= 0) return;

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        renderHistory(graphics, scale, true);
        graphics.pose().popMatrix();
    }

    private static HistoryView historyView(int guiWidth, float scale) {
        Font font = Minecraft.getInstance().font;
        int wrap = ChatLayout.wrapWidth();

        List<RenderLine> lines = new ArrayList<>();
        for (ChatLayout.ParsedMessage message : history) lines.addAll(buildLines(message, wrap, wrap, font));

        int maxOffset = Math.max(0, lines.size() - HISTORY_VISIBLE_LINES);
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;

        int panelWidth = wrap + PADDING * 3 + SCROLLBAR_WIDTH;
        int panelHeight = HISTORY_VISIBLE_LINES * LINE_HEIGHT + PADDING * 2;
        int right = Mth.ceil(guiWidth / scale);
        int left = right - panelWidth;
        int top = Mth.floor(TOP_MARGIN / scale);
        int bottom = top + panelHeight;

        int end = lines.size() - scrollOffset;
        int start = Math.max(0, end - HISTORY_VISIBLE_LINES);

        return new HistoryView(lines, start, end, left, top, right, bottom, wrap, maxOffset);
    }

    private static int lineX(HistoryView view, RenderLine line, Font font) {
        int x = view.left() + PADDING;
        if (line.centered()) x += (view.wrap() - font.width(line.text())) / 2;
        return x;
    }

    private static int firstLineY(HistoryView view) {
        return view.bottom() - PADDING - (view.end() - view.start()) * LINE_HEIGHT;
    }

    private static void renderHistory(GuiGraphicsExtractor graphics, float scale, boolean interactive) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        HistoryView view = historyView(graphics.guiWidth(), scale);

        graphics.fill(view.left(), view.top(), view.right(), view.bottom(), Math.max(backgroundColor(mc) >>> 24, 0x80) << 24);

        if (view.lines().isEmpty()) {
            graphics.text(font, "No system messages yet", view.left() + PADDING, view.top() + PADDING, 0xFFAAAAAA, true);
            return;
        }

        ActiveTextCollector collector = interactive ? graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_AND_CURSOR) : null;
        ActiveTextCollector.Parameters parameters = collector != null ? collector.defaultParameters().withOpacity(textOpacity(mc)) : null;
        int textColor = textColor(mc);

        int lineY = firstLineY(view);
        for (int i = view.start(); i < view.end(); i++) {
            RenderLine line = view.lines().get(i);
            int x = lineX(view, line, font);
            if (collector != null) {
                collector.accept(TextAlignment.LEFT, x, lineY, parameters, line.text());
            } else {
                graphics.text(font, line.text(), x, lineY, textColor, true);
            }
            lineY += LINE_HEIGHT;
        }

        if (view.maxOffset() > 0) {
            int panelHeight = view.bottom() - view.top();
            int trackTop = view.top() + PADDING;
            int trackHeight = panelHeight - PADDING * 2;
            int thumbHeight = Math.max(8, trackHeight * HISTORY_VISIBLE_LINES / view.lines().size());
            int thumbTop = trackTop + (trackHeight - thumbHeight) * (view.maxOffset() - scrollOffset) / view.maxOffset();
            int barRight = view.right() - PADDING;
            int barLeft = barRight - SCROLLBAR_WIDTH;

            graphics.fill(barLeft, trackTop, barRight, trackTop + trackHeight, 0x40FFFFFF);
            graphics.fill(barLeft, thumbTop, barRight, thumbTop + thumbHeight, 0xC0FFFFFF);
        }
    }

    public static Style findStyleAt(double mouseX, double mouseY, boolean includeInsertions) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        float scale = ChatLayout.chatScale();
        if (scale <= 0) return null;

        HistoryView view = historyView(mc.getWindow().getGuiScaledWidth(), scale);
        if (view.lines().isEmpty()) return null;

        ActiveTextCollector.ClickableStyleFinder finder = new ActiveTextCollector.ClickableStyleFinder(font, (int) (mouseX / scale), (int) (mouseY / scale))
                .includeInsertions(includeInsertions);

        int lineY = firstLineY(view);
        for (int i = view.start(); i < view.end(); i++) {
            RenderLine line = view.lines().get(i);
            finder.accept(TextAlignment.LEFT, lineX(view, line, font), lineY, line.text());
            lineY += LINE_HEIGHT;
        }

        return finder.result();
    }

    private static float textOpacity(Minecraft mc) {
        return (float) (mc.options.chatOpacity().get() * 0.9 + 0.1);
    }

    private static int textColor(Minecraft mc) {
        int alpha = (int) (255.0 * textOpacity(mc));
        return (alpha << 24) | 0xFFFFFF;
    }

    private static int backgroundColor(Minecraft mc) {
        int alpha = (int) (255.0 * mc.options.textBackgroundOpacity().get());
        return alpha << 24;
    }
}