package com.skyblockutils.features.dungeons;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class DowntimeTracker {
    public static boolean downtimeRequested = false;
    public static String requesterUsername = "";
    static String reason = "";

    public static void trackDowntime(String message) {
        if (!ModConfig.INSTANCE.downtimeTracker) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!ModFunctions.isInDungeons(Minecraft.getInstance())) return;

        if (message.startsWith("Party >")) {
            String messageContent = message.split(": ")[1].trim().toLowerCase();

            if ((messageContent.startsWith("dt") || messageContent.startsWith("!dt"))) {
                downtimeRequested = true;
                requesterUsername = message.replaceAll("\\[.+] ", "").split(" ")[2].replaceAll(":", "");
                reason = messageContent.replaceAll("^!?dt ?", "").isEmpty() ? "No reason given" : "Reason: " + messageContent.replaceAll("^!?dt ?", "");
                OnScreenNotification.builder()
                        .title("Downtime Requested")
                        .subtitle("Downtime requested by " + requesterUsername + "\n" + reason)
                        .tickTime(100)
                        .withSound(true)
                        .send();
            }
            return;
        }

        if (message.contains("Click HERE to re-queue") && downtimeRequested) {
            player.connection.sendCommand("pc [Strayer's Skyblock Utils] Downtime requested by " + requesterUsername + " " + reason);
            OnScreenNotification.builder()
                    .title("Downtime Requested")
                    .subtitle("Downtime requested by " + requesterUsername + "\n" + reason)
                    .tickTime(100)
                    .withSound(true)
                    .send();
        }
    }

    public static void resetDowntimeTracker() {
        downtimeRequested = false;
        requesterUsername = "";
        reason = "";
    }
}