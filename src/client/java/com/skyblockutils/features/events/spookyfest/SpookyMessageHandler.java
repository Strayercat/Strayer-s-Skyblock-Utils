package com.skyblockutils.features.events.spookyfest;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.OnScreenNotification;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public class SpookyMessageHandler {
    private static final List<String> allowedLoot = List.of("Green Candy", "Purple Candy", "Ectoplasm", "Blast o' Lantern", "Candy Corn", "Pumpkin Guts", "Rock Candy", "Spooky Cupcake", "Bat Person Talisman", "Vampirism VI", "Candy the Fish");
    private static boolean expecingLoot = false;
    private static final List<Component> buffer = new ArrayList<>();

    public static boolean handleMessage(Component message) {
        if (message.getString().equals("SPOOKY! A Trick or Treat Chest has appeared!") && ModConfig.INSTANCE.spookyChestTitle) {
            ModFunctions.showTitle(Minecraft.getInstance(), "§6SPOOKY", 30, true);
            return false;
        }

        if (message.getString().matches("TRICK! A .* has tricked you!") && ModConfig.INSTANCE.spookyTrickJumpscare) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return true;
            mc.player.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER);
            ModFunctions.showTitle(Minecraft.getInstance(), "§4BOO!", 10, false);
        }

        if (ModConfig.INSTANCE.spookyLootNotification && message.getString().equals("TREAT! Your Trick or Treat Chest rewarded you with:")) {
            buffer.clear();
            expecingLoot = true;
            return false;
        }

        if (!expecingLoot || message.getString().contains(":")) return true;

        if (allowedLoot.contains(message.getString().replaceAll("x\\d", "").trim())) {
            buffer.add(message);
            return false;
        } else {
            if (message.getString().contains("\uE010") || message.getString().contains("\uE008") || message.getString().contains("\uE003")) return true;
            expecingLoot = false;
            OnScreenNotification.builder()
                    .title("Spooky Chest Rewards")
                    .subtitle(buffer)
                    .send();
        }

        return true;
    }
}