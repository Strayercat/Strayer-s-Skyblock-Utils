package com.skyblockutils.features;

import com.skyblockutils.ModFunctions;
import net.minecraft.client.MinecraftClient;

public class ChatCommands {
    public static void handleCommands(String message) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) return;

        String[] parts = message.split(": ", 2);
        if (parts.length < 2) return;

        String messageContent = parts[1];

        if (messageContent.equalsIgnoreCase("!tps")) {
            MinecraftClient.getInstance().getNetworkHandler().sendChatMessage("Tps: " + String.format("%.1f", ModFunctions.tps));
        }
    }
}
