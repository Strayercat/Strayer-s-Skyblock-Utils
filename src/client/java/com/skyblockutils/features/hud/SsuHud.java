package com.skyblockutils.features.hud;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.DailyReminders;
import com.skyblockutils.features.dungeons.AutoRejoin;
import com.skyblockutils.features.dungeons.DowntimeTracker;
import com.skyblockutils.features.hud.layout.HudContext;
import com.skyblockutils.features.hud.layout.HudElement;
import com.skyblockutils.features.hud.layout.HudLayoutEntry;
import com.skyblockutils.features.party.PartyInfo;
import com.skyblockutils.utils.FunFacts;
import com.skyblockutils.utils.SSU;
import com.skyblockutils.utils.SideBarUtils;
import com.skyblockutils.utils.ModStyle;
import com.skyblockutils.utils.SSUIndicator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.skyblockutils.utils.ModStyle.*;

public class SsuHud {
    private static final Identifier CAT_IMAGE = Identifier.fromNamespaceAndPath("skyblockutils", "textures/hud/cat.png");

    private static boolean isVisible = false;
    public static boolean showBossBar = true;
    public static String funFact = null;
    public static boolean funFactHandled = false;

    private static final int BASE_HUD_WIDTH = 200;
    private static final int BASE_LINE_HEIGHT = 12;
    private static final int BASE_DIVIDER_HEIGHT = 6;
    private static final int BASE_PADDING = 10;
    private static final int TEXT_INSET = 5;
    private static final int CORNER_RADIUS = 5;
    private static final int GEM_SIZE = 8;
    private static final int GEM_VISIBLE_WIDTH = 7;
    private static final float CAT_SCALE = 1.0f / 6.0f;
    private static final int CAT_TEXTURE_SIZE = 418;

