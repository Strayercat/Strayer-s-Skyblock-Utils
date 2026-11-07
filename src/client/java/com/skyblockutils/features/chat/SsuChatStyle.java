package com.skyblockutils.features.chat;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.hud.SystemChatHud;
import com.skyblockutils.utils.ModStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

public final class SsuChatStyle {
    public static final int INPUT_RADIUS = 3;
    public static final int INPUT_PADDING = 2;
    public static final int INPUT_EXTRA_HEIGHT = 3;
    public static final int INPUT_TEXT_FROM_BOTTOM = 14;
    public static final int INPUT_RIGHT_INSET = 5;
    private static final int EDIT_BOX_BACKGROUND = 0xE0000000;
    private static final int INACTIVE_BORDER = 0x80808080;

    private SsuChatStyle() {
    }

    public static boolean inputEnabled() {
        return ModConfig.INSTANCE.ssuStyleChat && ModConfig.INSTANCE.ssuStyleChatInput;
    }

    public static boolean appliesToCurrentScreen() {
        if (!inputEnabled()) return false;
        Screen screen = Minecraft.getInstance().gui.screen();
        return screen instanceof ChatScreen || screen instanceof SystemChatHud;
    }

    public static int accent() {
        return 0xFF000000 | ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
    }

    public static int editBoxBackground() {
        return EDIT_BOX_BACKGROUND;
    }

    public static int editBoxBorder(boolean active, boolean focused) {
        if (!active) return INACTIVE_BORDER;
        return focused ? accent() : (accent() & 0xFFFFFF) | 0x80000000;
    }
}
