package com.skyblockutils.features.mining;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.utils.SideBarUtils;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.entity.Entity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CorlTimer {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static boolean corlTimerEnabled = false;
    public static BlockPos corlPos = null;
    public static UUID corlUUID = null;
    public static boolean waitTime = false;

    public static void corlTimerTick(net.minecraft.client.MinecraftClient client) {
        if (!corlTimerEnabled || client.world == null) return;

        if (corlPos == null) {
            Iterable<Entity> entities = client.world.getEntities();
            for (Entity entity : entities) {
                if (entity.getName().getString().contains("Corleone")) {
                    corlPos = entity.getBlockPos();
                    corlUUID = entity.getUuid();
                    break;
                }
            }
            return;
        }

        if (corlUUID != null) {
            if (client.world.getEntity(corlUUID) == null) {
                corlUUID = null;
                waitTime = true;

                scheduler.schedule(() -> {
                    if (client.world == null) return;
                    client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0F));
                    ModFunctions.showTitle(client, "§6CORL", 20);
                    waitTime = false;
                }, 1, TimeUnit.MINUTES);
            }
            return;
        }

        if (waitTime) return;

        Box blockBox = new Box(corlPos).expand(5.0, 3.0, 5.0);
        List<Entity> entities = client.world.getOtherEntities(null, blockBox);

        for (Entity entity : entities) {
            if (entity.getName().getString().contains("Corleone")) {
                corlUUID = entity.getUuid();
            }
        }
    }

    public static void toggleCorlTimer(net.minecraft.client.MinecraftClient client) {
        if (corlTimerEnabled) {
            corlTimerEnabled = false;
            client.inGameHud.getChatHud().addMessage(Text.literal("§6[SSU] §cToggled off"));
        } else {
            String currentLocation = SideBarUtils.getSideBarInfo("location");
            List<String> allowedLocations = List.of("⏣ Mithril Deposits", "⏣ Precursor Remnants", "⏣ Jungle", "⏣ Goblin Holdout", "⏣ Crystal Nucleus", "⏣ Magma Fields");

            System.out.println(allowedLocations.contains(currentLocation));
            System.out.println(currentLocation);
            System.out.println(allowedLocations);
            if (!allowedLocations.contains(currentLocation)) {
                client.inGameHud.getChatHud().addMessage(Text.literal("§6[SSU] §cYou must be in the Crystal Hollows to use Corleone Timer"));
                return;
            }

            corlTimerEnabled = true;
            client.inGameHud.getChatHud().addMessage(Text.literal("§6[SSU] §aToggled on"));
        }
    }

    public static void resetCorlTimer() {
        corlTimerEnabled = false;
        corlPos = null;
        corlUUID = null;
        waitTime = false;
    }
}