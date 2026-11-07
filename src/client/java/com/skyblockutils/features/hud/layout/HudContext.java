package com.skyblockutils.features.hud.layout;

import com.skyblockutils.utils.RoundedRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class HudContext {
    private static final float EDGE_EPSILON = 0.5f;

    public final GuiGraphicsExtractor graphics;
    public final Minecraft mc;
    public final Font font;
    public final HudManager.Bounds bounds;
    public final int width;
    public final int height;
    public final boolean preview;

    private final int screenWidth;
    private final int screenHeight;

    public HudContext(GuiGraphicsExtractor graphics, HudManager.Bounds bounds, boolean preview) {
        this.graphics = graphics;
        this.mc = Minecraft.getInstance();
        this.font = mc.font;
        this.bounds = bounds;
        this.width = bounds.width();
        this.height = bounds.height();
        this.preview = preview;
        this.screenWidth = mc.getWindow().getGuiScaledWidth();
        this.screenHeight = mc.getWindow().getGuiScaledHeight();
    }

    public float screenX(float localX) {
        return bounds.x() + localX * bounds.scale();
    }

    public float screenY(float localY) {
        return bounds.y() + localY * bounds.scale();
    }

    public float scale() {
        return bounds.scale();
    }

    public boolean stuckLeft(float localX) {
        return screenX(localX) <= EDGE_EPSILON;
    }

    public boolean stuckRight(float localX) {
        return screenX(localX) >= screenWidth - EDGE_EPSILON;
    }

    public boolean stuckTop(float localY) {
        return screenY(localY) <= EDGE_EPSILON;
    }

    public boolean stuckBottom(float localY) {
        return screenY(localY) >= screenHeight - EDGE_EPSILON;
    }

    public RoundedRect.Edges edges(int x1, int y1, int x2, int y2) {
        return new RoundedRect.Edges(stuckLeft(x1), stuckTop(y1), stuckRight(x2), stuckBottom(y2));
    }

    public int[] inner(int x1, int y1, int x2, int y2) {
        return RoundedRect.inner(x1, y1, x2, y2, edges(x1, y1, x2, y2));
    }

    public boolean fitsOnScreen(float x1, float y1, float x2, float y2) {
        return screenX(x1) >= -EDGE_EPSILON && screenY(y1) >= -EDGE_EPSILON
                && screenX(x2) <= screenWidth + EDGE_EPSILON && screenY(y2) <= screenHeight + EDGE_EPSILON;
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        if (x2 > x1 && y2 > y1 && (color >>> 24) != 0) graphics.fill(x1, y1, x2, y2, color);
    }

    public void panel(int x1, int y1, int x2, int y2, int radius, int background, int border) {
        RoundedRect.panel(graphics, x1, y1, x2, y2, radius, background, border, edges(x1, y1, x2, y2));
    }

    public void overlay(Identifier texture, float x, float y, float size, int textureSize) {
        if (!fitsOnScreen(x, y, x + size, y + size)) return;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, y);
            graphics.pose().scale(size / textureSize, size / textureSize);
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, textureSize, textureSize, textureSize, textureSize);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    public List<FormattedCharSequence> wrap(Component text, int maxWidth) {
        return font.split(text, Math.max(1, maxWidth));
    }

    public void text(FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        graphics.text(font, text, x, y, color, shadow);
    }

    public void text(Component text, int x, int y, int color, boolean shadow) {
        graphics.text(font, text, x, y, color, shadow);
    }

    public void text(String text, int x, int y, int color, boolean shadow) {
        graphics.text(font, text, x, y, color, shadow);
    }

    public void scaledText(FormattedCharSequence text, float x, float y, float textScale, int color, boolean shadow) {
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, y);
            graphics.pose().scale(textScale, textScale);
            graphics.text(font, text, 0, 0, color, shadow);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    public void scaledText(String text, float x, float y, float textScale, int color, boolean shadow) {
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, y);
            graphics.pose().scale(textScale, textScale);
            graphics.text(font, text, 0, 0, color, shadow);
        } finally {
            graphics.pose().popMatrix();
        }
    }
}
