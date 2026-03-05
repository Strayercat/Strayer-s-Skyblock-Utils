package com.skyblockutils.features.dungeons;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundEvents;

public class DowntimeTracker {
    public static boolean downtimeRequested = false;
    public static String requesterUsername = "";
    static String reason = "";

    public static void trackDowntime(String message) {
        if (!ModConfig.INSTANCE.downtimeTracker) return;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        if (!ModFunctions.isInDungeons(MinecraftClient.getInstance())) return;

        if (message.startsWith("Party >")) {
            String messageContent = message.split(": ")[1].trim().toLowerCase();

            if ((messageContent.startsWith("dt") || messageContent.startsWith("!dt"))) {
                downtimeRequested = true;
                requesterUsername = message.replaceAll("\\[.+] ", "").split(" ")[2].replaceAll(":", "");
                reason = messageContent.replaceAll("^!?dt ?", "").isEmpty() ? "No reason given" : "Reason: " + messageContent.replaceAll("^!?dt ?", "");
                OnScreenNotification.renderNotification("Downtime Requested", "By: " + requesterUsername, 100);
                player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE.value());
            }
            return;
        }

        if (message.contains("Click HERE to re-queue") && downtimeRequested) {
            player.networkHandler.sendChatMessage("[Strayer's Skyblock Utils] Downtime requested by " + requesterUsername + " " + reason);
            OnScreenNotification.renderNotification("DOWNTIME REQUESTED", "By: " + requesterUsername + "\n" + reason, 100);
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE.value());
        }
    }

    public static void resetDowntimeTracker() {
        downtimeRequested = false;
        requesterUsername = "";
        reason = "";
    }
}