package com.skyblockutils.utils;

import com.skyblockutils.ModFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

public class SideBarUtils {
    public static String location = "";
    public static String motes = "";
    public static String date = "";
    public static String time = "";
    public static String purse = "";
    public static String bits = "";

    public static MinecraftClient client = MinecraftClient.getInstance();

    public static void updateSidebarInfo() {
        if (client.world == null) return;

        var scoreboard = client.world.getScoreboard();
        var sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null) return;

        List<ScoreboardEntry> entries = new ArrayList<>(scoreboard.getScoreboardEntries(sidebar));

        for (ScoreboardEntry entry : entries) {
            String playerName = entry.owner();
            Team team = scoreboard.getScoreHolderTeam(playerName);
            String lineText = team != null
                    ? team.getPrefix().getString() + playerName + team.getSuffix().getString()
                    : playerName;
            lineText = lineText.replaceAll("(?i)§.", "").trim();
            if (lineText.contains("⏣") || lineText.contains("ф")) location = lineText;
        }

        for (ScoreboardEntry entry : entries) {
            String playerName = entry.owner();
            Team team = scoreboard.getScoreHolderTeam(playerName);
            String lineText = team != null
                    ? team.getPrefix().getString() + playerName + team.getSuffix().getString()
                    : playerName;
            lineText = lineText.replaceAll("(?i)§.", "").trim();

            if (ModFunctions.mapLocationToGeneralArea(location).equals("Rift")) {
                if (lineText.matches("Motes: [\\d,.]+")) {
                    String val = lineText.replaceFirst("Motes: ", "").trim();
                    if (!val.isEmpty()) motes = val;
                }
            } else {
                if (lineText.matches("(Early |Late )?(Winter|Summer|Spring|Autumn) \\d+(st|nd|rd|th)")) date = lineText;
                if (lineText.matches("\\d+:\\d+(am|pm) .")) time = lineText;
                if (lineText.matches("(Piggy|Purse): [\\d,.]+( \\(\\+\\d+\\))?")) {
                    String val = lineText.replaceFirst("(Piggy|Purse): ", "").trim();
                    if (!val.isEmpty()) purse = val;
                }
                if (lineText.matches("Bits: \\d+")) {
                    String val = lineText.replaceFirst("Bits: ", "").trim();
                    if (!val.isEmpty()) bits = val;
                }
            }
        }
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

    public static void resetSidebarInfo() {
        location = "";
        motes = "";
        date = "";
        time = "";
        purse = "";
        bits = "";
    }
}
