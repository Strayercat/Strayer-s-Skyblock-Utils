package com.skyblockutils.features.chat.emoji;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.chat.emoji.EmojiRegistry.Emoji;
import com.skyblockutils.utils.ModStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class EmojiPicker {
    public static final int BUTTON_SIZE = 10;
    public static final int INPUT_OFFSET = BUTTON_SIZE + 2;

    private static final Identifier BUTTON = Identifier.fromNamespaceAndPath("skyblockutils", "emoji_button");
    private static final Identifier BUTTON_HOVER = Identifier.fromNamespaceAndPath("skyblockutils", "emoji_button_hover");

    private static final int COLUMNS = 10;
    private static final int CELL = 14;
    private static final float EMOJI_SCALE = 1.5f;
    private static final int VISIBLE_HEIGHT = CELL * 7;
    private static final int SEPARATOR_HEIGHT = 7;
    private static final int PADDING = 5;
    private static final int SEARCH_HEIGHT = 14;
    private static final int FOOTER = 11;
    private static final int RADIUS = 6;
    private static final int WIDTH = COLUMNS * CELL + PADDING * 2;
    private static final int HEIGHT = SEARCH_HEIGHT + VISIBLE_HEIGHT + FOOTER + PADDING * 2;
    private static final int PANEL_X = 2;
    private static final int HOVER_COLOR = 0x40FFFFFF;
    private static final int MAX_QUERY_LENGTH = 32;

    private static final int KEY_ESCAPE = 256;
    private static final int KEY_ENTER = 257;
    private static final int KEY_BACKSPACE = 259;
    private static final int KEY_KP_ENTER = 335;

    private record Line(List<Emoji> emojis) {
        boolean separator() {
            return emojis == null;
        }

        int height() {
            return separator() ? SEPARATOR_HEIGHT : CELL;
        }
    }

    private final List<Line> lines = new ArrayList<>();
    private List<Emoji> results = List.of();
    private String query = "";
    private boolean open = false;
    private int scroll = 0;
    private boolean pickedFromSearch = false;

    private static EmojiPicker boundPicker;
    private static EditBox boundInput;

    public static void bind(EmojiPicker picker, EditBox input) {
        boundPicker = picker;
        boundInput = input;
    }

    public static EmojiPicker capturing(EditBox input) {
        return input == boundInput && boundPicker != null && boundPicker.isOpen() ? boundPicker : null;
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
        setQuery("");
    }

    private void toggle() {
        if (open) {
            close();
            return;
        }
        open = true;
        rebuild();
    }

    private void setQuery(String query) {
        this.query = query;
        pickedFromSearch = false;
        if (open) rebuild();
    }

    private void rebuild() {
        lines.clear();
        scroll = 0;

        List<Emoji> custom = EmojiRegistry.custom();
        List<Emoji> normal = EmojiRegistry.normal();

        if (query.isEmpty()) {
            results = List.of();
            addRows(custom);
            if (!custom.isEmpty()) lines.add(new Line(null));
            addRows(normal);
            return;
        }

        List<Emoji> all = new ArrayList<>(custom.size() + normal.size());
        all.addAll(custom);
        all.addAll(normal);
        results = ModFunctions.fuzzySearchAny(query, all, Emoji::names);
        addRows(results);
    }

    private void addRows(List<Emoji> emojis) {
        for (int i = 0; i < emojis.size(); i += COLUMNS) {
            lines.add(new Line(emojis.subList(i, Math.min(i + COLUMNS, emojis.size()))));
        }
    }

    private int maxScroll() {
        int height = 0;
        for (int i = lines.size() - 1; i >= 0; i--) {
            height += lines.get(i).height();
            if (height > VISIBLE_HEIGHT) return i + 1;
        }
        return 0;
    }

    private static int buttonX() {
        return 3;
    }

    private static int buttonY(int screenHeight) {
        return screenHeight - 13;
    }

    private static int panelTop(int screenHeight) {
        return panelBottom(screenHeight) - HEIGHT;
    }

    private static int panelBottom(int screenHeight) {
        return screenHeight - 16;
    }

    private static int gridTop(int screenHeight) {
        return panelTop(screenHeight) + PADDING + SEARCH_HEIGHT;
    }

    private static boolean isOverButton(double mouseX, double mouseY, int screenHeight) {
        int x = buttonX();
        int y = buttonY(screenHeight);
        return mouseX >= x && mouseX < x + BUTTON_SIZE && mouseY >= y && mouseY < y + BUTTON_SIZE;
    }

    private boolean isOverPanel(double mouseX, double mouseY, int screenHeight) {
        return open && mouseX >= PANEL_X && mouseX < PANEL_X + WIDTH
                && mouseY >= panelTop(screenHeight) && mouseY < panelBottom(screenHeight);
    }

    private Emoji emojiAt(double mouseX, double mouseY, int screenHeight) {
        int gridX = PANEL_X + PADDING;
        int gridY = gridTop(screenHeight);
        if (mouseX < gridX || mouseX >= gridX + COLUMNS * CELL) return null;

        int y = gridY;
        for (int i = scroll; i < lines.size(); i++) {
            Line line = lines.get(i);
            if (y + line.height() > gridY + VISIBLE_HEIGHT) break;
            if (!line.separator() && mouseY >= y && mouseY < y + CELL) {
                int column = (int) (mouseX - gridX) / CELL;
                return column < line.emojis().size() ? line.emojis().get(column) : null;
            }
            y += line.height();
        }
        return null;
    }

    public void renderButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int screenHeight) {
        Identifier sprite = open || isOverButton(mouseX, mouseY, screenHeight) ? BUTTON_HOVER : BUTTON;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, buttonX(), buttonY(screenHeight), BUTTON_SIZE, BUTTON_SIZE);
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int screenHeight) {
        if (!open) return;

        Font font = Minecraft.getInstance().font;
        int mainColor = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
        int x1 = PANEL_X;
        int y1 = panelTop(screenHeight);
        int x2 = x1 + WIDTH;
        int y2 = panelBottom(screenHeight);
        int gridX = x1 + PADDING;
        int gridWidth = COLUMNS * CELL;

        graphics.nextStratum();
        roundedBox(graphics, x1, y1, x2, y2, mainColor);

        renderSearch(graphics, font, gridX, y1 + PADDING, gridWidth, mainColor);

        int gridY = gridTop(screenHeight);
        Emoji hovered = emojiAt(mouseX, mouseY, screenHeight);

        if (!query.isEmpty() && results.isEmpty()) {
            String empty = "No emojis found";
            graphics.text(font, empty, gridX + (gridWidth - font.width(empty)) / 2, gridY + VISIBLE_HEIGHT / 2 - 4, ModStyle.COLOR_SUBTITLE, false);
        }

        int y = gridY;
        for (int i = scroll; i < lines.size(); i++) {
            Line line = lines.get(i);
            if (y + line.height() > gridY + VISIBLE_HEIGHT) break;

            if (line.separator()) {
                int barY = y + SEPARATOR_HEIGHT / 2;
                graphics.fill(gridX, barY, gridX + gridWidth, barY + 1, mainColor);
            } else {
                for (int c = 0; c < line.emojis().size(); c++) {
                    Emoji emoji = line.emojis().get(c);
                    int cellX = gridX + c * CELL;
                    if (emoji == hovered) graphics.fill(cellX, y, cellX + CELL, y + CELL, HOVER_COLOR);

                    graphics.pose().pushMatrix();
                    graphics.pose().translate(cellX + 1, y + 1);
                    graphics.pose().scale(EMOJI_SCALE, EMOJI_SCALE);
                    graphics.text(font, emoji.text(), 0, 0, 0xFFFFFFFF, false);
                    graphics.pose().popMatrix();
                }
            }
            y += line.height();
        }

        Emoji shown = hovered != null ? hovered : results.isEmpty() ? null : results.getFirst();
        String label = shown != null ? shown.shortcode() : "Emojis";
        label = font.plainSubstrByWidth(label, gridWidth);
        graphics.text(font, label, gridX, gridY + VISIBLE_HEIGHT + 3, shown != null ? ModStyle.COLOR_TITLE : ModStyle.COLOR_SUBTITLE, false);

        if (hovered != null) renderHoverLabel(graphics, font, hovered.shortcode(), mouseX, mouseY, mainColor);
    }

    private static void renderHoverLabel(GuiGraphicsExtractor graphics, Font font, String text, int mouseX, int mouseY, int outline) {
        int width = font.width(text) + 6;
        int height = 12;
        int x = mouseX + 6;
        int y = mouseY - height - 2;
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        if (x + width > screenWidth - 2) x = mouseX - width - 6;
        if (y < 2) y = mouseY + 10;

        graphics.fill(x, y, x + width, y + height, 0xEE101010);
        graphics.fill(x, y, x + width, y + 1, outline);
        graphics.fill(x, y + height - 1, x + width, y + height, outline);
        graphics.fill(x, y, x + 1, y + height, outline);
        graphics.fill(x + width - 1, y, x + width, y + height, outline);
        graphics.text(font, text, x + 3, y + 2, ModStyle.COLOR_TITLE, false);
    }

    private void renderSearch(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int mainColor) {
        int textY = y + 1;
        int lineY = y + SEARCH_HEIGHT - 4;

        if (query.isEmpty()) {
            graphics.text(font, "Type to search...", x + 1, textY, ModStyle.COLOR_SUBTITLE, false);
        } else {
            String shown = font.plainSubstrByWidth(query, width - 8, true);
            graphics.text(font, shown, x + 1, textY, ModStyle.COLOR_TITLE, false);
            if ((System.currentTimeMillis() / 500) % 2 == 0) {
                graphics.text(font, "_", x + 1 + font.width(shown), textY, ModStyle.COLOR_TITLE, false);
            }
        }

        graphics.fill(x, lineY, x + width, lineY + 1, mainColor);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int screenHeight, Consumer<String> insert) {
        if (isOverButton(mouseX, mouseY, screenHeight)) {
            toggle();
            return true;
        }
        if (!open) return false;

        if (isOverPanel(mouseX, mouseY, screenHeight)) {
            Emoji emoji = emojiAt(mouseX, mouseY, screenHeight);
            if (emoji != null) {
                insert.accept(emoji.text());
                if (!query.isEmpty()) pickedFromSearch = true;
            }
            return true;
        }

        close();
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY, int screenHeight) {
        if (!isOverPanel(mouseX, mouseY, screenHeight)) return false;
        scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll());
        return true;
    }

    public boolean charTyped(String chars) {
        if (!open) return false;

        StringBuilder next = new StringBuilder(query);
        for (char c : chars.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c == ':') continue;
            next.append(c == ' ' ? '_' : c);
        }
        if (next.length() > MAX_QUERY_LENGTH) next.setLength(MAX_QUERY_LENGTH);
        if (!next.toString().equals(query)) setQuery(next.toString());
        return true;
    }

    public boolean keyPressed(int key, boolean control, Consumer<String> insert) {
        if (!open) return false;

        switch (key) {
            case KEY_ESCAPE -> {
                if (query.isEmpty()) close();
                else setQuery("");
                return true;
            }
            case KEY_BACKSPACE -> {
                if (!query.isEmpty()) setQuery(control ? "" : query.substring(0, query.length() - 1));
                return true;
            }
            case KEY_ENTER, KEY_KP_ENTER -> {
                if (query.isEmpty() || pickedFromSearch) return false;
                if (!results.isEmpty()) insert.accept(results.getFirst().text());
                setQuery("");
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private static void roundedBox(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int outline) {
        for (int y = y1; y < y2; y++) {
            int outer = inset(y - y1, y2 - 1 - y, EmojiPicker.RADIUS);
            if (y == y1 || y == y2 - 1) {
                graphics.fill(x1 + outer, y, x2 - outer, y + 1, outline);
                continue;
            }

            int inner = Math.max(outer + 1, inset(y - y1 - 1, y2 - 2 - y, EmojiPicker.RADIUS - 1) + 1);
            graphics.fill(x1 + outer, y, x1 + inner, y + 1, outline);
            graphics.fill(x2 - inner, y, x2 - outer, y + 1, outline);
            graphics.fill(x1 + inner, y, x2 - inner, y + 1, ModStyle.COLOR_BACKGROUND);
        }
    }

    private static int inset(int fromTop, int fromBottom, int radius) {
        int row = Math.min(fromTop, fromBottom);
        if (row >= radius) return 0;
        double dy = radius - row - 0.5;
        return (int) Math.round(radius - Math.sqrt(radius * radius - dy * dy));
    }
}
