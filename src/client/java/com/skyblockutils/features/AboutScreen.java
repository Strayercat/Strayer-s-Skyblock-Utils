package com.skyblockutils.features;

import com.skyblockutils.config.ClothConfigHandler;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.mixin.client.ScreenInvoker;
import com.skyblockutils.utils.GemColor;
import com.skyblockutils.utils.ModStyle;
import com.skyblockutils.utils.SSU;
import com.skyblockutils.utils.SSUIndicator;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AboutScreen extends Screen {
    private static final int INFO_BUTTON_SIZE = 20;
    private static final int INFO_BUTTON_MARGIN = 2;

    private static final int MAX_PANEL_WIDTH = 300;
    private static final int SCREEN_MARGIN = 10;
    private static final int PADDING = 10;
    private static final int HEADER_HEIGHT = 24;
    private static final int FOOTER_HEIGHT = 30;
    private static final int LINE_HEIGHT = 10;
    private static final int PARAGRAPH_GAP = 6;
    private static final int ROW_GAP = 4;
    private static final int SCROLL_STEP = 12;

    private static final int COLOR_TEXT = 0xFFDDDDDD;
    private static final int COLOR_DESC = 0xFFAAAAAA;

    private static final String INTRO = "A collection of tweaks that make Hypixel Skyblock smoother and more enjoyable: "
            + "dungeon tools, mining waypoints and reminders, chat and party improvements, a customizable HUD "
            + "and plenty of small quality of life features. It started as a personal project to fix the things "
            + "I wished other mods did differently, and grew from there.";
    private static final String FEEDBACK = "Found a bug or have an idea? Let me know on the GitHub page!";
    private static final String GEMS_INTRO = "Other SSU users are marked by a gem next to their name. Its color shows who they are:";

    private static final GemColor[] UNDETERMINED = {
            GemColor.GOLD, GemColor.GREEN, GemColor.ORANGE, GemColor.PINK, GemColor.WHITE, GemColor.BLACK, GemColor.GRAY
    };

    private static final List<GemRow> ROWS = List.of(
            new GemRow(new GemColor[]{GemColor.SPECIAL}, "Developer", -1, "The creator of the mod (that's me, Strayer! (You may know me as StrayerKatz.))"),
            new GemRow(new GemColor[]{GemColor.PURPLE}, "VIP", 0xFFAA55FF, "People who have supported me closely during development"),
            new GemRow(new GemColor[]{GemColor.RED}, "Helper", 0xFFFF5555, "People who gave useful feedback, reported important bugs or shared ideas"),
            new GemRow(new GemColor[]{GemColor.BLUE}, "User", 0xFF55AAFF, "Everyone else using SSU"),
            new GemRow(UNDETERMINED, "Undetermined", 0xFF888888, "Not assigned yet, stay tuned!")
    );

    private final Screen parent;
    private int panelWidth;
    private int panelHeight;
    private int panelX;
    private int panelY;
    private int contentHeight;
    private int scroll = 0;

    public AboutScreen(Screen parent) {
        super(Component.literal("Strayer's Skyblock Utils"));
        this.parent = parent;
    }

    public static void registerInfoButton() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, _) -> {
            if (!ClothConfigHandler.CONFIG_TITLE.equals(screen.getTitle().getString())) return;
            Button button = Button.builder(Component.literal("ℹ"), _ -> client.setScreenAndShow(new AboutScreen(screen)))
                    .bounds(width - INFO_BUTTON_SIZE - INFO_BUTTON_MARGIN, INFO_BUTTON_MARGIN, INFO_BUTTON_SIZE, INFO_BUTTON_SIZE)
                    .build();
            ((ScreenInvoker) screen).ssu$addRenderableWidget(button);
        });
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_PANEL_WIDTH, this.width - SCREEN_MARGIN * 2);
        contentHeight = measure(buildLines(innerWidth()));
        panelHeight = Math.min(HEADER_HEIGHT + contentHeight + FOOTER_HEIGHT, this.height - SCREEN_MARGIN * 2);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
        scroll = Mth.clamp(scroll, 0, maxScroll());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), _ -> this.onClose())
                .bounds(this.width / 2 - 50, panelY + panelHeight - FOOTER_HEIGHT + 5, 100, 20)
                .build());
    }

    private int innerWidth() {
        return panelWidth - PADDING * 2;
    }

    private int viewTop() {
        return panelY + HEADER_HEIGHT;
    }

    private int viewBottom() {
        return panelY + panelHeight - FOOTER_HEIGHT;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - (viewBottom() - viewTop()));
    }

    private List<Line> buildLines(int width) {
        List<Line> lines = new ArrayList<>();
        addParagraph(lines, Component.literal(INTRO).withColor(COLOR_TEXT), width);
        addParagraph(lines, Component.literal(FEEDBACK).withColor(COLOR_TEXT), width);
        lines.add(Line.divider());
        addParagraph(lines, Component.literal(GEMS_INTRO).withColor(COLOR_TEXT), width);

        for (GemRow row : ROWS) {
            int indent = font.width(gemPrefix(row.gems()));
            int labelColor = row.labelColor() == -1 ? 0xFF000000 | SSUIndicator.prismaticColor() : row.labelColor();
            MutableComponent text = Component.literal(row.label()).withColor(labelColor)
                    .append(Component.literal(" - " + row.description()).withColor(COLOR_DESC));
            List<FormattedCharSequence> split = font.split(text, Math.max(20, width - indent));
            for (int i = 0; i < split.size(); i++) {
                boolean last = i == split.size() - 1;
                lines.add(new Line(i == 0 ? row.gems() : null, split.get(i), indent, LINE_HEIGHT + (last ? ROW_GAP : 0), false));
            }
        }
        return lines;
    }

    private void addParagraph(List<Line> lines, Component text, int width) {
        List<FormattedCharSequence> split = font.split(text, width);
        for (int i = 0; i < split.size(); i++) {
            boolean last = i == split.size() - 1;
            lines.add(new Line(null, split.get(i), 0, LINE_HEIGHT + (last ? PARAGRAPH_GAP : 0), false));
        }
    }

    private static int measure(List<Line> lines) {
        int height = 0;
        for (Line line : lines) height += line.height();
        return height;
    }

    private static String gemPrefix(GemColor[] gems) {
        StringBuilder sb = new StringBuilder();
        for (GemColor gem : gems) {
            sb.appendCodePoint(gem == GemColor.SPECIAL ? SSUIndicator.currentSpecialGlyph() : gem.glyph);
        }
        return sb.append(": ").toString();
    }

    @Override
    public void extractBackground(final @NotNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (parent != null) parent.extractRenderState(graphics, -1, -1, delta);
        graphics.nextStratum();
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        int mainColor = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
        int x1 = panelX;
        int y1 = panelY;
        int x2 = panelX + panelWidth;
        int y2 = panelY + panelHeight;

        graphics.fill(x1, y1, x2, y2, 0xEE1A1A1A);
        drawBorder(graphics, x1, y1, panelWidth, panelHeight, mainColor);
        Component title = SSU.gradientText(this.title.getString(),
                ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TITLE_START),
                ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TITLE_END));
        graphics.centeredText(this.font, title, this.width / 2, y1 + 8, 0xFFFFFFFF);
        graphics.fill(x1 + PADDING, y1 + HEADER_HEIGHT - 5, x2 - PADDING, y1 + HEADER_HEIGHT - 4, mainColor);

        int textX = x1 + PADDING;
        int top = viewTop();
        int bottom = viewBottom();

        graphics.enableScissor(x1, top, x2, bottom);
        int y = top - scroll;
        for (Line line : buildLines(innerWidth())) {
            if (y + line.height() >= top && y <= bottom) {
                if (line.separator()) {
                    int barY = y + PARAGRAPH_GAP / 2;
                    graphics.fill(textX + innerWidth() / 4, barY, textX + innerWidth() * 3 / 4, barY + 1, 0xFF444444);
                } else {
                    if (line.gems() != null) graphics.text(this.font, gemPrefix(line.gems()), textX, y, 0xFFFFFFFF, false);
                    graphics.text(this.font, line.text(), textX + line.indent(), y, 0xFFFFFFFF, false);
                }
            }
            y += line.height();
        }
        graphics.disableScissor();

        if (maxScroll() > 0) {
            int trackHeight = bottom - top;
            int thumbHeight = Math.max(10, trackHeight * trackHeight / contentHeight);
            int thumbY = top + (trackHeight - thumbHeight) * scroll / maxScroll();
            graphics.fill(x2 - 4, thumbY, x2 - 2, thumbY + thumbHeight, mainColor);
        }

        graphics.fill(x1 + PADDING, bottom + 1, x2 - PADDING, bottom + 2, mainColor);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (maxScroll() == 0) return false;
        scroll = Mth.clamp(scroll - (int) (scrollY * SCROLL_STEP), 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isConfirmation()) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    private static void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private record GemRow(GemColor[] gems, String label, int labelColor, String description) {}

    private record Line(GemColor[] gems, FormattedCharSequence text, int indent, int height, boolean separator) {
        static Line divider() {
            return new Line(null, FormattedCharSequence.EMPTY, 0, PARAGRAPH_GAP + 2, true);
        }
    }
}