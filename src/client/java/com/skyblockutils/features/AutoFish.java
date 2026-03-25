package com.skyblockutils.features;

import com.skyblockutils.ModFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.util.Hand;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class AutoFish {
    public static boolean autoFishEnabled = false;
    public static boolean shouldReelIn = false;
    public static boolean shouldCast = false;
    public static long reelInTime = 0;
    public static long castTime = 0;
    public static long lastCastTime = 0;
    public static long plingDelay = 0;
    public static UUID hookedEntityUUID = null;
    public static long hookedEntityCheckTime = 0;

    public static void autoFish(net.minecraft.client.MinecraftClient client) {
        if (!autoFishEnabled || client.player == null || client.interactionManager == null || client.world == null)
            return;
        if (!(client.player.getMainHandStack().getItem() instanceof FishingRodItem)) {
            ModFunctions.displayMessageWithHeader(("§cAutofish toggled off"));
            resetAutoFish();
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (shouldReelIn && reelInTime <= currentTime) {
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            shouldReelIn = false;
            shouldCast = true;
            castTime = currentTime + getRandomDelay(150, 100);
            return;
        }

        if (shouldCast && castTime <= currentTime || (client.player.fishHook == null && !shouldReelIn && !shouldCast && currentTime - lastCastTime > 300)) {
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            shouldCast = false;
            lastCastTime = currentTime;
            return;
        }

        if (hookedEntityUUID != null && hookedEntityCheckTime < currentTime) {
            var entity = client.world.getEntity(hookedEntityUUID);
            if (entity != null && entity.isAlive()) {
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            }
            hookedEntityUUID = null;
            hookedEntityCheckTime = 0;
            return;
        }

        if (client.player.fishHook == null) return;
        if (client.player.fishHook.getHookedEntity() == null) return;
        if (!(client.player.fishHook.getHookedEntity() instanceof ArmorStandEntity) && hookedEntityCheckTime == 0) {
            hookedEntityUUID = client.player.fishHook.getHookedEntity().getUuid();
            hookedEntityCheckTime = currentTime + 400;
        }
    }

    public static void toggleAutoFish(net.minecraft.client.MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        if (!autoFishEnabled) {
            if (client.player.getMainHandStack().getItem() instanceof FishingRodItem) {
                autoFishEnabled = true;
                ModFunctions.displayMessageWithHeader("§aAutofish toggled on");
            } else {
                ModFunctions.displayMessageWithHeader("§cYou must hold a fishing rod in your main hand to use Autofish");
            }
        } else {
            resetAutoFish();
            if (client.player.fishHook != null) client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            ModFunctions.displayMessageWithHeader("§cAutofish toggled off");
        }
    }


    public static void resetAutoFish() {
        if (autoFishEnabled) {
            autoFishEnabled = false;
            shouldReelIn = false;
            shouldCast = false;
            reelInTime = 0;
            castTime = 0;
            lastCastTime = 0;
            plingDelay = 0;
            hookedEntityCheckTime = 0;
            hookedEntityUUID = null;
        }
    }

    public static void registerListener(MinecraftClient client) {
        client.getSoundManager().registerListener((sound, weightedSoundSet, a) -> {
            if (AutoFish.autoFishEnabled && sound.getId().toString().contains("note_block.pling")) {
                long currentTime = System.currentTimeMillis();
                if (!shouldReelIn && currentTime >= plingDelay) {
                    shouldReelIn = true;
                    reelInTime = currentTime + getRandomDelay(250, 100);
                    plingDelay = currentTime + 1000;
                }
            }
        });
    }

    public static long getRandomDelay(long min, long range) {
        return ThreadLocalRandom.current().nextLong(min, min + range + 1);
    }
}