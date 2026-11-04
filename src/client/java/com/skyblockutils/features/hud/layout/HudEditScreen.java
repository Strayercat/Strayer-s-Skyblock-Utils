package com.skyblockutils.features.hud.layout;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.ModStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HudEditScreen extends Screen {
    public static boolean requested = false;

    private static final float SNAP_DISTANCE = 4f;
    private static final float EDGE_GRAB = 3f;
    private static final float SCALE_STEP = 0.05f;
    private static final float FINE_SCALE_STEP = 0.01f;
    private static final int OUTLINE_IDLE = 0x80FFFFFF;
    private static final int GUIDE_COLOR = 0x60FFFFFF;

    private final Screen parent;

    private HudElement dragging;
    private float grabOffsetX;
    private float grabOffsetY;

    private HudElement resizing;
    private boolean resizeFromLeft;
    private float fixedEdge;

    private boolean snappedCenterX;
    private boolean snappedCenterY;

    public HudEditScreen(Screen parent) {
        super(Component.literal("SSU HUD Editor"));
        this.parent = parent;
    }

    public static void handleRequest(Minecraft client) {
        if (!requested) return;
        requested = false;
        client.setScreenAndShow(new HudEditScreen(client.gui.screen()));
    }

    @Override
    protected void init() {
        int buttonWidth = 90;
        int y = this.height / 2 + 6;
        this.addRenderableWidget(Button.builder(Component.literal("Reset All"), _ -> HudManager.resetAll())
                .bounds(this.width / 2 - buttonWidth - 2, y, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), _ -> this.onClose())
                .bounds(this.width / 2 + 2, y, buttonWidth, 20).build());
    }

    @Override
    public void extractBackground(final @NotNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        if (this.minecraft.level == null) {
            super.extractBackground(graphics, mouseX, mouseY, a);
            return;
        }
        graphics.fill(0, 0, this.width, this.height, 0x50000000);
    }

    @Override
    public void extractRenderState(final @NotNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        if (dragging != null && snappedCenterX) graphics.fill(this.width / 2, 0, this.width / 2 + 1, this.height, GUIDE_COLOR);
        if (dragging != null && snappedCenterY) graphics.fill(0, this.height / 2, this.width, this.height / 2 + 1, GUIDE_COLOR);

        int accent = 0xFF000000 | ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
        HudElement hovered = active() != null ? active() : elementAt(mouseX, mouseY);

        for (HudElement element : HudManager.elements()) {
            HudManager.Bounds bounds = HudManager.draw(element, graphics, true);
            boolean hot = element == hovered;
            outline(graphics, bounds, hot ? accent : OUTLINE_IDLE);
            if (hot) drawHandles(graphics, bounds, accent);
        }

        if (hovered != null) drawLabel(graphics, hovered, HudManager.bounds(hovered, true));

        String help = "Drag to move • Scroll to scale (Shift: fine) • Drag sides to resize";
        String help2 = "Double click: auto width • Right click: reset • Shift while dragging: no snap";
        graphics.centeredText(this.font, help, this.width / 2, this.height / 2 - 22, 0xFFFFFFFF);
        graphics.centeredText(this.font, help2, this.width / 2, this.height / 2 - 11, 0xFFAAAAAA);

        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private HudElement active() {
        return dragging != null ? dragging : resizing;
    }

    private void outline(GuiGraphicsExtractor graphics, HudManager.Bounds b, int color) {
        int x1 = Mth.floor(b.x());
        int y1 = Mth.floor(b.y());
        int x2 = Mth.ceil(b.right());
        int y2 = Mth.ceil(b.bottom());
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }

    private void drawHandles(GuiGraphicsExtractor graphics, HudManager.Bounds b, int color) {
        int midY = Mth.floor(b.y() + b.scaledHeight() / 2f);
        int handleHalf = Math.min(6, Mth.floor(b.scaledHeight() / 2f));
        int x1 = Mth.floor(b.x());
        int x2 = Mth.ceil(b.right());
        graphics.fill(x1, midY - handleHalf, x1 + 2, midY + handleHalf, color);
        graphics.fill(x2 - 2, midY - handleHalf, x2, midY + handleHalf, color);
    }

    private void drawLabel(GuiGraphicsExtractor graphics, HudElement element, HudManager.Bounds b) {
        HudLayoutEntry layout = HudManager.layout(element);
        String label = (element.isEnabled() ? "" : "§7") + element.name
                + (element.isEnabled() ? "" : " (disabled)")
                + " §7" + Math.round(b.scale() * 100) + "%"
                + (layout.width > 0 ? " • " + layout.width + "px" : "");
        int textWidth = this.font.width(label);
        int x = Mth.clamp(Mth.floor(b.x()), 2, Math.max(2, this.width - textWidth - 2));
        int y = b.bottom() + 12 <= this.height ? Mth.ceil(b.bottom()) + 2 : Mth.floor(b.y()) - 11;
        if (y < 0) y = Mth.floor(b.y()) + 2;
        graphics.fill(x - 2, y - 1, x + textWidth + 2, y + 10, 0xC0000000);
        graphics.text(this.font, label, x, y, 0xFFFFFFFF, true);
    }

    private HudElement elementAt(double mouseX, double mouseY) {
        List<HudElement> elements = HudManager.elements();
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement element = elements.get(i);
            if (HudManager.bounds(element, true).contains(mouseX, mouseY)) return element;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(final @NotNull MouseButtonEvent event, final boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;

        HudElement hit = elementAt(event.x(), event.y());
        if (hit == null) return false;

        if (event.button() == 1) {
            HudManager.reset(hit);
            return true;
        }

        if (event.button() != 0) return false;

        if (doubleClick) {
            HudManager.editable(hit).width = 0;
            return true;
        }

        HudManager.Bounds b = HudManager.bounds(hit, true);
        if (event.x() - b.x() <= EDGE_GRAB) {
            resizing = hit;
            resizeFromLeft = true;
            fixedEdge = b.right();
        } else if (b.right() - event.x() <= EDGE_GRAB) {
            resizing = hit;
            resizeFromLeft = false;
            fixedEdge = b.x();
        } else {
            dragging = hit;
            grabOffsetX = (float) (event.x() - b.x());
            grabOffsetY = (float) (event.y() - b.y());
        }
        return true;
    }

    @Override
    public boolean mouseDragged(final @NotNull MouseButtonEvent event, final double dragX, final double dragY) {
        if (dragging != null) {
            HudManager.Bounds b = HudManager.bounds(dragging, true);
            boolean snap = !this.minecraft.hasShiftDown();
            HudLayoutEntry layout = HudManager.editable(dragging);
            snappedCenterX = applyAxis(layout, true, (float) event.x() - grabOffsetX, b.scaledWidth(), this.width, snap);
            snappedCenterY = applyAxis(layout, false, (float) event.y() - grabOffsetY, b.scaledHeight(), this.height, snap);
            return true;
        }

        if (resizing != null) {
            HudManager.Bounds b = HudManager.bounds(resizing, true);
            float span = resizeFromLeft ? fixedEdge - (float) event.x() : (float) event.x() - fixedEdge;
            int newWidth = Math.round(span / b.scale());
            HudLayoutEntry layout = HudManager.editable(resizing);
            layout.width = Math.clamp(newWidth, resizing.minWidth(), Math.max(resizing.minWidth(), resizing.maxWidth()));

            HudManager.Bounds resized = HudManager.bounds(resizing, true);
            float x = resizeFromLeft ? fixedEdge - resized.scaledWidth() : fixedEdge;
            applyAxis(layout, true, x, resized.scaledWidth(), this.width, false);
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final @NotNull MouseButtonEvent event) {
        dragging = null;
        resizing = null;
        snappedCenterX = false;
        snappedCenterY = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        HudElement hit = active() != null ? active() : elementAt(x, y);
        if (hit == null || scrollY == 0) return false;

        HudManager.Bounds b = HudManager.bounds(hit, true);
        float step = this.minecraft.hasShiftDown() ? FINE_SCALE_STEP : SCALE_STEP;
        float scale = b.scale() + (float) Math.signum(scrollY) * step;
        scale = Math.round(scale * 100f) / 100f;

        HudLayoutEntry layout = HudManager.editable(hit);
        layout.scale = Math.clamp(scale, HudManager.MIN_SCALE, HudManager.MAX_SCALE);
        return true;
    }

    private static boolean applyAxis(HudLayoutEntry layout, boolean horizontal, float pos, float size, int screen, boolean snap) {
        float max = Math.max(0f, screen - size);
        float center = max / 2f;
        pos = Math.clamp(pos, 0f, max);

        HudLayoutEntry.Anchor anchor;
        float offset;
        boolean centered = false;

        if (snap && pos <= SNAP_DISTANCE) {
            anchor = HudLayoutEntry.Anchor.START;
            offset = 0f;
        } else if (snap && max - pos <= SNAP_DISTANCE) {
            anchor = HudLayoutEntry.Anchor.END;
            offset = 0f;
        } else if (snap && Math.abs(pos - center) <= SNAP_DISTANCE) {
            anchor = HudLayoutEntry.Anchor.CENTER;
            offset = 0f;
            centered = true;
        } else {
            float mid = pos + size / 2f;
            if (mid < screen / 3f) {
                anchor = HudLayoutEntry.Anchor.START;
                offset = pos;
            } else if (mid > screen * 2f / 3f) {
                anchor = HudLayoutEntry.Anchor.END;
                offset = max - pos;
            } else {
                anchor = HudLayoutEntry.Anchor.CENTER;
                offset = pos - center;
            }
        }

        if (horizontal) {
            layout.anchorX = anchor;
            layout.offsetX = offset;
        } else {
            layout.anchorY = anchor;
            layout.offsetY = offset;
        }
        return centered;
    }

    @Override
    public void removed() {
        ModConfig.save();
    }

    @Override
    public void onClose() {
        if (this.parent == null) {
            super.onClose();
            return;
        }
        this.minecraft.setScreenAndShow(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
