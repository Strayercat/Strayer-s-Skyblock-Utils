package com.skyblockutils.features.voice;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.ModStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    public static void render(GuiGraphicsExtractor context) {
        if (!ModConfig.INSTANCE.voiceChatEnabled || !ModConfig.INSTANCE.voiceHud || !VoiceChat.isRunning()) return;

        List<VoicePeer> peers = VoiceChat.hudPeers();
        if (peers.isEmpty()) return;

        long now = System.currentTimeMillis();
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(icon(VoiceChat.deafened, VoiceChat.muted, VoiceChat.isTransmitting()), "You", COLOR_NAME));
        for (VoicePeer peer : peers) {
            rows.add(switch (peer.state) {
                case CONNECTED -> new Row(icon(peer.remoteDeafened, peer.remoteMuted, peer.isSpeaking(now)), peer.name, COLOR_NAME);
                case FAILED -> new Row(null, peer.name, COLOR_FAILED);
                default -> new Row(null, peer.name, COLOR_PENDING);
            });
        }

        Minecraft mc = Minecraft.getInstance();
        int lineStep = Math.max(ICON_SIZE, Math.round(mc.font.lineHeight * TEXT_SCALE)) + 2;
        int nameX = PADDING + ICON_SIZE + ICON_GAP;

        int width = PADDING * 2 + scaledWidth(mc, TITLE);
        for (Row row : rows) width = Math.max(width, nameX + scaledWidth(mc, row.name()) + PADDING);
        width = Math.max(MIN_WIDTH, width);

        int titleStep = Math.round(mc.font.lineHeight * TEXT_SCALE) + 2;
        int height = PADDING + titleStep + DIVIDER_MARGIN * 2 + DIVIDER_HEIGHT + rows.size() * lineStep + PADDING - 2;

        boolean rounded = ModConfig.INSTANCE.notificationStyle == ModStyle.NotificationStyle.ROUNDED;
        int radius = rounded ? CORNER_RADIUS : 0;
        int accent = 0xFF000000 | ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
        int titleColor = 0xFF000000 | ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TITLE_END);

        fillBottomRightRounded(context, 0, 0, width, height, accent, radius);
        fillBottomRightRounded(context, 0, 0, width - 1, height - 1, BACKGROUND_COLOR, Math.max(0, radius - 1));

        int lineY = PADDING;
        drawScaledText(context, mc, TITLE, (width - scaledWidth(mc, TITLE)) / 2, lineY, titleColor);
        lineY += titleStep + DIVIDER_MARGIN;

        context.fill(0, lineY, width - 1, lineY + DIVIDER_HEIGHT, accent);
        lineY += DIVIDER_HEIGHT + DIVIDER_MARGIN;

        int textOffset = (lineStep - 2 - Math.round(mc.font.lineHeight * TEXT_SCALE)) / 2 + 1;
        for (Row row : rows) {
            if (row.icon() != null) {
                context.blitSprite(RenderPipelines.GUI_TEXTURED, row.icon(), PADDING, lineY, ICON_SIZE, ICON_SIZE);
            }
            drawScaledText(context, mc, row.name(), nameX, lineY + textOffset, row.color());
            lineY += lineStep;
        }
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

    private static void drawScaledText(GuiGraphicsExtractor context, Minecraft mc, String text, int x, int y, int color) {
        context.pose().pushMatrix();
        context.pose().translate(x, y);
        context.pose().scale(TEXT_SCALE, TEXT_SCALE);
        context.text(mc.font, text, 0, 0, color, false);
        context.pose().popMatrix();
    }

    private static void fillBottomRightRounded(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, int color, int radius) {
        radius = Math.min(radius, Math.min(x2 - x1, y2 - y1) / 2);
        if (radius <= 0) {
            context.fill(x1, y1, x2, y2, color);
            return;
        }

        context.fill(x1, y1, x2, y2 - radius, color);
        for (int i = 0; i < radius; i++) {
            double dy = radius - i - 0.5;
            int inset = (int) Math.round(radius - Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            context.fill(x1, y2 - i - 1, x2 - inset, y2 - i, color);
        }
    }
}
