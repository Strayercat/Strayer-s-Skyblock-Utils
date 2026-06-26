package com.skyblockutils.features.glowingPlayers;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.CustomEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class GlowingPlayersGui {
    public static boolean configScreenRequested = false;

    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTransparentBackground(false)
                .setDefaultBackgroundTexture(Identifier.fromNamespaceAndPath("skyblockutils", "textures/gui/background.png"))
                .setTitle(Component.literal("Glowing Players Config"))
                .setSavingRunnable(ModConfig::save);

        buildGlowingPlayersGui(builder);
        return builder.build();
    }

    public static void handleConfigScreen(Minecraft client) {
        if (configScreenRequested) client.setScreenAndShow(createConfigScreen(client.gui.screen()));
        configScreenRequested = false;
    }

    public static void refreshScreen(Minecraft client) {
        if (client.gui.screen() == null) return;
        Screen parent = client.gui.screen();
        client.setScreenAndShow(createConfigScreen(parent));
    }

    public static void buildGlowingPlayersGui(ConfigBuilder builder) {
        var category = builder.getOrCreateCategory(Component.literal(""));

        category.addEntry(new CustomEntry()
                .addText("Glowing Players", CustomEntry.Alignment.LEFT, 0xFFFFFFFF)
                .addButton(Component.literal("+"), 20, CustomEntry.Alignment.RIGHT, () -> GlowingPlayerCreationScreen.openScreen(Minecraft.getInstance().gui.screen()), Component.literal("Add Glowing Player"))
                .addButton(Component.literal("⟳"), 20, CustomEntry.Alignment.RIGHT, () -> refreshScreen(Minecraft.getInstance()), Component.literal("Reload GUI"))
        );

        for (GlowingPlayers.GlowingPlayer p : ModConfig.INSTANCE.getGlowingPlayers()) {
            Screen parent = builder.getParentScreen();
            category.addEntry(new GlowingPlayerEntry(p, () -> {
                GlowingPlayers.remove(p.username);
                Minecraft client = Minecraft.getInstance();
                client.setScreenAndShow(GlowingPlayersGui.createConfigScreen(parent));
            }));
        }
    }
}