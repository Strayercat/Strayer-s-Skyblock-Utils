package com.skyblockutils.features.browser;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.ScreenOpener;
import net.dimaskama.mcef.api.MCEFApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Util;

import java.net.URI;

public final class InGameBrowser {
    private static final String BROWSER_MOD = "mcef-modern";

    private static boolean preloaded;

    private InGameBrowser() {
    }

    public static boolean available() {
        return ModConfig.INSTANCE.browserInGame && FabricLoader.getInstance().isModLoaded(BROWSER_MOD);
    }

    public static void tick() {
        if (preloaded || !ModConfig.INSTANCE.browserPreload || !available()) return;
        preloaded = true;
        MCEFApi.initialize();
    }

    public static void open(String url, Screen parent) {
        if (available()) Minecraft.getInstance().gui.setScreen(new BrowserScreen(url, parent));
        else openExternal(url);
    }

    public static void openFromCommand(String url) {
        Screen current = Minecraft.getInstance().gui.screen();
        Screen parent = current instanceof ChatScreen ? null : current;
        ScreenOpener.runNextTick(() -> open(url, parent));
    }

    public static void openExternal(String url) {
        try {
            Util.getPlatform().openUri(URI.create(url));
        } catch (Exception e) {
            ModFunctions.sendSystemMessage("§cCouldn't open " + url + ": " + e.getMessage(), false);
        }
    }
}
