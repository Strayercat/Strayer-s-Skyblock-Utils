package com.skyblockutils.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerTeam;

import java.util.List;

public class SideBarUtils {
    public static String location = "";
    public static Minecraft client = Minecraft.getInstance();

    private static final String LOCATION_INDICATOR = "\uE067";

    private static final String WIND_COMPASS_ICON_LEFT = "\uE060";
    private static final String WIND_COMPASS_ICON_RIGHT = "\uE061";
    private static final String WIND_COMPASS_SPINNER = "≈";

    public static void updateLocation() {
        if (client.level == null) return;

        var scoreboard = client.level.getScoreboard();
        var sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return;

        scoreboard.listPlayerScores(sidebar).stream()
                .map(entry -> {
                    PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
                    String raw = team != null
                            ? team.getPlayerPrefix().getString() + entry.owner() + team.getPlayerSuffix().getString()
                            : entry.owner();
                    return raw.replaceAll("(?i)§.", "").trim();
                })
                .filter(line -> line.contains(LOCATION_INDICATOR) || line.contains("ф"))
                .findFirst()
                .ifPresent(line -> location = line.replaceAll(LOCATION_INDICATOR, "").trim());
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
    }
}