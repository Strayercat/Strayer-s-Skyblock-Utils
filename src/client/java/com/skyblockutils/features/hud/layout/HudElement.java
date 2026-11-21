package com.skyblockutils.features.hud.layout;

import java.util.List;
import java.util.function.BooleanSupplier;

public abstract class HudElement {
    public record Option(String key, String label, BooleanSupplier available, BooleanSupplier defaultValue, boolean global) {
        public Option(String key, String label, BooleanSupplier available, BooleanSupplier defaultValue) {
            this(key, label, available, defaultValue, false);
        }

        public Option(String key, String label, BooleanSupplier defaultValue) {
            this(key, label, () -> true, defaultValue, false);
        }

        public static Option global(String key, String label, BooleanSupplier defaultValue) {
            return new Option(key, label, () -> true, defaultValue, true);
        }
    }

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

    public int contentWidth(int layoutWidth) {
        return layoutWidth;
    }

    public abstract int height(int width, boolean preview);

    public boolean isEnabled() {
        return true;
    }

    public abstract boolean isActive();

    public abstract void render(HudContext ctx);

    public List<Option> options() {
        return List.of();
    }

    public final boolean show(String key) {
        for (Option option : options()) {
            if (option.key().equals(key)) return HudManager.optionEnabled(this, option);
        }
        return true;
    }
}
