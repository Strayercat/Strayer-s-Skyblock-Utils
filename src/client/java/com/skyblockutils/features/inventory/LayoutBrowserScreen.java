package com.skyblockutils.features.inventory;

import com.skyblockutils.config.ConfigPaths;
import com.skyblockutils.features.hud.layout.HudLocations;
import com.skyblockutils.utils.FuzzySearch;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class LayoutBrowserScreen extends Screen {
    private static final int ROW = 22;
    private static final int LIST_TOP = 46;
    private static final SimpleDateFormat DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final Screen parent;
    private final String location;
    private final Consumer<ButtonLayout> onLoad;
    private final Map<String, Optional<ButtonLayout>> parsed = new HashMap<>();
    private List<ButtonLayout.Saved> all = List.of();
    private List<ButtonLayout.Saved> filtered = List.of();
    private ButtonLayout.Saved selected;
    private String query = "";
    private int scroll;
    private boolean confirmDelete;
    private long lastClick;

    private Button loadButton;
    private Button renameButton;
    private Button deleteButton;

    public LayoutBrowserScreen(Screen parent, String location, Consumer<ButtonLayout> onLoad) {
        super(Component.literal("Saved Button Layouts"));
        this.parent = parent;
        this.location = location;
        this.onLoad = onLoad;
    }

    private int listX() {
        return this.width / 2 - listW() / 2;
    }

    private int listW() {
        return Math.min(260, this.width - 40);
    }

    private int visibleRows() {
        return Math.max(1, (this.height - LIST_TOP - 36) / ROW);
    }

    private int maxScroll() {
        return Math.max(0, filtered.size() - visibleRows());
    }

    @Override
    protected void init() {
        all = ButtonLayout.listSaved();
        parsed.clear();
        if (selected != null) selected = all.stream().filter(s -> s.path().equals(selected.path())).findFirst().orElse(null);

        EditBox search = new EditBox(this.font, listX(), 22, listW(), 18, Component.empty());
        search.setHint(Component.literal("Search…"));
        search.setValue(query);
        search.setResponder(v -> {
            query = v;
            scroll = 0;
            refilter();
        });
        this.addRenderableWidget(search);

        int y = this.height - 26;
        int w = 60;
        int x = this.width / 2 - (5 * w + 16) / 2;
        loadButton = this.addRenderableWidget(Button.builder(Component.literal("Load"), _ -> load()).bounds(x, y, w, 20).build());
        renameButton = this.addRenderableWidget(Button.builder(Component.literal("Rename"), _ -> rename()).bounds(x + (w + 4), y, w, 20).build());
        deleteButton = this.addRenderableWidget(Button.builder(Component.literal("Delete"), _ -> delete()).bounds(x + 2 * (w + 4), y, w, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Open Folder"), _ -> Util.getPlatform().openPath(ConfigPaths.SAVED_BUTTONS))
                .bounds(x + 3 * (w + 4), y, w, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Back"), _ -> onClose()).bounds(x + 4 * (w + 4), y, w, 20).build());
        refilter();
        updateButtons();
    }

    private void refilter() {
        filtered = FuzzySearch.searchAny(query.trim(), all, saved -> List.of(saved.name()));
        scroll = Math.clamp(scroll, 0, maxScroll());
    }

    private void updateButtons() {
        boolean has = selected != null;
        loadButton.active = has && layout(selected) != null;
        renameButton.active = has;
        deleteButton.active = has;
        deleteButton.setMessage(Component.literal(confirmDelete ? "Confirm?" : "Delete").withStyle(confirmDelete ? ChatFormatting.RED : ChatFormatting.RESET));
    }

    private ButtonLayout layout(ButtonLayout.Saved saved) {
        return parsed.computeIfAbsent(saved.path().toString(), _ -> Optional.ofNullable(ButtonLayout.readSaved(saved))).orElse(null);
    }

    private void load() {
        if (selected == null) return;
        ButtonLayout layout = layout(selected);
        if (layout == null) return;
        this.minecraft.setScreenAndShow(parent);
        onLoad.accept(layout.copy());
    }

    private void rename() {
        if (selected == null) return;
        ButtonLayout.Saved target = selected;
        this.minecraft.setScreenAndShow(new NamePromptScreen(this, "Rename Layout", target.name(), name -> {
            if (ButtonLayout.renameSaved(target, name)) selected = null;
            this.rebuildWidgets();
        }));
    }

    private void delete() {
        if (selected == null) return;
        if (!confirmDelete) {
            confirmDelete = true;
            updateButtons();
            return;
        }
        ButtonLayout.deleteSaved(selected);
        selected = null;
        confirmDelete = false;
        this.rebuildWidgets();
    }

    private ButtonLayout.Saved entryAt(double mouseX, double mouseY) {
        int x = listX();
        if (mouseX < x || mouseX >= x + listW() || mouseY < LIST_TOP) return null;
        int row = (int) ((mouseY - LIST_TOP) / ROW);
        if (row >= visibleRows()) return null;
        int index = row + scroll;
        return index < filtered.size() ? filtered.get(index) : null;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
        int x = listX();
        int w = listW();
        int rows = visibleRows();
        graphics.fill(x - 2, LIST_TOP - 2, x + w + 2, LIST_TOP + rows * ROW + 2, 0xA0000000);

        if (filtered.isEmpty()) {
            String text = all.isEmpty() ? "No saved layouts yet. Use Export → Save to file." : "No layouts match your search.";
            graphics.centeredText(this.font, Component.literal(text), this.width / 2, LIST_TOP + 8, 0xFF888888);
        }

        ButtonLayout.Saved hovered = entryAt(mouseX, mouseY);
        for (int i = 0; i < rows; i++) {
            int index = i + scroll;
            if (index >= filtered.size()) break;
            ButtonLayout.Saved saved = filtered.get(index);
            int y = LIST_TOP + i * ROW;
            if (saved.equals(selected)) graphics.fill(x, y, x + w, y + ROW, 0x60FFFFFF);
            else if (saved.equals(hovered)) graphics.fill(x, y, x + w, y + ROW, 0x30FFFFFF);
            ButtonLayout layout = layout(saved);
            graphics.text(this.font, Component.literal(saved.name()), x + 4, y + 2, layout == null ? 0xFFFF5555 : 0xFFFFFFFF, true);
            String info = DATE.format(new Date(saved.modified().toMillis())) + " · " + (layout == null ? "Unreadable" : layout.describeLocations());
            graphics.text(this.font, Component.literal(this.font.plainSubstrByWidth(info, w - 8)), x + 4, y + 12, 0xFF888888, false);
        }

        if (maxScroll() > 0) {
            int track = rows * ROW;
            int thumb = Math.max(8, track * rows / filtered.size());
            int thumbY = LIST_TOP + (track - thumb) * scroll / maxScroll();
            graphics.fill(x + w + 3, thumbY, x + w + 6, thumbY + thumb, 0xFFAAAAAA);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        if (hovered != null) drawPreview(graphics, hovered, mouseX, mouseY);
    }

    private void drawPreview(GuiGraphicsExtractor graphics, ButtonLayout.Saved saved, int mouseX, int mouseY) {
        ButtonLayout layout = layout(saved);
        if (layout == null) return;
        boolean own = layout.hasOwnSet(location);
        String caption = location.isEmpty() || !own ? "Default" : HudLocations.name(location);
        int w = InventoryButtons.GRID_W + 10;
        int h = InventoryButtons.GRID_H + 22;
        int x = mouseX + 12;
        int y = mouseY - h / 2;
        if (x + w > this.width - 2) x = mouseX - w - 12;
        y = Math.clamp(y, 2, Math.max(2, this.height - h - 2));
        Draw.panel(graphics, x, y, w, h);
        graphics.text(this.font, Component.literal(caption), x + 5, y + 4, 0xFF404040, false);
        InventoryButtons.drawGrid(graphics, this.font, layout.set(location), x + 5, y + 16, null);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        ButtonLayout.Saved entry = entryAt(event.x(), event.y());
        if (entry != null && event.button() == 0) {
            long now = System.currentTimeMillis();
            boolean twice = entry.equals(selected) && now - lastClick < 400;
            lastClick = now;
            if (!entry.equals(selected)) confirmDelete = false;
            selected = entry;
            updateButtons();
            if (twice || doubleClick) load();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY == 0) return false;
        scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll());
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
