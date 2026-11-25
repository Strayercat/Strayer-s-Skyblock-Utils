package com.skyblockutils.features.inventory;

import com.skyblockutils.ModKeyBindings;
import com.skyblockutils.features.chat.emoji.EmojiRegistry;
import com.skyblockutils.utils.FuzzySearch;
import com.skyblockutils.utils.SSU;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class IconPickerScreen extends Screen {
    private static final int CELL = 18;
    private static final int TABS_Y = 22;
    private static final int SEARCH_Y = 44;
    private static final int RECENT_Y = 76;
    private static final int SPRITE_ROOT = "textures/gui/sprites/".length();

    public enum Tab {
        MINECRAFT("Minecraft"), SKYBLOCK("SkyBlock"), SSU("SSU"), EMOJIS("Emojis");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private record Entry(String spec, ItemStack stack, Identifier sprite, String glyph, List<Component> tooltip, List<String> keys) {
        ButtonIcons.Icon icon() {
            return new ButtonIcons.Icon(stack == null ? ItemStack.EMPTY : stack, sprite, glyph);
        }
    }

    private static Tab lastTab = Tab.MINECRAFT;
    private static String lastQuery = "";
    private static List<Entry> minecraftEntries;
    private static List<Entry> ssuEntries;
    private static List<Entry> emojiEntries;
    private static List<Entry> skyblockEntries;
    private static int skyblockSize = -1;

    private final Screen parent;
    private final Consumer<String> onPick;
    private Tab tab = lastTab;
    private String query = lastQuery;
    private boolean headsOnly;
    private List<Entry> filtered = List.of();
    private List<Entry> recent = List.of();
    private int scroll;
    private PopupMenu menu;
    private SkyblockItems.State seenState;

    public IconPickerScreen(Screen parent, Consumer<String> onPick) {
        super(Component.literal("Pick an Icon"));
        this.parent = parent;
        this.onPick = onPick;
    }

    private int columns() {
        return Math.max(4, Math.min(20, (this.width - 40) / CELL));
    }

    private int gridLeft() {
        return this.width / 2 - columns() * CELL / 2;
    }

    private int gridTop() {
        return recent.isEmpty() ? RECENT_Y : RECENT_Y + CELL + 8;
    }

    private int visibleRows() {
        return Math.max(1, (this.height - gridTop() - 40) / CELL);
    }

    private int maxScroll() {
        int rows = (filtered.size() + columns() - 1) / columns();
        return Math.max(0, rows - visibleRows());
    }

    @Override
    protected void init() {
        menu = null;
        int gridW = columns() * CELL;
        int left = gridLeft();
        int tabW = (gridW - 3 * 3) / 4;
        for (int i = 0; i < Tab.values().length; i++) {
            Tab t = Tab.values()[i];
            Component label = Component.literal(t.label).withStyle(t == tab ? ChatFormatting.GREEN : ChatFormatting.WHITE);
            this.addRenderableWidget(Button.builder(label, _ -> switchTab(t, false)).bounds(left + i * (tabW + 3), TABS_Y, tabW, 18).build());
        }

        int pasteW = 54;
        int extraW = tab == Tab.SKYBLOCK ? 64 : 0;
        EditBox search = new EditBox(this.font, left, SEARCH_Y, gridW - pasteW - 3 - (extraW > 0 ? extraW + 3 : 0), 18, Component.empty());
        search.setHint(Component.literal("Search " + tab.label + "…"));
        search.setMaxLength(256);
        search.setValue(query);
        search.setResponder(v -> {
            query = v;
            lastQuery = v;
            scroll = 0;
            refilter();
        });
        this.addRenderableWidget(search);
        this.setInitialFocus(search);

        if (tab == Tab.SKYBLOCK) {
            Component label = Component.literal("Heads only").withStyle(headsOnly ? ChatFormatting.GREEN : ChatFormatting.GRAY);
            this.addRenderableWidget(Button.builder(label, _ -> {
                headsOnly = !headsOnly;
                scroll = 0;
                this.rebuildWidgets();
            }).bounds(left + gridW - pasteW - extraW - 3, SEARCH_Y, extraW, 18).build());
        }

        Button paste = this.addRenderableWidget(Button.builder(Component.literal("Paste"), _ -> pasteClipboard())
                .bounds(left + gridW - pasteW, SEARCH_Y, pasteW, 18).build());
        paste.setTooltip(Tooltip.create(Component.literal("Use an icon from your clipboard. Hover any item, even in a menu, and press ")
                .append(keyName()).append(" to copy its icon (works for any head's texture).")));

        recent = new ArrayList<>();
        List<Entry> recentEntries = new ArrayList<>();
        for (String spec : ButtonLayout.recent()) {
            ButtonIcons.Icon icon = ButtonIcons.icon(spec);
            List<Component> tooltip = icon.sprite() == null && icon.glyph() == null && !icon.item().isEmpty()
                    ? null : List.of(Component.literal(spec).withStyle(ChatFormatting.GRAY));
            recentEntries.add(new Entry(spec, icon.item(), icon.sprite(), icon.glyph(), tooltip, List.of(spec)));
            if (recentEntries.size() >= columns()) break;
        }
        recent = recentEntries;

        refilter();
    }

    private Component keyName() {
        return ModKeyBindings.COPY_ICON_KEY.isUnbound()
                ? Component.literal("[unbound, set it in Controls]").withStyle(ChatFormatting.RED)
                : ModKeyBindings.COPY_ICON_KEY.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.YELLOW);
    }

    private void switchTab(Tab next, boolean heads) {
        tab = next;
        lastTab = next;
        headsOnly = heads;
        scroll = 0;
        this.rebuildWidgets();
    }

    private void pasteClipboard() {
        String text = Minecraft.getInstance().keyboardHandler.getClipboard().trim();
        if (text.isEmpty() || text.contains("\n") || text.startsWith(ButtonLayout.EXPORT_PREFIX)) return;
        pick(text);
    }

    private void pick(String spec) {
        onPick.accept(spec);
    }

    private List<Entry> entries() {
        return switch (tab) {
            case MINECRAFT -> minecraft();
            case SKYBLOCK -> skyblock();
            case SSU -> ssu();
            case EMOJIS -> emojis();
        };
    }

    private void refilter() {
        List<Entry> source = entries();
        if (tab == Tab.SKYBLOCK && headsOnly) source = source.stream().filter(e -> e.stack().is(Items.PLAYER_HEAD)).toList();
        filtered = FuzzySearch.searchAny(query.trim(), source, Entry::keys);
        scroll = Math.clamp(scroll, 0, maxScroll());
    }

    private static List<Entry> minecraft() {
        if (minecraftEntries != null) return minecraftEntries;
        List<Entry> list = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ItemStack stack = new ItemStack(item);
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            String shortId = id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
            list.add(new Entry("item:" + shortId, stack, null, null, null, List.of(stack.getHoverName().getString(), shortId)));
        }
        minecraftEntries = list;
        return list;
    }

    private static List<Entry> skyblock() {
        List<SkyblockItems.SbItem> items = SkyblockItems.items();
        if (skyblockEntries != null && skyblockSize == items.size()) return skyblockEntries;
        List<Entry> list = new ArrayList<>();
        for (SkyblockItems.SbItem item : items) {
            list.add(new Entry(item.spec(), SkyblockItems.stack(item), null, null, null, List.of(item.name(), item.id().toLowerCase(Locale.ROOT))));
        }
        skyblockEntries = list;
        skyblockSize = items.size();
        return list;
    }

    private static List<Entry> ssu() {
        if (ssuEntries != null) return ssuEntries;
        List<Entry> list = new ArrayList<>();
        Minecraft.getInstance().getResourceManager()
                .listResources("textures/gui/sprites", id -> id.getNamespace().equals(SSU.NAMESPACE) && id.getPath().endsWith(".png")
                        && (id.getPath().contains("/sprites/buttons/") || id.getPath().substring(id.getPath().lastIndexOf('/') + 1).startsWith("ssu_gem")))
                .keySet().stream()
                .map(id -> id.getPath().substring(SPRITE_ROOT, id.getPath().length() - 4))
                .sorted((a, b) -> Boolean.compare(a.startsWith("ssu_gem"), b.startsWith("ssu_gem")) != 0
                        ? Boolean.compare(a.startsWith("ssu_gem"), b.startsWith("ssu_gem")) : a.compareTo(b))
                .forEach(path -> {
                    String name = pretty(path);
                    list.add(new Entry("sprite:" + path, null, SSU.id(path), null,
                            List.of(Component.literal(name), Component.literal(path).withStyle(ChatFormatting.DARK_GRAY)), List.of(name, path)));
                });
        ssuEntries = list;
        return list;
    }

    private static List<Entry> emojis() {
        if (emojiEntries != null) return emojiEntries;
        List<Entry> list = new ArrayList<>();
        for (EmojiRegistry.Emoji emoji : EmojiRegistry.custom()) list.add(emoji(emoji, true));
        for (EmojiRegistry.Emoji emoji : EmojiRegistry.normal()) list.add(emoji(emoji, false));
        emojiEntries = list;
        return list;
    }

    private static Entry emoji(EmojiRegistry.Emoji emoji, boolean custom) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(emoji.text() + " " + emoji.shortcode()));
        if (custom) tooltip.add(Component.literal("SSU exclusive").withStyle(ChatFormatting.DARK_GRAY));
        return new Entry("emoji:" + emoji.name(), null, null, emoji.text(), tooltip, emoji.names());
    }

    private static String pretty(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1).replace("ssu_gem", "gem").replace('_', ' ').trim();
        return name.isEmpty() ? path : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private Entry entryAt(double mouseX, double mouseY) {
        int left = gridLeft();
        if (mouseX < left || mouseX >= left + columns() * CELL) return null;
        int column = (int) ((mouseX - left) / CELL);
        if (!recent.isEmpty() && mouseY >= RECENT_Y && mouseY < RECENT_Y + CELL) {
            return column < recent.size() ? recent.get(column) : null;
        }
        int top = gridTop();
        if (mouseY < top) return null;
        int row = (int) ((mouseY - top) / CELL);
        if (row >= visibleRows()) return null;
        int index = (row + scroll) * columns() + column;
        return index < filtered.size() ? filtered.get(index) : null;
    }

    @Override
    public void tick() {
        super.tick();
        SkyblockItems.State state = SkyblockItems.state();
        if (tab == Tab.SKYBLOCK && state != seenState) {
            seenState = state;
            refilter();
        }
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        boolean menuOpen = menu != null;
        int mx = menuOpen ? -1 : mouseX;
        int my = menuOpen ? -1 : mouseY;
        graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
        int left = gridLeft();
        int columns = columns();
        Entry hovered = entryAt(mx, my);

        if (!recent.isEmpty()) {
            graphics.text(this.font, Component.literal("Recent"), left, RECENT_Y - 10, 0xFF888888, false);
            graphics.fill(left - 2, RECENT_Y - 1, left + columns * CELL + 2, RECENT_Y + CELL + 1, 0x60000000);
            for (int i = 0; i < recent.size(); i++) drawEntry(graphics, recent.get(i), left + i * CELL, RECENT_Y, recent.get(i) == hovered);
        }

        int top = gridTop();
        int rows = visibleRows();
        graphics.fill(left - 2, top - 2, left + columns * CELL + 2, top + rows * CELL + 2, 0xA0000000);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int index = (row + scroll) * columns + column;
                if (index >= filtered.size()) break;
                Entry entry = filtered.get(index);
                drawEntry(graphics, entry, left + column * CELL, top + row * CELL, entry == hovered);
            }
        }

        int maxScroll = maxScroll();
        if (maxScroll > 0) {
            int trackX = left + columns * CELL + 4;
            int trackH = rows * CELL;
            int thumbH = Math.max(8, trackH * rows / (rows + maxScroll));
            int thumbY = top + (trackH - thumbH) * scroll / maxScroll;
            graphics.fill(trackX, top, trackX + 3, top + trackH, 0x60000000);
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xFFAAAAAA);
        }

        int statusY = top + rows * CELL + 6;
        graphics.text(this.font, Component.literal(filtered.size() + " results" + status()), left, statusY, 0xFF777777, false);
        graphics.text(this.font, Component.literal("Hover any item and press ").append(keyName()).append(" to copy its icon, then Paste"),
                left, statusY + 11, 0xFF777777, false);

        super.extractRenderState(graphics, mx, my, delta);

        if (menuOpen) {
            menu.render(graphics, this.font, mouseX, mouseY);
            return;
        }
        if (hovered != null) {
            if (hovered.tooltip() != null) {
                graphics.setComponentTooltipForNextFrame(this.font, hovered.tooltip(), mouseX, mouseY);
            } else {
                graphics.setTooltipForNextFrame(this.font, hovered.stack(), mouseX, mouseY);
            }
        }
    }

    private String status() {
        if (tab != Tab.SKYBLOCK) return "";
        return switch (SkyblockItems.state()) {
            case LOADING -> " · loading SkyBlock items…";
            case FAILED -> " · couldn't reach the Hypixel API";
            default -> "";
        };
    }

    private void drawEntry(GuiGraphicsExtractor graphics, Entry entry, int x, int y, boolean hovered) {
        if (hovered) graphics.fill(x, y, x + CELL, y + CELL, 0x60FFFFFF);
        ButtonIcons.draw(graphics, this.font, entry.icon(), x + 1, y + 1, 16);
    }

    private void openHeadMenu(int x, int y) {
        menu = PopupMenu.open(this.font, Component.literal("Player Head"), List.of(
                PopupMenu.Item.of("Your head", () -> pick("self")),
                PopupMenu.Item.of("Plain head", () -> pick("item:player_head")),
                PopupMenu.Item.of("By username…", () -> this.minecraft.setScreenAndShow(new NamePromptScreen(this, "Player Head", "", name -> pick("head:" + name)))),
                PopupMenu.Item.separator(),
                PopupMenu.Item.of("&eSkyBlock heads →", () -> switchTab(Tab.SKYBLOCK, true))
        ), x, y, this.width, this.height);
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
            Entry entry = entryAt(event.x(), event.y());
            if (entry != null) {
                if (entry.spec().equals("item:player_head") && tab == Tab.MINECRAFT && !recent.contains(entry)) {
                    openHeadMenu((int) event.x(), (int) event.y());
                } else {
                    pick(entry.spec());
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (menu != null) {
            if (menu.contains(x, y)) menu.scroll(scrollY);
            return true;
        }
        if (scrollY == 0) return false;
        scroll = Math.clamp(scroll - (int) Math.signum(scrollY) * (this.minecraft.hasShiftDown() ? 5 : 1), 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (menu != null && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            menu = null;
            return true;
        }
        if (event.isConfirmation() && getFocused() instanceof EditBox && !filtered.isEmpty()) {
            Entry first = filtered.getFirst();
            if (first.spec().equals("item:player_head") && tab == Tab.MINECRAFT) openHeadMenu(this.width / 2, this.height / 2);
            else pick(first.spec());
            return true;
        }
        return super.keyPressed(event);
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
