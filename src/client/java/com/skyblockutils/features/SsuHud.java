package com.skyblockutils.features;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.dungeons.AutoRejoin;
import com.skyblockutils.features.dungeons.DowntimeTracker;
import com.skyblockutils.features.party.PartyInfo;
import com.skyblockutils.utils.FunFacts;
import com.skyblockutils.utils.SSU;
import com.skyblockutils.utils.SideBarUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_SUBTITLE = 0xFFAAAAAA;
    private static final int COLOR_LINE = 0xFFAAFFAA;
    private static final int COLOR_OUTLINE = 0xFF00AA00;
    private static final int COLOR_BG = 0xAA000000;
    private static final int COLOR_DIVIDER = 0xFF00AA00;
    private static final int COLOR_MOTES = 0xFFAA00FF;
    private static final int COLOR_PURSE = 0xFFFFB500;
    private static final int COLOR_BITS = 0xFF66EEFF;

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
            return new HudLine("", COLOR_DIVIDER, true, null);
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
        int scaledHudHeight = (int)(unscaledHudHeight * scale);

        context.fill(renderX, 0, renderX + scaledHudWidth, scaledHudHeight, COLOR_BG);
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        context.fill(unscaledRenderX, 0, unscaledRenderX + 1, unscaledHudHeight, COLOR_OUTLINE);
        context.fill(unscaledRenderX + unscaledHudWidth - 1, 0, unscaledRenderX + unscaledHudWidth, unscaledHudHeight, COLOR_OUTLINE);
        context.fill(unscaledRenderX, 0, unscaledRenderX + unscaledHudWidth, 1, COLOR_OUTLINE);
        context.fill(unscaledRenderX, unscaledHudHeight - 1, unscaledRenderX + unscaledHudWidth, unscaledHudHeight, COLOR_OUTLINE);

        for (HudLine line : lines) {
            if (line.isDivider()) {
                int lineY = unscaledRenderY + 1;
                context.fill(unscaledRenderX + 4, lineY, unscaledRenderX + BASE_HUD_WIDTH - 4, lineY + 1, COLOR_DIVIDER);
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

        context.getMatrices().popMatrix();
        context.getMatrices().pushMatrix();
        float imageScale = (1.0f / 6.0f) * scale;
        context.getMatrices().scale(imageScale, imageScale);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, CAT_IMAGE,
                (int)((renderX + scaledHudWidth) / imageScale) - 31,
                (int)((scaledHudHeight / 2.0f) / imageScale) - 175,
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
            lines.add(HudLine.of("Current time: " + time, COLOR_LINE));
            anyGeneralInfo = true;
        }

        if (ModConfig.INSTANCE.hudPing) {
            lines.add(HudLine.of("Ping: " + ModFunctions.getPing(client), COLOR_LINE));
            anyGeneralInfo = true;
        }

        if (ModConfig.INSTANCE.hudTps) {
            lines.add(HudLine.of("Server tps: " + String.format("%.1f", ModFunctions.tps), COLOR_LINE));
            anyGeneralInfo = true;
        }

        if (ModConfig.INSTANCE.hudFps) {
            lines.add(HudLine.of("Fps: " + client.getCurrentFps(), COLOR_LINE));
            anyGeneralInfo = true;
        }

        if (ModConfig.INSTANCE.hudCoords) {
            if (client.player == null) return lines;
            lines.add(HudLine.rich(Text.literal("X: ").formatted(Formatting.DARK_GREEN).append(Text.literal(String.valueOf((int) client.player.getX())).formatted(Formatting.WHITE)).append(" ")
                    .append(Text.literal("Y: ").formatted(Formatting.DARK_GREEN)).append(Text.literal(String.valueOf((int) client.player.getY())).formatted(Formatting.WHITE)).append(" ")
                    .append(Text.literal("Z: ").formatted(Formatting.DARK_GREEN)).append(Text.literal(String.valueOf((int) client.player.getZ())).formatted(Formatting.WHITE))
            ));
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
        lines.add(HudLine.of("Scoreboard Info", COLOR_TITLE));

        String motes = SideBarUtils.motes;
        String date = SideBarUtils.date;
        String time = SideBarUtils.time;
        String sbLoc = SideBarUtils.location;
        String purse = SideBarUtils.purse;
        String bits = SideBarUtils.bits;

        if (ModFunctions.mapLocationToGeneralArea(sbLoc).equals("Rift")) {
            if (sbLoc != null) lines.add(HudLine.of(sbLoc, COLOR_LINE));
            if (motes != null) lines.add(HudLine.of("Motes: " + motes, COLOR_MOTES));
        } else {
            if (date != null) lines.add(HudLine.of(date, COLOR_LINE));
            if (time != null) lines.add(HudLine.of(time, COLOR_LINE));
            if (sbLoc != null) lines.add(HudLine.of(sbLoc, COLOR_LINE));
            if (purse != null) lines.add(HudLine.of("Purse: " + purse, COLOR_PURSE));
            if (bits != null) lines.add(HudLine.of("Bits: " + bits, COLOR_BITS));
        }
    }

    private static void addPartySection(List<HudLine> lines, boolean preceded) {
        if (preceded) addDivider(lines);
        lines.add(HudLine.of("Party Info", COLOR_TITLE));

        if (!PartyInfo.isInParty) {
            lines.add(HudLine.of("Not currently in a party", COLOR_LINE));
            return;
        }

        lines.add(HudLine.of("Leader:", COLOR_SUBTITLE));
        lines.add(HudLine.of(PartyInfo.leader, COLOR_LINE));
        lines.add(HudLine.of("Members:", COLOR_SUBTITLE));

        String memberList = PartyInfo.members.toString().replace("[", "").replace("]", "");
        lines.addAll(wrapText(memberList));
    }

    private static void addDungeonSection(List<HudLine> lines, boolean preceded) {
        if (preceded) addDivider(lines);
        lines.add(HudLine.of("Dungeon HUD", COLOR_TITLE));
        lines.add(HudLine.of("Downtime requested: " + (DowntimeTracker.downtimeRequested ? "Yes" : "No"), COLOR_LINE));

        if (DowntimeTracker.downtimeRequested) lines.add(HudLine.of("Requested by: " + DowntimeTracker.requesterUsername, COLOR_LINE));

        lines.add(HudLine.of("Floor Auto-Rejoin: " + (AutoRejoin.autoRejoinEnabled ? "On" : "Off"), COLOR_LINE));
        if (AutoRejoin.autoRejoinEnabled) lines.add(HudLine.of("Current floor: " + AutoRejoin.currentFloor, COLOR_LINE));
    }

    private static List<HudLine> wrapText(String text){
        MinecraftClient client = MinecraftClient.getInstance();
        List<HudLine> wrappedLines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (client.textRenderer.getWidth(testLine) > 190 && !currentLine.isEmpty()) {
                wrappedLines.add(HudLine.of(currentLine.toString(), SsuHud.COLOR_LINE));
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        if (!currentLine.isEmpty()) {
            wrappedLines.add(HudLine.of(currentLine.toString(), SsuHud.COLOR_LINE));
        }

        return wrappedLines;
    }
}