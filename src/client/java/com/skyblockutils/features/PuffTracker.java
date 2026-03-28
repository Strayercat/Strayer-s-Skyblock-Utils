package com.skyblockutils.features;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.utils.ModStyle;
import com.skyblockutils.utils.SideBarUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PuffTracker {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static boolean puffTrackerEnabled = false;
    public static List<Integer> puffsAroundPlayer = new ArrayList<>();

    private static ScheduledFuture<?> currentScheduledAction;

    public static void togglePuffTimer() {
        if (puffTrackerEnabled) {
            puffTrackerEnabled = false;
            currentScheduledAction.cancel(true);
            puffsAroundPlayer.clear();
            ModFunctions.displayMessageWithHeader("§cPuff Timer toggled off");
        } else {
            if (!ModFunctions.mapLocationToGeneralArea(SideBarUtils.location).equals("Rift")) {
                ModFunctions.displayMessageWithHeader("§cYou must be in The Rift to use Puff Timer");
                return;
            }

            puffTrackerEnabled = true;
            ModFunctions.displayMessageWithHeader("§aPuff Timer toggled on");
        }
    }

    public static void handleMetadataPacket(int entityId) {
        if (!puffTrackerEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        Entity entity = client.world.getEntityById(entityId);
        if (entity == null) return;

        Text customName = entity.getCustomName();
        if (customName == null) return;

        String plainName = customName.getString();

        if (plainName.contains("Puff")) {
            if (client.player == null) return;
            if (entity.getBlockPos().isWithinDistance(client.player.getBlockPos(), 5)) {
                if (puffsAroundPlayer.isEmpty()) {
                    startTimer(client);
                }
                puffsAroundPlayer.add(entityId);
            }
        }
    }

    public static void handleEntityDespawn(int entityId) {
        if (!puffTrackerEnabled) return;
        puffsAroundPlayer.remove(Integer.valueOf(entityId));
    }

    public static void startTimer(MinecraftClient client) {
        System.out.println("started");
        if (currentScheduledAction != null && !currentScheduledAction.isDone()) {
            currentScheduledAction.cancel(false);
        }
        currentScheduledAction = scheduler.schedule(() -> {
            if (client.world == null) return;
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0F));
            ModFunctions.showTitle(client, Text.literal("KILL PUFFS").withColor(ModStyle.getColor(ModStyle.ColorStyle.OCEAN, ModStyle.ColorType.MAIN)), 20);
        }, 75, TimeUnit.SECONDS);
    }

    public static void tick(MinecraftClient client) {
        if (!puffTrackerEnabled) return;
        if (client.world == null) return;

        if (puffsAroundPlayer.isEmpty() && currentScheduledAction != null && !currentScheduledAction.isCancelled()) {
            currentScheduledAction.cancel(false);
        }
    }
}