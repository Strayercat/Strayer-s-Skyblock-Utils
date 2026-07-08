package com.skyblockutils.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerTeam;

import java.util.List;

public class SideBarUtils {
    public static String location = "";
    public static Minecraft client = Minecraft.getInstance();

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
                .filter(line -> line.contains("\uE067") || line.contains("ф"))
                .findFirst()
                .ifPresent(line -> location = line.replaceAll("\uE067", "").trim());
    }

    public static List<String> getSidebarLines() {
        if (client.level == null) return List.of();

        var scoreboard = client.level.getScoreboard();
        var sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return List.of();

        return scoreboard.listPlayerScores(sidebar).stream()
                .sorted((a, b) -> b.value() - a.value())
                .map(entry -> {
                    PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
                    return team != null
                            ? team.getPlayerPrefix().getString() + entry.owner() + team.getPlayerSuffix().getString()
                            : entry.owner();
                })
                .toList();
    }

    public static void resetLocation() {
        location = "";
    }
}