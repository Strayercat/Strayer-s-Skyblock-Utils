package com.skyblockutils.utils;

import net.minecraft.network.chat.Component;

import java.util.regex.Pattern;

public final class TextUtils {
    private static final Pattern FORMATTING = Pattern.compile("§.");

    private TextUtils() {
    }

    public static String strip(String text) {
        return FORMATTING.matcher(text).replaceAll("");
    }

    public static String strip(Component text) {
        return strip(text.getString());
    }

    public static String clean(String text) {
        return strip(text).trim();
    }

    public static String clean(Component text) {
        return clean(text.getString());
    }
}
