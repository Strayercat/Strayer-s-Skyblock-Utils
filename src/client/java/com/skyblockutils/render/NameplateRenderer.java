package com.skyblockutils.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.skyblockutils.mixin.client.RenderTypeFeatureRendererInvoker;
import com.skyblockutils.utils.GemColor;
import com.skyblockutils.utils.SSUIndicator;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public final class NameplateRenderer {
    private static final String NAME_MARKER = "ssu:nameplate:";
    private static final String SCORE_MARKER = "ssu:nameplate_score:";

    public static final int NAME_SHIFT_WITH_SCORE = -13;
    public static final int SCORE_OFFSET = 19;
    public static final float SCALE = 0.8F;
    public static final float NORMAL_LIFT = 0.15F;
    public static final float TOWARD_CAMERA = 0.45F;

    private static final RenderType FRAME = type("nameplate");
    private static final RenderType FRAME_TRIM = type("nameplate_trim");
    private static final RenderType GEM = type("nameplate_gem");
    private static final RenderType SUB = type("nameplate_sub");
    private static final RenderType SUB_TRIM = type("nameplate_sub_trim");

    private static final float PX = 4.0F;
    private static final float PRISMATIC_HUE_PER_UNIT = 0.008F;
    private static final float PRISMATIC_STEP = 4.0F;

    private static final float Z_SUB = -0.9F;
    private static final float Z_SUB_TRIM = -0.75F;
    private static final float Z_FRAME = -0.6F;
    private static final float Z_OVERLAY = -0.3F;

    private static final float FRAME_W = 188.0F;
    private static final float FRAME_H = 114.0F;
    private static final float FRAME_INNER_W = 131.0F;
    private static final float FRAME_INNER_CENTER_Y = 58.5F;
    private static final float FRAME_PADDING = 3.0F;
    private static final float FRAME_LEFT_CAP = 44.0F;
    private static final float FRAME_CENTER_U0 = 45.0F;
    private static final float FRAME_CENTER_U1 = 144.0F;
    private static final float FRAME_RIGHT_U0 = 145.0F;

    private static final float GEM_X = 89.0F;
    private static final float GEM_Y = 92.0F;
    private static final float GEM_SIZE = 12.0F;

    private static final float SUB_W = 25.0F;
    private static final float SUB_H = 66.0F;
    private static final float SUB_CAP = 12.0F;
    private static final float SUB_BORDER = 9.0F;
    private static final float SUB_TEXT_TOP = 17.0F;
    private static final float SUB_PADDING = 2.0F;

    private static final float TEXT_CENTER_Y = 4.0F;

    private NameplateRenderer() {
    }

    private static RenderType type(String texture) {
        return RenderTypes.text(Identifier.fromNamespaceAndPath("skyblockutils", "textures/entity/" + texture + ".png"));
    }

    public static Component mark(Component name, String playerName) {
        return wrap(name, NAME_MARKER + playerName);
    }

    public static Component markScore(Component score, String playerName) {
        return wrap(score, SCORE_MARKER + playerName);
    }

    private static Component wrap(Component text, String insertion) {
        MutableComponent out = Component.empty().withStyle(style -> style.withInsertion(insertion));
        return out.append(text);
    }

    public static boolean isMarked(Component text) {
        String insertion = text.getStyle().getInsertion();
        return insertion != null && (insertion.startsWith(NAME_MARKER) || insertion.startsWith(SCORE_MARKER));
    }

    public static String markedName(Component text) {
        return markerValue(text, NAME_MARKER);
    }

    public static boolean isScore(Component text) {
        return markerValue(text, SCORE_MARKER) != null;
    }

    private static String markerValue(Component text, String marker) {
        String insertion = text.getStyle().getInsertion();
        return insertion != null && insertion.startsWith(marker) ? insertion.substring(marker.length()) : null;
    }

    public static void submit(NameTagFeatureRenderer renderer, NameTagFeatureRenderer.Submit submit) {
        if (submit.displayMode() != Font.DisplayMode.NORMAL) return;
        RenderTypeFeatureRendererInvoker invoker = (RenderTypeFeatureRendererInvoker) renderer;

        String name = markerValue(submit.text(), NAME_MARKER);
        if (name != null) {
            submitFrame(invoker, submit, Tint.of(name));
            return;
        }
        String scoreName = markerValue(submit.text(), SCORE_MARKER);
        if (scoreName != null) submitSub(invoker, submit, Tint.of(scoreName));
    }

    private static void submitFrame(RenderTypeFeatureRendererInvoker invoker, NameTagFeatureRenderer.Submit submit, Tint tint) {
        Matrix4fc pose = submit.pose();
        int light = submit.lightCoords();

        float textWidth = -2.0F * submit.x();
        float extra = Math.max(0.0F, (textWidth + 2.0F * FRAME_PADDING) * PX - FRAME_INNER_W);
        float stretch = 1.0F + extra / 2.0F;
        float[][] segments = {
                {0.0F, FRAME_LEFT_CAP, FRAME_LEFT_CAP},
                {FRAME_LEFT_CAP, FRAME_CENTER_U0, stretch},
                {FRAME_CENTER_U0, FRAME_CENTER_U1, FRAME_CENTER_U1 - FRAME_CENTER_U0},
                {FRAME_CENTER_U1, FRAME_RIGHT_U0, stretch},
                {FRAME_RIGHT_U0, FRAME_W, FRAME_W - FRAME_RIGHT_U0}
        };

        float left = -width(segments) / 2.0F / PX;
        float top = submit.y() + TEXT_CENTER_Y - FRAME_INNER_CENTER_Y / PX;

        drawStretched(invoker.ssu$getVertexBuilder(FRAME), pose, light, Tint.WHITE, segments, FRAME_W, FRAME_H, left, top, Z_FRAME);
        drawStretched(invoker.ssu$getVertexBuilder(FRAME_TRIM), pose, light, tint, segments, FRAME_W, FRAME_H, left, top, Z_OVERLAY);

        float gemX = left + (FRAME_LEFT_CAP + stretch + GEM_X - FRAME_CENTER_U0) / PX;
        float gemY = top + GEM_Y / PX;
        float gemSize = GEM_SIZE / PX;
        quad(invoker.ssu$getVertexBuilder(GEM), pose, light, tint,
                gemX, gemY, gemX + gemSize, gemY + gemSize, Z_OVERLAY, 0.0F, 1.0F);
    }

    private static void submitSub(RenderTypeFeatureRendererInvoker invoker, NameTagFeatureRenderer.Submit submit, Tint tint) {
        Matrix4fc pose = submit.pose();
        int light = submit.lightCoords();

        float textWidth = -2.0F * submit.x();
        float total = Math.max(SUB_W, (textWidth + 2.0F * SUB_PADDING) * PX + 2.0F * SUB_BORDER);
        float[][] segments = {
                {0.0F, SUB_CAP, SUB_CAP},
                {SUB_CAP, SUB_CAP + 1.0F, total - 2.0F * SUB_CAP},
                {SUB_CAP + 1.0F, SUB_W, SUB_W - SUB_CAP - 1.0F}
        };

        float left = -total / 2.0F / PX;
        float top = submit.y() - SUB_TEXT_TOP / PX;

        drawStretched(invoker.ssu$getVertexBuilder(SUB), pose, light, Tint.WHITE, segments, SUB_W, SUB_H, left, top, Z_SUB);
        drawStretched(invoker.ssu$getVertexBuilder(SUB_TRIM), pose, light, tint, segments, SUB_W, SUB_H, left, top, Z_SUB_TRIM);
    }

    private static float width(float[][] segments) {
        float total = 0.0F;
        for (float[] segment : segments) total += segment[2];
        return total;
    }

    private static void drawStretched(VertexConsumer buffer, Matrix4fc pose, int light, Tint tint, float[][] segments,
                                      float texW, float texH, float left, float top, float z) {
        float bottom = top + texH / PX;
        float px = 0.0F;
        for (float[] segment : segments) {
            float x0 = left + px / PX;
            float x1 = left + (px + segment[2]) / PX;
            quad(buffer, pose, light, tint, x0, top, x1, bottom, z, segment[0] / texW, segment[1] / texW);
            px += segment[2];
        }
    }

    private static void quad(VertexConsumer buffer, Matrix4fc pose, int light, Tint tint,
                             float x0, float y0, float x1, float y1, float z,
                             float u0, float u1) {
        if (!tint.prismatic() || x1 - x0 <= PRISMATIC_STEP) {
            rawQuad(buffer, pose, light, tint, x0, y0, x1, y1, z, u0, u1);
            return;
        }
        int steps = (int) Math.ceil((x1 - x0) / PRISMATIC_STEP);
        for (int i = 0; i < steps; i++) {
            float a = (float) i / steps;
            float b = (float) (i + 1) / steps;
            rawQuad(buffer, pose, light, tint,
                    x0 + (x1 - x0) * a, y0, x0 + (x1 - x0) * b, y1, z,
                    u0 + (u1 - u0) * a, u0 + (u1 - u0) * b);
        }
    }

    private static void rawQuad(VertexConsumer buffer, Matrix4fc pose, int light, Tint tint,
                                float x0, float y0, float x1, float y1, float z,
                                float u0, float u1) {
        int c0 = tint.at(x0);
        int c1 = tint.at(x1);
        vertex(buffer, pose, light, c0, x0, y0, z, u0, (float) 0.0);
        vertex(buffer, pose, light, c0, x0, y1, z, u0, (float) 1.0);
        vertex(buffer, pose, light, c1, x1, y1, z, u1, (float) 1.0);
        vertex(buffer, pose, light, c1, x1, y0, z, u1, (float) 0.0);
    }

    private static void vertex(VertexConsumer buffer, Matrix4fc pose, int light, int color, float x, float y, float z, float u, float v) {
        Vector3f p = pose.transformPosition(x, y, z, new Vector3f());
        buffer.addVertex(p.x, p.y, p.z).setColor(color).setUv(u, v).setLight(light);
    }

    private record Tint(int rgb, boolean prismatic) {
        static final Tint WHITE = new Tint(0xFFFFFF, false);

        static Tint of(String playerName) {
            GemColor color = SSUIndicator.colorOf(playerName);
            return color == GemColor.SPECIAL ? new Tint(0, true) : new Tint(color.tint, false);
        }

        int at(float x) {
            return 0xFF000000 | (prismatic ? SSUIndicator.prismaticColor(x * PRISMATIC_HUE_PER_UNIT) : rgb);
        }
    }
}