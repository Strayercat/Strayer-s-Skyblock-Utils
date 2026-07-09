package com.skyblockutils.features;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.utils.SideBarUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.GameType;

import java.awt.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.List;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import static com.skyblockutils.utils.Scheduler.scheduler;

public class DailyReminders {
    public enum ReminderType {
        ISLE, COMMISSION, MATRIARCH
    }

    private static final ZoneId EST_ZONE = ZoneId.of("America/New_York");

    private static final Comparator<PlayerInfo> TAB_ORDER = Comparator.<PlayerInfo>comparingInt(p -> -p.getTabListOrder())
            .thenComparingInt(p -> p.getGameMode() == GameType.SPECTATOR ? 1 : 0)
            .thenComparing(p -> p.getTeam() != null ? p.getTeam().getName() : "", String::compareToIgnoreCase)
            .thenComparing(p -> p.getProfile().name(), String::compareToIgnoreCase);

    private static final Map<ReminderType, List<String>> dailyLocationMap = new HashMap<>();
    private static final Set<ReminderType> alreadyReminded = new HashSet<>();

    public static void init() {
        dailyLocationMap.put(ReminderType.ISLE, List.of("Crimson Isle"));
        dailyLocationMap.put(ReminderType.COMMISSION, List.of("Dwarven Mines", "Crystal Hollows"));
        dailyLocationMap.put(ReminderType.MATRIARCH, List.of("Crimson Isle"));
    }

    public static void tick(Minecraft client) {
        String location = SideBarUtils.location;

        if (client.level == null) return;

        for (Map.Entry<ReminderType, List<String>> entry : dailyLocationMap.entrySet()) {
            ReminderType type = entry.getKey();
            List<String> locations = entry.getValue();

            if (locations.contains(location) && !ModConfig.INSTANCE.completedTypes.contains(type)) {
                if (!alreadyReminded.contains(type) && !ModConfig.INSTANCE.disabledTypes.contains(type)) {
                    alreadyReminded.add(type);

                    if (ModConfig.INSTANCE.dailyReminders) {
                        scheduler.schedule(() -> {
                            if (!ModConfig.INSTANCE.disabledTypes.contains(type)) {
                                sendReminder(client, type);
                            }
                        }, 2, TimeUnit.SECONDS);
                    }
                }

                if (type == ReminderType.ISLE && client.level.getGameTime() % 100 == 0) {
                    List<String> factionQuestLines = getFactionQuestLines(client);
                    int completedQuests = factionQuestLines.stream().filter(q -> q.contains("✔")).toList().size();
                    if (completedQuests == 5) {
                        ModConfig.INSTANCE.completedTypes.add(type);
                        ModConfig.save();

                        if (ModConfig.INSTANCE.dailyReminders) OnScreenNotification.builder()
                                .title("Isle Quests Completed!")
                                .subtitle("You've completed your daily isle quests. Don't forget to claim your rewards at the town board!")
                                .withSound(true)
                                .tickTime(60)
                                .send();
                    }
                    continue;
                }

                if (type == ReminderType.COMMISSION) {
                    if (!(client.gui.screen() instanceof AbstractContainerScreen<?> containerScreen)) continue;
                    if (!containerScreen.getTitle().getString().contains("Commissions")) continue;

                    for (Slot slot : containerScreen.getMenu().slots) {
                        ItemStack stack = slot.getItem();
                        if (stack.isEmpty() || stack.getItem() != Items.WRITABLE_BOOK) continue;

                        String name = stack.getHoverName().getString();
                        if (!name.matches("Commission #\\d+")) continue;

                        ItemLore lore = stack.get(DataComponents.LORE);
                        boolean hasDailyBonus = lore != null && lore.lines().stream()
                                .anyMatch(line -> line.getString().contains("(Daily Bonus)"));

                        if (!hasDailyBonus) {
                            ModConfig.INSTANCE.completedTypes.add(type);
                            ModConfig.save();

                            if (ModConfig.INSTANCE.dailyReminders) OnScreenNotification.builder()
                                    .title("Commissions Completed!")
                                    .subtitle("You've completed your daily commissions. Don't forget to claim your rewards!")
                                    .withSound(true)
                                    .tickTime(60)
                                    .send();
                        }
                        break;
                    }
                    continue;
                }

                if (type == ReminderType.MATRIARCH && client.level.getGameTime() % 100 == 0) {
                    String matriarchStatus = getMatriarchStatus(client);
                    System.out.println(matriarchStatus);
                    if (matriarchStatus.contains("Available")) continue;

                    ModConfig.INSTANCE.completedTypes.add(type);
                    ModConfig.save();

                    if (ModConfig.INSTANCE.dailyReminders) OnScreenNotification.builder()
                            .title("Matriarch Completed!")
                            .subtitle("You've completed your daily Matriarch!")
                            .withSound(true)
                            .tickTime(60)
                            .send();
                }
            }
        }
    }

