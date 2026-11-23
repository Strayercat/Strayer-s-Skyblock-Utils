package com.skyblockutils.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.UnaryOperator;

public final class ScreenOpener {
    private static UnaryOperator<Screen> pending;

    private ScreenOpener() {
    }

    public static void openNextTick(UnaryOperator<Screen> factory) {
        pending = factory;
    }

    public static void tick(Minecraft client) {
        if (pending == null) return;
        UnaryOperator<Screen> factory = pending;
        pending = null;
        client.setScreenAndShow(factory.apply(client.gui.screen()));
    }
}
