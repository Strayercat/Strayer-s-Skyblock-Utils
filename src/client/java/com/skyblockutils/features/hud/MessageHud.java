package com.skyblockutils.features.hud;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.chat.ChatLayout;
import com.skyblockutils.features.chat.MessageFeed;
import com.skyblockutils.features.hud.layout.HudContext;
import com.skyblockutils.features.hud.layout.HudElement;
import com.skyblockutils.features.hud.layout.HudLayoutEntry;
import com.skyblockutils.features.hud.layout.HudManager;
import com.skyblockutils.utils.ModStyle;
import net.minecraft.client.KeyMapping;
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
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class MessageHud {
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 9;
    private static final int HISTORY_VISIBLE_LINES = 10;
    private static final int SCROLL_STEP = 3;
    private static final int SCROLLBAR_WIDTH = 2;
    private static final int TAP_TICKS = 4;
    private static final int SEARCH_BAR_HEIGHT = 12;
    private static final int SEARCH_BAR_GAP = 2;
    private static final int CORNER_RADIUS = 4;

    private static final List<MessageHud> ALL = new ArrayList<>();

    private record RenderLine(FormattedCharSequence text, boolean centered, ChatLayout.ParsedMessage message) {}

    private record HistoryView(List<RenderLine> lines, int start, int end, int left, int top, int right, int bottom, int wrap, int maxOffset) {}

    public final String title;
    public final String emptyText;
    public final String copiedText;
    private final MessageFeed feed;
    private final Supplier<KeyMapping> historyKey;
    private final BooleanSupplier enabled;
    private final BooleanSupplier context;
    private final Supplier<HudLayoutEntry> defaultLayout;
    public final HudElement element;

    private int scrollOffset = 0;
    private int heldTicks = 0;
    private boolean ignoreUntilRelease = false;
    private String searchQuery = "";
    private String searchLower = "";

    public MessageHud(String id, String name, String title, String emptyText, String copiedText, MessageFeed feed, Supplier<KeyMapping> historyKey,
                      BooleanSupplier enabled, BooleanSupplier context, Supplier<HudLayoutEntry> defaultLayout) {
        this.title = title;
        this.emptyText = emptyText;
        this.copiedText = copiedText;
        this.feed = feed;
        this.historyKey = historyKey;
        this.enabled = enabled;
        this.context = context;
        this.defaultLayout = defaultLayout;
        this.element = createElement(id, name);
        feed.onAdded(this::onMessageAdded);
        ALL.add(this);
    }

    public static boolean scrollAny(double vertical) {
        for (MessageHud hud : ALL) {
            if (hud.onScroll(vertical)) return true;
        }
        return false;
    }

    private void onMessageAdded(MessageFeed.HistoryEntry entry) {
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

            List<FormattedCharSequence> wrapped = font.split(ChatLayout.indented(line.component(), wrap), wrap);
            if (wrapped.isEmpty()) {
                lines.add(new RenderLine(FormattedCharSequence.EMPTY, false, message));
                continue;
            }

            boolean centered = line.type() == ChatLayout.LineType.CENTERED;
            for (FormattedCharSequence seq : wrapped) lines.add(new RenderLine(seq, centered, message));
        }
        return lines;
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        boolean keyDown = historyKey.get().isDown();

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
            mc.gui.setScreen(new MessageHistoryScreen(this));
            return;
        }

        heldTicks = 0;
    }

    public boolean liveHere() {
        return enabled.getAsBoolean() && HudManager.isVisible(element, HudManager.currentLocation());
    }

    private boolean historyAvailable() {
        return context.getAsBoolean() && (liveHere() || (enabled.getAsBoolean() && element.show("historyAnywhere")));
    }

    private boolean canPin() {
        return historyAvailable() && Minecraft.getInstance().gui.screen() == null;
    }

    private boolean isPinned() {
        return Minecraft.getInstance().gui.screen() instanceof MessageHistoryScreen screen && screen.hud == this;
    }

    private boolean isPeeking() {
        return historyAvailable()
                && !ignoreUntilRelease
                && (historyKey.get().isDown() || heldTicks > 0)
                && Minecraft.getInstance().gui.screen() == null;
    }

    public boolean onScroll(double vertical) {
        if (!isPeeking() || vertical == 0) return false;
        scrollHistory(vertical);
        return true;
    }

    public void scrollHistory(double vertical) {
        if (vertical == 0) return;
        scrollOffset += (int) Math.signum(vertical) * SCROLL_STEP;
        if (scrollOffset < 0) scrollOffset = 0;
    }

    public void resetPinnedState() {
        searchQuery = "";
        searchLower = "";
        scrollOffset = 0;
    }

    public String searchQuery() {
        return searchQuery;
    }

    public void setSearch(String query) {
        searchQuery = query;
        searchLower = query.isBlank() ? "" : query.toLowerCase(Locale.ROOT);
        scrollOffset = 0;
    }

    private boolean matches(MessageFeed.HistoryEntry entry) {
        return searchLower.isEmpty() || entry.searchText().contains(searchLower);
    }

    public boolean hasNoMatch() {
        if (searchLower.isEmpty()) return false;
        for (MessageFeed.HistoryEntry entry : feed.history()) {
            if (matches(entry)) return false;
        }
        return true;
    }

    public int[] searchBarBounds() {
        HudManager.Bounds b = HudManager.bounds(element, false);
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int x = Mth.floor(b.x());
        int width = Mth.ceil(b.right()) - x;
        int y = b.bottom() + SEARCH_BAR_GAP + SEARCH_BAR_HEIGHT <= screenHeight
                ? Mth.ceil(b.bottom()) + SEARCH_BAR_GAP
                : Mth.floor(b.y()) - SEARCH_BAR_GAP - SEARCH_BAR_HEIGHT;
        return new int[]{x, Math.max(0, y), width, SEARCH_BAR_HEIGHT};
    }

    private int wrap() {
        return Math.max(20, HudManager.layoutWidth(element, false) - PADDING * 3 - SCROLLBAR_WIDTH);
    }

    private boolean historyMode(boolean preview) {
        return preview || isPeeking() || isPinned();
    }

    private List<RenderLine> liveLines(int textWidth) {
        Font font = Minecraft.getInstance().font;
        int wrap = wrap();
        List<RenderLine> lines = new ArrayList<>();
        for (ChatLayout.ParsedMessage message : feed.activeMessages()) lines.addAll(buildLines(message, textWidth, wrap, font));
        if (lines.size() > HISTORY_VISIBLE_LINES) return lines.subList(lines.size() - HISTORY_VISIBLE_LINES, lines.size());
        return lines;
    }

    private HudElement createElement(String id, String name) {
        return new HudElement(id, name) {
            private final List<Option> options = List.of(
                    Option.global("historyAnywhere", "Open history where this HUD is hidden", () -> true)
            );

            @Override
            public List<Option> options() {
                return options;
            }

            @Override
            public HudLayoutEntry defaultLayout() {
                return defaultLayout.get();
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
                return enabled.getAsBoolean();
            }

            @Override
            public boolean isActive() {
                if (isPinned()) return true;
                if (isPeeking()) return false;
                scrollOffset = 0;
                return !context.getAsBoolean() || feed.activeMessages().isEmpty();
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
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (!liveHere() && isPeeking() && !HudManager.isEditing()) {
            HudManager.draw(element, graphics, false);
            return;
        }
        HudManager.render(element, graphics);
    }

    public void renderPinned(GuiGraphicsExtractor graphics) {
        HudManager.draw(element, graphics, false);
    }

    private void renderLive(HudContext ctx) {
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

    private HistoryView historyView(int width, int height) {
        Font font = Minecraft.getInstance().font;
        int wrap = wrap();

        List<RenderLine> lines = new ArrayList<>();
        for (MessageFeed.HistoryEntry entry : feed.history()) {
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

    private void renderHistory(HudContext ctx, boolean interactive) {
        Minecraft mc = ctx.mc;
        Font font = ctx.font;
        GuiGraphicsExtractor graphics = ctx.graphics;
        HistoryView view = historyView(ctx.width, ctx.height);

        ctx.panel(view.left(), view.top(), view.right(), view.bottom(), CORNER_RADIUS, Math.max(backgroundColor(mc) >>> 24, 0x80) << 24, borderColor());

        if (view.lines().isEmpty()) {
            String empty = searchLower.isEmpty() ? emptyText : "No matching messages";
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

    public String messageTextAt(double mouseX, double mouseY) {
        HudManager.Bounds bounds = HudManager.bounds(element, false);
        if (!bounds.contains(mouseX, mouseY)) return null;

        HistoryView view = historyView(bounds.width(), bounds.height());
        if (view.lines().isEmpty()) return null;

        double localX = (mouseX - bounds.x()) / bounds.scale();
        double localY = (mouseY - bounds.y()) / bounds.scale();
        double offsetY = localY - firstLineY(view);
        if (offsetY < 0) return null;
        int index = view.start() + (int) Math.floor(offsetY / LINE_HEIGHT);
        if (index < view.start() || index >= view.end()) return null;

        RenderLine hovered = view.lines().get(index);
        Font font = Minecraft.getInstance().font;
        int textX = lineX(view, hovered, font);
        if (localX < textX || localX > textX + font.width(hovered.text())) return null;

        List<String> parts = new ArrayList<>();
        for (ChatLayout.ParsedLine line : hovered.message().lines()) {
            if (line.type() == ChatLayout.LineType.SEPARATOR) continue;
            String text = line.component().getString().replaceAll("§.", "").strip();
            if (!text.isEmpty()) parts.add(text);
        }
        return parts.isEmpty() ? null : String.join("\n", parts);
    }

    public Style findStyleAt(double mouseX, double mouseY, boolean includeInsertions) {
        Font font = Minecraft.getInstance().font;

        HudManager.Bounds bounds = HudManager.bounds(element, false);
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
