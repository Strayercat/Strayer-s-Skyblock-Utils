package com.skyblockutils.features.dungeons;

import com.skyblockutils.features.party.PartyInfo;
import com.skyblockutils.utils.GuiBlocker;
import com.skyblockutils.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

import java.util.Map;
import java.util.Objects;

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
        if (!Objects.equals(PartyInfo.leader, Minecraft.getInstance().getUser().getName())) return;
        LocalPlayer player = Minecraft.getInstance().player;

        if (!message.startsWith("Party >")) return;
        String messageContent = message.split(": ")[1].toLowerCase();
        if (!messageContent.matches("![mf][1-7]")) return;

        int floor = Character.getNumericValue(messageContent.charAt(2));

        if (player == null) return;
        ClientPacketListener clientNetworkHandler = player.connection;

        if (PartyInfo.members.size() < 4) return;

        GuiBlocker.shouldHideScreen = true;
        clientNetworkHandler.sendCommand("joindungeon " + (messageContent.charAt(1) == 'm' ? "MASTER_" : "") + "CATACOMBS_FLOOR_" + translations.get(floor));
    }
}