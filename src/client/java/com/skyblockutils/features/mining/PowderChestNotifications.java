package com.skyblockutils.features.mining;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.ChatBlockRouter;
import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.utils.SideBarUtils;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PowderChestNotifications {
    private static final String LOCKPICK_HEADER = "CHEST LOCKPICKED";
    private static final String LOOT_HEADER = "LOOT CHEST COLLECTED";

    public static final ChatBlockRouter.Handler BLOCK_HANDLER = new ChatBlockRouter.Handler() {
        @Override
        public boolean isActive() {
            return ModConfig.INSTANCE.powderChestNotification
                    && "Crystal Hollows".equals(ModFunctions.mapLocationToGeneralArea(SideBarUtils.location));
        }

        @Override
        public boolean matches(String header) {
            return header.contains(LOCKPICK_HEADER) || header.contains(LOOT_HEADER);
        }

        @Override
        public void accept(ChatBlockRouter.Block block) {
            List<Component> subtitle = block.lines().stream()
                    .filter(line -> {
                        String text = ChatBlockRouter.clean(line);
                        return !text.isEmpty() && !text.equals("REWARDS");
                    })
                    .map(line -> OnScreenNotification.removeText(line, " Gemstone"))
                    .toList();

            OnScreenNotification.builder()
                    .title(block.headerText().contains(LOCKPICK_HEADER) ? "CHEST LOCKPICKED" : "LOOT CHEST")
                    .subtitle(subtitle)
                    .tickTime(ModConfig.INSTANCE.powderChestNotificationTime)
                    .send();
        }
    };
}
