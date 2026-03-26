package com.skyblockutils.features;

import com.skyblockutils.config.ModConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static com.skyblockutils.config.ClothConfigHandler.*;

public class GlowingPlayersGui {
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTransparentBackground(false)
                .setDefaultBackgroundTexture(Identifier.of("skyblockutils", "textures/config/background.png"))
                .setTitle(Text.literal("Glowing Players Config"))
                .setSavingRunnable(ModConfig::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        buildGeneralCategory(builder, eb);
        buildHudCategory(builder, eb);
        buildChatFiltersCategory(builder, eb);

        return builder.build();
    }

    public static void handleConfigScreen(MinecraftClient client) {
        client.setScreen(createConfigScreen(client.currentScreen));
    }
}
