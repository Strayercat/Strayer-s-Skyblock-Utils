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
    public final HudElement element;
    public final Minecraft mc;
    public final Font font;
    public final HudManager.Bounds bounds;
    public final int width;
    public final int height;
    public final boolean preview;

    private final int screenWidth;
    private final int screenHeight;

    public HudContext(GuiGraphicsExtractor graphics, HudElement element, HudManager.Bounds bounds, boolean preview) {
        this.graphics = graphics;
        this.element = element;
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

    private HudManager.Attachment attachment(int x1, int y1, int x2, int y2) {
        return HudManager.attachment(element, screenX(x1), screenY(y1), screenX(x2), screenY(y2), preview);
    }

    public RoundedRect.Edges edges(int x1, int y1, int x2, int y2) {
        return edges(x1, y1, x2, y2, attachment(x1, y1, x2, y2));
    }

    private RoundedRect.Edges edges(int x1, int y1, int x2, int y2, HudManager.Attachment a) {
        return new RoundedRect.Edges(stuckLeft(x1) || a.hideLeft(), stuckTop(y1) || a.hideTop(),
                stuckRight(x2) || a.hideRight(), stuckBottom(y2) || a.hideBottom());
    }

    private RoundedRect.Corners corners(int x1, int y1, int x2, int y2, HudManager.Attachment a) {
        if (element != null && HudManager.layout(element).squareCorners) return new RoundedRect.Corners(false, false, false, false);
        boolean left = stuckLeft(x1), top = stuckTop(y1), right = stuckRight(x2), bottom = stuckBottom(y2);
        return new RoundedRect.Corners(
                !(left || top || a.squareTopLeft()),
                !(right || top || a.squareTopRight()),
                !(left || bottom || a.squareBottomLeft()),
                !(right || bottom || a.squareBottomRight())
        );
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
        HudManager.Attachment a = attachment(x1, y1, x2, y2);
        RoundedRect.panel(graphics, x1, y1, x2, y2, radius, background, border, edges(x1, y1, x2, y2, a), corners(x1, y1, x2, y2, a));
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
