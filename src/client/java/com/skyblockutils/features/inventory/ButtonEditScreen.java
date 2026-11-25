package com.skyblockutils.features.inventory;

import com.skyblockutils.features.hud.layout.HudLocations;
import com.skyblockutils.features.hud.layout.HudManager;
import com.skyblockutils.utils.ClipboardUtils;
import com.skyblockutils.utils.OnScreenNotification;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ButtonEditScreen extends Screen {
    private static final int MARGIN = 10;
    private static final int TOP = 22;
    private static final int PREVIEW_TOP = 48;
    private static final int ROW_H = 19;
    private static final int BOX_H = 16;
    private static final int LABEL_W = 46;
    private static final int TYPE_W = 26;
    private static final int CELL_W = 16;
    private static final int CELL_H = 12;
    private static final int CELL_GAP = 1;
    private static final int CHART_COLUMNS = 8;
    private static final int CHART_ROWS = 4;
    private static final int ICON_DEBOUNCE = 8;
    private static final int DRAG_THRESHOLD = 3;

    private record Code(String insert, String label, String name, int rgb, boolean gradient) {
        static Code legacy(char code, String name, int rgb) {
            return new Code("&" + code, String.valueOf(code), name, rgb, false);
        }
    }

    private static final List<Code> COLORS = List.of(
            Code.legacy('0', "Black", 0x000000), Code.legacy('1', "Dark Blue", 0x0000AA),
            Code.legacy('2', "Dark Green", 0x00AA00), Code.legacy('3', "Dark Aqua", 0x00AAAA),
            Code.legacy('4', "Dark Red", 0xAA0000), Code.legacy('5', "Dark Purple", 0xAA00AA),
            Code.legacy('6', "Gold", 0xFFAA00), Code.legacy('7', "Gray", 0xAAAAAA),
            Code.legacy('8', "Dark Gray", 0x555555), Code.legacy('9', "Blue", 0x5555FF),
            Code.legacy('a', "Green", 0x55FF55), Code.legacy('b', "Aqua", 0x55FFFF),
            Code.legacy('c', "Red", 0xFF5555), Code.legacy('d', "Light Purple", 0xFF55FF),
            Code.legacy('e', "Yellow", 0xFFFF55), Code.legacy('f', "White", 0xFFFFFF));

    private static final List<Code> FORMATS = List.of(
            Code.legacy('l', "Bold", -1), Code.legacy('o', "Italic", -1),
            Code.legacy('n', "Underline", -1), Code.legacy('m', "Strikethrough", -1),
            Code.legacy('k', "Obfuscated", -1), Code.legacy('r', "Reset", -1));

    private static ButtonLayout.Button clipboardButton;

    private final Screen parent;
    private ButtonLayout working = ButtonLayout.INSTANCE.copy();
    private String location = HudManager.currentLocation();
    private int selectedRow = 0;
    private int selectedColumn = 0;
    private EditBox lastBox;
    private String pendingIcon;
    private int iconTicks;
    private PopupMenu menu;
    private Button exportButton;
    private Button importButton;

    private InventoryButtons.Hit pressed;
    private double pressX;
    private double pressY;
    private boolean dragging;

    private float scale;
    private int previewX;
    private int chartX;
    private int chartY;
    private int fieldsX;
    private int boxX;
    private int boxW;

    public ButtonEditScreen(Screen parent) {
        super(Component.literal("Inventory Buttons"));
        this.parent = parent;
    }

    private ButtonLayout.ButtonSet set() {
        return working.set(location);
    }

    private static String locationName(String key) {
        return key.isEmpty() ? "Default" : HudLocations.name(key);
    }

    private boolean usesDefault() {
        return !location.isEmpty() && !working.hasOwnSet(location);
    }

    private boolean rowSelected() {
        return selectedColumn < 0;
    }

    private ButtonLayout.Button selected() {
        return rowSelected() ? null : set().button(selectedRow, selectedColumn);
    }

    private List<Code> ssuCodes() {
        List<Code> codes = new ArrayList<>();
        for (ButtonText.Token token : ButtonText.ssuTokens()) {
            codes.add(new Code("&{" + token.code() + "}", token.code().equals(ButtonText.GRADIENT) ? "G" : token.code().substring(0, 1).toUpperCase(), token.name(), token.rgb(), token.code().equals(ButtonText.GRADIENT)));
        }
        return codes;
    }

    private void layout() {
        int chartH = 10 + CHART_ROWS * (CELL_H + CELL_GAP) + 10;
        float available = this.height - PREVIEW_TOP - chartH - 10 - 30;
        scale = Math.clamp(available / InventoryButtons.GRID_H, 1f, 2f);
        previewX = MARGIN + 4;
        chartX = MARGIN;
        chartY = PREVIEW_TOP + Math.round(InventoryButtons.GRID_H * scale) + 10;
        int leftW = Math.max(Math.round(InventoryButtons.GRID_W * scale) + 8, CHART_COLUMNS * (CELL_W + CELL_GAP));
        fieldsX = MARGIN + leftW + 14;
        boxX = fieldsX + LABEL_W;
        boxW = Math.max(80, Math.min(260, this.width - boxX - MARGIN));
    }

    @Override
    protected void init() {
        layout();
        lastBox = null;
        menu = null;

        this.addRenderableWidget(Button.builder(Component.literal("Location: " + locationName(location) + " ▼"), b -> openLocationMenu(b.getX(), b.getY() + b.getHeight()))
                .bounds(MARGIN, TOP, 150, 18).build());
        if (!location.isEmpty() && !usesDefault()) {
            this.addRenderableWidget(Button.builder(Component.literal("Use Default here"), _ -> toggleOwnSet()).bounds(MARGIN + 154, TOP, 150, 18).build());
        }

        int y = PREVIEW_TOP + 12;
        if (usesDefault()) {
            int w = Math.min(200, this.width - fieldsX - MARGIN);
            this.addRenderableWidget(Button.builder(Component.literal("Customize Default"), _ -> selectLocation(""))
                    .bounds(fieldsX, y + 24, w, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Customize for " + locationName(location)), _ -> toggleOwnSet())
                    .bounds(fieldsX, y + 48, w, 20).build());
        } else if (rowSelected()) {
            ButtonLayout.Row row = set().row(selectedRow);
            lastBox = field(boxX, y, boxW, row.label, v -> row.label = v);
        } else {
            ButtonLayout.Button button = selected();
            if (button == null) return;
            lastBox = field(boxX, y, boxW, button.name, v -> button.name = v);
            y += ROW_H;
            field(boxX, y, boxW - 42, button.icon, v -> {
                pendingIcon = v;
                iconTicks = ICON_DEBOUNCE;
            });
            this.addRenderableWidget(Button.builder(Component.literal("Pick…"), _ -> openPicker()).bounds(boxX + boxW - 40, y, 40, BOX_H).build());
            y += ROW_H;
            actionRow(y, () -> button.click, v -> button.click = v);
            y += ROW_H;
            field(boxX, y, boxW, button.shiftName, v -> button.shiftName = v);
            y += ROW_H;
            actionRow(y, () -> button.shift, v -> button.shift = v);
            y += ROW_H;
            field(boxX, y, boxW, button.ctrlName, v -> button.ctrlName = v);
            y += ROW_H;
            actionRow(y, () -> button.ctrl, v -> button.ctrl = v);
        }

        int buttonsY = this.height - 26;
        String[] labels = {"Done", "Cancel", "Export…", "Import…", location.isEmpty() || usesDefault() ? "Reset Default" : "Reset Location"};
        Runnable[] actions = {this::save, this::onClose, this::openExportMenu, this::openImportMenu, this::resetSet};
        int w = 72;
        int total = labels.length * w + (labels.length - 1) * 4;
        int x = this.width / 2 - total / 2;
        for (int i = 0; i < labels.length; i++) {
            Runnable action = actions[i];
            Button widget = this.addRenderableWidget(Button.builder(Component.literal(labels[i]), _ -> action.run())
                    .bounds(x + i * (w + 4), buttonsY, w, 20).build());
            if (i == 2) exportButton = widget;
            if (i == 3) importButton = widget;
        }

        if (lastBox != null) this.setInitialFocus(lastBox);
    }

    private void actionRow(int y, Supplier<String> getter, Consumer<String> setter) {
        ButtonAction action = ButtonAction.parse(getter.get());
        int x = boxX;
        for (ButtonAction.Type type : ButtonAction.Type.values()) {
            boolean active = action.type() == type;
            Component label = Component.literal(type == ButtonAction.Type.COMMAND ? "Cmd" : type.label)
                    .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.GRAY);
            this.addRenderableWidget(Button.builder(label, _ -> {
                ButtonAction current = ButtonAction.parse(getter.get());
                if (current.type() == type) return;
                String value = current.value();
                if (type == ButtonAction.Type.SSU) value = ButtonAction.Ssu.values()[0].id;
                else if (current.type() == ButtonAction.Type.SSU) value = "";
                setter.accept(ButtonAction.serialize(type, value));
                this.rebuildWidgets();
            }).bounds(x, y, TYPE_W, BOX_H).build());
            x += TYPE_W + 1;
        }
        x += 2;
        int valueW = boxX + boxW - x;
        if (action.type() == ButtonAction.Type.SSU) {
            ButtonAction.Ssu current = ButtonAction.Ssu.byId(action.value());
            String label = (current == null ? "Choose…" : current.label) + " ▼";
            this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                List<PopupMenu.Item> items = new ArrayList<>();
                for (ButtonAction.Ssu ssu : ButtonAction.Ssu.values()) {
                    items.add(PopupMenu.Item.of((ssu == current ? "&a" : "") + ssu.label, () -> {
                        setter.accept(ButtonAction.serialize(ButtonAction.Type.SSU, ssu.id));
                        this.rebuildWidgets();
                    }));
                }
                items.add(PopupMenu.Item.separator());
                items.add(PopupMenu.Item.of("&7None", () -> {
                    setter.accept("");
                    this.rebuildWidgets();
                }));
                menu = PopupMenu.open(this.font, null, items, b.getX(), b.getY() + b.getHeight(), this.width, this.height);
            }).bounds(x, y, valueW, BOX_H).build());
        } else {
            ButtonAction.Type type = action.type();
            EditBox box = field(x, y, valueW, action.value(), v -> setter.accept(ButtonAction.serialize(type, v)));
            box.setHint(Component.literal(switch (type) {
                case COMMAND -> "command without /";
                case CHAT -> "message";
                default -> "https://…";
            }).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private EditBox field(int x, int y, int w, String value, Consumer<String> responder) {
        EditBox box = new EditBox(this.font, x, y, w, BOX_H, Component.empty());
        box.setMaxLength(32767);
        box.setValue(value);
        box.setResponder(responder);
        return this.addRenderableWidget(box);
    }

    private void flushIcon() {
        if (pendingIcon == null) return;
        ButtonLayout.Button button = selected();
        if (button != null && !usesDefault()) button.icon = pendingIcon.trim();
        pendingIcon = null;
        iconTicks = 0;
    }

    private void select(int row, int column) {
        flushIcon();
        selectedRow = row;
        selectedColumn = column;
        this.rebuildWidgets();
    }

    private void selectLocation(String key) {
        flushIcon();
        location = key;
        this.rebuildWidgets();
    }

    private void toggleOwnSet() {
        flushIcon();
        if (location.isEmpty()) return;
        if (working.hasOwnSet(location)) working.removeOwnSet(location);
        else working.createOwnSet(location);
        this.rebuildWidgets();
    }

    private void openLocationMenu(int x, int y) {
        List<PopupMenu.Item> items = new ArrayList<>();
        List<String> options = HudLocations.options(HudManager.currentLocation());
        int selectedIndex = 0;
        for (int i = 0; i < options.size(); i++) {
            String key = options.get(i);
            if (key.equals(location)) selectedIndex = i;
            String prefix = key.equals(location) ? "&a" : "";
            String suffix = key.isEmpty() ? "" : working.hasOwnSet(key) ? " &8(custom)" : " &8(default)";
            if (!key.isEmpty() && key.equals(HudManager.currentLocation())) suffix += " &7(current)";
            String name = locationName(key);
            items.add(PopupMenu.Item.of(prefix + name + suffix, () -> selectLocation(key)));
        }
        menu = PopupMenu.open(this.font, null, items, x, y, this.width, this.height);
        menu.scrollTo(selectedIndex);
    }

    private void openPicker() {
        flushIcon();
        ButtonLayout.Button button = selected();
        if (button == null) return;
        this.minecraft.setScreenAndShow(new IconPickerScreen(this, spec -> {
            button.icon = spec;
            ButtonLayout.addRecent(spec);
            this.minecraft.setScreenAndShow(this);
        }));
    }

    private void openExportMenu() {
        flushIcon();
        menu = PopupMenu.open(this.font, Component.literal("Export Layout"), List.of(
                PopupMenu.Item.of("Copy as text", () -> {
                    ClipboardUtils.copyTextToClipboard(working.export());
                    notify("Layout Copied", "The layout string is in your clipboard");
                }),
                PopupMenu.Item.of("Save to file…", () -> this.minecraft.setScreenAndShow(new NamePromptScreen(this, "Save Layout", "", name -> {
                    boolean exists = Files.exists(ButtonLayout.savedPath(name));
                    if (working.saveAs(name))
                        notify(exists ? "Layout Overwritten" : "Layout Saved", "Saved as " + ButtonLayout.fileName(name) + ".json");
                    else notify("Save Failed", "Couldn't write the layout file");
                })))
        ), exportButton.getX(), exportButton.getY() - 44, this.width, this.height);
    }

    private void openImportMenu() {
        flushIcon();
        menu = PopupMenu.open(this.font, Component.literal("Import Layout"), List.of(
                PopupMenu.Item.of("Paste from clipboard", () -> {
                    try {
                        working = ButtonLayout.importText(this.minecraft.keyboardHandler.getClipboard());
                        notify("Layout Imported", "Press Done to keep it");
                        this.rebuildWidgets();
                    } catch (RuntimeException e) {
                        notify("Import Failed", e.getMessage() == null ? "Invalid layout" : e.getMessage());
                    }
                }),
                PopupMenu.Item.of("Load from file…", () -> this.minecraft.setScreenAndShow(new LayoutBrowserScreen(this, location, layout -> {
                    working = layout;
                    notify("Layout Loaded", "Press Done to keep it");
                })))
        ), importButton.getX(), importButton.getY() - 44, this.width, this.height);
    }

    private static void notify(String title, String subtitle) {
        OnScreenNotification.builder().title(title).subtitle(subtitle).tickTime(60).send();
    }

    private void resetSet() {
        pendingIcon = null;
        iconTicks = 0;
        if (location.isEmpty() || usesDefault()) working.defaults = ButtonLayout.defaultSet();
        else working.locations.put(location, working.defaults.copy());
        this.rebuildWidgets();
    }

    private void save() {
        flushIcon();
        ButtonLayout.INSTANCE = working;
        ButtonLayout.save();
        onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (iconTicks > 0 && --iconTicks == 0) flushIcon();
    }

    private InventoryButtons.Hit previewHit(double mouseX, double mouseY) {
        return InventoryButtons.hitAt((mouseX - previewX) / scale, (mouseY - PREVIEW_TOP) / scale);
    }

    private Code chartAt(double mouseX, double mouseY) {
        int top = chartY + 10;
        if (mouseX < chartX || mouseY < top) return null;
        int column = (int) ((mouseX - chartX) / (CELL_W + CELL_GAP));
        int row = (int) ((mouseY - top) / (CELL_H + CELL_GAP));
        if (column >= CHART_COLUMNS || row >= CHART_ROWS) return null;
        if ((mouseX - chartX) - column * (CELL_W + CELL_GAP) >= CELL_W || (mouseY - top) - row * (CELL_H + CELL_GAP) >= CELL_H)
            return null;
        List<Code> list = switch (row) {
            case 0, 1 -> COLORS.subList(row * CHART_COLUMNS, row * CHART_COLUMNS + CHART_COLUMNS);
            case 2 -> FORMATS;
            default -> ssuCodes();
        };
        return column < list.size() ? list.get(column) : null;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (getFocused() instanceof EditBox box) lastBox = box;
        boolean menuOpen = menu != null;
        int mx = menuOpen ? -1 : mouseX;
        int my = menuOpen ? -1 : mouseY;

        graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
        drawPreview(graphics, mx, my);
        drawChart(graphics, mx, my);
        drawFieldLabels(graphics);

        super.extractRenderState(graphics, mx, my, delta);

        if (dragging && pressed != null) drawDragged(graphics, mouseX, mouseY);
        if (menuOpen) {
            menu.render(graphics, this.font, mouseX, mouseY);
            return;
        }

        InventoryButtons.Hit hovered = previewHit(mouseX, mouseY);
        if (!dragging && hovered != null && !hovered.isLabel()) {
            List<Component> lines = set().button(hovered.row(), hovered.column()).tooltip();
            if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }
        Code code = chartAt(mouseX, mouseY);
        if (code != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(ButtonText.parse(code.insert() + code.name()));
            lines.add(Component.literal(code.insert()).withStyle(ChatFormatting.GRAY));
            if (code.insert().startsWith("&{"))
                lines.add(Component.literal("Follows your SSU color style").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.literal("Click to insert").withStyle(ChatFormatting.DARK_GRAY));
            graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }
    }

    private void drawPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int panelW = Math.round(InventoryButtons.GRID_W * scale);
        int panelH = Math.round(InventoryButtons.GRID_H * scale);
        Draw.panel(graphics, previewX - 4, PREVIEW_TOP - 4, panelW + 8, panelH + 8);

        InventoryButtons.Hit hovered = previewHit(mouseX, mouseY);
        graphics.pose().pushMatrix();
        graphics.pose().translate(previewX, PREVIEW_TOP);
        graphics.pose().scale(scale, scale);
        InventoryButtons.drawGrid(graphics, this.font, set(), 0, 0, hovered != null && !hovered.isLabel() ? hovered : null);
        graphics.pose().popMatrix();

        if (usesDefault()) {
            graphics.fill(previewX - 3, PREVIEW_TOP - 3, previewX + panelW + 3, PREVIEW_TOP + panelH + 3, 0x80000000);
            return;
        }
        if (hovered != null && hovered.isLabel()) outlineLabel(graphics, hovered.row(), 0x80FFFFFF);
        if (dragging && hovered != null && pressed != null && hovered.isLabel() == pressed.isLabel()) {
            if (hovered.isLabel()) outlineLabel(graphics, hovered.row(), 0xFF55FF55);
            else outlineCell(graphics, hovered, 0xFF55FF55);
        }
        if (rowSelected()) outlineLabel(graphics, selectedRow, 0xFFFFFF55);
        else outlineCell(graphics, new InventoryButtons.Hit(selectedRow, selectedColumn), 0xFFFFFF55);
    }

    private void outlineLabel(GuiGraphicsExtractor graphics, int row, int color) {
        int y = PREVIEW_TOP + Math.round(InventoryButtons.rowY(row) * scale) - 1;
        Draw.border(graphics, previewX - 1, y, Math.round(InventoryButtons.GRID_W * scale) + 2, Math.round(InventoryButtons.LABEL_H * scale) + 1, color);
    }

    private void outlineCell(GuiGraphicsExtractor graphics, InventoryButtons.Hit hit, int color) {
        int x = previewX + Math.round(InventoryButtons.buttonX(hit.column()) * scale) - 1;
        int y = PREVIEW_TOP + Math.round(InventoryButtons.buttonY(hit.row()) * scale) - 1;
        Draw.border(graphics, x, y, Math.round(InventoryButtons.BUTTON_W * scale) + 2, Math.round(InventoryButtons.BUTTON_H * scale) + 2, color);
    }

    private void drawDragged(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(mouseX - InventoryButtons.BUTTON_W * scale / 2, mouseY - InventoryButtons.BUTTON_H * scale / 2);
        graphics.pose().scale(scale, scale);
        if (pressed.isLabel()) {
            graphics.text(this.font, ButtonText.parse(set().row(pressed.row()).label), 0, 4, 0xFFFFFFFF, true);
        } else {
            InventoryButtons.drawCell(graphics, this.font, set().button(pressed.row(), pressed.column()), 0, 0, true);
        }
        graphics.pose().popMatrix();
    }

    private void drawChart(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, Component.literal("Color codes"), chartX, chartY, 0xFFAAAAAA, false);
        Code hovered = chartAt(mouseX, mouseY);
        List<List<Code>> rows = List.of(COLORS.subList(0, 8), COLORS.subList(8, 16), FORMATS, ssuCodes());
        for (int row = 0; row < rows.size(); row++) {
            List<Code> codes = rows.get(row);
            for (int column = 0; column < codes.size(); column++) {
                Code code = codes.get(column);
                int x = chartX + column * (CELL_W + CELL_GAP);
                int y = chartY + 10 + row * (CELL_H + CELL_GAP);
                if (code.gradient()) {
                    int start = ButtonText.ssuTokens().get(2).rgb();
                    int end = ButtonText.ssuTokens().get(3).rgb();
                    for (int i = 0; i < CELL_W; i++)
                        graphics.fill(x + i, y, x + i + 1, y + CELL_H, 0xFF000000 | mix(start, end, i / (float) (CELL_W - 1)));
                } else {
                    graphics.fill(x, y, x + CELL_W, y + CELL_H, code.rgb() < 0 ? 0xFF3A3A3A : 0xFF000000 | code.rgb());
                }
                if (code.equals(hovered)) Draw.border(graphics, x - 1, y - 1, CELL_W + 2, CELL_H + 2, 0xFFFFFFFF);
                Component label = row == 2 ? ButtonText.parse(code.insert() + code.label()) : Component.literal(code.label());
                int textColor = code.rgb() >= 0 && luminance(code.rgb()) > 140 ? 0xFF000000 : 0xFFFFFFFF;
                graphics.text(this.font, label, x + (CELL_W - this.font.width(label)) / 2, y + 2, textColor, false);
            }
        }
        graphics.text(this.font, Component.literal("&#RRGGBB for any hex color"), chartX, chartY + 12 + CHART_ROWS * (CELL_H + CELL_GAP), 0xFF777777, false);
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return r << 16 | g << 8 | bl;
    }

    private static int luminance(int rgb) {
        return ((rgb >> 16 & 0xFF) * 299 + (rgb >> 8 & 0xFF) * 587 + (rgb & 0xFF) * 114) / 1000;
    }

    private void drawFieldLabels(GuiGraphicsExtractor graphics) {
        String where = locationName(location);
        if (usesDefault()) {
            graphics.text(this.font, Component.literal(where + " uses the Default set."), fieldsX, PREVIEW_TOP, 0xFFFFFF55, true);
            graphics.text(this.font, Component.literal("Edit Default, or give this location its own buttons."), fieldsX, PREVIEW_TOP + 12, 0xFF999999, false);
            return;
        }

        Component header = rowSelected()
                ? Component.literal(where + " · Row " + (selectedRow + 1) + " name")
                : Component.literal(where + " · Row " + (selectedRow + 1) + " · Button " + (selectedColumn + 1));
        graphics.text(this.font, header, fieldsX, PREVIEW_TOP, 0xFFFFFF55, true);

        String[] labels = rowSelected()
                ? new String[]{"Name"}
                : new String[]{"Name", "Icon", "Click", "Shift name", "Shift", "Ctrl name", "Ctrl"};
        int y = PREVIEW_TOP + 12;
        for (String label : labels) {
            graphics.text(this.font, Component.literal(label), fieldsX, y + 4, 0xFFAAAAAA, false);
            y += ROW_H;
        }

        if (!rowSelected()) {
            var selected = selected();
            if (selected == null) return;
            ButtonIcons.Icon icon = ButtonIcons.icon(pendingIcon != null ? pendingIcon : selected.icon);
            int iconX = fieldsX + this.font.width("Icon") + 4;
            ButtonIcons.draw(graphics, this.font, icon, iconX, PREVIEW_TOP + 12 + ROW_H + 2, 12);
        }

        int hintY = y + 2;
        for (String hint : new String[]{
                "Drag to move or swap · Right click for more",
                rowSelected() ? "Drag a row name onto another to swap rows" : "Names left empty show the action instead"}) {
            graphics.text(this.font, Component.literal(hint), fieldsX, hintY, 0xFF777777, false);
            hintY += 10;
        }
    }

    private void openContextMenu(InventoryButtons.Hit hit, int mouseX, int mouseY) {
        flushIcon();
        ButtonLayout.ButtonSet set = set();
        List<PopupMenu.Item> items = new ArrayList<>();
        if (hit.isLabel()) {
            ButtonLayout.Row row = set.row(hit.row());
            items.add(PopupMenu.Item.of("Clear row name", () -> {
                row.label = "";
                this.rebuildWidgets();
            }));
            items.add(PopupMenu.Item.of("Clear whole row", () -> {
                for (int c = 0; c < ButtonLayout.COLUMNS; c++) set.set(hit.row(), c, new ButtonLayout.Button());
                row.label = "";
                this.rebuildWidgets();
            }));
            items.add(PopupMenu.Item.of("Reset row to default", () -> {
                ButtonLayout.Row fresh = ButtonLayout.defaultSet().row(hit.row());
                set.rows.set(hit.row(), fresh);
                this.rebuildWidgets();
            }));
            menu = PopupMenu.open(this.font, ButtonText.parse(row.label.isBlank() ? "Row " + (hit.row() + 1) : row.label), items, mouseX, mouseY, this.width, this.height);
            return;
        }

        ButtonLayout.Button button = set.button(hit.row(), hit.column());
        items.add(PopupMenu.Item.of("Edit", () -> select(hit.row(), hit.column())));
        items.add(PopupMenu.Item.of("Copy", () -> clipboardButton = button.copy()));
        items.add(PopupMenu.Item.of("Paste", () -> {
            set.set(hit.row(), hit.column(), clipboardButton.copy());
            this.rebuildWidgets();
        }, clipboardButton != null));
        items.add(PopupMenu.Item.of("Pick icon…", () -> {
            select(hit.row(), hit.column());
            openPicker();
        }));
        items.add(PopupMenu.Item.separator());
        items.add(PopupMenu.Item.of("Reset to default", () -> {
            set.set(hit.row(), hit.column(), ButtonLayout.defaultSet().button(hit.row(), hit.column()).copy());
            this.rebuildWidgets();
        }));
        items.add(PopupMenu.Item.of("&cClear", () -> {
            set.set(hit.row(), hit.column(), new ButtonLayout.Button());
            this.rebuildWidgets();
        }, !button.isEmpty()));
        Component title = button.name.isBlank() ? Component.literal("Button " + (hit.column() + 1)) : ButtonText.parse(button.name);
        menu = PopupMenu.open(this.font, title, items, mouseX, mouseY, this.width, this.height);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (menu != null) {
            PopupMenu open = menu;
            menu = null;
            if (open.contains(event.x(), event.y())) open.click(event.x(), event.y());
            return true;
        }
        if (event.button() == 0) {
            Code code = chartAt(event.x(), event.y());
            if (code != null) {
                if (lastBox != null && !usesDefault()) {
                    lastBox.insertText(code.insert());
                    this.setFocused(lastBox);
                }
                return true;
            }
        }
        InventoryButtons.Hit hit = previewHit(event.x(), event.y());
        if (hit != null && !usesDefault()) {
            if (event.button() == 0) {
                pressed = hit;
                pressX = event.x();
                pressY = event.y();
                dragging = false;
                return true;
            }
            if (event.button() == 1) {
                openContextMenu(hit, (int) event.x(), (int) event.y());
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dragX, double dragY) {
        if (pressed != null) {
            if (!dragging && Math.hypot(event.x() - pressX, event.y() - pressY) > DRAG_THRESHOLD) {
                flushIcon();
                dragging = true;
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (pressed == null) return super.mouseReleased(event);
        InventoryButtons.Hit from = pressed;
        boolean wasDragging = dragging;
        pressed = null;
        dragging = false;

        if (!wasDragging) {
            select(from.row(), from.column());
            return true;
        }
        InventoryButtons.Hit to = previewHit(event.x(), event.y());
        if (to == null || to.equals(from) || to.isLabel() != from.isLabel()) return true;

        ButtonLayout.ButtonSet set = set();
        if (from.isLabel()) {
            ButtonLayout.Row a = set.row(from.row());
            ButtonLayout.Row b = set.row(to.row());
            set.rows.set(from.row(), b);
            set.rows.set(to.row(), a);
            if (rowSelected() && selectedRow == from.row()) selectedRow = to.row();
        } else {
            ButtonLayout.Button a = set.button(from.row(), from.column());
            ButtonLayout.Button b = set.button(to.row(), to.column());
            set.set(from.row(), from.column(), b);
            set.set(to.row(), to.column(), a);
            selectedRow = to.row();
            selectedColumn = to.column();
        }
        this.rebuildWidgets();
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (menu != null) {
            if (menu.contains(x, y)) menu.scroll(scrollY);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (menu != null && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            menu = null;
            return true;
        }
        return super.keyPressed(event);
    }
}