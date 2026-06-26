package com.skyblockutils.features.hud;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.dungeons.AutoRejoin;
import com.skyblockutils.features.dungeons.DowntimeTracker;
import com.skyblockutils.features.party.PartyInfo;
import com.skyblockutils.utils.FunFacts;
import com.skyblockutils.utils.SSU;
import com.skyblockutils.utils.SideBarUtils;
import com.skyblockutils.utils.ModStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.skyblockutils.utils.ModStyle.*;

public class SsuHud {
    private static final Identifier CAT_IMAGE = Identifier.of("skyblockutils", "textures/hud/cat.png");

    private static boolean isVisible = false;
    public static boolean showBossBar = true;
    public static String funFact = null;
    public static boolean funFactHandled = false;

    public static float scale;

    private static final int BASE_HUD_WIDTH = 200;
    private static final int BASE_LINE_HEIGHT = 12;
    private static final int BASE_DIVIDER_HEIGHT = 6;
    private static final int BASE_PADDING = 10;

    private static final int COLOR_TEXT = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TEXT);
    private static final int COLOR_MAIN = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);

    public static void setVisible(boolean visible) {
        if (!ModConfig.INSTANCE.hudEnabled) return;
        isVisible = visible;
        showBossBar = !visible;
    }

    private record HudLine(String text, int color, boolean isDivider, Text richText) {
        static HudLine of(String text, int color) {
            return new HudLine(text, color, false, null);
        }

        static HudLine rich(Text text) {
            return new HudLine("", 0, false, text);
        }

        static HudLine divider() {
            return new HudLine("", COLOR_MAIN, true, null);
        }
    }

    public static void onHudRender(DrawContext context, String location) {
        if (!isVisible) return;

        scale = ModConfig.INSTANCE.hudScale / 100.0f;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        List<HudLine> lines = getHudLines(client, location);
        if (lines.size() <= 2) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int dividerCount = (int) lines.stream().filter(HudLine::isDivider).count();
        int scaledHudWidth = (int) (BASE_HUD_WIDTH * scale);
        int renderX = screenWidth / 2 - scaledHudWidth / 2;
        int unscaledRenderX = (int) (renderX / scale);
        int unscaledRenderY = BASE_PADDING / 2;
        int unscaledHudWidth = BASE_HUD_WIDTH;
        int unscaledHudHeight = (lines.size() - dividerCount) * BASE_LINE_HEIGHT + dividerCount * BASE_DIVIDER_HEIGHT + BASE_PADDING;
        int scaledHudHeight = (int) (unscaledHudHeight * scale);

        context.fill(renderX, 0, renderX + scaledHudWidth, scaledHudHeight, COLOR_BACKGROUND);
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        context.fill(unscaledRenderX, 0, unscaledRenderX + 1, unscaledHudHeight, COLOR_MAIN);
        context.fill(unscaledRenderX + unscaledHudWidth - 1, 0, unscaledRenderX + unscaledHudWidth, unscaledHudHeight, COLOR_MAIN);
        context.fill(unscaledRenderX, 0, unscaledRenderX + unscaledHudWidth, 1, COLOR_MAIN);
        context.fill(unscaledRenderX, unscaledHudHeight - 1, unscaledRenderX + unscaledHudWidth, unscaledHudHeight, COLOR_MAIN);

        for (HudLine line : lines) {
            if (line.isDivider()) {
                int lineY = unscaledRenderY + 1;
                context.fill(unscaledRenderX + 4, lineY, unscaledRenderX + BASE_HUD_WIDTH - 4, lineY + 1, COLOR_MAIN);
                unscaledRenderY += BASE_DIVIDER_HEIGHT;
            } else {
                if (line.richText() != null) {
                    context.drawTextWithShadow(client.textRenderer, line.richText(), unscaledRenderX + 5, unscaledRenderY, 0xFFFFFFFF);
                } else {
                    context.drawTextWithShadow(client.textRenderer, line.text(), unscaledRenderX + 5, unscaledRenderY, line.color());
                }
                unscaledRenderY += BASE_LINE_HEIGHT;
            }
        }

        if (ModConfig.INSTANCE.customSidebar) return;

        context.getMatrices().popMatrix();
        context.getMatrices().pushMatrix();
        float imageScale = (1.0f / 6.0f) * scale;
        context.getMatrices().scale(imageScale, imageScale);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, CAT_IMAGE,
                (int) ((renderX + scaledHudWidth) / imageScale) - 31,
                (int) ((scaledHudHeight / 2.0f) / imageScale) - 175,
                0, 0, 418, 418, 418, 418);
        context.getMatrices().popMatrix();
    }

    private static @NotNull List<HudLine> getHudLines(MinecraftClient client, String location) {
        List<HudLine> lines = new ArrayList<>();
        lines.add(HudLine.rich(SSU.FULL_NAME));
        addDivider(lines);

        boolean anyGeneralInfo = false;

        if (ModConfig.INSTANCE.hudTime) {
            DateTimeFormatter fmt = ModConfig.INSTANCE.hudTime12hFormat
                    ? DateTimeFormatter.ofPattern("h:mm a")
                    : DateTimeFormatter.ofPattern("H:mm");
            String time = LocalTime.now().format(fmt).toUpperCase().replaceAll("\\.", "");
            lines.add(HudLine.of("Current time: " + time, COLOR_TEXT));
            anyGeneralInfo = true;
        }

        if (ModConfig.INSTANCE.hudPing) {
            lines.add(HudLine.of("Ping: " + ModFunctions.getPing(client), COLOR_TEXT));
            anyGeneralInfo = true;
        }

        if (ModConfig.INSTANCE.hudTps) {
            lines.add(HudLine.of("Server tps: " + String.format("%.1f", ModFunctions.tps), COLOR_TEXT));
            anyGeneralInfo = true;
        }

        if (ModConfig.INSTANCE.hudFps) {
            lines.add(HudLine.of("Fps: " + client.getCurrentFps(), COLOR_TEXT));
            anyGeneralInfo = true;
        }

        if (ModConfig.INSTANCE.hudCoords) {
            lines.add(HudLine.rich(ModFunctions.getFormattedCoordinates()));
        }

        if (ModConfig.INSTANCE.sideBarInHud) {
            addSidebarSection(lines, anyGeneralInfo);
            anyGeneralInfo = true;
        }

        if (ModConfig.INSTANCE.hudPartyInfo) {
            boolean showParty = !ModFunctions.isInDungeons(client)
                    || ModConfig.INSTANCE.hudPartyInfoInDungeons;
            if (showParty) {
                addPartySection(lines, anyGeneralInfo);
                anyGeneralInfo = true;
            }
        }

        if (location.equals("⏣ Dungeon Hub") || ModFunctions.isInDungeons(client)) {
            addDungeonSection(lines, anyGeneralInfo);
            return lines;
        }

        if (location.equals("⏣ Your Island") && ModConfig.INSTANCE.hudIslandFunFact) {
            if (!funFactHandled) funFact = FunFacts.funFacts.get((int) (Math.random() * FunFacts.funFacts.size()));
            if (anyGeneralInfo) addDivider(lines);
            lines.addAll(wrapText("Fun fact: " + funFact));
            funFactHandled = true;
        }

        return lines;
    }

    private static void addDivider(List<HudLine> lines) {
        lines.add(HudLine.divider());
    }

    private static void addSidebarSection(List<HudLine> lines, boolean preceded) {
        if (preceded) addDivider(lines);

        List<String> scoreboardLines = new java.util.ArrayList<>(SideBarUtils.getSidebarLines().stream()
                .filter(line -> !line.replaceAll("(?i)§.", "").trim().isEmpty()
                        && !line.replaceAll("§.", "").trim().matches("\\d{2}/\\d{2}/\\d{2} .*")
                        && !line.replaceAll("§.", "").trim().matches("www.hypixel.net"))
                .map(line -> line.trim())
                .toList());

        for (String line : scoreboardLines) {
            lines.add(HudLine.of(line, 0xFFFFFFFF));
        }
    }

    private static void addPartySection(List<HudLine> lines, boolean preceded) {
        if (preceded) addDivider(lines);
        lines.add(HudLine.of("Party Info", COLOR_TITLE));

        if (!PartyInfo.isInParty) {
            lines.add(HudLine.of("Not currently in a party", COLOR_TEXT));
            return;
        }

        lines.add(HudLine.of("Leader:", COLOR_SUBTITLE));
        lines.add(HudLine.of(PartyInfo.leader, COLOR_TEXT));
        lines.add(HudLine.of("Members:", COLOR_SUBTITLE));

        String memberList = PartyInfo.members.toString().replace("[", "").replace("]", "");
        lines.addAll(wrapText(memberList));
    }

    private static void addDungeonSection(List<HudLine> lines, boolean preceded) {
        if (preceded) addDivider(lines);
        lines.add(HudLine.of("Dungeon HUD", COLOR_TITLE));
        lines.add(HudLine.of("Downtime requested: " + (DowntimeTracker.downtimeRequested ? "Yes" : "No"), COLOR_TEXT));

        if (DowntimeTracker.downtimeRequested)
            lines.add(HudLine.of("Requested by: " + DowntimeTracker.requesterUsername, COLOR_TEXT));

        lines.add(HudLine.of("Floor Auto-Rejoin: " + (AutoRejoin.autoRejoinEnabled ? "On" : "Off"), COLOR_TEXT));
        if (AutoRejoin.autoRejoinEnabled)
            lines.add(HudLine.of("Current floor: " + AutoRejoin.currentFloor, COLOR_TEXT));
    }

    private static List<HudLine> wrapText(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<HudLine> wrappedLines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (client.textRenderer.getWidth(testLine) > 190 && !currentLine.isEmpty()) {
                wrappedLines.add(HudLine.of(currentLine.toString(), SsuHud.COLOR_TEXT));
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        if (!currentLine.isEmpty()) {
            wrappedLines.add(HudLine.of(currentLine.toString(), SsuHud.COLOR_TEXT));
        }

        return wrappedLines;
    }
}