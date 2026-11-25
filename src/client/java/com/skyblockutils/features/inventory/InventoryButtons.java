package com.skyblockutils.features.inventory;

import com.mojang.blaze3d.platform.NativeImage;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.hud.layout.HudManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InventoryButtons {
    public static final int AREA_X = 96;
    public static final int AREA_Y = 4;
    public static final int AREA_W = 76;
    public static final int AREA_H = 79;
    public static final int BUTTON_W = 18;
    public static final int BUTTON_H = 16;
    public static final int ROW_SPACING = 26;
    public static final int BUTTON_SPACING = 19;
    public static final int LABEL_H = 8;
    public static final int GRID_W = BUTTON_W + (ButtonLayout.COLUMNS - 1) * BUTTON_SPACING;
    public static final int GRID_H = (ButtonLayout.ROWS - 1) * ROW_SPACING + LABEL_H + BUTTON_H;

    private static final int FIRST_X = 96;
    private static final int FIRST_Y = 5;
    private static final int LABEL_COLOR = -12566464;
    private static final float LABEL_SCALE = 0.75f;
    private static final int ICON_SIZE = 12;
    private static final int OFFHAND_X = 76;
    private static final int OFFHAND_Y = 61;
    private static final int OFFHAND_SIZE = 18;
    private static final int FALLBACK_BACKGROUND = 0xFFC6C6C6;
    private static final int[][] BACKGROUND_SAMPLES = {
            {86, 20}, {86, 45}, {86, 70}, {90, 10}, {160, 10}, {160, 70}, {110, 70}, {140, 70}
    };

    private static final int SPRITE_W = 20;
    private static final int SPRITE_H = 18;
    private static final int FACE_U = 2;
    private static final int FACE_V = 2;
    private static final int FACE_W = 6;
    private static final int FACE_H = 2;
    private static final Identifier INVENTORY_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/inventory.png");

    private static Identifier sampledTexture;
    private static int sampledGeneration = -1;
    private static int background = FALLBACK_BACKGROUND;

    public record Hit(int row, int column) {
        public boolean isLabel() {
            return column < 0;
        }
    }

    private InventoryButtons() {
    }

    public static boolean active() {
        return StrayersSkyblockUtilsClient.isInSkyblock && ModConfig.INSTANCE.inventoryButtons;
    }

    public static void resetHead() {
        ButtonIcons.resetHead();
    }

    public static ButtonLayout.ButtonSet currentSet() {
        return ButtonLayout.INSTANCE.set(HudManager.currentLocation());
    }

    public static int rowY(int row) {
        return row * ROW_SPACING;
    }

    public static int buttonX(int column) {
        return column * BUTTON_SPACING;
    }

    public static int buttonY(int row) {
        return rowY(row) + LABEL_H;
    }

    public static Hit hitAt(double x, double y) {
        for (int row = 0; row < ButtonLayout.ROWS; row++) {
            if (y >= rowY(row) && y < rowY(row) + LABEL_H && x >= 0 && x < GRID_W) return new Hit(row, -1);
            for (int column = 0; column < ButtonLayout.COLUMNS; column++) {
                int bx = buttonX(column);
                int by = buttonY(row);
                if (x >= bx && x < bx + BUTTON_W && y >= by && y < by + BUTTON_H) return new Hit(row, column);
            }
        }
        return null;
    }

    private static ButtonLayout.Button buttonAt(int left, int top, double mouseX, double mouseY) {
        Hit hit = hitAt(mouseX - left - FIRST_X, mouseY - top - FIRST_Y);
        return hit == null || hit.isLabel() ? null : currentSet().button(hit.row(), hit.column());
    }

    public static void extractPanel(GuiGraphicsExtractor graphics, Font font, int left, int top, int mouseX, int mouseY) {
        draw(graphics, left, top, mouseX, mouseY);
        drawLabels(graphics, font, currentSet(), left + FIRST_X, top + FIRST_Y);
    }

    public static void coverOffhand(GuiGraphicsExtractor graphics, int left, int top) {
        graphics.fill(left + OFFHAND_X, top + OFFHAND_Y, left + OFFHAND_X + OFFHAND_SIZE, top + OFFHAND_Y + OFFHAND_SIZE, backgroundColor(INVENTORY_TEXTURE));
    }

    public static boolean coversMostly(int left, int top, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return false;
        return mostlyInside(left + AREA_X, top + AREA_Y, AREA_W, AREA_H, x, y, w, h)
                || mostlyInside(left + OFFHAND_X, top + OFFHAND_Y, OFFHAND_SIZE, OFFHAND_SIZE, x, y, w, h);
    }

    private static boolean mostlyInside(int zoneX, int zoneY, int zoneW, int zoneH, int x, int y, int w, int h) {
        int ix = Math.max(0, Math.min(x + w, zoneX + zoneW) - Math.max(x, zoneX));
        int iy = Math.max(0, Math.min(y + h, zoneY + zoneH) - Math.max(y, zoneY));
        return (long) ix * iy * 2 >= (long) w * h;
    }

    public static void drawButton(GuiGraphicsExtractor graphics, int x, int y, boolean hovered) {
        Identifier sprite = RecipeBookComponent.RECIPE_BUTTON_SPRITES.get(true, hovered);
        int halfW = BUTTON_W / 2;
        int halfH = BUTTON_H / 2;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, SPRITE_W, SPRITE_H, 0, 0, x, y, halfW, halfH);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, SPRITE_W, SPRITE_H, SPRITE_W - halfW, 0, x + halfW, y, halfW, halfH);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, SPRITE_W, SPRITE_H, 0, SPRITE_H - halfH, x, y + halfH, halfW, halfH);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, SPRITE_W, SPRITE_H, SPRITE_W - halfW, SPRITE_H - halfH, x + halfW, y + halfH, halfW, halfH);
        for (int fy = 2; fy < BUTTON_H - 2; fy += FACE_H) {
            int h = Math.min(FACE_H, BUTTON_H - 2 - fy);
            for (int fx = 2; fx < BUTTON_W - 2; fx += FACE_W) {
                int w = Math.min(FACE_W, BUTTON_W - 2 - fx);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, SPRITE_W, SPRITE_H, FACE_U, FACE_V, x + fx, y + fy, w, h);
            }
        }
    }

    public static void drawCell(GuiGraphicsExtractor graphics, Font font, ButtonLayout.Button button, int x, int y, boolean hovered) {
        drawButton(graphics, x, y, hovered);
        ButtonIcons.draw(graphics, font, ButtonIcons.icon(button.icon), x + 3, y + 2, ICON_SIZE);
    }

    public static void drawGrid(GuiGraphicsExtractor graphics, Font font, ButtonLayout.ButtonSet set, int x, int y, Hit hovered) {
        for (int row = 0; row < ButtonLayout.ROWS; row++) {
            for (int column = 0; column < ButtonLayout.COLUMNS; column++) {
                boolean hot = hovered != null && hovered.row() == row && hovered.column() == column;
                drawCell(graphics, font, set.button(row, column), x + buttonX(column), y + buttonY(row), hot);
            }
        }
        drawLabels(graphics, font, set, x, y);
    }

    public static void drawLabels(GuiGraphicsExtractor graphics, Font font, ButtonLayout.ButtonSet set, int x, int y) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 1, y);
        graphics.pose().scale(LABEL_SCALE, LABEL_SCALE);
        for (int row = 0; row < ButtonLayout.ROWS; row++) {
            graphics.text(font, ButtonText.parse(set.row(row).label), 0, Math.round(rowY(row) / LABEL_SCALE), LABEL_COLOR, false);
        }
        graphics.pose().popMatrix();
    }

    private static void draw(GuiGraphicsExtractor graphics, int left, int top, int mouseX, int mouseY) {
        graphics.fill(left + AREA_X, top + AREA_Y, left + AREA_X + AREA_W, top + AREA_Y + AREA_H, backgroundColor(InventoryButtons.INVENTORY_TEXTURE));

        Font font = Minecraft.getInstance().font;
        Hit hovered = hitAt(mouseX - left - FIRST_X, mouseY - top - FIRST_Y);
        ButtonLayout.ButtonSet set = currentSet();
        for (int row = 0; row < ButtonLayout.ROWS; row++) {
            for (int column = 0; column < ButtonLayout.COLUMNS; column++) {
                boolean hot = hovered != null && hovered.row() == row && hovered.column() == column;
                drawCell(graphics, font, set.button(row, column), left + FIRST_X + buttonX(column), top + FIRST_Y + buttonY(row), hot);
            }
        }
    }

    private static int backgroundColor(Identifier texture) {
        ButtonIcons.checkReload();
        if (texture.equals(sampledTexture) && sampledGeneration == ButtonIcons.generation()) return background;
        sampledTexture = texture;
        sampledGeneration = ButtonIcons.generation();
        background = sample(texture);
        return background;
    }

    private static int sample(Identifier texture) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) return FALLBACK_BACKGROUND;
        try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
            int factor = Math.max(1, image.getWidth() / 256);
            Map<Integer, Integer> counts = new HashMap<>();
            for (int[] point : BACKGROUND_SAMPLES) {
                int x = point[0] * factor;
                int y = point[1] * factor;
                if (x >= image.getWidth() || y >= image.getHeight()) continue;
                int color = image.getPixel(x, y);
                if ((color >>> 24) < 0xF0) continue;
                counts.merge(color, 1, Integer::sum);
            }
            return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(FALLBACK_BACKGROUND);
        } catch (Exception e) {
            return FALLBACK_BACKGROUND;
        }
    }

    public static void extractTooltip(GuiGraphicsExtractor graphics, Font font, int left, int top, int mouseX, int mouseY) {
        ButtonLayout.Button button = buttonAt(left, top, mouseX, mouseY);
        if (button == null) return;
        List<Component> lines = button.tooltip();
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    public static boolean click(AbstractContainerScreen<?> screen, int left, int top, double mouseX, double mouseY) {
        ButtonLayout.Button button = buttonAt(left, top, mouseX, mouseY);
        if (button == null) return false;
        Minecraft client = Minecraft.getInstance();
        button.actionFor(client.hasShiftDown(), client.hasControlDown()).run(screen);
        return true;
    }
}