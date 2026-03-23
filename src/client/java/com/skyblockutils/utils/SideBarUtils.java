package com.skyblockutils.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.Team;

import java.util.Collection;

public class SideBarUtils {
    public static String date = null;
    public static String time = null;
    public static String location = null;
    public static String purse = null;
    public static String bits = null;

    public static MinecraftClient client = MinecraftClient.getInstance();
    public static String getSideBarInfo(String info) {
        if (client.world == null) return ""; // Return empty instead of null

        var scoreboard = client.world.getScoreboard();
        var sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null) return "";

        // Reset variables so old data doesn't persist from previous areas
        date = "";
        time = "";
        location = "";
        purse = "";
        bits = "";

        Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(sidebar);
        for (ScoreboardEntry entry : entries) {
            String playerName = entry.owner();
            Team team = scoreboard.getScoreHolderTeam(playerName);
            String lineText;

            if (team != null) {
                lineText = team.getPrefix().getString() + playerName + team.getSuffix().getString();
            } else {
                lineText = playerName;
            }

            lineText = lineText.replaceAll("(?i)§.", "").trim(); // Case-insensitive regex for safety

            if (lineText.contains("⏣")) location = lineText;
            // ... (Your other regex checks)
        }

        // Use yield or simple return with a fallback to empty string
        return switch (info) {
            case "date" -> date;
            case "time" -> time;
            case "location" -> location;
            case "purse" -> purse;
            case "bits" -> bits;
            default -> "";
        };
    }
}
