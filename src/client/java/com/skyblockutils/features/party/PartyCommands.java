package com.skyblockutils.features.party;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import java.util.Objects;

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
            if (!Objects.equals(PartyInfo.leader, MinecraftClient.getInstance().getSession().getUsername())) return;

            String username = parts[0].split(" ")[parts[0].split(" ").length - 1];

            if (username.equalsIgnoreCase(MinecraftClient.getInstance().getSession().getUsername())) return;

            networkHandler.sendChatCommand("party transfer " + username);
        }

        if (messageContent.equalsIgnoreCase("!w") ||
                messageContent.equalsIgnoreCase("!pw") ||
                messageContent.equalsIgnoreCase("!pwarp") ||
                messageContent.equalsIgnoreCase("!warp")) {
            if (!Objects.equals(PartyInfo.leader, MinecraftClient.getInstance().getSession().getUsername())) return;
            System.out.println(PartyInfo.leader);
            System.out.println(MinecraftClient.getInstance().getSession().getUsername());

            networkHandler.sendChatCommand("party warp");
        }

        if (messageContent.equalsIgnoreCase("!allinv")) {
            if (!Objects.equals(PartyInfo.leader, MinecraftClient.getInstance().getSession().getUsername())) return;

            if (allInvEnabled) {
                networkHandler.sendChatMessage("Allinv is already enabled");
                return;
            }

            networkHandler.sendChatCommand("party settings allinvite");
            allInvEnabled = true;
        }
    }
}
