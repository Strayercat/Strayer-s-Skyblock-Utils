package com.skyblockutils.utils;

import com.skyblockutils.config.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class SSU {
    public static Component getName() {
        return buildPrefix(false);
    }

    public static Component getFullName() {
        return buildPrefix(true);
    }

    private static Component buildPrefix(boolean fullName) {
        int colorStart = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TITLE_START);
        int colorEnd = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TITLE_END);
        MutableComponent result = Component.empty();
        result.append(Component.literal("§7["));
        result.append(gradientText(fullName ? "Strayer's Skyblock Utils" : "Skyblock Utils", colorStart, colorEnd));
        result.append(Component.literal("§7] "));
        return result;
    }

    public static MutableComponent gradientText(String message, int colorFrom, int colorTo) {
        MutableComponent result = Component.empty();
        int len = message.length();

        for (int i = 0; i < len; i++) {
            float t = (len == 1) ? 0f : (float) i / (len - 1);
            int r = (int) (((colorFrom >> 16) & 0xFF) * (1 - t) + ((colorTo >> 16) & 0xFF) * t);
            int g = (int) (((colorFrom >> 8) & 0xFF) * (1 - t) + ((colorTo >> 8) & 0xFF) * t);
            int b = (int) ((colorFrom & 0xFF) * (1 - t) + (colorTo & 0xFF) * t);
            int color = (r << 16) | (g << 8) | b;
            result.append(Component.literal(String.valueOf(message.charAt(i)))
                    .setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(color)));
        }

        return result;
    }
}