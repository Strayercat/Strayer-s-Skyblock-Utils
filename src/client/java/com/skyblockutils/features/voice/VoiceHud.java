package com.skyblockutils.features.voice;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.hud.layout.HudContext;
import com.skyblockutils.features.hud.layout.HudElement;
import com.skyblockutils.features.hud.layout.HudLayoutEntry;
import com.skyblockutils.utils.ModStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class VoiceHud {
    private static final Identifier ICON_SPEAKING = Identifier.fromNamespaceAndPath("skyblockutils", "voice_speaking");
    private static final Identifier ICON_MUTED = Identifier.fromNamespaceAndPath("skyblockutils", "voice_muted");
    private static final Identifier ICON_DEAFENED = Identifier.fromNamespaceAndPath("skyblockutils", "voice_deafened");

    private static final String TITLE = "Voice";
    private static final int MIN_WIDTH = 80;
    private static final int PADDING = 5;
    private static final int ICON_SIZE = 8;
    private static final int ICON_GAP = 3;
    private static final int CORNER_RADIUS = 4;
    private static final int DIVIDER_HEIGHT = 1;
    private static final int DIVIDER_MARGIN = 2;
    private static final float TEXT_SCALE = 0.8f;

    private static final int BACKGROUND_COLOR = 0xFF1A1A1A;
    private static final int COLOR_NAME = 0xFFFFFFFF;
    private static final int COLOR_PENDING = 0xFFAAAAAA;
    private static final int COLOR_FAILED = 0xFFFF5555;

    private record Row(Identifier icon, String name, int color) {}

    public static final HudElement ELEMENT = new HudElement("voice_chat", "Voice Chat") {
        @Override
        public HudLayoutEntry defaultLayout() {
            return new HudLayoutEntry(HudLayoutEntry.Anchor.START, HudLayoutEntry.Anchor.START, 0, 0, 1f);
        }

        @Override
        public int minWidth() {
            return 50;
        }

        @Override
        public int maxWidth() {
            return 250;
        }

        @Override
        public int autoWidth(boolean preview) {
            Minecraft mc = Minecraft.getInstance();
            int width = PADDING * 2 + scaledWidth(mc, TITLE);
            for (Row row : rows(preview)) width = Math.max(width, nameX() + scaledWidth(mc, row.name()) + PADDING);
            return Math.max(MIN_WIDTH, width);
        }

        @Override
        public int height(int width, boolean preview) {
            Minecraft mc = Minecraft.getInstance();
            return PADDING + titleStep(mc) + DIVIDER_MARGIN * 2 + DIVIDER_HEIGHT + rows(preview).size() * lineStep(mc) + PADDING - 2;
        }

        @Override
        public boolean isEnabled() {
            return ModConfig.INSTANCE.voiceChatEnabled && ModConfig.INSTANCE.voiceHud;
        }

        @Override
        public boolean isActive() {
            return !VoiceChat.isRunning() || VoiceChat.hudPeers().isEmpty();
        }

        @Override
        public void render(HudContext ctx) {
            renderHud(ctx);
        }
    };

    private static List<Row> rows(boolean preview) {
        List<Row> rows = new ArrayList<>();
        if (preview && ELEMENT.isActive()) {
            rows.add(new Row(ICON_SPEAKING, "You", COLOR_NAME));
            rows.add(new Row(null, "Teammate", COLOR_NAME));
            rows.add(new Row(ICON_MUTED, "Friend", COLOR_NAME));
            rows.add(new Row(null, "Connecting", COLOR_PENDING));
            return rows;
        }

        long now = System.currentTimeMillis();
        rows.add(new Row(icon(VoiceChat.deafened, VoiceChat.muted, VoiceChat.isTransmitting()), "You", COLOR_NAME));
        for (VoicePeer peer : VoiceChat.hudPeers()) {
            rows.add(switch (peer.state) {
                case CONNECTED -> new Row(icon(peer.remoteDeafened, peer.remoteMuted, peer.isSpeaking(now)), peer.name, COLOR_NAME);
                case FAILED -> new Row(null, peer.name, COLOR_FAILED);
                default -> new Row(null, peer.name, COLOR_PENDING);
            });
        }
        return rows;
    }

    private static int nameX() {
        return PADDING + ICON_SIZE + ICON_GAP;
    }

    private static int lineStep(Minecraft mc) {
        return Math.max(ICON_SIZE, Math.round(mc.font.lineHeight * TEXT_SCALE)) + 2;
    }

    private static int titleStep(Minecraft mc) {
        return Math.round(mc.font.lineHeight * TEXT_SCALE) + 2;
    }

    private static void renderHud(HudContext ctx) {
        Minecraft mc = ctx.mc;
        int accent = 0xFF000000 | ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
        int titleColor = 0xFF000000 | ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TITLE_END);
        int lineStep = lineStep(mc);

        ctx.panel(0, 0, ctx.width, ctx.height, CORNER_RADIUS, BACKGROUND_COLOR, accent);
        int[] inner = ctx.inner(0, 0, ctx.width, ctx.height);

        int lineY = PADDING;
        String title = fit(mc, TITLE, ctx.width - PADDING * 2);
        ctx.scaledText(title, (ctx.width - scaledWidth(mc, title)) / 2f, lineY, TEXT_SCALE, titleColor, false);
        lineY += titleStep(mc) + DIVIDER_MARGIN;

        ctx.fill(inner[0], lineY, inner[2], lineY + DIVIDER_HEIGHT, accent);
        lineY += DIVIDER_HEIGHT + DIVIDER_MARGIN;

        int textOffset = (lineStep - 2 - Math.round(mc.font.lineHeight * TEXT_SCALE)) / 2 + 1;
        int nameWidth = ctx.width - nameX() - PADDING;
        for (Row row : rows(ctx.preview)) {
            if (row.icon() != null) {
                ctx.graphics.blitSprite(RenderPipelines.GUI_TEXTURED, row.icon(), PADDING, lineY, ICON_SIZE, ICON_SIZE);
            }
            ctx.scaledText(fit(mc, row.name(), nameWidth), nameX(), lineY + textOffset, TEXT_SCALE, row.color(), false);
            lineY += lineStep;
        }
    }

    private static String fit(Minecraft mc, String text, int maxScaledWidth) {
        if (scaledWidth(mc, text) <= maxScaledWidth) return text;
        String ellipsis = "…";
        for (int end = text.length() - 1; end > 0; end--) {
            String candidate = text.substring(0, end) + ellipsis;
            if (scaledWidth(mc, candidate) <= maxScaledWidth) return candidate;
        }
        return ellipsis;
    }

    private static Identifier icon(boolean deafened, boolean muted, boolean speaking) {
        if (deafened) return ICON_DEAFENED;
        if (muted) return ICON_MUTED;
        if (speaking) return ICON_SPEAKING;
        return null;
    }

    private static int scaledWidth(Minecraft mc, String text) {
        return Math.round(mc.font.width(text) * TEXT_SCALE);
    }
}
