package com.skyblockutils.features.dungeons;

import com.skyblockutils.features.party.PartyInfo;
import com.skyblockutils.utils.GuiBlocker;
import com.skyblockutils.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import  net.minecraft.client.network.ClientPlayerEntity;

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
        if (!Objects.equals(PartyInfo.leader, MinecraftClient.getInstance().getSession().getUsername())) return;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (!message.startsWith("Party >")) return;
        String messageContent = message.split(": ")[1];
        if (!messageContent.matches("![mf][1-7]")) return;

        int floor = Character.getNumericValue(messageContent.charAt(2));

        if (player == null) return;
        ClientPlayNetworkHandler clientNetworkHandler = player.networkHandler;

        if (messageContent.charAt(1) == 'm') {
            GuiBlocker.shouldHideScreen = true;
            clientNetworkHandler.sendChatMessage("/joindungeon MASTER_CATACOMBS_FLOOR_" + translations.get(floor));
            return;
        }

        if (messageContent.charAt(1) == 'f') {
            GuiBlocker.shouldHideScreen = true;
            clientNetworkHandler.sendChatMessage("/joindungeon CATACOMBS_FLOOR_" + translations.get(floor));
        }
    }
}