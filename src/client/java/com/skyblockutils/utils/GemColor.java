package com.skyblockutils.utils;

import net.minecraft.resources.Identifier;

public enum GemColor {
    BLUE(0, "ssu_gem", 0xE7A5, 0x3AC8D8),
    SPECIAL(1, "ssu_gem_special", 0xE7B0, 0xFFFFFF),
    PURPLE(2, "ssu_gem_purple", 0xE7A6, 0xA855F7),
    GOLD(3, "ssu_gem_gold", 0xE7A7, 0xEAB308),
    RED(4, "ssu_gem_red", 0xE7A8, 0xDC2626),
    GREEN(5, "ssu_gem_green", 0xE7A9, 0x16A34A),
    ORANGE(6, "ssu_gem_orange", 0xE7AA, 0xEA6C14),
    PINK(7, "ssu_gem_pink", 0xE7AB, 0xEC4899),
    WHITE(8, "ssu_gem_white", 0xE7AC, 0xC8CDD7),
    BLACK(9, "ssu_gem_black", 0xE7AD, 0x4A4A55),
    GRAY(10, "ssu_gem_gray", 0xE7AE, 0x8C8C96);

    private static final GemColor[] BY_ID = new GemColor[256];

    static {
        for (GemColor color : values()) BY_ID[color.id] = color;
    }

    public final int id;
    public final Identifier sprite;
    public final int glyph;
    public final int tint;

    GemColor(int id, String sprite, int glyph, int tint) {
        this.id = id;
        this.sprite = Identifier.fromNamespaceAndPath("skyblockutils", sprite);
        this.glyph = glyph;
        this.tint = tint;
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
