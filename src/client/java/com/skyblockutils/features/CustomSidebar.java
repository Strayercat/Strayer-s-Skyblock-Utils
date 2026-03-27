package com.skyblockutils.features;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.SSU;
import com.skyblockutils.utils.SideBarUtils;
import com.skyblockutils.utils.ModStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

import static com.skyblockutils.utils.ModStyle.COLOR_BACKGROUND;

public class CustomSidebar {
    private static final Identifier CAT_IMAGE = Identifier.of("skyblockutils", "textures/hud/cat_sidebar.png");
    private static final int COLOR_MAIN = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
    private static final int COLOR_TITLE_START = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TITLE_START);
    private static final int COLOR_TITLE_END = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TITLE_END);
    private static int sideBarWidth;

    public static void displayCustomSidebar(DrawContext context) {
        if (ModConfig.INSTANCE.sideBarInHud) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int sideBarHeight = 120;

        drawRoundedRect(context, client, screenWidth - sideBarWidth, (screenHeight / 2) - (sideBarHeight / 2), screenWidth, (screenHeight / 2) + (sideBarHeight / 2), 12);
    }

    public static void drawRoundedRect(DrawContext context, MinecraftClient client, int x1, int y1, int x2, int y2, int radius) {
        // Background
        context.fill(x1 + radius, y1, x2, y2, COLOR_BACKGROUND);
        context.fill(x1, y1 + radius, x1 + radius, y2 - radius, COLOR_BACKGROUND);

        // Outline
        context.fill(x1 + radius, y1, x2, y1 + 1, COLOR_MAIN);
        context.fill(x1 + radius, y2 - 1, x2, y2, COLOR_MAIN);
        context.fill(x1, y1 + radius, x1 + 1, y2 - radius, COLOR_MAIN);

        // Corners and their outline
        drawCorner(context, x1 + radius - 1, y1 + radius - 1, radius, 2);
        drawCorner(context, x1 + radius - 1, y2 - radius, radius, 1);
        drawCornerOutline(context, x1 + radius - 1, y1 + radius - 1, radius, 2);
        drawCornerOutline(context, x1 + radius - 1, y2 - radius, radius, 1);

        // Title + separation bar
        int barPosition = y1 + radius + 3;
        context.drawTextWithShadow(client.textRenderer, SSU.gradientText("Skyblock", COLOR_TITLE_START, COLOR_TITLE_END), x1 + ((x2 - x1) / 2) - client.textRenderer.getWidth("Skyblock") / 2, y1 + ((barPosition - y1) / 2) - client.textRenderer.fontHeight / 2 + 1, 0xFFFFFFFF);
        context.fill(x1, barPosition, x2, barPosition + 1, COLOR_MAIN);

        drawSidebarText(context, client, x1, x2, barPosition + 1, y2 - barPosition + 1, 10);

        context.getMatrices().pushMatrix();
        float imageScale = 0.13f;
        int imageSize = (int)(418 * imageScale); // ~83px
        context.getMatrices().translate(x1 + (float)(x2 - x1) / 2 - (float)imageSize / 2 - 10, y1 - imageSize + 31 * imageScale);
        context.getMatrices().scale(imageScale, imageScale);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, CAT_IMAGE, 0, 0, 0, 0, 418, 418, 418, 418);
        context.getMatrices().popMatrix();
    }

    private static void drawCorner(DrawContext context, int cx, int cy, int r, int quadrant) {
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                if (x * x + y * y <= (r - 1) * (r - 1)) {
                    int drawX = cx + x;
                    int drawY = cy + y;

                    switch (quadrant) {
                        case 1:
                            if (x <= 0 && y >= 0)
                                context.fill(drawX, drawY, drawX + 1, drawY + 1, COLOR_BACKGROUND);
                            break; // BL
                        case 2:
                            if (x <= 0 && y <= 0)
                                context.fill(drawX, drawY, drawX + 1, drawY + 1, COLOR_BACKGROUND);
                            break; // TL
                    }
                }
            }
        }
    }

    private static void drawCornerOutline(DrawContext context, int cx, int cy, int r, int quadrant) {
        int outer = r * r;
        int inner = (r - 1) * (r - 1);

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                int distSq = x * x + y * y;

                if (distSq <= outer && distSq > inner) {
                    int drawX = cx + x;
                    int drawY = cy + y;

                    if (Math.abs(x) == r) drawX += (x > 0 ? -1 : 1);
                    if (Math.abs(y) == r) drawY += (y > 0 ? -1 : 1);

                    switch (quadrant) {
                        case 1:
                            if (x <= 0 && y >= 0)
                                context.fill(drawX, drawY, drawX + 1, drawY + 1, CustomSidebar.COLOR_MAIN);
                            break;
                        case 2:
                            if (x <= 0 && y <= 0)
                                context.fill(drawX, drawY, drawX + 1, drawY + 1, CustomSidebar.COLOR_MAIN);
                            break;
                    }
                }
            }
        }
    }

    public static void drawSidebarText(DrawContext context, MinecraftClient client, int x1, int x2, int topPosition, int maxHeight, int padding) {
        float textScale = 0.8f;

        List<Text> lines = new java.util.ArrayList<>(SideBarUtils.getSidebarLines().stream()
                .filter(line -> !line.replaceAll("(?i)§.", "").trim().isEmpty()
                        && !line.replaceAll("§.", "").trim().matches("\\d{2}/\\d{2}/\\d{2} .*")
                        && !line.replaceAll("§.", "").trim().matches("www.hypixel.net"))
                .map(line -> (Text) Text.literal(line.trim()))
                .toList());

        if (ModConfig.INSTANCE.sidebarCoords) {
            lines.add(ModFunctions.getFormattedCoordinates());
        }

        int maxTextWidth = lines.stream()
                .mapToInt(line -> (int) (client.textRenderer.getWidth(line) * textScale))
                .max()
                .orElse(0);

        sideBarWidth = Math.max(110, maxTextWidth);

        if (lines.isEmpty()) return;

        int scaledFontHeight = (int) (client.textRenderer.fontHeight * textScale);
        int maxSpacePerLine = (maxHeight - padding) / lines.size();
        int verticalOffset = maxSpacePerLine / 2 - scaledFontHeight / 2 + padding / 2;

        for (int i = 0; i < lines.size(); i++) {
            int x = x1 + padding / 2;
            int y = topPosition + verticalOffset + (i * maxSpacePerLine);

            context.getMatrices().pushMatrix();
            context.getMatrices().translate(x, y);
            context.getMatrices().scale(textScale, textScale);
            context.drawTextWithShadow(client.textRenderer, lines.get(i), 0, 0, 0xFFFFFFFF);
            context.getMatrices().popMatrix();
        }
    }
}
