package com.skyblockutils.utils;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class SSU {
    public static final Text NAME = buildPrefix(false);
    public static final Text FULL_NAME = buildPrefix(true);

    private static Text buildPrefix(boolean fullName) {
        MutableText result = Text.empty();
        if (!fullName) result.append(Text.literal("§7["));
        result.append(gradientText(fullName ? "Strayer's Skyblock Utils" : "Skyblock Utils", 0x4CFF00, 0x93E27D));
        if (!fullName) result.append(Text.literal("§7] "));
        return result;
    }

    public static Text gradientText(String message, int colorFrom, int colorTo) {
        MutableText result = Text.empty();
        int len = message.length();

        for (int i = 0; i < len; i++) {
            float t = (len == 1) ? 0f : (float) i / (len - 1);

            int r = (int) (((colorFrom >> 16) & 0xFF) * (1 - t) + ((colorTo >> 16) & 0xFF) * t);
            int g = (int) (((colorFrom >> 8)  & 0xFF) * (1 - t) + ((colorTo >> 8)  & 0xFF) * t);
            int b = (int) (( colorFrom        & 0xFF) * (1 - t) + ( colorTo        & 0xFF) * t);

            int color = (r << 16) | (g << 8) | b;
            result.append(Text.literal(String.valueOf(message.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(color)));
        }

        return result;
    }
}
