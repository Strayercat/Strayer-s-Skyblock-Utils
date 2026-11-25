package com.skyblockutils.features.inventory;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class PopupMenu {
    public record Item(Component label, Runnable action, boolean enabled) {
        public static Item of(String label, Runnable action) {
            return new Item(ButtonText.parse(label), action, true);
        }

        public static Item of(String label, Runnable action, boolean enabled) {
            return new Item(ButtonText.parse(label), action, enabled);
        }

        public static Item separator() {
            return new Item(null, null, false);
        }

        boolean isSeparator() {
            return label == null;
        }
    }

    private static final int ROW = 12;
    private static final int SEPARATOR = 5;
    private static final int PAD = 4;

    private final List<Item> items;
    private final Component title;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int contentHeight;
    private int scroll;

    private PopupMenu(Component title, List<Item> items, int x, int y, int width, int height, int contentHeight) {
        this.title = title;
        this.items = items;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.contentHeight = contentHeight;
    }

    public static PopupMenu open(Font font, Component title, List<Item> items, int mouseX, int mouseY, int screenW, int screenH) {
        int width = title == null ? 0 : font.width(title);
        int content = 0;
        for (Item item : items) {
            if (item.isSeparator()) {
                content += SEPARATOR;
            } else {
                width = Math.max(width, font.width(item.label()));
                content += ROW;
            }
        }
        width += PAD * 2 + 4;
        int header = title == null ? 0 : ROW + 2;
        int height = Math.min(content + header + 2, screenH - 8);
        int x = Math.clamp(mouseX, 2, Math.max(2, screenW - width - 2));
        int y = Math.clamp(mouseY, 2, Math.max(2, screenH - height - 2));
        return new PopupMenu(title, items, x, y, width, height, content);
    }

    private int header() {
        return title == null ? 0 : ROW + 2;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - (height - header() - 2));
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private Item itemAt(double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY) || mouseY < y + header() + 1) return null;
        int cursor = y + header() + 1 - scroll;
        for (Item item : items) {
            int h = item.isSeparator() ? SEPARATOR : ROW;
            if (mouseY >= cursor && mouseY < cursor + h) return item.isSeparator() ? null : item;
            cursor += h;
        }
        return null;
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        graphics.fill(x, y, x + width, y + height, 0xF0101010);
        Draw.border(graphics, x, y, width, height, 0xFF5A5A5A);
        if (title != null) {
            graphics.text(font, title, x + PAD, y + 3, 0xFFFFFF55, true);
            graphics.fill(x + 1, y + ROW + 1, x + width - 1, y + ROW + 2, 0xFF3A3A3A);
        }
        int top = y + header() + 1;
        graphics.enableScissor(x + 1, top, x + width - 1, y + height - 1);
        Item hovered = itemAt(mouseX, mouseY);
        int cursor = top - scroll;
        for (Item item : items) {
            if (item.isSeparator()) {
                graphics.fill(x + 3, cursor + 2, x + width - 3, cursor + 3, 0xFF3A3A3A);
                cursor += SEPARATOR;
                continue;
            }
            if (item == hovered && item.enabled()) graphics.fill(x + 1, cursor, x + width - 1, cursor + ROW, 0x40FFFFFF);
            graphics.text(font, item.label(), x + PAD, cursor + 2, item.enabled() ? 0xFFFFFFFF : 0xFF666666, item.enabled());
            cursor += ROW;
        }
        graphics.disableScissor();
        if (maxScroll() > 0) {
            int track = height - header() - 2;
            int thumb = Math.max(6, track * track / contentHeight);
            int thumbY = top + (track - thumb) * scroll / maxScroll();
            graphics.fill(x + width - 3, thumbY, x + width - 1, thumbY + thumb, 0xA0FFFFFF);
        }
    }

    public boolean click(double mouseX, double mouseY) {
        Item item = itemAt(mouseX, mouseY);
        if (item == null || !item.enabled()) return false;
        item.action().run();
        return true;
    }

    public void scroll(double amount) {
        scroll = Math.clamp(scroll - (long) (int) Math.signum(amount) * ROW, 0, maxScroll());
    }

    public void scrollTo(int index) {
        scroll = Math.clamp((long) index * ROW - (height - header()) / 2, 0, maxScroll());
    }
}
