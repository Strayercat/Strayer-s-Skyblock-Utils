package com.skyblockutils.features.hud.layout;

public abstract class HudElement {
    public final String id;
    public final String name;

    protected HudElement(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public abstract HudLayoutEntry defaultLayout();

    public int minWidth() {
        return 60;
    }

    public int maxWidth() {
        return 500;
    }

    public abstract int autoWidth(boolean preview);

    public int contentWidth(int layoutWidth, boolean preview) {
        return layoutWidth;
    }

    public abstract int height(int width, boolean preview);

    public boolean isEnabled() {
        return true;
    }

    public abstract boolean isActive();

    public abstract void render(HudContext ctx);
}
