package com.skyblockutils.features.party;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.OnScreenNotification;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.Objects;

public class PartyCommands {
    public static boolean allInvEnabled = false;

    public static void handlePartyCommands(String message) {
        if (message.matches("From .*: Boop!")) handleBoop(message);
        if (!message.startsWith("Party >")) return;

        ClientPacketListener networkHandler = Minecraft.getInstance().getConnection();
        if (networkHandler == null) return;

        String[] parts = message.split(": ");
        if (parts.length < 2) return;

        String messageContent = parts[1];

        if (messageContent.equalsIgnoreCase("!pt") || messageContent.equalsIgnoreCase("!ptme")) {
            if (!Objects.equals(PartyInfo.leader, Minecraft.getInstance().getUser().getName())) return;

            String username = parts[0].split(" ")[parts[0].split(" ").length - 1];

            if (username.equalsIgnoreCase(Minecraft.getInstance().getUser().getName())) return;

            networkHandler.sendCommand("party transfer " + username);
        }

        if (messageContent.equalsIgnoreCase("!w") ||
                messageContent.equalsIgnoreCase("!pw") ||
                messageContent.equalsIgnoreCase("!pwarp") ||
                messageContent.equalsIgnoreCase("!warp")) {
            if (!Objects.equals(PartyInfo.leader, Minecraft.getInstance().getUser().getName())) return;
            networkHandler.sendCommand("party warp");
        }

        if (messageContent.equalsIgnoreCase("!allinv")) {
            if (!Objects.equals(PartyInfo.leader, Minecraft.getInstance().getUser().getName())) return;

            if (allInvEnabled) {
                networkHandler.sendChat("Allinv is already enabled");
                return;
            }

            networkHandler.sendCommand("party settings allinvite");
            allInvEnabled = true;
        }
    }

    private static void handleBoop(String message) {
        if (!ModConfig.INSTANCE.boopPartyInvites) return;

        String username = message.replaceAll("\\[.*] ", "").split(" ")[1].replaceAll(":", "");

        OnScreenNotification.builder()
                .title("§l§dBoop!")
                .subtitle(username + " §l§dBooped!§r you!")
                .tickTime(200)
                .withSound(true)
                .send();
    }
}
