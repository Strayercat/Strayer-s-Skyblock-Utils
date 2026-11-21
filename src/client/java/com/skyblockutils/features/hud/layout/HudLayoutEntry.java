package com.skyblockutils.features.hud.layout;

import java.util.HashMap;
import java.util.Map;

public class HudLayoutEntry {
    public enum Anchor { START, CENTER, END }

    public enum Side { ABOVE, BELOW, LEFT, RIGHT }

    public Anchor anchorX;
    public Anchor anchorY;
    public float offsetX;
    public float offsetY;
    public float scale;
    public int width = 0;
    public String attachTo = null;
    public Side attachSide = null;
    public float attachOffset = 0f;
    public boolean squareCorners = false;
    public Map<String, Boolean> options = null;

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
        copy.attachTo = attachTo;
        copy.attachSide = attachSide;
        copy.attachOffset = attachOffset;
        copy.squareCorners = squareCorners;
        copy.options = options == null ? null : new HashMap<>(options);
        return copy;
    }

    public boolean isAttached() {
        return attachTo != null && attachSide != null;
    }

    public void detach() {
        attachTo = null;
        attachSide = null;
        attachOffset = 0f;
    }

    public HudLayoutEntry sanitize() {
        if (anchorX == null) anchorX = Anchor.START;
        if (anchorY == null) anchorY = Anchor.START;
        if (!Float.isFinite(offsetX)) offsetX = 0f;
        if (!Float.isFinite(offsetY)) offsetY = 0f;
        if (!Float.isFinite(scale) || scale <= 0f) scale = 1f;
        scale = Math.clamp(scale, HudManager.MIN_SCALE, HudManager.MAX_SCALE);
        if (width < 0) width = 0;
        if (!Float.isFinite(attachOffset)) attachOffset = 0f;
        if (attachTo == null || attachSide == null) detach();
        return this;
    }
}
