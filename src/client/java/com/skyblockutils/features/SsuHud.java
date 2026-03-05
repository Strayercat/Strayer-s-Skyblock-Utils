package com.skyblockutils.features;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.dungeons.DowntimeTracker;
import com.skyblockutils.features.dungeons.DungeonPartyCommands;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SsuHud {
    private static boolean isVisible = false;
    public static boolean showBossBar = true;
    public static String funFact;
    public static int hudWidth = 200;
    public static int lineHeight = 12;

    public static void setVisible(boolean visible) {
        if (!ModConfig.INSTANCE.hudEnabled) return;
        isVisible = visible;
        showBossBar = !visible;
    }

    private record HudLine(String text, int color) {
    }

    public static void onHudRender(DrawContext context, String location) {
        if (!isVisible) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        List<HudLine> lines = getHudLines(client, location);
        if (lines.size() == 2) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int hudHeight = lines.size() * lineHeight + 10;
        int textAlignment = screenWidth / 2 - (hudWidth / 2) + 5;
        int y = 5;

        context.fill((screenWidth / 2) - (hudWidth / 2), 0, (screenWidth / 2) + (hudWidth / 2), hudHeight, 0xAA000000);

        for (HudLine line : lines) {
            context.drawTextWithShadow(client.textRenderer, line.text(), textAlignment, y, line.color());
            y += lineHeight;
        }
    }

    private static @NotNull List<HudLine> getHudLines(MinecraftClient client, String location) {
        int whiteColor = 0xFFFFFFFF;
        int lineColor = 0xFFAAFFAA;
//        List<String> hubLocations = List.of("⏣ Village", "⏣ Combat Settlement", "⏣ Graveyard", "⏣ Mining District", "⏣ Farm", "⏣ Fishing Outpost", "⏣ Colosseum", "⏣ Wilderness", "⏣ Mountain", "⏣ Unincorporated", "⏣ Ruins", "⏣ Forest", "⏣ Community Center", "⏣ Election Room", "⏣ Canvas Room", "⏣ Auction House", "⏣ Bazaar Alley", "⏣ Bank", "⏣ Builder's House", "⏣ Pet Care", "⏣ Taylor's Shop", "⏣ Fashion Shop", "⏣ Fisherman's Hut", "⏣ Thaumaturgist", "⏣ Tavern", "⏣ Crypts", "⏣ Coal Mine", "⏣ Catacombs Entrance", "⏣ Foraging Camp", "⏣ Sewer", "⏣ Rabbit House", "⏣ Flower House", "⏣ Artist's Abode", "⏣ Wizard Tower", "⏣ Shen's Auction", "⏣ Trade Center", "⏣ Hexatorum");
        List<HudLine> lines = new java.util.ArrayList<>(List.of(
                new HudLine("STRAYER'S SKYBLOCK UTILS", 0xFF66FF66),
                new HudLine("--------------------------------", whiteColor)));

        if (ModConfig.INSTANCE.hudTime)
            lines.add(new HudLine("Current time: " + LocalTime.now()
                    .format(ModConfig.INSTANCE.hudTime12hFormat
                            ? DateTimeFormatter.ofPattern("h:mm a")
                            : DateTimeFormatter.ofPattern("H:mm")).toUpperCase().replaceAll("\\.", ""), lineColor));
        if (ModConfig.INSTANCE.hudPing) lines.add(new HudLine("Ping: " + ModFunctions.getPing(client), lineColor));
        if (ModConfig.INSTANCE.hudTps)
            lines.add(new HudLine("Server tps: " + String.format("%.1f", ModFunctions.tps), lineColor));

        if (location.equals("⏣ Dungeon Hub") || ModFunctions.isInDungeons(client)) {
            if (ModConfig.INSTANCE.hudTps || ModConfig.INSTANCE.hudPing || ModConfig.INSTANCE.hudTime)
                lines.add(new HudLine("--------------------------------", whiteColor));
            lines.add(new HudLine("Dungeon HUD", whiteColor));
            lines.add(new HudLine("Downtime requested: " + (DowntimeTracker.downtimeRequested ? "Yes" : "No"), lineColor));
            lines.add(new HudLine("Requested by: " + (DowntimeTracker.downtimeRequested ? DowntimeTracker.requesterUsername : ""), lineColor));
            lines.add(new HudLine("", whiteColor));
            lines.add(new HudLine("Floor Auto-Rejoin: " + (DungeonPartyCommands.autoRejoinEnabled ? "On" : "Off"), lineColor));
            lines.add(new HudLine("Current floor: " + DungeonPartyCommands.currentFloor, lineColor));
            return lines;
        }

        if (location.equals("⏣ Your Island")) {
            if (ModConfig.INSTANCE.hudIslandFunFact) {
                lines.add(new HudLine("--------------------------------", whiteColor));
                lines.addAll(wrapText("Fun fact: " + funFact, hudWidth - 10, lineColor));
            }
            return lines;
        }
        return lines;
    }

    private static List<HudLine> wrapText(String text, int maxWidth, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<HudLine> wrappedLines = new java.util.ArrayList<>();

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            int lineWidth = client.textRenderer.getWidth(testLine);

            if (lineWidth > maxWidth && !currentLine.isEmpty()) {
                wrappedLines.add(new HudLine(currentLine.toString(), color));
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        if (!currentLine.isEmpty()) {
            wrappedLines.add(new HudLine(currentLine.toString(), color));
        }

        return wrappedLines;
    }
}