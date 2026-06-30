package com.skyblockutils.features.mining;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.utils.SideBarUtils;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PowderChestNotifications {
    private static boolean expectingChest = false;
    private static boolean reading = false;
    private static final List<Component> buffer = new ArrayList<>();

    public static boolean handleMessage(Component message) {
        if (!ModConfig.INSTANCE.powderChestNotification || !expectingChest) return true;

        if (message.getString().startsWith("▬▬▬▬")) {
            if (!reading) {
                reading = true;
                buffer.clear();
            } else {
                reading = false;
                parseBuffer();
                expectingChest = false;
            }
            return false;
        }

        if (reading) {
            buffer.add(message);
            return false;
        }

        return true;
    }

    public static void handleChestClick() {
        if (ModConfig.INSTANCE.powderChestNotification && ModFunctions.mapLocationToGeneralArea(SideBarUtils.location).equals("Crystal Hollows")) expectingChest = true;
    }

    private static void parseBuffer() {
        List<Component> lines = buffer.stream()
                .filter(c -> {
                    String trimmed = c.getString().trim();
                    return !trimmed.equals("CHEST LOCKPICKED") && !trimmed.equals("REWARDS") && !trimmed.equals("LOOT CHEST COLLECTED");
                })
                .map(c -> OnScreenNotification.removeText(c, " Gemstone"))
                .collect(Collectors.toList());

        OnScreenNotification.builder()
                .title(buffer.getFirst().getString().trim().equals("CHEST LOCKPICKED") ? "CHEST LOCKPICKED" : "LOOT CHEST")
                .subtitle(lines)
                .tickTime(ModConfig.INSTANCE.powderChestNotificationTime)
                .send();
    }
}