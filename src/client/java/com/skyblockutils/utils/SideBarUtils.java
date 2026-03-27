package com.skyblockutils.utils;

import com.skyblockutils.ModFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SideBarUtils {
    public static String location = "";
    public static String motes = "";
    public static String date = "";
    public static String time = "";
    public static String purse = "";
    public static String bits = "";

    private static final Pattern MOTES = Pattern.compile("Motes: [\\d,.]+");
    private static final Pattern DATE = Pattern.compile("(Early |Late )?(Winter|Summer|Spring|Autumn) \\d+(st|nd|rd|th)");
    private static final Pattern TIME = Pattern.compile("\\d+:\\d+(am|pm) .");
    private static final Pattern PURSE = Pattern.compile("(Piggy|Purse): [\\d,.]+( \\(\\+\\d+\\))?");
    private static final Pattern BITS = Pattern.compile("Bits: \\d+");

    public static MinecraftClient client = MinecraftClient.getInstance();

    public static void updateSidebarInfo() {
        if (client.world == null) return;

        var scoreboard = client.world.getScoreboard();
        var sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null) return;

        List<ScoreboardEntry> entries = new ArrayList<>(scoreboard.getScoreboardEntries(sidebar));

        // First pass: location only
        for (ScoreboardEntry entry : entries) {
            String lineText = stripFormatting(scoreboard.getScoreHolderTeam(entry.owner()), entry.owner());
            if (lineText.contains("⏣") || lineText.contains("ф")) {
                location = lineText;
                break;
            }
        }

        boolean inRift = ModFunctions.mapLocationToGeneralArea(location).equals("Rift");
        int found = 0;
        int target = inRift ? 1 : 4;

        // Second pass: everything else
        for (ScoreboardEntry entry : entries) {
            if (found >= target) break;

            String lineText = stripFormatting(scoreboard.getScoreHolderTeam(entry.owner()), entry.owner());

            if (inRift) {
                if (MOTES.matcher(lineText).matches()) { motes = lineText.replaceFirst("Motes: ", "").trim(); found++; }
            } else {
                if (DATE.matcher(lineText).matches()) { date = lineText; found++; }
                else if (TIME.matcher(lineText).matches()) { time = lineText; found++; }
                else if (PURSE.matcher(lineText).matches()) { purse = lineText.replaceFirst("(Piggy|Purse): ", "").trim(); found++; }
                else if (BITS.matcher(lineText).matches()) { bits = lineText.replaceFirst("Bits: ", "").trim(); found++; }
            }
        }
    }

    private static String stripFormatting(Team team, String playerName) {
        String raw = team != null
                ? team.getPrefix().getString() + playerName + team.getSuffix().getString()
                : playerName;
        return raw.replaceAll("(?i)§.", "").trim();
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