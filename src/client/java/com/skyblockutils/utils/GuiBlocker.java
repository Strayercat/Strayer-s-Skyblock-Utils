package com.skyblockutils.utils;

import com.skyblockutils.config.ModConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.Screen;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GuiBlocker {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static String[] titles = {"undersized party!", "creating instance....", "need party leader!"};
    public static boolean shouldHideScreen = false;

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, _, _) -> {
            if (screen instanceof Screen containerScreen) {
                String title = containerScreen.getTitle().getString().toLowerCase();
                if (shouldHideScreen) {
                    if (Arrays.asList(titles).contains(title)) {
                        client.execute(() -> {
                            if (client.gui.screen() != null) {
                                client.gui.screen().onClose();
                            }
                        });
                    }

                    scheduler.schedule(() -> {
                        shouldHideScreen = false;
                    }, 1500, TimeUnit.MILLISECONDS);
                }

                if (ModConfig.INSTANCE.autoHoppityEggs && title.matches("^chocolate .+ egg$")) {
                    client.execute(() -> {
                        if (client.gui.screen() != null) {
                            client.gui.screen().onClose();
                        }
                    });
                }
            }
        });
    }
}