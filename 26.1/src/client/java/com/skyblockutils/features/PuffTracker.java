package com.skyblockutils.features;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.utils.ModStyle;
import com.skyblockutils.utils.SideBarUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;

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

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        Entity entity = client.level.getEntity(entityId);
        if (entity == null) return;

        Component customName = entity.getCustomName();
        if (customName == null) return;

        String plainName = customName.getString();

        if (plainName.contains("Puff")) {
            if (client.player == null) return;
            if (entity.blockPosition().closerThan(client.player.blockPosition(), 5)) {
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

    public static void startTimer(Minecraft client) {
        System.out.println("started");
        if (currentScheduledAction != null && !currentScheduledAction.isDone()) {
            currentScheduledAction.cancel(false);
        }
        currentScheduledAction = scheduler.schedule(() -> {
            if (client.level == null) return;
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1.0F));
            ModFunctions.showTitle(client, Component.literal("KILL PUFFS").withColor(ModStyle.getColor(ModStyle.ColorStyle.OCEAN, ModStyle.ColorType.MAIN)), 20);
        }, 75, TimeUnit.SECONDS);
    }

    public static void tick(Minecraft client) {
        if (!puffTrackerEnabled) return;
        if (client.level == null) return;

        if (puffsAroundPlayer.isEmpty() && currentScheduledAction != null && !currentScheduledAction.isCancelled()) {
            currentScheduledAction.cancel(false);
        }
    }
}