    private static List<String> getFactionQuestLines(Minecraft client) {
        if (client.player == null) return List.of();

        List<PlayerInfo> entries = client.player.connection.getListedOnlinePlayers().stream()
                .sorted(TAB_ORDER)
                .toList();

        List<String> lines = entries.stream()
                .map(info -> {
                    Component display = info.getTabListDisplayName() != null
                            ? info.getTabListDisplayName()
                            : Component.literal(info.getProfile().name());
                    return display.getString();
                })
                .toList();

        int headerIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("Faction Quests:")) {
                headerIndex = i;
                break;
            }
        }

        if (headerIndex == -1) return List.of();

        int from = headerIndex + 1;
        int to = Math.min(from + 5, lines.size());
        return lines.subList(from, to);
    }

    private static String getMatriarchStatus(Minecraft client) {
        if (client.player == null) return "";

        List<PlayerInfo> entries = client.player.connection.getListedOnlinePlayers().stream()
                .sorted(TAB_ORDER)
                .toList();

        return entries.stream()
                .map(info -> {
                    Component display = info.getTabListDisplayName() != null
                            ? info.getTabListDisplayName()
                            : Component.literal(info.getProfile().name());
                    return display.getString();
                })
                .filter(l -> l.contains("Matriarch:"))
                .findFirst()
                .orElse("");
    }

    public static void disable(ReminderType type) {
        if (!ModConfig.INSTANCE.disabledTypes.contains(type)) ModConfig.INSTANCE.disabledTypes.add(type);
        ModConfig.save();
    }

    private static void sendReminder(Minecraft client, ReminderType type) {
        if (client.player == null) return;

        String messageToSend = switch (type) {
            case ISLE -> "You haven't completed your isle dailies! ";
            case COMMISSION -> "You haven't completed your daily commissions! ";
            case MATRIARCH -> "Don't forget to grab your heavy pearls from the Matriarch! ";
        };

        OnScreenNotification.builder()
                .title("Daily Reminder")
                .subtitle(messageToSend)
                .tickTime(200)
                .withSound(true)
                .metadata(type)
                .send();
    }

    public static void reset() {
        alreadyReminded.clear();

        ZonedDateTime nowEst = ZonedDateTime.now(EST_ZONE);
        ZonedDateTime lastResetEst = ModConfig.INSTANCE.lastReset.toInstant().atZone(EST_ZONE);

        boolean pastMidnightEst = nowEst.toLocalDate().isAfter(lastResetEst.toLocalDate());
        boolean over24Hours = System.currentTimeMillis() - ModConfig.INSTANCE.lastReset.getTime()
                >= 1000L * 60 * 60 * 24;

        if (pastMidnightEst && over24Hours) {
            dailyReset();
        }
    }

    public static void dailyReset() {
        ModConfig.INSTANCE.lastReset = new Date(System.currentTimeMillis());
        ModConfig.INSTANCE.disabledTypes.clear();
        ModConfig.INSTANCE.completedTypes.clear();
        ModConfig.save();
    }
}