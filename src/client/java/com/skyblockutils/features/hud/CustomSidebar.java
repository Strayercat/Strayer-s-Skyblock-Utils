package com.skyblockutils.features.hud;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.hud.layout.HudContext;
import com.skyblockutils.features.hud.layout.HudElement;
import com.skyblockutils.features.hud.layout.HudLayoutEntry;
import com.skyblockutils.utils.SSU;
import com.skyblockutils.utils.SideBarUtils;
import com.skyblockutils.utils.ModStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

import static com.skyblockutils.utils.ModStyle.COLOR_BACKGROUND;

public class CustomSidebar {
    private static final Identifier CAT_IMAGE = Identifier.fromNamespaceAndPath("skyblockutils", "textures/hud/cat_sidebar.png");
    private static final String TITLE = "Skyblock";
    private static final int MIN_AUTO_WIDTH = 110;
    private static final int CORNER_RADIUS = 10;
    private static final int HEADER_HEIGHT = 15;
    private static final int SIDEBAR_HEIGHT = 120;
    private static final int PADDING = 5;
    private static final float TEXT_SCALE = 0.8f;
    private static final float CAT_SCALE = 0.13f;
    private static final int CAT_TEXTURE_SIZE = 418;

    public static final HudElement ELEMENT = new HudElement("custom_sidebar", "Custom Sidebar") {
        @Override
        public HudLayoutEntry defaultLayout() {
            return new HudLayoutEntry(HudLayoutEntry.Anchor.END, HudLayoutEntry.Anchor.CENTER, 0, 0, 1f);
        }

        @Override
        public int maxWidth() {
            return 300;
        }

        @Override
        public int autoWidth(boolean preview) {
            Minecraft mc = Minecraft.getInstance();
            int max = 0;
            for (Component line : lines(preview)) max = Math.max(max, Math.round(mc.font.width(line) * TEXT_SCALE));
            return Math.max(MIN_AUTO_WIDTH, max + PADDING * 2);
        }

        @Override
        public int height(int width, boolean preview) {
            return SIDEBAR_HEIGHT;
        }

        @Override
        public boolean isEnabled() {
            return ModConfig.INSTANCE.customSidebar && !ModConfig.INSTANCE.sideBarInHud;
        }

        @Override
        public boolean isActive() {
            return !StrayersSkyblockUtilsClient.isInSkyblock;
        }

        @Override
        public void render(HudContext ctx) {
            renderSidebar(ctx);
        }
    };

    private static List<Component> lines(boolean preview) {
        List<Component> lines = new ArrayList<>(SideBarUtils.getSidebarLines().stream()
                .filter(line -> !line.replaceAll("(?i)§.", "").trim().isEmpty()
                        && !line.replaceAll("§.", "").trim().matches("\\d{2}/\\d{2}/\\d{2} .*")
                        && !line.replaceAll("§.", "").trim().matches("www.hypixel.net"))
                .map(line -> (Component) Component.literal(line))
                .toList());

        if (lines.isEmpty() && preview) {
            lines.add(Component.literal("§fLate Winter 19th"));
            lines.add(Component.literal("§78:10am §b☽"));
            lines.add(Component.literal("§7⏣ §5Void Sepulture"));
            lines.add(Component.literal("§fPurse: §614,767"));
            lines.add(Component.literal("§fBits: §b10"));
        }

        if (ModConfig.INSTANCE.sidebarCoords && Minecraft.getInstance().player != null) {
            lines.add(ModFunctions.getFormattedCoordinates());
        }

        return lines;
    }

    private static List<FormattedCharSequence> rows(List<Component> lines, int width, float textScale) {
        Minecraft mc = Minecraft.getInstance();
        int wrap = Math.max(1, Math.round((width - PADDING * 2) / textScale));
        List<FormattedCharSequence> rows = new ArrayList<>();
        for (Component line : lines) rows.addAll(mc.font.split(line, wrap));
        return rows;
    }

    private static void renderSidebar(HudContext ctx) {
        ModStyle.ColorStyle style = ModConfig.INSTANCE.colorStyle;
        int accent = ModStyle.getColor(style, ModStyle.ColorType.MAIN);
        int titleStart = ModStyle.getColor(style, ModStyle.ColorType.TITLE_START);
        int titleEnd = ModStyle.getColor(style, ModStyle.ColorType.TITLE_END);

        ctx.panel(0, 0, ctx.width, ctx.height, CORNER_RADIUS, COLOR_BACKGROUND, accent);

        ctx.text(SSU.gradientText(TITLE, titleStart, titleEnd), ctx.width / 2 - ctx.font.width(TITLE) / 2,
                HEADER_HEIGHT / 2 - ctx.font.lineHeight / 2 + 1, 0xFFFFFFFF, true);

        int[] inner = ctx.inner(0, 0, ctx.width, ctx.height);
        ctx.fill(inner[0], HEADER_HEIGHT, inner[2], HEADER_HEIGHT + 1, accent);

        int bodyTop = HEADER_HEIGHT + 1;
        int bodyHeight = ctx.height - bodyTop - PADDING * 2;
        List<Component> lines = lines(ctx.preview);
        if (!lines.isEmpty() && bodyHeight > 0) {
            float textScale = TEXT_SCALE;
            List<FormattedCharSequence> rows = rows(lines, ctx.width, textScale);
            int minStep = ctx.font.lineHeight + 1;
            if (rows.size() * minStep * textScale > bodyHeight) {
                textScale = Math.max(0.3f, bodyHeight / (float) (rows.size() * minStep));
                rows = rows(lines, ctx.width, textScale);
            }

            float space = bodyHeight / (float) rows.size();
            float offset = space / 2f - ctx.font.lineHeight * textScale / 2f;
            for (int i = 0; i < rows.size(); i++) {
                ctx.scaledText(rows.get(i), PADDING, bodyTop + PADDING + offset + i * space, textScale, 0xFFFFFFFF, true);
            }
        }

        if (!ModConfig.INSTANCE.cat) return;

        float catSize = CAT_TEXTURE_SIZE * CAT_SCALE;
        ctx.overlay(CAT_IMAGE, ctx.width / 2f - catSize / 2f - 10, -catSize + 31 * CAT_SCALE, catSize, CAT_TEXTURE_SIZE);
    }
}
