package com.skyblockutils.features.glowingPlayers;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.CustomEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GlowingPlayersGui {
    public static boolean configScreenRequested = false;

    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTransparentBackground(false)
                .setDefaultBackgroundTexture(Identifier.of("skyblockutils", "textures/gui/background.png"))
                .setTitle(Text.literal("Glowing Players Config"))
                .setSavingRunnable(ModConfig::save);

        buildGlowingPlayersGui(builder);
        return builder.build();
    }

    public static void handleConfigScreen(MinecraftClient client) {
        if (configScreenRequested) client.setScreen(createConfigScreen(client.currentScreen));
        configScreenRequested = false;
    }

    public static void refreshScreen(MinecraftClient client) {
        if (client.currentScreen == null) return;

        Window window = client.getWindow();
        double mouseX = client.mouse.getX();
        double mouseY = client.mouse.getY();

        client.currentScreen.close();
        client.setScreen(createConfigScreen(client.currentScreen));

        InputUtil.setCursorParameters(window, InputUtil.GLFW_CURSOR_NORMAL, mouseX, mouseY);
    }

    public static void buildGlowingPlayersGui(ConfigBuilder builder) {
        var category = builder.getOrCreateCategory(Text.literal(""));

        category.addEntry(new CustomEntry()
                .addText("Glowing Players", CustomEntry.Alignment.LEFT, 0xFFFFFFFF)
                .addButton(Text.literal("+"), 20, CustomEntry.Alignment.RIGHT, () -> GlowingPlayerCreationScreen.openScreen(MinecraftClient.getInstance().currentScreen), Text.of("Add Glowing Player"))
                .addButton(Text.literal("⟳"), 20, CustomEntry.Alignment.RIGHT, () -> refreshScreen(MinecraftClient.getInstance()), Text.literal("Reload GUI"))
        );

        for (GlowingPlayers.GlowingPlayer p : ModConfig.INSTANCE.getGlowingPlayers()) {
            Screen parent = builder.getParentScreen();
            category.addEntry(new GlowingPlayerEntry(p, () -> {
                GlowingPlayers.remove(p.username);
                MinecraftClient client = MinecraftClient.getInstance();
                client.setScreen(GlowingPlayersGui.createConfigScreen(parent));
            }));
        }
    }
}
