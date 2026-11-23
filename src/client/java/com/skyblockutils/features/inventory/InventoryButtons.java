package com.skyblockutils.features.inventory;

import com.skyblockutils.StrayersSkyblockUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class InventoryButtons {
    public static final int AREA_X = 96;
    public static final int AREA_Y = 4;
    public static final int AREA_W = 76;
    public static final int AREA_H = 79;
    public static final int BUTTON_W = 18;
    public static final int BUTTON_H = 16;
    public static final int VANILLA_RECIPE_X = AREA_X;
    public static final int VANILLA_RECIPE_Y = AREA_Y;

    private static final int FIRST_X = 96;
    private static final int FIRST_Y = 5;
    private static final int ROW_SPACING = 26;
    private static final int BUTTON_SPACING = 19;
    private static final int LABEL_COLOR = -12566464;
    private static final float LABEL_SCALE = 0.75f;
    private static final float ICON_SCALE = 0.75f;
    private static final int STRIP_U = 96;
    private static final int STRIP_V = 4;
    private static final int STRIP_H = 12;

    private static final int SPRITE_W = 20;
    private static final int SPRITE_H = 18;
    private static final int FACE_U = 2;
    private static final int FACE_V = 2;
    private static final int FACE_W = 6;
    private static final int FACE_H = 2;
    private static final String SKYBLOCKER_SCREEN = "de.hysky.skyblocker.skyblock.item.SkyblockInventoryScreen";

    private static int deferredLeft;
    private static int deferredTop;
    private static boolean deferred;
    private static Identifier deferredTexture;

    private record Entry(ItemStack icon, String tooltip, String command, boolean recipeBook) {
        Entry(ItemStack icon, String tooltip, String command) {
            this(icon, tooltip, command, false);
        }
    }

    private record Row(String label, List<Entry> entries) {
    }

    private static final Entry EMPTY = new Entry(ItemStack.EMPTY, null, null);

    private static final List<Row> ROWS = List.of(
            new Row("Stashes", List.of(
                    new Entry(new ItemStack(Items.BUNDLE), "Open Material Stash", "viewstash material"),
                    new Entry(new ItemStack(Items.ITEM_FRAME), "Open Item Stash", "viewstash item"),
                    new Entry(new ItemStack(Items.HOPPER), "Pickup All Stashes", "pickupstash"),
                    new Entry(new ItemStack(Items.LAVA_BUCKET), "Clear All Stashes", "clearstash")
            )),
            new Row("TODO", List.of(EMPTY, EMPTY, EMPTY, EMPTY)),
            new Row("Utility", List.of(new Entry(ItemStack.EMPTY, "Recipe Book", "recipebook", true), EMPTY, EMPTY, EMPTY))
    );

    private InventoryButtons() {
    }

    public static boolean active() {
        return StrayersSkyblockUtilsClient.isInSkyblock;
    }

    private static int rowY(int row) {
        return FIRST_Y + row * ROW_SPACING;
    }

    private static int buttonX(int index) {
        return FIRST_X + index * BUTTON_SPACING;
    }

    private static int buttonY(int row) {
        return rowY(row) + 8;
    }

    private static Entry entryAt(int left, int top, double mouseX, double mouseY) {
        for (int row = 0; row < ROWS.size(); row++) {
            List<Entry> entries = ROWS.get(row).entries();
            for (int i = 0; i < entries.size(); i++) {
                int x = left + buttonX(i);
                int y = top + buttonY(row);
                if (mouseX >= x && mouseX < x + BUTTON_W && mouseY >= y && mouseY < y + BUTTON_H) return entries.get(i);
            }
        }
        return null;
    }

    public static void extractBackground(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, Identifier inventoryTexture, int left, int top, int mouseX, int mouseY) {
        if (screen.getClass().getName().equals(SKYBLOCKER_SCREEN)) {
            deferred = true;
            deferredLeft = left;
            deferredTop = top;
            deferredTexture = inventoryTexture;
            return;
        }
        draw(graphics, inventoryTexture, left, top, mouseX, mouseY);
    }

    public static void extractDeferred(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!deferred) return;
        deferred = false;
        draw(graphics, deferredTexture, deferredLeft, deferredTop, mouseX, mouseY);
    }

    private static void drawButton(GuiGraphicsExtractor graphics, int x, int y, boolean hovered, boolean keepBook) {
        Identifier sprite = RecipeBookComponent.RECIPE_BUTTON_SPRITES.get(true, hovered);
        int halfW = BUTTON_W / 2;
        int halfH = BUTTON_H / 2;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, SPRITE_W, SPRITE_H, 0, 0, x, y, halfW, halfH);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, SPRITE_W, SPRITE_H, SPRITE_W - halfW, 0, x + halfW, y, halfW, halfH);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, SPRITE_W, SPRITE_H, 0, SPRITE_H - halfH, x, y + halfH, halfW, halfH);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, SPRITE_W, SPRITE_H, SPRITE_W - halfW, SPRITE_H - halfH, x + halfW, y + halfH, halfW, halfH);
        if (keepBook) return;
        for (int fy = 2; fy < BUTTON_H - 2; fy += FACE_H) {
            int h = Math.min(FACE_H, BUTTON_H - 2 - fy);
            for (int fx = 2; fx < BUTTON_W - 2; fx += FACE_W) {
                int w = Math.min(FACE_W, BUTTON_W - 2 - fx);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, SPRITE_W, SPRITE_H, FACE_U, FACE_V, x + fx, y + fy, w, h);
            }
        }
    }

    private static void draw(GuiGraphicsExtractor graphics, Identifier inventoryTexture, int left, int top, int mouseX, int mouseY) {
        for (int y = 0; y < AREA_H; y += STRIP_H) {
            int h = Math.min(STRIP_H, AREA_H - y);
            graphics.blit(RenderPipelines.GUI_TEXTURED, inventoryTexture, left + AREA_X, top + AREA_Y + y, STRIP_U, STRIP_V, AREA_W, h, 256, 256);
        }

        Entry hovered = entryAt(left, top, mouseX, mouseY);
        for (int row = 0; row < ROWS.size(); row++) {
            List<Entry> entries = ROWS.get(row).entries();
            for (int i = 0; i < entries.size(); i++) {
                Entry entry = entries.get(i);
                int x = left + buttonX(i);
                int y = top + buttonY(row);
                drawButton(graphics, x, y, entry == hovered, entry.recipeBook());
                if (entry.icon().isEmpty()) continue;
                graphics.pose().pushMatrix();
                graphics.pose().translate(x + 3, y + 2);
                graphics.pose().scale(ICON_SCALE, ICON_SCALE);
                graphics.fakeItem(entry.icon(), 0, 0);
                graphics.pose().popMatrix();
            }
        }
    }

    public static void extractLabels(GuiGraphicsExtractor graphics, Font font) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(LABEL_SCALE, LABEL_SCALE);
        for (int row = 0; row < ROWS.size(); row++) {
            graphics.text(font, ROWS.get(row).label(), Math.round((FIRST_X + 1) / LABEL_SCALE), Math.round(rowY(row) / LABEL_SCALE), LABEL_COLOR, false);
        }
        graphics.pose().popMatrix();
    }

    public static void extractTooltip(GuiGraphicsExtractor graphics, Font font, int left, int top, int mouseX, int mouseY) {
        Entry entry = entryAt(left, top, mouseX, mouseY);
        if (entry == null || entry.tooltip() == null) return;
        graphics.setTooltipForNextFrame(font, Component.literal(entry.tooltip()), mouseX, mouseY);
    }

    public static boolean click(AbstractContainerScreen<?> screen, int left, int top, double mouseX, double mouseY) {
        Entry entry = entryAt(left, top, mouseX, mouseY);
        if (entry == null) return false;
        if (entry.command() != null) {
            Minecraft client = Minecraft.getInstance();
            if (client.getConnection() != null) {
                screen.clearFocus();
                client.getConnection().sendCommand(entry.command());
            }
        }
        return true;
    }
}
