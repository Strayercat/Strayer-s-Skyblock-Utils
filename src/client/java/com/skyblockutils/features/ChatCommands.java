package com.skyblockutils.features;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.mixin.client.ClientPlayNetworkHandlerAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import java.net.NetworkInterface;

public class ChatCommands {
    public static void handleCommands(String message) {
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == null) return;

        String[] parts = message.split(": ", 2);
        if (parts.length < 2) return;

        String messageContent = parts[1];

        if (messageContent.equalsIgnoreCase("!tps")) networkHandler.sendChatMessage("Tps: " + String.format("%.1f", ModFunctions.tps));
        if(messageContent.equalsIgnoreCase("!ping")) networkHandler.sendChatMessage("Ping: " + ModFunctions.ping);
    }
}
