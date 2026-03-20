package com.skyblockutils.features.party;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public class PartyCommands {
    public static boolean allInvEnabled = false;
    public static void handlePartyCommands(String message) {
        if (!message.startsWith("Party >")) return;

        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == null) return;

        String[] parts = message.split(": ");
        if (parts.length < 2) return;

        String messageContent = parts[1];

        if (messageContent.equalsIgnoreCase("!pt") || messageContent.equalsIgnoreCase("!ptme")) {
            String username = parts[0].split(" ")[parts[0].split(" ").length - 1];
            if (username.equalsIgnoreCase(MinecraftClient.getInstance().getSession().getUsername())) return;
            networkHandler.sendChatCommand("party transfer " + username);
        }

        if (messageContent.equalsIgnoreCase("!w") ||
                messageContent.equalsIgnoreCase("!pw") ||
                messageContent.equalsIgnoreCase("!pwarp") ||
                messageContent.equalsIgnoreCase("!warp")) {
            networkHandler.sendChatCommand("party warp");
        }

        if (messageContent.equalsIgnoreCase("!allinv")) {
            if (allInvEnabled) {
                networkHandler.sendChatMessage("eAllinv is already enabled");
                return;
            }

            networkHandler.sendChatCommand("party settings allinvite");
            allInvEnabled = true;
        }
    }
}
