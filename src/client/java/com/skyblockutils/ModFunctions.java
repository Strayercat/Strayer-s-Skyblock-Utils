package com.skyblockutils;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.AutoFish;
import com.skyblockutils.features.CorlTimer;
import com.skyblockutils.features.SsuHud;
import com.skyblockutils.features.dungeons.DowntimeTracker;
import com.skyblockutils.features.dungeons.DungeonPartyCommands;
import com.skyblockutils.mixin.client.BossHealthOverlayAccessor;
import com.skyblockutils.mixin.client.GuiAccessor;
import com.skyblockutils.utils.FunFacts;
import com.skyblockutils.utils.GuiBlocker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.profiler.MultiValueDebugSampleLogImpl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ModFunctions {
    public static boolean playerWelcomedToIsland = false;
    public static boolean funFactFetched = false;
    public static long lastTimePingCalculated;
    public static int ping = 0;
    public static float tps = 0;

    public static void connectionEventDataReset(String type) {
        if (type.equals("Join")) {
            StrayersSkyblockUtilsClient.location = "";
            playerWelcomedToIsland = false;
            funFactFetched = false;

            GuiBlocker.shouldHideScreen = false;

            DowntimeTracker.resetDowntimeTracker();
            CorlTimer.resetCorlTimer();
            AutoFish.resetAutoFish();
        } else {
            StrayersSkyblockUtilsClient.isInSkyblock = false;
            StrayersSkyblockUtilsClient.location = "";
            playerWelcomedToIsland = false;
            funFactFetched = false;

            DungeonPartyCommands.resetDungeonPartyCommands();
            CorlTimer.resetCorlTimer();
            AutoFish.resetAutoFish();
            DowntimeTracker.resetDowntimeTracker();
        }
    }

    public static void handlePlayerOnIsland(MinecraftClient client, String location) {
        if (ModConfig.INSTANCE.welcomeHomeMessage) {
            if (!playerWelcomedToIsland && location.contains("⏣ Your Island"))
                showTitle(client, "§2WELCOME HOME", 40);
            playerWelcomedToIsland = true;
        }

        if (ModConfig.INSTANCE.hudIslandFunFact && !funFactFetched) {
            SsuHud.funFact = FunFacts.funFacts.get((int) Math.floor(Math.random() * FunFacts.funFacts.size()));
            funFactFetched = true;
        }
    }

    public static void handleKeybinds(MinecraftClient client) {
        while (ModKeyBindings.RUN_T_FUNCTION_KEY.wasPressed()) CorlTimer.toggleCorlTimer(client);
        while (ModKeyBindings.RUN_R_FUNCTION_KEY.wasPressed()) AutoFish.toggleAutoFish(client);
        while (ModKeyBindings.RUN_HOME_FUNCTION_KEY.wasPressed())
            sendCoordinates(client, ModConfig.INSTANCE.coordinatesSendLocation ? "withLocation" : "");
        SsuHud.setVisible(ModKeyBindings.RUN_Z_FUNCTION_KEY.isPressed());
    }

    public static int getPing(MinecraftClient client) {
        if (lastTimePingCalculated > System.currentTimeMillis() - 1000) return ping;
        MultiValueDebugSampleLogImpl pingLog = client.getDebugHud().getPingLog();
        if (pingLog.getLength() == 0) return 0;
        ping = (int) pingLog.get(pingLog.getLength() - 1);
        lastTimePingCalculated = System.currentTimeMillis();
        return ping;
    }

    public static void sendCoordinates(MinecraftClient client, String argumentsString) {
        if (client.player == null || client.getNetworkHandler() == null) return;

        List<String> arguments = Arrays.asList(argumentsString.split("-"));

        String coordinates = "x:" + (int) client.player.getX()
                + " y:" + (int) client.player.getY()
                + " z:" + (int) client.player.getZ();

        String coordinatesMessage = (arguments.contains("withLocation")
                ? ModFunctions.getCurrentLocation(client) + " | "
                : "") + coordinates;

        client.getNetworkHandler().sendChatMessage(coordinatesMessage);
    }

    public static void showTitle(net.minecraft.client.MinecraftClient client, String title, int displayTime) {
        if (client.player != null) {
            GuiAccessor guiAccessor = (GuiAccessor) client.inGameHud;

            guiAccessor.setTitleFadeInTime(10);
            guiAccessor.setTitleStayTime(displayTime);
            guiAccessor.setTitleFadeOutTime(10);

            client.inGameHud.setTitle(Text.literal(title));
        }
    }

    public static Boolean isInSkyblock(net.minecraft.client.MinecraftClient client) {
        if (client.world == null) return null;
        var scoreboard = client.world.getScoreboard();
        var sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null) return null;
        return sidebar.getDisplayName().getString().contains("SKYBLOCK");
    }

    public static String getCurrentLocation(net.minecraft.client.MinecraftClient client) {
        String location = "None";
        if (client.world == null) return location;
        var scoreboard = client.world.getScoreboard();
        var sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null) return location;

        Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(sidebar);
        for (ScoreboardEntry entry : entries) {
            String playerName = entry.owner();
            Team team = scoreboard.getScoreHolderTeam(playerName);
            String fullText;

            if (team != null) {
                fullText = team.getPrefix().getString() + playerName + team.getSuffix().getString();
            } else {
                fullText = playerName;
            }

            if (fullText.contains("⏣")) location = fullText.replaceAll("§.", "").trim();
        }
        return location;
    }

    public static Boolean isInDungeons(net.minecraft.client.MinecraftClient client) {
        boolean isInCatacombs = StrayersSkyblockUtilsClient.location.contains("The Catacombs");
        boolean hasF3Boss = ((BossHealthOverlayAccessor) client.inGameHud.getBossBarHud()).getEvents().values().stream().findFirst().map(bossBar -> bossBar.getName().getString().replaceAll("§.", "").contains("The Professor")).orElse(false);

        return isInCatacombs || hasF3Boss;
    }
}