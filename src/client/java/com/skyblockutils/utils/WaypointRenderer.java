package com.skyblockutils.utils;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public class WaypointRenderer {
    public enum WaypointStyle {
        TEXT, BADGE
    }

    public record Waypoint(BlockPos pos, String label, int color, WaypointStyle style, char badgeLetter) {
        public Waypoint(BlockPos pos, String label, int color, WaypointStyle style) {
            this(pos, label, color, style, label.charAt(0));
        }
    }

    private static final Identifier WAYPOINT_TEXTURE =
            Identifier.fromNamespaceAndPath("skyblockutils", "textures/waypoint.png");

    static final RenderPipeline BADGE_TEXTURE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation("pipeline/waypoint_badge")
                    .withVertexShader("core/rendertype_beacon_beam")
                    .withFragmentShader("core/rendertype_beacon_beam")
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                    .withVertexBinding(0, DefaultVertexFormat.BLOCK)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                    .withCull(false)
                    .build()
    );

    private static final RenderType BADGE_RENDER_TYPE = RenderType.create(
            "waypoint_badge_tex",
            RenderSetup.builder(BADGE_TEXTURE_PIPELINE)
                    .withTexture("Sampler0", WAYPOINT_TEXTURE)
                    .createRenderSetup()
    );

    public static void render(LevelRenderContext context, Waypoint waypoint, double maxDistance) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        Camera camera = context.gameRenderer().mainCamera();
        Vec3 cameraPos = camera.position();
        Font font = client.font;

        double effectiveMax = maxDistance < 0 ? Double.MAX_VALUE : maxDistance;
        Vec3 center = Vec3.atCenterOf(waypoint.pos());
        double distance = cameraPos.distanceTo(center);
        if (distance > effectiveMax) return;

        switch (waypoint.style()) {
            case TEXT -> renderText(waypoint, distance, cameraPos, camera, font, context.submitNodeCollector());
            case BADGE -> renderBadge(waypoint, distance, cameraPos, camera, font, context.submitNodeCollector());
        }
    }

    private static void renderText(Waypoint waypoint, double distance, Vec3 cameraPos, Camera camera, Font font, SubmitNodeCollector collector) {
        float scale = (float) Math.max(distance / 10f, 1f) * 0.025f;
        PoseStack matrices = buildMatrices(waypoint.pos(), cameraPos, camera, scale);

        int r = (waypoint.color() >> 16) & 0xFF;
        int g = (waypoint.color() >> 8) & 0xFF;
        int b = waypoint.color() & 0xFF;
        int labelColor = (0xFF << 24) | (r << 16) | (g << 8) | b;

        String label = waypoint.label();
        String distanceText = Math.round(distance) + "m";

        collector.order(0).submitText(matrices, -font.width(label) / 2f, 0,
                Language.getInstance().getVisualOrder(FormattedText.of(label)),
                false, Font.DisplayMode.SEE_THROUGH, 0xF000F0, labelColor, 0, 0);

        int lineHeight = font.lineHeight + 1;
        collector.order(0).submitText(matrices, -font.width(distanceText) / 2f, lineHeight,
                Language.getInstance().getVisualOrder(FormattedText.of(distanceText)),
                false, Font.DisplayMode.SEE_THROUGH, 0xF000F0, 0xFFFFFFFF, 0, 0);
    }

    private static void renderBadge(Waypoint waypoint, double distance, Vec3 cameraPos, Camera camera, Font font, SubmitNodeCollector collector) {
        float scale = (float) Math.max(distance / 10f, 1f) * 0.035f;
        PoseStack matrices = buildMatrices(waypoint.pos(), cameraPos, camera, scale);

        int color = waypoint.color();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int tintColor = (0xFF << 24) | (r << 16) | (g << 8) | b;

        int texW = 14, texH = 20;
        float halfW = texW / 2f;

        // Order 0: badge background submitted directly on collector
        collector.submitCustomGeometry(matrices, BADGE_RENDER_TYPE, (pose, buffer) -> {
            buffer.addVertex(pose, -halfW, -texH, 0).setColor(tintColor).setUv(0f, 0f).setLight(0xF000F0).setNormal(0, 0, 1);
            buffer.addVertex(pose,  halfW, -texH, 0).setColor(tintColor).setUv(1f, 0f).setLight(0xF000F0).setNormal(0, 0, 1);
            buffer.addVertex(pose,  halfW,     0, 0).setColor(tintColor).setUv(1f, 1f).setLight(0xF000F0).setNormal(0, 0, 1);
            buffer.addVertex(pose, -halfW,     0, 0).setColor(tintColor).setUv(0f, 1f).setLight(0xF000F0).setNormal(0, 0, 1);
        });

        // Order 1: letter and distance on top
        String letter = String.valueOf(waypoint.badgeLetter());
        collector.order(1).submitText(matrices, -font.width(letter) / 2f, -texH * 0.55f - font.lineHeight / 2f,
                Language.getInstance().getVisualOrder(FormattedText.of(letter)),
                false, Font.DisplayMode.SEE_THROUGH, 0xF000F0, 0xFFFFFFFF, 0, 0);

        String distanceText = Math.round(distance) + "m";
        collector.order(1).submitText(matrices, -font.width(distanceText) / 2f, 5,
                Language.getInstance().getVisualOrder(FormattedText.of(distanceText)),
                false, Font.DisplayMode.SEE_THROUGH, 0xF000F0, 0xFFFFFFFF, 0, 0);
    }

    private static PoseStack buildMatrices(BlockPos pos, Vec3 cameraPos, Camera camera, float scale) {
        Vec3 center = Vec3.atCenterOf(pos);
        PoseStack matrices = new PoseStack();
        matrices.translate(center.x - cameraPos.x, center.y - cameraPos.y + 0.5, center.z - cameraPos.z);
        matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot() + 180));
        matrices.mulPose(Axis.XP.rotationDegrees(-camera.xRot()));
        matrices.scale(scale, -scale, scale);
        return matrices;
    }
}