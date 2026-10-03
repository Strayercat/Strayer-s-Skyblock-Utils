package com.skyblockutils.features.events.spookyfest;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.OnScreenNotification;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.skyblockutils.utils.Scheduler.scheduler;

public class SpookyMessageHandler {
    private static final List<String> allowedLoot = List.of("Green Candy", "Purple Candy", "Ectoplasm", "Blast o' Lantern", "Candy Corn", "Pumpkin Guts", "Rock Candy", "Spooky Cupcake", "Bat Person Talisman", "Enchanted Book (Vampirism VI)", "Candy the Fish");
    private static final Pattern AMOUNT_SUFFIX = Pattern.compile("\\s*x[\\d,]+$");
    private static boolean expectingLoot = false;
    private static ScheduledFuture<?> pendingNotification = null;
    private static final List<Component> buffer = new ArrayList<>();

    public static boolean handleMessage(Component message) {
        String text = message.getString();

        if (text.equals("SPOOKY! A Trick or Treat Chest has appeared!") && ModConfig.INSTANCE.spookyChestTitle) {
            ModFunctions.showTitle(Minecraft.getInstance(), "§6SPOOKY", 30, true);
            return false;
        }

        if (text.matches("TRICK! A .* has tricked you!") && ModConfig.INSTANCE.spookyTrickJumpscare) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return true;
            mc.player.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER);
            ModFunctions.showTitle(Minecraft.getInstance(), "§4BOO!", 10, false);
        }

        if (ModConfig.INSTANCE.spookyLootNotification && text.equals("TREAT! Your Trick or Treat Chest rewarded you with:")) {
            synchronized (buffer) {
                buffer.clear();
            }
            expectingLoot = true;
            return false;
        }

        if (!expectingLoot || text.contains(":")) return true;

        String itemName = AMOUNT_SUFFIX.matcher(text.trim()).replaceAll("").trim();

        if (allowedLoot.contains(itemName)) {
            synchronized (buffer) {
                buffer.add(message);
            }

            if (pendingNotification != null) pendingNotification.cancel(false);
            pendingNotification = scheduler.schedule(() -> Minecraft.getInstance().execute(SpookyMessageHandler::sendNotification), 250, TimeUnit.MILLISECONDS);

            return false;
        }

        return true;
    }

    private static void sendNotification() {
        List<Component> rewards;
        synchronized (buffer) {
            rewards = new ArrayList<>(buffer);
            buffer.clear();
        }

        expectingLoot = false;
        pendingNotification = null;

        OnScreenNotification.builder().title("Spooky Chest Rewards").subtitle(rewards).send();
    }
}