package com.skyblockutils.features.dungeons;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.GuiBlocker;
import com.skyblockutils.utils.OnScreenNotification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoRejoin {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static boolean autoRejoinEnabled = false;
    public static String currentFloor = "";
    public static boolean partialParty = false;


    public static void autoRejoin(String message) {
        if (!autoRejoinEnabled) return;
        if (MinecraftClient.getInstance().player == null) return;

        if (message.contains("left the party.") || message.contains("has disbanded the party!") || message.contains("has been removed from the party.")) {
            partialParty = true;
            return;
        }

        if (message.contains("Click HERE to re-queue")) {
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
                MinecraftClient.getInstance().player.networkHandler.sendChatCommand("joindungeon " + ((currentFloor.startsWith("M") ? "MASTER_" : "") + "CATACOMBS_FLOOR_" + DungeonPartyCommands.translations.get(Character.getNumericValue(currentFloor.charAt(1)))));
            }, 2, TimeUnit.SECONDS);
        }
    }

    public static void resetAutoRejoin() {
        autoRejoinEnabled = false;
        currentFloor = "";
        partialParty = false;
    }
}
