package com.skyblockutils.features.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;

final class Draw {
    private Draw() {
    }

    static void border(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFFC6C6C6);
        border(graphics, x, y, w, h, 0xFF373737);
    }
}
