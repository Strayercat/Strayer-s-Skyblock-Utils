package com.skyblockutils.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.UnaryOperator;

public final class ScreenOpener {
    private static Runnable pending;

    private ScreenOpener() {
    }

    public static void openNextTick(UnaryOperator<Screen> factory) {
        runNextTick(() -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreenAndShow(factory.apply(client.gui.screen()));
        });
    }

    public static void runNextTick(Runnable action) {
        pending = action;
    }

    public static void tick() {
        if (pending == null) return;
        Runnable action = pending;
        pending = null;
        action.run();
    }
}