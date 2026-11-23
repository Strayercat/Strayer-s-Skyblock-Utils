package com.skyblockutils.features.hud.layout;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.ModStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HudEditScreen extends Screen {
    private static final float SNAP_DISTANCE = 4f;
    private static final float EDGE_GRAB = 3f;
    private static final float SCALE_STEP = 0.05f;
    private static final float FINE_SCALE_STEP = 0.01f;
    private static final int OUTLINE_IDLE = 0x80FFFFFF;
    private static final int GUIDE_COLOR = 0x60FFFFFF;
    private static final int HELP_COLOR = 0x70FFFFFF;
    private static final int HELP_TITLE_COLOR = 0x90FFFFFF;
    private static final int PANEL_COLOR = 0xF0101010;
    private static final int HOVER_COLOR = 0x40FFFFFF;
    private static final int DISABLED_COLOR = 0xFF666666;
    private static final int ROW = 12;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BOTTOM_MARGIN = 6;
    private static final int DROPDOWN_WIDTH = 170;
    private static final int SMALL_BUTTON_WIDTH = 90;
    private static final float MENU_MAX_HEIGHT = 0.6f;
    private static final int MENU_SCROLL_STEP = ROW * 2;
    private static final int SCROLLBAR_SPACE = 6;
    private static final int MENU_HEADER = ROW + 3;
    private static final double MENU_SCROLL_SPEED = 18;

    private static final String[] HELP = {
            "Left drag: move  •  Drag the left or right edge: resize",
            "Scroll: scale  •  Shift + scroll: fine scale  •  Double click: auto width",
            "Drag next to another HUD to stick them together  •  Hold Shift to move freely",
            "Right click a HUD: options  •  Middle click a HUD: hide it here",
            "Right click empty space: bring back hidden HUDs",
            "Each location has its own layout. HUDs you haven't changed there use All Locations."
    };

    private record MenuItem(String label, Runnable action, boolean enabled) {
        static MenuItem separator() {
            return new MenuItem(null, null, false);
        }

        boolean isSeparator() {
            return label == null;
        }
    }

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

    private String menuTitle;
    private List<MenuItem> menuItems;
    private int menuX;
    private int menuY;
    private int menuWidth;
    private int menuScrollTarget;
    private double menuScroll;
    private long menuScrollNanos;

    public HudEditScreen(Screen parent) {
        super(Component.literal("SSU HUD Editor"));
        this.parent = parent;
        HudManager.setEditLocation(HudManager.currentLocation());
    }

    @Override
    protected void init() {
        locationOptions = HudLocations.options(HudManager.currentLocation());
        dropdownOpen = false;
        closeMenu();

        int rowWidth = DROPDOWN_WIDTH + SMALL_BUTTON_WIDTH * 2 + 8;
        int x = this.width / 2 - rowWidth / 2;
        int y = this.height - BUTTON_HEIGHT - BOTTOM_MARGIN;
        locationButton = this.addRenderableWidget(Button.builder(Component.empty(), _ -> toggleDropdown())
                .bounds(x, y, DROPDOWN_WIDTH, BUTTON_HEIGHT).build());
        resetButton = this.addRenderableWidget(Button.builder(Component.empty(), _ -> HudManager.resetAll())
                .bounds(x + DROPDOWN_WIDTH + 4, y, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), _ -> this.onClose())
                .bounds(x + DROPDOWN_WIDTH + SMALL_BUTTON_WIDTH + 8, y, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        updateLabels();
    }

    private void updateLabels() {
        String location = HudManager.editLocation();
        locationButton.setMessage(Component.literal("Location: " + HudLocations.name(location) + (dropdownOpen ? " ▼" : " ▲")));
        resetButton.setMessage(Component.literal(location.isEmpty() ? "Reset All" : "Reset Location"));
    }

    private String locationName() {
        return HudLocations.name(HudManager.editLocation());
    }

    private List<HudElement> shown() {
        List<HudElement> list = new ArrayList<>();
        for (HudElement element : HudManager.elements()) {
            if (HudManager.isShownInEditor(element)) list.add(element);
        }
        return list;
    }

    private List<HudElement> hidden() {
        List<HudElement> list = new ArrayList<>();
        for (HudElement element : HudManager.elements()) {
            if (element.isEnabled() && !HudManager.isVisible(element, HudManager.editLocation())) list.add(element);
        }
        return list;
    }

    private void toggleDropdown() {
        closeMenu();
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

    private int visibleRows() {
        int available = (locationButton.getY() - 4) / ROW;
        return Math.max(1, Math.min(locationOptions.size(), available));
    }

    private int dropdownTop() {
        return locationButton.getY() - visibleRows() * ROW;
    }

    private int maxScroll() {
        return Math.max(0, locationOptions.size() - visibleRows());
    }

    private boolean overDropdown(double mouseX, double mouseY) {
        int x = locationButton.getX();
        int top = dropdownTop();
        return mouseX >= x && mouseX < x + DROPDOWN_WIDTH && mouseY >= top && mouseY < locationButton.getY();
    }

    private void drawDropdown(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int accent) {
        int x = locationButton.getX();
        int top = dropdownTop();
        int rows = visibleRows();
        graphics.fill(x, top, x + DROPDOWN_WIDTH, top + rows * ROW, PANEL_COLOR);
        outlineRect(graphics, x, top, x + DROPDOWN_WIDTH, top + rows * ROW, accent);

        for (int i = 0; i < rows; i++) {
            int index = dropdownScroll + i;
            if (index >= locationOptions.size()) break;
            String option = locationOptions.get(index);
            int y = top + i * ROW;
            if (mouseX >= x && mouseX < x + DROPDOWN_WIDTH && mouseY >= y && mouseY < y + ROW) {
                graphics.fill(x + 1, y, x + DROPDOWN_WIDTH - 1, y + ROW, HOVER_COLOR);
            }
            boolean selected = option.equals(HudManager.editLocation());
            boolean current = option.equals(HudManager.currentLocation()) && !option.isEmpty();
            String label = HudLocations.name(option) + (current ? " §7(current)" : "");
            graphics.text(this.font, label, x + 4, y + 2, selected ? accent : 0xFFFFFFFF, true);
        }

        if (maxScroll() > 0) {
            int trackHeight = rows * ROW;
            int thumbHeight = Math.max(6, trackHeight * rows / locationOptions.size());
            int thumbY = top + (trackHeight - thumbHeight) * dropdownScroll / maxScroll();
            graphics.fill(x + DROPDOWN_WIDTH - 3, thumbY, x + DROPDOWN_WIDTH - 1, thumbY + thumbHeight, 0xA0FFFFFF);
        }
    }

    private void openMenu(String title, List<MenuItem> items, double mouseX, double mouseY) {
        dropdownOpen = false;
        updateLabels();
        menuTitle = title;
        menuItems = items;
        menuScrollTarget = 0;
        menuScroll = 0;
        int width = this.font.width(title);
        for (MenuItem item : items) {
            if (!item.isSeparator()) width = Math.max(width, this.font.width(item.label()));
        }
        menuWidth = width + 12 + SCROLLBAR_SPACE;
        menuX = (int) mouseX;
        menuY = (int) mouseY;
        clampMenu();
    }

    private void clampMenu() {
        menuX = Mth.clamp(menuX, 2, Math.max(2, this.width - menuWidth - 2));
        menuY = Mth.clamp(menuY, 2, Math.max(2, this.height - menuHeight() - 2));
        menuScrollTarget = Mth.clamp(menuScrollTarget, 0, maxMenuScroll());
        menuScroll = Mth.clamp(menuScroll, 0, maxMenuScroll());
    }

    private void closeMenu() {
        menuItems = null;
        menuTitle = null;
    }

    private boolean menuOpen() {
        return menuItems != null;
    }

    private int itemHeight(MenuItem item) {
        return item.isSeparator() ? 5 : ROW;
    }

    private int contentHeight() {
        int height = 0;
        for (MenuItem item : menuItems) height += itemHeight(item);
        return height;
    }

    private int visibleContentHeight() {
        int max = Math.max(ROW, (int) (this.height * MENU_MAX_HEIGHT) - MENU_HEADER - 2);
        return Math.min(contentHeight(), max);
    }

    private int maxMenuScroll() {
        return Math.max(0, contentHeight() - visibleContentHeight());
    }

    private int menuHeight() {
        return MENU_HEADER + visibleContentHeight() + 2;
    }

    private MenuItem menuItemAt(double mouseX, double mouseY) {
        if (mouseX < menuX || mouseX >= menuX + menuWidth) return null;
        int top = menuY + MENU_HEADER;
        if (mouseY < top || mouseY >= top + visibleContentHeight()) return null;
        double localY = mouseY - top + Math.round(menuScroll);
        int y = 0;
        for (MenuItem item : menuItems) {
            int h = itemHeight(item);
            if (localY >= y && localY < y + h) return item.isSeparator() ? null : item;
            y += h;
        }
        return null;
    }

    private void animateMenuScroll() {
        long now = System.nanoTime();
        double dt = menuScrollNanos == 0 ? 0 : Math.min(0.1, (now - menuScrollNanos) / 1e9);
        menuScrollNanos = now;
        double diff = menuScrollTarget - menuScroll;
        if (Math.abs(diff) < 0.5) menuScroll = menuScrollTarget;
        else menuScroll += diff * (1 - Math.exp(-dt * MENU_SCROLL_SPEED));
    }

    private boolean overMenu(double mouseX, double mouseY) {
        return mouseX >= menuX && mouseX < menuX + menuWidth && mouseY >= menuY && mouseY < menuY + menuHeight();
    }

    private void drawMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int accent) {
        animateMenuScroll();
        int height = menuHeight();
        int right = menuX + menuWidth;
        MenuItem hovered = menuItemAt(mouseX, mouseY);
        graphics.fill(menuX, menuY, right, menuY + height, PANEL_COLOR);
        outlineRect(graphics, menuX, menuY, right, menuY + height, accent);
        graphics.text(this.font, menuTitle, menuX + 6, menuY + 3, accent, true);
        graphics.fill(menuX + 1, menuY + ROW + 1, right - 1, menuY + ROW + 2, 0x60FFFFFF);

        int top = menuY + MENU_HEADER;
        int visible = visibleContentHeight();
        int maxScroll = maxMenuScroll();
        int itemRight = maxScroll > 0 ? right - SCROLLBAR_SPACE : right;
        graphics.enableScissor(menuX + 1, top, right - 1, top + visible);
        int y = top - (int) Math.round(menuScroll);
        for (MenuItem item : menuItems) {
            int h = itemHeight(item);
            if (y + h > top && y < top + visible) {
                if (item.isSeparator()) {
                    graphics.fill(menuX + 4, y + 2, itemRight - 4, y + 3, 0x30FFFFFF);
                } else {
                    if (item == hovered && item.enabled()) graphics.fill(menuX + 1, y, itemRight - 1, y + ROW, HOVER_COLOR);
                    graphics.text(this.font, item.label(), menuX + 6, y + 2, item.enabled() ? 0xFFFFFFFF : DISABLED_COLOR, true);
                }
            }
            y += h;
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int total = contentHeight();
            int thumbHeight = Math.max(6, visible * visible / total);
            int thumbY = top + (int) Math.round((visible - thumbHeight) * menuScroll / maxScroll);
            graphics.fill(right - 4, top, right - 2, top + visible, 0x30FFFFFF);
            graphics.fill(right - 4, thumbY, right - 2, thumbY + thumbHeight, 0xA0FFFFFF);
        }
    }

    private void openElementMenu(HudElement element, double mouseX, double mouseY) {
        HudLayoutEntry layout = HudManager.layout(element);
        String location = HudManager.editLocation();
        boolean inLocation = !location.isEmpty();
        HudElement parent = HudManager.parentOf(element);

        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem(inLocation ? "Hide in " + locationName() : "Hide in all locations", () -> HudManager.setVisible(element, false), true));
        List<MenuItem> options = new ArrayList<>();
        for (HudElement.Option option : element.options()) {
            if (!option.available().getAsBoolean()) continue;
            boolean enabled = HudManager.optionEnabled(element, option);
            options.add(new MenuItem((enabled ? "§a✔ §r" : "§8✘ §7") + option.label() + (option.global() ? " §8(all locations)" : ""), () -> HudManager.toggleOption(element, option), true));
        }
        if (!options.isEmpty()) {
            items.add(MenuItem.separator());
            items.addAll(options);
        }
        items.add(MenuItem.separator());
        items.add(new MenuItem("Reset position", () -> HudManager.resetPosition(element), true));
        items.add(new MenuItem("Reset size", () -> HudManager.resetSize(element), !HudManager.isSizeDefault(element)));
        items.add(new MenuItem("Auto width", () -> HudManager.editable(element).width = 0, layout.width > 0));
        items.add(new MenuItem("Scale to 100%", () -> HudManager.editable(element).scale = 1f, layout.scale != 1f));
        items.add(MenuItem.separator());
        items.add(new MenuItem("Center horizontally", () -> center(element, true), true));
        items.add(new MenuItem("Center vertically", () -> center(element, false), true));
        items.add(new MenuItem(parent != null ? "Unstick from " + parent.name : "Unstick", () -> unstick(element), parent != null));
        items.add(new MenuItem(layout.squareCorners ? "Use rounded corners" : "Use square corners", () -> HudManager.editable(element).squareCorners = !layout.squareCorners, true));
        if (inLocation) {
            items.add(MenuItem.separator());
            items.add(new MenuItem("Use the All Locations layout", () -> HudManager.useDefault(element), HudManager.hasOverride(element, location)));
            items.add(new MenuItem("Make this the All Locations layout", () -> HudManager.makeDefault(element), HudManager.hasOverride(element, location)));
        }
        items.add(MenuItem.separator());
        items.add(new MenuItem("Reset everything", () -> HudManager.reset(element), true));
        openMenu(element.name, items, mouseX, mouseY);
    }

    private void openHiddenMenu(double mouseX, double mouseY) {
        List<MenuItem> items = new ArrayList<>();
        for (HudElement element : hidden()) {
            items.add(new MenuItem("Show " + element.name, () -> HudManager.setVisible(element, true), true));
        }
        if (items.isEmpty()) items.add(new MenuItem("No hidden HUDs here", null, false));
        openMenu("Hidden in " + locationName(), items, mouseX, mouseY);
    }

    private void center(HudElement element, boolean horizontal) {
        HudManager.Bounds b = HudManager.bounds(element, true);
        HudLayoutEntry layout = HudManager.editable(element);
        if (layout.isAttached()) {
            applyAxis(layout, true, b.x(), b.scaledWidth(), this.width, false);
            applyAxis(layout, false, b.y(), b.scaledHeight(), this.height, false);
            layout.detach();
        }
        if (horizontal) {
            layout.anchorX = HudLayoutEntry.Anchor.CENTER;
            layout.offsetX = 0f;
        } else {
            layout.anchorY = HudLayoutEntry.Anchor.CENTER;
            layout.offsetY = 0f;
        }
    }

    private void unstick(HudElement element) {
        HudManager.Bounds b = HudManager.bounds(element, true);
        HudLayoutEntry layout = HudManager.editable(element);
        applyAxis(layout, true, b.x(), b.scaledWidth(), this.width, false);
        applyAxis(layout, false, b.y(), b.scaledHeight(), this.height, false);
        layout.detach();
    }

    @Override
    public void extractBackground(final @NotNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        if (this.minecraft.level == null) {
            super.extractBackground(graphics, mouseX, mouseY, a);
            return;
        }
        graphics.fill(0, 0, this.width, this.height, 0x50000000);
    }

    private void drawHelp(GuiGraphicsExtractor graphics) {
        int lineHeight = this.font.lineHeight + 3;
        int y = this.height / 2 - (HELP.length + 2) * lineHeight / 2;
        graphics.centeredText(this.font, "HUD Editor • " + locationName(), this.width / 2, y, HELP_TITLE_COLOR);
        y += lineHeight * 2;
        for (String line : HELP) {
            graphics.centeredText(this.font, line, this.width / 2, y, HELP_COLOR);
            y += lineHeight;
        }
    }

    @Override
    public void extractRenderState(final @NotNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        drawHelp(graphics);
        if (dragging != null && snappedCenterX) graphics.fill(this.width / 2, 0, this.width / 2 + 1, this.height, GUIDE_COLOR);
        if (dragging != null && snappedCenterY) graphics.fill(0, this.height / 2, this.width, this.height / 2 + 1, GUIDE_COLOR);

        int accent = 0xFF000000 | ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
        boolean overlayOpen = dropdownOpen || menuOpen();
        HudElement hovered = active() != null ? active() : overlayOpen ? null : elementAt(mouseX, mouseY);

        for (HudElement element : shown()) {
            HudManager.Bounds bounds = HudManager.draw(element, graphics, true);
            boolean hot = element == hovered || (snapped != null && element == snapped.parent());
            outline(graphics, bounds, hot ? accent : OUTLINE_IDLE);
            if (hot) drawHandles(graphics, bounds, accent);
        }

        if (hovered != null) drawLabel(graphics, hovered, HudManager.bounds(hovered, true));

        super.extractRenderState(graphics, mouseX, mouseY, a);
        if (dropdownOpen) drawDropdown(graphics, mouseX, mouseY, accent);
        if (menuOpen()) drawMenu(graphics, mouseX, mouseY, accent);
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
        HudElement parent = HudManager.parentOf(element);
        String label = element.name
                + " §7" + Math.round(b.scale() * 100) + "%"
                + (layout.width > 0 ? " • " + layout.width + "px" : "")
                + (!location.isEmpty() ? (HudManager.hasOverride(element, location) ? " • custom" : " • All Locations") : "")
                + (parent != null ? " • stuck to " + parent.name : "")
                + (layout.squareCorners ? " • square" : "");
        int textWidth = this.font.width(label);
        int x = Mth.clamp(Mth.floor(b.x()), 2, Math.max(2, this.width - textWidth - 2));
        int y = b.bottom() + 12 <= this.height ? Mth.ceil(b.bottom()) + 2 : Mth.floor(b.y()) - 11;
        if (y < 0) y = Mth.floor(b.y()) + 2;
        graphics.fill(x - 2, y - 1, x + textWidth + 2, y + 10, 0xC0000000);
        graphics.text(this.font, label, x, y, 0xFFFFFFFF, true);
    }

    private HudElement elementAt(double mouseX, double mouseY) {
        List<HudElement> elements = shown();
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement element = elements.get(i);
            if (HudManager.bounds(element, true).contains(mouseX, mouseY)) return element;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(final @NotNull MouseButtonEvent event, final boolean doubleClick) {
        if (menuOpen()) {
            if (overMenu(event.x(), event.y())) {
                MenuItem item = menuItemAt(event.x(), event.y());
                if (item != null && item.enabled() && event.button() == 0) {
                    closeMenu();
                    if (item.action() != null) item.action().run();
                }
                return true;
            }
            closeMenu();
            if (event.button() != 1) return true;
        }

        if (dropdownOpen) {
            if (overDropdown(event.x(), event.y())) {
                int index = dropdownScroll + (int) ((event.y() - dropdownTop()) / ROW);
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

        if (event.button() == 1) {
            if (hit != null) openElementMenu(hit, event.x(), event.y());
            else openHiddenMenu(event.x(), event.y());
            return true;
        }

        if (hit == null) return false;

        if (event.button() == 2) {
            HudManager.setVisible(hit, false);
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
        if (menuOpen()) {
            if (overMenu(x, y) && scrollY != 0) {
                menuScrollTarget = Mth.clamp(menuScrollTarget - (int) Math.signum(scrollY) * MENU_SCROLL_STEP, 0, maxMenuScroll());
            }
            return true;
        }
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
