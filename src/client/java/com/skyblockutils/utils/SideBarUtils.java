package com.skyblockutils.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.Team;

import java.util.List;

public class SideBarUtils {
    public static String location = "";

    public static MinecraftClient client = MinecraftClient.getInstance();

    public static void updateLocation() {
        if (client.world == null) return;

        var scoreboard = client.world.getScoreboard();
        var sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null) return;

        scoreboard.getScoreboardEntries(sidebar).stream()
                .map(entry -> {
                    Team team = scoreboard.getScoreHolderTeam(entry.owner());
                    String raw = team != null
                            ? team.getPrefix().getString() + entry.owner() + team.getSuffix().getString()
                            : entry.owner();
                    return raw.replaceAll("(?i)§.", "").trim();
                })
                .filter(line -> line.contains("⏣") || line.contains("ф"))
                .findFirst()
                .ifPresent(line -> location = line);
    }

    public static List<String> getSidebarLines() {
        if (client.world == null) return List.of();

        var scoreboard = client.world.getScoreboard();
        var sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null) return List.of();

        return scoreboard.getScoreboardEntries(sidebar).stream()
                .sorted((a, b) -> b.value() - a.value())
                .map(entry -> {
                    Team team = scoreboard.getScoreHolderTeam(entry.owner());
                    return team != null
                            ? team.getPrefix().getString() + entry.owner() + team.getSuffix().getString()
                            : entry.owner();
                })
                .toList();
    }

    public static void resetLocation() {
        location = "";
    }
}