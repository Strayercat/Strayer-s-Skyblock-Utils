package com.skyblockutils.features.dungeons;

import com.skyblockutils.features.party.PartyInfo;
import com.skyblockutils.utils.GuiBlocker;
import com.skyblockutils.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

import java.util.Map;

public class DungeonPartyCommands {
    public static final Map<Integer, String> translations = Map.of(
            1, "ONE",
            2, "TWO",
            3, "THREE",
            4, "FOUR",
            5, "FIVE",
            6, "SIX",
            7, "SEVEN"
    );

    public static void handleDungeonPartyCommands(String message) {
        if (!ModConfig.INSTANCE.dungeonPartyCommands) return;
        if (!message.startsWith("Party >")) return;
        if (PartyInfo.isLeader()) return;

        String[] parts = message.split(": ", 2);
        if (parts.length < 2) return;

        String messageContent = parts[1].trim().toLowerCase();
        if (!messageContent.matches("![mf][1-7]")) return;

        int floor = Character.getNumericValue(messageContent.charAt(2));

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ClientPacketListener clientNetworkHandler = player.connection;

        if (PartyInfo.members.size() < 4) return;

        GuiBlocker.shouldHideScreen = true;
        clientNetworkHandler.sendCommand("joindungeon " + (messageContent.charAt(1) == 'm' ? "MASTER_" : "") + "CATACOMBS_FLOOR_" + translations.get(floor));
    }
}