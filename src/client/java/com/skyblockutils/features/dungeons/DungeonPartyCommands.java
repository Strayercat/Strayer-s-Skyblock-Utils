package com.skyblockutils.features.dungeons;

import com.skyblockutils.features.party.PartyInfo;
import com.skyblockutils.utils.GuiBlocker;
import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import  net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DungeonPartyCommands {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static boolean autoRejoinEnabled = false;
    public static String currentFloorJoinCommand = "";
    public static String currentFloor = "";
    public static boolean waitingForDhub = false;
    public static boolean partialParty = false;

    public static void handleDungeonPartyCommands(String message) {
        if (!ModConfig.INSTANCE.dungeonPartyCommands) return;
        if (!Objects.equals(PartyInfo.leader, MinecraftClient.getInstance().getSession().getUsername())) return;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (!message.startsWith("Party >")) return;
        String messageContent = message.split(": ")[1];
        if (!messageContent.matches("![mf][1-7]")) return;

        int floor = Character.getNumericValue(messageContent.charAt(2));

        Map<Integer, String> translations = new HashMap<>();
        translations.put(1, "ONE");
        translations.put(2, "TWO");
        translations.put(3, "THREE");
        translations.put(4, "FOUR");
        translations.put(5, "FIVE");
        translations.put(6, "SIX");
        translations.put(7, "SEVEN");

        if (player == null) return;
        ClientPlayNetworkHandler clientNetworkHandler = player.networkHandler;

        if (messageContent.charAt(1) == 'm') {
            GuiBlocker.shouldHideScreen = true;
             if(autoRejoinEnabled) {
                 currentFloorJoinCommand = "/joindungeon MASTER_CATACOMBS_FLOOR_" + translations.get(floor);
                 currentFloor = messageContent.replaceAll("!", "").toUpperCase();
             }
            clientNetworkHandler.sendChatMessage("/joindungeon MASTER_CATACOMBS_FLOOR_" + translations.get(floor));
            return;
        }

        if (messageContent.charAt(1) == 'f') {
            GuiBlocker.shouldHideScreen = true;
            if(autoRejoinEnabled) {
                currentFloorJoinCommand = "/joindungeon CATACOMBS_FLOOR_" + translations.get(floor);
                currentFloor = messageContent.replaceAll("!", "").toUpperCase();
            }
            clientNetworkHandler.sendChatMessage("/joindungeon CATACOMBS_FLOOR_" + translations.get(floor));
        }
    }

    public static void autoRejoin(String message) {
        if (!autoRejoinEnabled) return;
        if (MinecraftClient.getInstance().player == null) return;

        if (message.contains("left the party.") || message.contains("has disbanded the party!") || message.contains("has been removed from the party.")) {
            partialParty = true;
            return;
        }

        if (message.contains("Click HERE to re-queue")) {
            if (currentFloorJoinCommand.isEmpty()) {
                OnScreenNotification.renderNotification("AUTO-REJOIN PAUSED", "Couldn't find a floor to rejoin. Make sure to use a '!m1' format command when using auto-rejoin.", 100);
                MinecraftClient.getInstance().player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE.value());
                return;
            }

            if (ModConfig.INSTANCE.autoRejoinReminders) {
                OnScreenNotification.renderNotification("AUTO-REJOIN REMINDER", "Auto-rejoin is still enabled ;)\nSelected floor: " + currentFloor, 100);
                MinecraftClient.getInstance().player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE.value());
            }

            if (DowntimeTracker.downtimeRequested) {
                OnScreenNotification.renderNotification("AUTO-REJOIN PAUSED", "Someone requested downtime, auto-rejoin has paused. Run a '!m1' format command again to resume.", 100);
                MinecraftClient.getInstance().player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE.value());
                return;
            }

            if (partialParty) {
                OnScreenNotification.renderNotification("AUTO-REJOIN PAUSED", "Your party is not full. Get a full party and run a '!m1' format command again to resume.", 100);
                MinecraftClient.getInstance().player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE.value());
                return;
            }

            scheduler.schedule(() -> {
                    partialParty = false;
                    GuiBlocker.shouldHideScreen = true;
                    MinecraftClient.getInstance().player.networkHandler.sendChatMessage(currentFloorJoinCommand);
            }, 2, TimeUnit.SECONDS);
        }
    }

    public static void resetDungeonPartyCommands() {
        autoRejoinEnabled = false;
        currentFloorJoinCommand = "";
        waitingForDhub = false;
        partialParty = false;
    }
}