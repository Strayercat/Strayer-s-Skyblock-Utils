package com.skyblockutils.features.party;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.OnScreenNotification;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class PartyCommands {
    public static boolean allInvEnabled = false;

    public static void handlePartyCommands(String message) {
        if (message.matches("From .*: Boop!")) handleBoop(message);
        if (!message.startsWith("Party >")) return;

        ClientPacketListener networkHandler = Minecraft.getInstance().getConnection();
        if (networkHandler == null) return;

        String[] parts = message.split(": ", 2);
        if (parts.length < 2) return;

        String messageContent = parts[1].trim();
        if (!messageContent.startsWith("!")) return;
        if (PartyInfo.isLeader()) return;

        if (messageContent.equalsIgnoreCase("!pt") || messageContent.equalsIgnoreCase("!ptme")) {
            String[] header = parts[0].split(" ");
            String username = header[header.length - 1];

            if (username.equalsIgnoreCase(Minecraft.getInstance().getUser().getName())) return;

            networkHandler.sendCommand("party transfer " + username);
        }

        if (messageContent.equalsIgnoreCase("!w") ||
                messageContent.equalsIgnoreCase("!pw") ||
                messageContent.equalsIgnoreCase("!pwarp") ||
                messageContent.equalsIgnoreCase("!warp")) {
            networkHandler.sendCommand("party warp");
        }

        if (messageContent.equalsIgnoreCase("!allinv")) {
            if (allInvEnabled) {
                networkHandler.sendCommand("pc Allinv is already enabled");
                return;
            }

            networkHandler.sendCommand("party settings allinvite");
            allInvEnabled = true;
        }
    }

    private static void handleBoop(String message) {
        if (!ModConfig.INSTANCE.boopPartyInvites) return;

        String username = message.replaceAll("\\[[^]]+] ", "").split(" ")[1].replaceAll(":", "");

        OnScreenNotification.builder()
                .title("§l§dBoop!")
                .subtitle(username + " §l§dBooped!§r you!")
                .tickTime(200)
                .withSound(true)
                .send();
    }
}
