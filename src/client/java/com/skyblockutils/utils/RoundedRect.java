package com.skyblockutils.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Vector2f;

public final class RoundedRect {
    private static final float EDGE_EPSILON = 0.5f;

    public record Edges(boolean left, boolean top, boolean right, boolean bottom) {
        public static final Edges NONE = new Edges(false, false, false, false);

        public Corners corners() {
            return new Corners(!(left || top), !(right || top), !(left || bottom), !(right || bottom));
        }
    }

    public record Corners(boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight) {
    }

    private interface RunSink {
        void accept(int left, int top, int right, int bottom);
    }

    private RoundedRect() {
    }

    public static Edges edgesOf(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        Vector2f topLeft = graphics.pose().transformPosition(x1, y1, new Vector2f());
        Vector2f bottomRight = graphics.pose().transformPosition(x2, y2, new Vector2f());
        return new Edges(
                Math.min(topLeft.x, bottomRight.x) <= EDGE_EPSILON,
                Math.min(topLeft.y, bottomRight.y) <= EDGE_EPSILON,
                Math.max(topLeft.x, bottomRight.x) >= screenWidth - EDGE_EPSILON,
                Math.max(topLeft.y, bottomRight.y) >= screenHeight - EDGE_EPSILON
        );
    }

    public static int[] inner(int x1, int y1, int x2, int y2, Edges edges) {
        return new int[]{
                x1 + (edges.left() ? 0 : 1),
                y1 + (edges.top() ? 0 : 1),
                x2 - (edges.right() ? 0 : 1),
                y2 - (edges.bottom() ? 0 : 1)
        };
    }

    public static void panel(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int radius, int background, int border) {
        panel(graphics, x1, y1, x2, y2, radius, background, border, edgesOf(graphics, x1, y1, x2, y2));
    }

    public static void panel(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int radius, int background, int border, Edges edges) {
        if (x2 <= x1 || y2 <= y1) return;
        Corners corners = edges.corners();
        if ((border >>> 24) == 0) {
            shapeRows(graphics, x1, y1, x2, y2, radius, background, corners, y1, y2);
            return;
        }

        int[] in = inner(x1, y1, x2, y2, edges);
        int outerRadius = clampRadius(radius, x2 - x1, y2 - y1);
        int innerRadius = clampRadius(outerRadius - 1, in[2] - in[0], in[3] - in[1]);

        Runs left = new Runs(fillSink(graphics, border));
        Runs right = new Runs(fillSink(graphics, border));
        Runs middle = new Runs(fillSink(graphics, background));

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

    public static void shapeRows(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int radius, int color, Corners corners, int rowFrom, int rowTo) {
        if ((color >>> 24) == 0) return;
        int r = clampRadius(radius, x2 - x1, y2 - y1);
        Runs runs = new Runs(fillSink(graphics, color));
        for (int row = Math.max(y1, rowFrom); row < Math.min(y2, rowTo); row++) {
            runs.add(row, x1 + leftInset(row, y1, y2, r, corners), x2 - rightInset(row, y1, y2, r, corners));
        }
        runs.flush();
    }

    private static RunSink fillSink(GuiGraphicsExtractor graphics, int color) {
        return (left, top, right, bottom) -> {
            if ((color >>> 24) != 0) graphics.fill(left, top, right, bottom, color);
        };
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
            if (right > left) sink.accept(left, startRow, right, endRow);
        }
    }
}
