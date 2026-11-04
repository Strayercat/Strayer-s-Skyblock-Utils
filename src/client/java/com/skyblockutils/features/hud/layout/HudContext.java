package com.skyblockutils.features.hud.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class HudContext {
    private static final float EDGE_EPSILON = 0.5f;

    public final GuiGraphicsExtractor graphics;
    public final Minecraft mc;
    public final Font font;
    public final HudManager.Bounds bounds;
    public final int width;
    public final int height;
    public final boolean preview;

    private final int screenWidth;
    private final int screenHeight;

    public record Corners(boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight) {
    }

    private interface RunSink {
        void accept(int left, int top, int right, int bottom);
    }

    public HudContext(GuiGraphicsExtractor graphics, HudManager.Bounds bounds, boolean preview) {
        this.graphics = graphics;
        this.mc = Minecraft.getInstance();
        this.font = mc.font;
        this.bounds = bounds;
        this.width = bounds.width();
        this.height = bounds.height();
        this.preview = preview;
        this.screenWidth = mc.getWindow().getGuiScaledWidth();
        this.screenHeight = mc.getWindow().getGuiScaledHeight();
    }

    public float screenX(float localX) {
        return bounds.x() + localX * bounds.scale();
    }

    public float screenY(float localY) {
        return bounds.y() + localY * bounds.scale();
    }

    public float scale() {
        return bounds.scale();
    }

    public boolean stuckLeft(float localX) {
        return screenX(localX) <= EDGE_EPSILON;
    }

    public boolean stuckRight(float localX) {
        return screenX(localX) >= screenWidth - EDGE_EPSILON;
    }

    public boolean stuckTop(float localY) {
        return screenY(localY) <= EDGE_EPSILON;
    }

    public boolean stuckBottom(float localY) {
        return screenY(localY) >= screenHeight - EDGE_EPSILON;
    }

    public Corners corners(int x1, int y1, int x2, int y2) {
        boolean left = stuckLeft(x1);
        boolean right = stuckRight(x2);
        boolean top = stuckTop(y1);
        boolean bottom = stuckBottom(y2);
        return new Corners(!(left || top), !(right || top), !(left || bottom), !(right || bottom));
    }

    public int[] inner(int x1, int y1, int x2, int y2) {
        return new int[]{
                x1 + (stuckLeft(x1) ? 0 : 1),
                y1 + (stuckTop(y1) ? 0 : 1),
                x2 - (stuckRight(x2) ? 0 : 1),
                y2 - (stuckBottom(y2) ? 0 : 1)
        };
    }

    public boolean fitsOnScreen(float x1, float y1, float x2, float y2) {
        return screenX(x1) >= -EDGE_EPSILON && screenY(y1) >= -EDGE_EPSILON
                && screenX(x2) <= screenWidth + EDGE_EPSILON && screenY(y2) <= screenHeight + EDGE_EPSILON;
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        if (x2 > x1 && y2 > y1 && (color >>> 24) != 0) graphics.fill(x1, y1, x2, y2, color);
    }

    public void panel(int x1, int y1, int x2, int y2, int radius, int background, int border) {
        Corners corners = corners(x1, y1, x2, y2);
        if ((border >>> 24) == 0) {
            shape(x1, y1, x2, y2, radius, background, corners);
            return;
        }

        int[] in = inner(x1, y1, x2, y2);
        int outerRadius = clampRadius(radius, x2 - x1, y2 - y1);
        int innerRadius = clampRadius(outerRadius - 1, in[2] - in[0], in[3] - in[1]);

        Runs left = new Runs(fillSink(border));
        Runs right = new Runs(fillSink(border));
        Runs middle = new Runs(fillSink(background));

        for (int row = y1; row < y2; row++) {
            int outerLeft = x1 + leftInset(row, y1, y2, outerRadius, corners);
            int outerRight = x2 - rightInset(row, y1, y2, outerRadius, corners);

            if (row < in[1] || row >= in[3]) {
                left.add(row, outerLeft, outerRight);
                right.skip();
                middle.skip();
                continue;
            }

            int innerLeft = Math.max(outerLeft, in[0] + leftInset(row, in[1], in[3], innerRadius, corners));
            int innerRight = Math.max(innerLeft, Math.min(outerRight, in[2] - rightInset(row, in[1], in[3], innerRadius, corners)));

            left.add(row, outerLeft, innerLeft);
            right.add(row, innerRight, outerRight);
            middle.add(row, innerLeft, innerRight);
        }

        left.flush();
        right.flush();
        middle.flush();
    }

    public void shape(int x1, int y1, int x2, int y2, int radius, int color, Corners corners) {
        shapeRows(x1, y1, x2, y2, radius, color, corners, y1, y2);
    }

    public void shapeRows(int x1, int y1, int x2, int y2, int radius, int color, Corners corners, int rowFrom, int rowTo) {
        if ((color >>> 24) == 0) return;
        spans(x1, y1, x2, y2, radius, corners, rowFrom, rowTo, fillSink(color));
    }

    public void overlay(Identifier texture, float x, float y, float size, int textureSize) {
        if (!fitsOnScreen(x, y, x + size, y + size)) return;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, y);
            graphics.pose().scale(size / textureSize, size / textureSize);
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, textureSize, textureSize, textureSize, textureSize);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    public List<FormattedCharSequence> wrap(Component text, int maxWidth) {
        return font.split(text, Math.max(1, maxWidth));
    }

    public void text(FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        graphics.text(font, text, x, y, color, shadow);
    }

    public void text(Component text, int x, int y, int color, boolean shadow) {
        graphics.text(font, text, x, y, color, shadow);
    }

    public void text(String text, int x, int y, int color, boolean shadow) {
        graphics.text(font, text, x, y, color, shadow);
    }

    public void scaledText(FormattedCharSequence text, float x, float y, float textScale, int color, boolean shadow) {
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, y);
            graphics.pose().scale(textScale, textScale);
            graphics.text(font, text, 0, 0, color, shadow);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    public void scaledText(String text, float x, float y, float textScale, int color, boolean shadow) {
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, y);
            graphics.pose().scale(textScale, textScale);
            graphics.text(font, text, 0, 0, color, shadow);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private RunSink fillSink(int color) {
        return (left, top, right, bottom) -> {
            if ((color >>> 24) != 0) graphics.fill(left, top, right, bottom, color);
        };
    }

    private void spans(int x1, int y1, int x2, int y2, int radius, Corners corners, int rowFrom, int rowTo, RunSink sink) {
        int r = clampRadius(radius, x2 - x1, y2 - y1);
        Runs runs = new Runs(sink);
        for (int row = Math.max(y1, rowFrom); row < Math.min(y2, rowTo); row++) {
            runs.add(row, x1 + leftInset(row, y1, y2, r, corners), x2 - rightInset(row, y1, y2, r, corners));
        }
        runs.flush();
    }

    private static int clampRadius(int radius, int w, int h) {
        return Math.max(0, Math.min(radius, Math.min(w, h) / 2));
    }

    private static int leftInset(int row, int top, int bottom, int radius, Corners corners) {
        int inset = 0;
        if (corners.topLeft()) inset = Math.max(inset, arc(row - top, radius));
        if (corners.bottomLeft()) inset = Math.max(inset, arc(bottom - 1 - row, radius));
        return inset;
    }

    private static int rightInset(int row, int top, int bottom, int radius, Corners corners) {
        int inset = 0;
        if (corners.topRight()) inset = Math.max(inset, arc(row - top, radius));
        if (corners.bottomRight()) inset = Math.max(inset, arc(bottom - 1 - row, radius));
        return inset;
    }

    private static int arc(int index, int radius) {
        if (index < 0 || index >= radius) return 0;
        double dy = radius - index - 0.5;
        return (int) Math.round(radius - Math.sqrt(Math.max(0, radius * radius - dy * dy)));
    }

    private static final class Runs {
        private final RunSink sink;
        private boolean pending = false;
        private int startRow;
        private int endRow;
        private int left;
        private int right;

        Runs(RunSink sink) {
            this.sink = sink;
        }

        void add(int row, int l, int r) {
            if (r <= l) {
                skip();
                return;
            }
            if (pending && row == endRow && l == left && r == right) {
                endRow++;
                return;
            }
            flush();
            pending = true;
            startRow = row;
            endRow = row + 1;
            left = l;
            right = r;
        }

        void skip() {
            flush();
        }

        void flush() {
            if (!pending) return;
            pending = false;
            int l = left;
            int r = right;
            if (r > l) sink.accept(l, startRow, r, endRow);
        }
    }
}
