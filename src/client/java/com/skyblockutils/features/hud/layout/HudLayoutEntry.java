package com.skyblockutils.features.hud.layout;

public class HudLayoutEntry {
    public enum Anchor { START, CENTER, END }

    public Anchor anchorX;
    public Anchor anchorY;
    public float offsetX;
    public float offsetY;
    public float scale;
    public int width = 0;

    public HudLayoutEntry(Anchor anchorX, Anchor anchorY, float offsetX, float offsetY, float scale) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.scale = scale;
    }

    public HudLayoutEntry copy() {
        HudLayoutEntry copy = new HudLayoutEntry(anchorX, anchorY, offsetX, offsetY, scale);
        copy.width = width;
        return copy;
    }

    public HudLayoutEntry sanitize() {
        if (anchorX == null) anchorX = Anchor.START;
        if (anchorY == null) anchorY = Anchor.START;
        if (!Float.isFinite(offsetX)) offsetX = 0f;
        if (!Float.isFinite(offsetY)) offsetY = 0f;
        if (!Float.isFinite(scale) || scale <= 0f) scale = 1f;
        scale = Math.clamp(scale, HudManager.MIN_SCALE, HudManager.MAX_SCALE);
        if (width < 0) width = 0;
        return this;
    }
}