    private static final int COLOR_TEXT = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TEXT);

    public static final HudElement ELEMENT = new HudElement("ssu_hud", "SSU HUD") {
        @Override
        public HudLayoutEntry defaultLayout() {
            return new HudLayoutEntry(HudLayoutEntry.Anchor.CENTER, HudLayoutEntry.Anchor.START, 0, 0, ModConfig.INSTANCE.hudScale / 100f);
        }

        @Override
        public int minWidth() {
            return 100;
        }

        @Override
        public int maxWidth() {
            return 400;
        }

        @Override
        public int autoWidth(boolean preview) {
            return BASE_HUD_WIDTH;
        }

        @Override
        public int height(int width, boolean preview) {
            return measure(rows(width, preview));
        }

        @Override
        public boolean isEnabled() {
            return ModConfig.INSTANCE.hudEnabled;
        }

        @Override
        public boolean isActive() {
            Minecraft client = Minecraft.getInstance();
            return !isVisible || client.player == null || getHudLines(client, SideBarUtils.location).size() <= 2;
        }

        @Override
        public void render(HudContext ctx) {
            renderHud(ctx);
        }
    };

    public static void setVisible(boolean visible) {
        if (!ModConfig.INSTANCE.hudEnabled) return;
        isVisible = visible;
        showBossBar = !visible;
    }

    private record HudLine(Component text, boolean isDivider) {
        static HudLine of(String text, int color) {
            return new HudLine(Component.literal(text).withColor(color), false);
        }

        static HudLine rich(Component text) {
            return new HudLine(text, false);
        }

        static HudLine divider() {
            return new HudLine(Component.empty(), true);
        }
    }

    private record Row(FormattedCharSequence text, boolean divider) {
    }

    private static List<Row> rows(int width, boolean preview) {
        Minecraft client = Minecraft.getInstance();
        List<HudLine> lines = client.player == null && preview ? sampleLines() : getHudLines(client, SideBarUtils.location);

        List<Row> rows = new ArrayList<>();
        int wrapWidth = width - TEXT_INSET * 2;
        boolean first = true;
        for (HudLine line : lines) {
            if (line.isDivider()) {
                rows.add(new Row(FormattedCharSequence.EMPTY, true));
                continue;
            }
            int lineWidth = first ? wrapWidth - GEM_VISIBLE_WIDTH - 2 : wrapWidth;
            first = false;
            List<FormattedCharSequence> wrapped = client.font.split(line.text(), Math.max(1, lineWidth));
            if (wrapped.isEmpty()) rows.add(new Row(FormattedCharSequence.EMPTY, false));
            for (FormattedCharSequence seq : wrapped) rows.add(new Row(seq, false));
        }
        return rows;
    }

    private static int measure(List<Row> rows) {
        int height = BASE_PADDING;
        for (Row row : rows) height += row.divider() ? BASE_DIVIDER_HEIGHT : BASE_LINE_HEIGHT;
        return height;
    }

    private static void renderHud(HudContext ctx) {
        List<Row> rows = rows(ctx.width, ctx.preview);
        int accent = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);

        ctx.panel(0, 0, ctx.width, ctx.height, CORNER_RADIUS, COLOR_BACKGROUND, accent);
        ctx.graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SSUIndicator.BADGE_BIG,
                ctx.width - TEXT_INSET - GEM_VISIBLE_WIDTH, BASE_PADDING / 2, GEM_SIZE, GEM_SIZE);

        int y = BASE_PADDING / 2;
        for (Row row : rows) {
            if (row.divider()) {
                ctx.fill(4, y + 1, ctx.width - 4, y + 2, accent);
                y += BASE_DIVIDER_HEIGHT;
            } else {
                ctx.text(row.text(), TEXT_INSET, y, 0xFFFFFFFF, true);
                y += BASE_LINE_HEIGHT;
            }
        }

        if (ModConfig.INSTANCE.customSidebar || !ModConfig.INSTANCE.cat) return;

        float catSize = CAT_TEXTURE_SIZE * CAT_SCALE;
        ctx.overlay(CAT_IMAGE, ctx.width - 31 * CAT_SCALE, ctx.height / 2f - 175 * CAT_SCALE, catSize, CAT_TEXTURE_SIZE);
    }

    private static List<HudLine> sampleLines() {
        List<HudLine> lines = new ArrayList<>();
        lines.add(HudLine.rich(SSU.getHudTitle()));
        lines.add(HudLine.divider());
        lines.add(HudLine.of("Current time: 12:00", COLOR_TEXT));
        lines.add(HudLine.of("Ping: 42", COLOR_TEXT));
        lines.add(HudLine.of("Server tps: 20.0", COLOR_TEXT));
        lines.add(HudLine.of("Fps: 144", COLOR_TEXT));
        lines.add(HudLine.divider());
        lines.add(HudLine.of("Party Info", COLOR_TITLE));
        lines.add(HudLine.of("Not currently in a party", COLOR_TEXT));
        return lines;
    }

    private static @NotNull List<HudLine> getHudLines(Minecraft client, String location) {
        List<HudLine> lines = new ArrayList<>();
        lines.add(HudLine.rich(SSU.getHudTitle()));
        lines.add(HudLine.divider());

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
            lines.add(HudLine.of("Ping: " + ModFunctions.ping, COLOR_TEXT));
            anyGeneralInfo = true;
        }

        if (ModConfig.INSTANCE.hudTps) {
            lines.add(HudLine.of("Server tps: " + String.format("%.1f", ModFunctions.tps), COLOR_TEXT));
            anyGeneralInfo = true;
        }

        if (ModConfig.INSTANCE.hudFps) {
            lines.add(HudLine.of("Fps: " + client.getFps(), COLOR_TEXT));
            anyGeneralInfo = true;
        }

        if (ModConfig.INSTANCE.hudCoords) {
            lines.add(HudLine.rich(ModFunctions.getFormattedCoordinates()));
        }

        if (ModConfig.INSTANCE.sideBarInHud) {
            addSidebarSection(lines, anyGeneralInfo);
            anyGeneralInfo = true;
        }

        if (ModConfig.INSTANCE.hudDailies) {
            if (!ModFunctions.isInDungeons(client)) {
                if (anyGeneralInfo) addDivider(lines);
                lines.add(HudLine.of("Daily Tasks: ", COLOR_TITLE));
                for (DailyReminders.ReminderType type : DailyReminders.ReminderType.values()) {
                    lines.add(HudLine.of(type + ": " + (ModConfig.INSTANCE.completedTypes.contains(type) ? "§aCompleted" : "§cIncomplete"), COLOR_TEXT));
                }
            }
        }

        if (ModConfig.INSTANCE.hudPartyInfo) {
            boolean showParty = !ModFunctions.isInDungeons(client)
                    || ModConfig.INSTANCE.hudPartyInfoInDungeons;
            if (showParty) {
                addPartySection(lines, anyGeneralInfo);
                anyGeneralInfo = true;
            }
        }

        if (location.equals("Dungeon Hub") || ModFunctions.isInDungeons(client)) {
            addDungeonSection(lines, anyGeneralInfo);
            return lines;
        }

        if (location.equals("Your Island") && ModConfig.INSTANCE.hudIslandFunFact) {
            if (!funFactHandled) funFact = FunFacts.funFacts.get((int) (Math.random() * FunFacts.funFacts.size()));
            if (anyGeneralInfo) addDivider(lines);
            lines.add(HudLine.of("Fun fact: " + funFact, COLOR_TEXT));
            funFactHandled = true;
        }

        return lines;
    }

    private static void addDivider(List<HudLine> lines) {
        lines.add(HudLine.divider());
    }

    private static void addSidebarSection(List<HudLine> lines, boolean preceded) {
        if (preceded) addDivider(lines);

        List<String> scoreboardLines = SideBarUtils.getSidebarLines().stream()
                .filter(line -> !line.replaceAll("(?i)§.", "").trim().isEmpty()
                        && !line.replaceAll("§.", "").trim().matches("\\d{2}/\\d{2}/\\d{2} .*")
                        && !line.replaceAll("§.", "").trim().matches("www.hypixel.net"))
                .map(String::trim)
                .toList();

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
        lines.add(HudLine.of(memberList, COLOR_TEXT));
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
}
