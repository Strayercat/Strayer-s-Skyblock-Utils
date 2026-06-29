package com.skyblockutils.features;

import com.skyblockutils.ModFunctions;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.entity.decoration.ArmorStand;

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

    public static void autoFish(Minecraft client) {
        if (!autoFishEnabled || client.player == null || client.gameMode == null || client.level == null)
            return;
        if (!(client.player.getMainHandItem().getItem() instanceof FishingRodItem)) {
            ModFunctions.displayTextMessageWithHeader(("§cAutofish toggled off"));
            resetAutoFish();
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (shouldReelIn && reelInTime <= currentTime) {
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            shouldReelIn = false;
            shouldCast = true;
            castTime = currentTime + getRandomDelay(150, 100);
            return;
        }

        if (shouldCast && castTime <= currentTime || (client.player.fishing == null && !shouldReelIn && !shouldCast && currentTime - lastCastTime > 300)) {
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            shouldCast = false;
            lastCastTime = currentTime;
            return;
        }

        if (hookedEntityUUID != null && hookedEntityCheckTime < currentTime) {
            var entity = client.level.getEntity(hookedEntityUUID);
            if (entity != null && entity.isAlive()) {
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            }
            hookedEntityUUID = null;
            hookedEntityCheckTime = 0;
            return;
        }

        if (client.player.fishing == null) return;
        if (client.player.fishing.getHookedIn() == null) return;
        if (!(client.player.fishing.getHookedIn() instanceof ArmorStand) && hookedEntityCheckTime == 0) {
            hookedEntityUUID = client.player.fishing.getHookedIn().getUUID();
            hookedEntityCheckTime = currentTime + 400;
        }
    }

    public static void toggleAutoFish(net.minecraft.client.Minecraft client) {
        if (client.player == null || client.gameMode == null) return;

        if (!autoFishEnabled) {
            if (client.player.getMainHandItem().getItem() instanceof FishingRodItem) {
                autoFishEnabled = true;
                ModFunctions.displayTextMessageWithHeader("§aAutofish toggled on");
            } else {
                ModFunctions.displayTextMessageWithHeader("§cYou must hold a fishing rod in your main hand to use Autofish");
            }
        } else {
            resetAutoFish();
            if (client.player.fishing != null) client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            ModFunctions.displayTextMessageWithHeader("§cAutofish toggled off");
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

    public static void registerListener(Minecraft client) {
        client.getSoundManager().addListener((sound, weightedSoundSet, a) -> {
            if (AutoFish.autoFishEnabled && sound.getIdentifier().toString().contains("note_block.pling")) {
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