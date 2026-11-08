package com.skyblockutils.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class SideBarUtils {
    public static String location = "";
    public static Minecraft client = Minecraft.getInstance();

    private static final String LOCATION_INDICATOR = "\uE067";

    private static final String WIND_COMPASS_ICON_LEFT = "\uE060";
    private static final String WIND_COMPASS_ICON_RIGHT = "\uE061";
    private static final String WIND_COMPASS_SPINNER = "≈";

    private static @Nullable ClientLevel trackedLevel;
    private static @Nullable Objective lastSeenSidebar;
    private static @Nullable Objective staleSidebar;
    private static boolean sidebarCleared = false;
    private static boolean ready = false;

    public static boolean isReady() {
        return ready;
    }

    public static void tick() {
        ClientLevel level = client.level;
        if (level != trackedLevel) {
            trackedLevel = level;
            staleSidebar = lastSeenSidebar;
            sidebarCleared = false;
            ready = false;
            location = "";
        }
        if (level == null) {
            lastSeenSidebar = null;
            return;
        }

        Scoreboard scoreboard = level.getScoreboard();
        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        lastSeenSidebar = sidebar;
        if (sidebar == null) {
            sidebarCleared = true;
            return;
        }
        if (!ready && !sidebarCleared && sidebar == staleSidebar) return;

        String found = readLocation(scoreboard, sidebar);
        if (found == null) return;
        location = found;
        ready = true;
    }

    private static @Nullable String readLocation(Scoreboard scoreboard, Objective sidebar) {
        return scoreboard.listPlayerScores(sidebar).stream()
                .map(entry -> {
                    PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
                    String raw = team != null
                            ? team.getPlayerPrefix().getString() + entry.owner() + team.getPlayerSuffix().getString()
                            : entry.owner();
                    return raw.replaceAll("(?i)§.", "").trim();
                })
                .filter(line -> line.contains(LOCATION_INDICATOR) || line.contains("ф"))
                .findFirst()
                .map(line -> line.replaceAll(LOCATION_INDICATOR, "").trim())
                .orElse(null);
    }

    public static List<String> getSidebarLines() {
        if (client.level == null) return List.of();

        var scoreboard = client.level.getScoreboard();
        var sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return List.of();

        return scoreboard.listPlayerScores(sidebar).stream()
                .sorted((a, b) -> Integer.compare(b.value(), a.value()))
                .map(entry -> {
                    PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
                    return team != null
                            ? team.getPlayerPrefix().getString() + entry.owner() + team.getPlayerSuffix().getString()
                            : entry.owner();
                })
                .map(line -> isWindCompassLine(line) ? line : line.trim())
                .toList();
    }

    private static boolean isWindCompassLine(String line) {
        return line.contains(WIND_COMPASS_ICON_LEFT)
                || line.contains(WIND_COMPASS_ICON_RIGHT)
                || line.contains(WIND_COMPASS_SPINNER);
    }

    public static void resetLocation() {
        location = "";
        ready = false;
        trackedLevel = null;
        lastSeenSidebar = null;
    }
}