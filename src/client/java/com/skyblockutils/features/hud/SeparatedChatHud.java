package com.skyblockutils.features.hud;

import com.skyblockutils.ModKeyBindings;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.chat.ChatLayout;
import com.skyblockutils.features.chat.SeparatedChat;
import com.skyblockutils.features.chat.SystemChatScreen;
import com.skyblockutils.features.hud.layout.HudContext;
import com.skyblockutils.features.hud.layout.HudElement;
import com.skyblockutils.features.hud.layout.HudLayoutEntry;
import com.skyblockutils.features.hud.layout.HudManager;
import com.skyblockutils.utils.ModStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SeparatedChatHud {
    private static final int TOP_MARGIN = 0;
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 9;
    private static final int HISTORY_VISIBLE_LINES = 10;
    private static final int SCROLL_STEP = 3;
    private static final int SCROLLBAR_WIDTH = 2;
    private static final int TAP_TICKS = 4;
    private static final int SEARCH_BAR_HEIGHT = 12;
    private static final int CORNER_RADIUS = 4;

    private static int scrollOffset = 0;
    private static int heldTicks = 0;
    private static boolean ignoreUntilRelease = false;
    private static String searchQuery = "";
    private static String searchLower = "";

    private record RenderLine(FormattedCharSequence text, boolean centered, ChatLayout.ParsedMessage message) {}

    private record HistoryView(List<RenderLine> lines, int start, int end, int left, int top, int right, int bottom, int wrap, int maxOffset) {}

    public static void onMessageAdded(SeparatedChat.HistoryEntry entry) {
        if (!matches(entry)) return;

        if ((isPeeking() || isPinned()) && scrollOffset > 0) {
            int wrap = wrap();
            scrollOffset += buildLines(entry.message(), wrap, wrap, Minecraft.getInstance().font).size();
        }
    }

    private static List<RenderLine> buildLines(ChatLayout.ParsedMessage message, int width, int wrap, Font font) {
        List<RenderLine> lines = new ArrayList<>();
        for (ChatLayout.ParsedLine line : message.lines()) {
            if (line.type() == ChatLayout.LineType.SEPARATOR) {
                lines.add(new RenderLine(ChatLayout.separator(line, width, font).getVisualOrderText(), false, message));
                continue;
            }

            List<FormattedCharSequence> wrapped = font.split(line.component(), wrap);
            if (wrapped.isEmpty()) {
                lines.add(new RenderLine(FormattedCharSequence.EMPTY, false, message));
                continue;
            }

            boolean centered = line.type() == ChatLayout.LineType.CENTERED;
            for (FormattedCharSequence seq : wrapped) lines.add(new RenderLine(seq, centered, message));
        }
        return lines;
    }

    public static void tick() {
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

    private static boolean isPinned() {
        return Minecraft.getInstance().gui.screen() instanceof SystemChatScreen;
    }

    private static boolean isPeeking() {
        return StrayersSkyblockUtilsClient.isInSkyblock
                && ModConfig.INSTANCE.separateMessage
                && !ignoreUntilRelease
                && (ModKeyBindings.SYSTEM_CHAT_HISTORY_KEY.isDown() || heldTicks > 0)
                && Minecraft.getInstance().gui.screen() == null;
    }

    public static boolean onScroll(double vertical) {
        if (!isPeeking() || vertical == 0) return false;
        scrollHistory(vertical);
        return true;
    }

    public static void scrollHistory(double vertical) {
        if (vertical == 0) return;
        scrollOffset += (int) Math.signum(vertical) * SCROLL_STEP;
        if (scrollOffset < 0) scrollOffset = 0;
    }

    public static void resetPinnedState() {
        searchQuery = "";
        searchLower = "";
        scrollOffset = 0;
    }

    public static String searchQuery() {
        return searchQuery;
    }

    public static void setSearch(String query) {
        searchQuery = query;
        searchLower = query.isBlank() ? "" : query.toLowerCase(Locale.ROOT);
        scrollOffset = 0;
    }

    private static boolean matches(SeparatedChat.HistoryEntry entry) {
        return searchLower.isEmpty() || entry.searchText().contains(searchLower);
    }

    public static boolean hasNoMatch() {
        if (searchLower.isEmpty()) return false;
        for (SeparatedChat.HistoryEntry entry : SeparatedChat.history()) {
            if (matches(entry)) return false;
        }
        return true;
    }

    public static int[] searchBarBounds() {
        HudManager.Bounds b = HudManager.bounds(ELEMENT, false);
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int x = Mth.floor(b.x());
        int width = Mth.ceil(b.right()) - x;
        int y = b.bottom() + SEARCH_BAR_HEIGHT <= screenHeight ? Mth.ceil(b.bottom()) : Mth.floor(b.y()) - SEARCH_BAR_HEIGHT;
        return new int[]{x, Math.max(0, y), width, SEARCH_BAR_HEIGHT};
    }

    private static int wrap() {
        return Math.max(20, HudManager.layoutWidth(ELEMENT, false) - PADDING * 3 - SCROLLBAR_WIDTH);
    }

    private static boolean historyMode(boolean preview) {
        return preview || isPeeking() || isPinned();
    }

    private static List<RenderLine> liveLines(int textWidth) {
        Font font = Minecraft.getInstance().font;
        int wrap = wrap();
        List<RenderLine> lines = new ArrayList<>();
        for (ChatLayout.ParsedMessage message : SeparatedChat.activeMessages()) lines.addAll(buildLines(message, textWidth, wrap, font));
        if (lines.size() > HISTORY_VISIBLE_LINES) return lines.subList(lines.size() - HISTORY_VISIBLE_LINES, lines.size());
        return lines;
    }

    public static final HudElement ELEMENT = new HudElement("system_chat", "System Messages") {
        @Override
        public HudLayoutEntry defaultLayout() {
            float scale = ChatLayout.chatScale();
            return new HudLayoutEntry(HudLayoutEntry.Anchor.END, HudLayoutEntry.Anchor.START, 0, TOP_MARGIN, scale > 0 ? scale : 1f);
        }

        @Override
        public int minWidth() {
            return 80;
        }

        @Override
        public int maxWidth() {
            return 600;
        }

        @Override
        public int autoWidth(boolean preview) {
            return ChatComponent.getWidth(Minecraft.getInstance().options.chatWidth().get()) + PADDING * 3 + SCROLLBAR_WIDTH;
        }

        @Override
        public int height(int width, boolean preview) {
            if (historyMode(preview)) return HISTORY_VISIBLE_LINES * LINE_HEIGHT + PADDING * 2;
            return Math.max(1, liveLines(wrap()).size()) * LINE_HEIGHT + PADDING * 2;
        }

        @Override
        public boolean isEnabled() {
            return ModConfig.INSTANCE.separateMessage;
        }

        @Override
        public boolean isActive() {
            if (isPinned()) return true;
            if (isPeeking()) return false;
            scrollOffset = 0;
            return SeparatedChat.activeMessages().isEmpty();
        }

        @Override
        public void render(HudContext ctx) {
            if (historyMode(ctx.preview)) {
                renderHistory(ctx, isPinned() && !ctx.preview);
            } else {
                renderLive(ctx);
            }
        }
    };

    public static void render(GuiGraphicsExtractor graphics) {
        HudManager.render(ELEMENT, graphics);
    }

    public static void renderPinned(GuiGraphicsExtractor graphics) {
        HudManager.draw(ELEMENT, graphics, false);
    }

    private static void renderLive(HudContext ctx) {
        Minecraft mc = ctx.mc;
        Font font = ctx.font;
        int textWidth = wrap();
        List<RenderLine> lines = liveLines(textWidth);
        if (lines.isEmpty()) return;

        ctx.panel(0, 0, ctx.width, ctx.height, CORNER_RADIUS, backgroundColor(mc), borderColor());

        int textColor = textColor(mc);
        int lineY = PADDING;
        for (RenderLine line : lines) {
            int x = PADDING;
            if (line.centered()) x += (textWidth - font.width(line.text())) / 2;
            ctx.text(line.text(), x, lineY, textColor, true);
            lineY += LINE_HEIGHT;
        }
    }

    private static HistoryView historyView(int width, int height) {
        Font font = Minecraft.getInstance().font;
        int wrap = wrap();

        List<RenderLine> lines = new ArrayList<>();
        for (SeparatedChat.HistoryEntry entry : SeparatedChat.history()) {
            if (matches(entry)) lines.addAll(buildLines(entry.message(), wrap, wrap, font));
        }

        int maxOffset = Math.max(0, lines.size() - HISTORY_VISIBLE_LINES);
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;

        int end = lines.size() - scrollOffset;
        int start = Math.max(0, end - HISTORY_VISIBLE_LINES);

        return new HistoryView(lines, start, end, 0, 0, width, height, wrap, maxOffset);
    }

    private static int lineX(HistoryView view, RenderLine line, Font font) {
        int x = view.left() + PADDING;
        if (line.centered()) x += (view.wrap() - font.width(line.text())) / 2;
        return x;
    }

    private static int firstLineY(HistoryView view) {
        return view.bottom() - PADDING - (view.end() - view.start()) * LINE_HEIGHT;
    }

    private static void renderHistory(HudContext ctx, boolean interactive) {
        Minecraft mc = ctx.mc;
        Font font = ctx.font;
        GuiGraphicsExtractor graphics = ctx.graphics;
        HistoryView view = historyView(ctx.width, ctx.height);

        ctx.panel(view.left(), view.top(), view.right(), view.bottom(), CORNER_RADIUS, Math.max(backgroundColor(mc) >>> 24, 0x80) << 24, borderColor());

        if (view.lines().isEmpty()) {
            String empty = searchLower.isEmpty() ? "No system messages yet" : "No matching messages";
            ctx.text(empty, view.left() + PADDING, view.top() + PADDING, 0xFFAAAAAA, true);
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
                ctx.text(line.text(), x, lineY, textColor, true);
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

            ctx.fill(barLeft, trackTop, barRight, trackTop + trackHeight, 0x40FFFFFF);
            ctx.fill(barLeft, thumbTop, barRight, thumbTop + thumbHeight, 0xC0FFFFFF);
        }
    }

    public static String messageTextAt(double mouseX, double mouseY) {
        HudManager.Bounds bounds = HudManager.bounds(ELEMENT, false);
        if (!bounds.contains(mouseX, mouseY)) return null;

        HistoryView view = historyView(bounds.width(), bounds.height());
        if (view.lines().isEmpty()) return null;

        double localY = (mouseY - bounds.y()) / bounds.scale();
        int index = view.start() + (int) Math.floor((localY - firstLineY(view)) / LINE_HEIGHT);
        if (index < view.start() || index >= view.end()) return null;

        ChatLayout.ParsedMessage message = view.lines().get(index).message();
        List<String> parts = new ArrayList<>();
        for (ChatLayout.ParsedLine line : message.lines()) {
            if (line.type() == ChatLayout.LineType.SEPARATOR) continue;
            String text = line.component().getString().replaceAll("§.", "").strip();
            if (!text.isEmpty()) parts.add(text);
        }
        return parts.isEmpty() ? null : String.join("\n", parts);
    }

    public static Style findStyleAt(double mouseX, double mouseY, boolean includeInsertions) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        HudManager.Bounds bounds = HudManager.bounds(ELEMENT, false);
        if (!bounds.contains(mouseX, mouseY)) return null;

        HistoryView view = historyView(bounds.width(), bounds.height());
        if (view.lines().isEmpty()) return null;

        int localX = (int) ((mouseX - bounds.x()) / bounds.scale());
        int localY = (int) ((mouseY - bounds.y()) / bounds.scale());
        ActiveTextCollector.ClickableStyleFinder finder = new ActiveTextCollector.ClickableStyleFinder(font, localX, localY)
                .includeInsertions(includeInsertions);

        int lineY = firstLineY(view);
        for (int i = view.start(); i < view.end(); i++) {
            RenderLine line = view.lines().get(i);
            finder.accept(TextAlignment.LEFT, lineX(view, line, font), lineY, line.text());
            lineY += LINE_HEIGHT;
        }

        return finder.result();
    }

    private static int borderColor() {
        return 0xFF000000 | ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
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