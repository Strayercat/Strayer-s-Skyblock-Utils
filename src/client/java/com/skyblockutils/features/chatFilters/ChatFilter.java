package com.skyblockutils.features.chatFilters;

import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.mixin.client.BossHealthOverlayAccessor;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.regex.Pattern;

public class ChatFilter {
    private static final List<ChatFilterDefinitions.FilterEntry> ALL_FILTERS = ChatFilterDefinitions.getAllEntries();

    private static final Pattern USER_SENT_MESSAGE_PATTERN = Pattern.compile("^\\[\\d{1,3}]\\s.*?\\s(?:\\[[A-Z]+\\+])?\\s.+: .+$");

    public static boolean filterMessages(String message) {

        if (!ModConfig.INSTANCE.chatFiltersEnabled) {
            return false;
        }

        if (!StrayersSkyblockUtilsClient.isInSkyblock) {
            return false;
        }

        if (USER_SENT_MESSAGE_PATTERN.matcher(message).find() || message.startsWith("Officer >") || message.startsWith("Guild >") || message.startsWith("Party >") || message.startsWith("Co-op >")) {
            return false;
        }

        for (ChatFilterDefinitions.FilterEntry filter : ALL_FILTERS) {
            if (ModConfig.INSTANCE.getChatFilter(filter.configKey)) {
                if (filter.requiredLocation != null) {
                    if (filter.requiredLocation.equals("The Catacombs")) {
                        boolean isInCatacombs = StrayersSkyblockUtilsClient.location.contains("The Catacombs");
                        boolean hasF3Boss = ((BossHealthOverlayAccessor) MinecraftClient.getInstance().inGameHud.getBossBarHud())
                                .getEvents().values().stream().findFirst()
                                .map(bossBar -> bossBar.getName().getString().replaceAll("§.", "").contains("The Professor"))
                                .orElse(false);

                        if (!isInCatacombs && !hasF3Boss) {
                            continue;
                        }
                    } else {
                        if (!StrayersSkyblockUtilsClient.location.contains(filter.requiredLocation)) {
                            continue;
                        }
                    }
                }

                if (filter.pattern.matcher(message).matches()) {
                    return true;
                }
            }
        }

        return false;
    }
}