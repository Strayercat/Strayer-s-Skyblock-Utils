package com.skyblockutils.utils;

import net.minecraft.resources.Identifier;

public enum GemColor {
    BLUE(0, "ssu_gem", 0xE7A5),
    SPECIAL(1, "ssu_gem_special", 0xE7B0),
    PURPLE(2, "ssu_gem_purple", 0xE7A6),
    GOLD(3, "ssu_gem_gold", 0xE7A7),
    RED(4, "ssu_gem_red", 0xE7A8),
    GREEN(5, "ssu_gem_green", 0xE7A9),
    ORANGE(6, "ssu_gem_orange", 0xE7AA),
    PINK(7, "ssu_gem_pink", 0xE7AB),
    WHITE(8, "ssu_gem_white", 0xE7AC),
    BLACK(9, "ssu_gem_black", 0xE7AD),
    GRAY(10, "ssu_gem_gray", 0xE7AE);

    private static final GemColor[] BY_ID = new GemColor[256];

    static {
        for (GemColor color : values()) BY_ID[color.id] = color;
    }

    public final int id;
    public final Identifier sprite;
    public final int glyph;

    GemColor(int id, String sprite, int glyph) {
        this.id = id;
        this.sprite = Identifier.fromNamespaceAndPath("skyblockutils", sprite);
        this.glyph = glyph;
    }

    public String glyphString() {
        return Character.toString(glyph);
    }

    public static GemColor byId(int id) {
        GemColor color = id >= 0 && id < BY_ID.length ? BY_ID[id] : null;
        return color != null ? color : BLUE;
    }

    public static boolean isGemGlyph(int codepoint) {
        return (codepoint >= 0xE7A5 && codepoint <= 0xE7AE) || SSUIndicator.isSpecialGlyph(codepoint);
    }
}
