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
    private static final int HIDDEN_OVERLAY = 0xB0000000;
    private static final int DROPDOWN_WIDTH = 184;
    private static final int DROPDOWN_ROW = 12;

    private final Screen parent;

    private HudElement dragging;
    private float grabOffsetX;
    private float grabOffsetY;

    private HudElement resizing;
    private boolean resizeFromLeft;
    private float fixedEdge;

    private boolean snappedCenterX;
    private boolean snappedCenterY;
    private HudManager.Snap snapped;

    private Button locationButton;
    private Button resetButton;
    private List<String> locationOptions = List.of();
    private boolean dropdownOpen;
    private int dropdownScroll;

    public HudEditScreen(Screen parent) {
        super(Component.literal("SSU HUD Editor"));
        this.parent = parent;
        HudManager.setEditLocation(HudManager.currentLocation());
    }

    public static void handleRequest(Minecraft client) {
        if (!requested) return;
        requested = false;
        client.setScreenAndShow(new HudEditScreen(client.gui.screen()));
    }

    @Override
    protected void init() {
        locationOptions = HudLocations.options(HudManager.currentLocation());
        dropdownOpen = false;

        int buttonWidth = 90;
        int y = this.height / 2 + 6;
        locationButton = this.addRenderableWidget(Button.builder(Component.empty(), _ -> toggleDropdown())
                .bounds(this.width / 2 - DROPDOWN_WIDTH / 2, this.height / 2 - 48, DROPDOWN_WIDTH, 20).build());
        resetButton = this.addRenderableWidget(Button.builder(Component.empty(), _ -> HudManager.resetAll())
                .bounds(this.width / 2 - buttonWidth - 2, y, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), _ -> this.onClose())
                .bounds(this.width / 2 + 2, y, buttonWidth, 20).build());
        updateLabels();
    }

    private void updateLabels() {
        String location = HudManager.editLocation();
        locationButton.setMessage(Component.literal("Location: " + HudLocations.name(location) + (dropdownOpen ? " ▲" : " ▼")));
        resetButton.setMessage(Component.literal(location.isEmpty() ? "Reset All" : "Reset Location"));
    }

    private void toggleDropdown() {
        dropdownOpen = !dropdownOpen;
        if (dropdownOpen) {
            int index = locationOptions.indexOf(HudManager.editLocation());
            dropdownScroll = Math.clamp(index - visibleRows() / 2, 0, maxScroll());
        }
        updateLabels();
    }

    private void selectLocation(String location) {
        HudManager.setEditLocation(location);
        dropdownOpen = false;
        dragging = null;
        resizing = null;
        updateLabels();
    }

    private int dropdownTop() {
        return locationButton.getY() + locationButton.getHeight();
    }

    private int visibleRows() {
        int available = (this.height - dropdownTop() - 4) / DROPDOWN_ROW;
        return Math.max(1, Math.min(locationOptions.size(), available));
    }

    private int maxScroll() {
        return Math.max(0, locationOptions.size() - visibleRows());
    }

    private boolean overDropdown(double mouseX, double mouseY) {
        int x = locationButton.getX();
        int top = dropdownTop();
        return mouseX >= x && mouseX < x + DROPDOWN_WIDTH && mouseY >= top && mouseY < top + visibleRows() * DROPDOWN_ROW;
    }

    private void drawDropdown(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int accent) {
        int x = locationButton.getX();
        int top = dropdownTop();
        int rows = visibleRows();
        graphics.fill(x, top, x + DROPDOWN_WIDTH, top + rows * DROPDOWN_ROW, 0xF0101010);
        outlineRect(graphics, x, top, x + DROPDOWN_WIDTH, top + rows * DROPDOWN_ROW, accent);

        for (int i = 0; i < rows; i++) {
            int index = dropdownScroll + i;
            if (index >= locationOptions.size()) break;
            String option = locationOptions.get(index);
            int y = top + i * DROPDOWN_ROW;
            boolean hovered = mouseX >= x && mouseX < x + DROPDOWN_WIDTH && mouseY >= y && mouseY < y + DROPDOWN_ROW;
            if (hovered) graphics.fill(x + 1, y, x + DROPDOWN_WIDTH - 1, y + DROPDOWN_ROW, 0x40FFFFFF);

            boolean selected = option.equals(HudManager.editLocation());
            boolean current = option.equals(HudManager.currentLocation()) && !option.isEmpty();
            String label = HudLocations.name(option) + (current ? " §7(current)" : "");
            graphics.text(this.font, label, x + 4, y + 2, selected ? accent : 0xFFFFFFFF, true);
        }

        if (maxScroll() > 0) {
            int trackHeight = rows * DROPDOWN_ROW;
            int thumbHeight = Math.max(6, trackHeight * rows / locationOptions.size());
            int thumbY = top + (trackHeight - thumbHeight) * dropdownScroll / maxScroll();
            graphics.fill(x + DROPDOWN_WIDTH - 3, thumbY, x + DROPDOWN_WIDTH - 1, thumbY + thumbHeight, 0xA0FFFFFF);
        }
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
        HudElement hovered = active() != null ? active() : dropdownOpen ? null : elementAt(mouseX, mouseY);

        for (HudElement element : HudManager.elements()) {
            HudManager.Bounds bounds = HudManager.draw(element, graphics, true);
            if (!HudManager.isVisible(element, HudManager.editLocation())) {
                graphics.fill(Mth.floor(bounds.x()), Mth.floor(bounds.y()), Mth.ceil(bounds.right()), Mth.ceil(bounds.bottom()), HIDDEN_OVERLAY);
            }
            boolean hot = element == hovered || (snapped != null && element == snapped.parent());
            outline(graphics, bounds, hot ? accent : OUTLINE_IDLE);
            if (hot) drawHandles(graphics, bounds, accent);
        }

        if (hovered != null) drawLabel(graphics, hovered, HudManager.bounds(hovered, true));

        String help = "Drag to move • Scroll to scale (Shift: fine) • Drag sides to resize";
        String help2 = "Double click: auto width • Right click: reset • Middle click: show/hide here • Shift: no snap/unstick";
        graphics.centeredText(this.font, help, this.width / 2, this.height / 2 - 22, 0xFFFFFFFF);
        graphics.centeredText(this.font, help2, this.width / 2, this.height / 2 - 11, 0xFFAAAAAA);

        super.extractRenderState(graphics, mouseX, mouseY, a);
        if (dropdownOpen) drawDropdown(graphics, mouseX, mouseY, accent);
    }

    private HudElement active() {
        return dragging != null ? dragging : resizing;
    }

    private void outline(GuiGraphicsExtractor graphics, HudManager.Bounds b, int color) {
        outlineRect(graphics, Mth.floor(b.x()), Mth.floor(b.y()), Mth.ceil(b.right()), Mth.ceil(b.bottom()), color);
    }

    private void outlineRect(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
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
        String location = HudManager.editLocation();
        boolean visible = HudManager.isVisible(element, location);
        String label = (element.isEnabled() ? "" : "§7") + element.name
                + (element.isEnabled() ? "" : " (disabled)")
                + " §7" + Math.round(b.scale() * 100) + "%"
                + (layout.width > 0 ? " • " + layout.width + "px" : "")
                + (!location.isEmpty() ? (HudManager.hasOverride(element, location) ? " • custom" : " • default") : "")
                + (HudManager.parentOf(element) != null ? " • stuck to " + HudManager.parentOf(element).name : "")
                + (visible ? "" : " • §chidden");
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
        if (dropdownOpen) {
            if (overDropdown(event.x(), event.y())) {
                int index = dropdownScroll + (int) ((event.y() - dropdownTop()) / DROPDOWN_ROW);
                if (index >= 0 && index < locationOptions.size()) selectLocation(locationOptions.get(index));
                return true;
            }
            if (!locationButton.isMouseOver(event.x(), event.y())) {
                dropdownOpen = false;
                updateLabels();
                return true;
            }
        }

        if (super.mouseClicked(event, doubleClick)) return true;

        HudElement hit = elementAt(event.x(), event.y());
        if (hit == null) return false;

        if (event.button() == 1) {
            HudManager.reset(hit);
            return true;
        }

        if (event.button() == 2) {
            HudManager.toggleVisible(hit);
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
            float x = (float) event.x() - grabOffsetX;
            float y = (float) event.y() - grabOffsetY;

            snapped = snap ? HudManager.findSnap(dragging, x, y, b.scaledWidth(), b.scaledHeight()) : null;
            if (snapped != null) {
                applyAxis(layout, true, snapped.x(), b.scaledWidth(), this.width, false);
                applyAxis(layout, false, snapped.y(), b.scaledHeight(), this.height, false);
                layout.attachTo = snapped.parent().id;
                layout.attachSide = snapped.side();
                layout.attachOffset = snapped.offset();
                snappedCenterX = false;
                snappedCenterY = false;
                return true;
            }

            layout.detach();
            snappedCenterX = applyAxis(layout, true, x, b.scaledWidth(), this.width, snap);
            snappedCenterY = applyAxis(layout, false, y, b.scaledHeight(), this.height, snap);
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
            HudElement parent = HudManager.parentOf(resizing);
            if (parent != null && (layout.attachSide == HudLayoutEntry.Side.ABOVE || layout.attachSide == HudLayoutEntry.Side.BELOW)) {
                layout.attachOffset = x - HudManager.bounds(parent, true).x();
            }
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final @NotNull MouseButtonEvent event) {
        dragging = null;
        resizing = null;
        snapped = null;
        snappedCenterX = false;
        snappedCenterY = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        if (dropdownOpen) {
            if (overDropdown(x, y) && scrollY != 0) dropdownScroll = Math.clamp(dropdownScroll - (int) Math.signum(scrollY), 0, maxScroll());
            return true;
        }

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
