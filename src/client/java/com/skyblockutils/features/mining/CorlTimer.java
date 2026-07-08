package com.skyblockutils.features.mining;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.ModStyle;
import com.skyblockutils.utils.SideBarUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.skyblockutils.utils.Scheduler.scheduler;

public class CorlTimer {
    public static boolean corlTimerEnabled = false;
    public static BlockPos corlPos = null;
    public static UUID corlUUID = null;
    public static boolean waitTime = false;

    public static void corlTimerTick(net.minecraft.client.Minecraft client) {
        if (!corlTimerEnabled || client.level == null) return;

        if (corlPos == null) {
            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity.getName().getString().contains("Corleone")) {
                    corlPos = entity.blockPosition();
                    corlUUID = entity.getUUID();
                    break;
                }
            }
            return;
        }

        if (corlUUID != null) {
            if (client.level.getEntity(corlUUID) == null) {
                corlUUID = null;
                waitTime = true;

                scheduler.schedule(() -> {
                    if (client.level == null || !corlTimerEnabled) return;
                    ModFunctions.showTitle(client, Component.literal("CORL").withColor(ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN)), 20, true);
                    waitTime = false;
                }, 1, TimeUnit.MINUTES);
            }
            return;
        }

        if (waitTime) return;

        AABB blockBox = new AABB(corlPos).inflate(5.0, 3.0, 5.0);
        List<Entity> entities = client.level.getEntitiesOfClass(Entity.class, blockBox);

        for (Entity entity : entities) {
            if (entity.getName().getString().contains("Corleone")) {
                corlUUID = entity.getUUID();
            }
        }
    }

    public static void toggleCorlTimer() {
        if (corlTimerEnabled) {
            corlTimerEnabled = false;
            ModFunctions.displayTextMessageWithHeader("§cCorleone Timer toggled off");
        } else {
            if (!ModFunctions.mapLocationToGeneralArea(SideBarUtils.location).equals("Crystal Hollows")) {
                ModFunctions.displayTextMessageWithHeader("§cYou must be in the Crystal Hollows to use Corleone Timer");
                return;
            }

            corlTimerEnabled = true;
            ModFunctions.displayTextMessageWithHeader("§aCorleone Timer toggled on");
        }
    }

    public static void resetCorlTimer() {
        corlTimerEnabled = false;
        corlPos = null;
        corlUUID = null;
        waitTime = false;
    }
}