package com.skyblockutils.features;

import com.skyblockutils.ModFunctions;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class AutoFish {
    private static final long CAST_TIMEOUT_MS = 1500;
    private static final long HOOK_GONE_GRACE_MS = 500;
    private static final int MAX_ENTITY_REELS = 3;
    private static final double BITE_RADIUS = 4.0;

    public static boolean autoFishEnabled = false;
    public static boolean shouldReelIn = false;
    public static boolean shouldCast = false;
    public static long reelInTime = 0;
    public static long castTime = 0;
    public static long lastCastTime = 0;
    public static long hookGoneSince = 0;
    public static int entityReels = 0;
    public static int reeledHookId = -1;
    public static int bitHookId = -1;
    public static UUID hookedEntityUUID = null;
    public static long hookedEntityCheckTime = 0;

    public static void autoFish(Minecraft client) {
        if (!autoFishEnabled || client.player == null || client.gameMode == null || client.level == null)
            return;
        if (!(client.player.getMainHandItem().getItem() instanceof FishingRodItem)) {
            ModFunctions.sendSystemMessage(("§cAutofish toggled off"), false);
            resetAutoFish();
            return;
        }

        long currentTime = System.currentTimeMillis();
        FishingHook hook = client.player.fishing;

        if (hook != null) hookGoneSince = 0;
        else if (hookGoneSince == 0) hookGoneSince = currentTime;

        if (!shouldReelIn && hook != null && hook.getId() != bitHookId && hasBiteIndicator(client, hook)) {
            bitHookId = hook.getId();
            shouldReelIn = true;
            entityReels = 0;
            reelInTime = currentTime + getRandomDelay(250, 100);
        }

        if (shouldReelIn && reelInTime <= currentTime) {
            reel(client);
            shouldReelIn = false;
            shouldCast = true;
            castTime = currentTime + getRandomDelay(150, 100);
            return;
        }

        boolean idle = hook == null
                && !shouldReelIn
                && !shouldCast
                && currentTime - lastCastTime > CAST_TIMEOUT_MS
                && currentTime - hookGoneSince > HOOK_GONE_GRACE_MS;

        if (shouldCast && castTime <= currentTime || idle) {
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            shouldCast = false;
            lastCastTime = currentTime;
            hookedEntityUUID = null;
            hookedEntityCheckTime = 0;
            return;
        }

        if (hookedEntityUUID != null && hookedEntityCheckTime < currentTime) {
            var entity = client.level.getEntity(hookedEntityUUID);
            hookedEntityUUID = null;
            hookedEntityCheckTime = 0;
            if (entity == null || !entity.isAlive() || client.player.fishing == null) return;

            reel(client);
            if (++entityReels >= MAX_ENTITY_REELS) {
                ModFunctions.sendSystemMessage("§cAutofish toggled off: a sea creature is blocking your line", false);
                resetAutoFish();
            }
            return;
        }

        if (hook == null || hook.getId() == reeledHookId) return;
        Entity hooked = hook.getHookedIn();
        if (hooked == null) return;
        if (hooked instanceof LivingEntity && !(hooked instanceof ArmorStand) && hookedEntityCheckTime == 0) {
            hookedEntityUUID = hooked.getUUID();
            hookedEntityCheckTime = currentTime + 400;
        }
    }

    private static boolean hasBiteIndicator(Minecraft client, FishingHook hook) {
        if (client.level == null) return false;
        return !client.level.getEntitiesOfClass(ArmorStand.class, hook.getBoundingBox().inflate(BITE_RADIUS), stand -> {
            Component name = stand.getCustomName();
            return name != null
                    && stand.isInvisible()
                    && stand.isCustomNameVisible()
                    && "!!!".equals(name.getString());
        }).isEmpty();
    }

    private static void reel(Minecraft client) {
        if (client.player == null || client.gameMode == null) return;
        if (client.player.fishing != null) reeledHookId = client.player.fishing.getId();
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
    }

    public static void toggleAutoFish(net.minecraft.client.Minecraft client) {
        if (client.player == null || client.gameMode == null) return;

        if (!autoFishEnabled) {
            if (client.player.getMainHandItem().getItem() instanceof FishingRodItem) {
                autoFishEnabled = true;
                ModFunctions.sendSystemMessage("§aAutofish toggled on", false);
            } else {
                ModFunctions.sendSystemMessage("§cYou must hold a fishing rod in your main hand to use Autofish", false);
            }
        } else {
            resetAutoFish();
            if (client.player.fishing != null) client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            ModFunctions.sendSystemMessage("§cAutofish toggled off", false);
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
            hookGoneSince = 0;
            entityReels = 0;
            reeledHookId = -1;
            bitHookId = -1;
            hookedEntityCheckTime = 0;
            hookedEntityUUID = null;
        }
    }

    public static long getRandomDelay(long min, long range) {
        return ThreadLocalRandom.current().nextLong(min, min + range + 1);
    }
}