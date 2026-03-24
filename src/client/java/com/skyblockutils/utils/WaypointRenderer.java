package com.skyblockutils.utils;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class WaypointRenderer {
    public enum WaypointStyle {
        TEXT, BADGE
    }

    public record Waypoint(BlockPos pos, String label, int color, WaypointStyle style, char badgeLetter) {
        public Waypoint(BlockPos pos, String label, int color, WaypointStyle style) {
            this(pos, label, color, style, label.charAt(0));
        }
    }

    public static void render(WorldRenderContext context, List<Waypoint> waypoints, double maxDistance) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        Camera camera = context.gameRenderer().getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        TextRenderer font = client.textRenderer;

        double effectiveMax = maxDistance < 0 ? Double.MAX_VALUE : maxDistance;

        for (Waypoint waypoint : waypoints) {
            Vec3d center = Vec3d.ofCenter(waypoint.pos());
            double distance = cameraPos.distanceTo(center);
            if (distance > effectiveMax) continue;

            switch (waypoint.style()) {
                case TEXT -> renderText(waypoint, distance, cameraPos, camera, font, immediate);
                case BADGE -> renderBadge(waypoint, distance, cameraPos, camera, font, immediate);
            }
        }
    }

    private static void renderText(Waypoint waypoint, double distance, Vec3d cameraPos, Camera camera, TextRenderer font, VertexConsumerProvider.Immediate immediate) {
        float scale = (float) Math.max(distance / 10f, 1f) * 0.025f;

        MatrixStack matrices = buildMatrices(waypoint.pos(), cameraPos, camera, scale);

        int lineHeight = font.fontHeight + 1;
        int r = (waypoint.color() >> 16) & 0xFF;
        int g = (waypoint.color() >> 8) & 0xFF;
        int b = waypoint.color() & 0xFF;
        int labelColor = (0xFF << 24) | (r << 16) | (g << 8) | b;

        String label = waypoint.label();
        String distanceText = Math.round(distance) + "m";

        font.draw(label, -font.getWidth(label) / 2f, 0, labelColor, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
        font.draw(distanceText, -font.getWidth(distanceText) / 2f, lineHeight, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);

        immediate.draw();
    }

    private static final RenderPipeline BADGE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation("pipeline/waypoint_badge")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .build()
    );

    private static final RenderLayer BADGE_BACKGROUND = RenderLayer.of(
            "waypoint_badge_background",
            RenderSetup.builder(BADGE_PIPELINE)
                    .translucent()
                    .build()
    );

    private static void renderBadge(Waypoint waypoint, double distance, Vec3d cameraPos, Camera camera, TextRenderer font, VertexConsumerProvider.Immediate immediate) {
        float scale = (float) Math.max(distance / 10f, 1f) * 0.035f;
        MatrixStack matrices = buildMatrices(waypoint.pos(), cameraPos, camera, scale);

        var pose = matrices.peek().getPositionMatrix();

        int r = (waypoint.color() >> 16) & 0xFF;
        int g = (waypoint.color() >> 8) & 0xFF;
        int b = waypoint.color() & 0xFF;
        int bgColor = (0xCC << 24) | (r << 16) | (g << 8) | b;

        int badgeW = 7;
        int badgeH = 14;
        int tipH = 5;

        var buf = immediate.getBuffer(BADGE_BACKGROUND);

        buf.vertex(pose, -badgeW, -badgeH, 0).color(bgColor);
        buf.vertex(pose,  badgeW, -badgeH, 0).color(bgColor);
        buf.vertex(pose,  badgeW, 0, 0).color(bgColor);
        buf.vertex(pose, -badgeW, 0, 0).color(bgColor);

        buf.vertex(pose, -badgeW, 0, 0).color(bgColor);
        buf.vertex(pose,  badgeW, 0, 0).color(bgColor);
        buf.vertex(pose,  0, tipH, 0).color(bgColor);
        buf.vertex(pose,  0, tipH, 0).color(bgColor);

        immediate.draw();

        String letter = String.valueOf(waypoint.badgeLetter());
        font.draw(
                net.minecraft.text.Text.literal(letter).asOrderedText(),
                -font.getWidth(letter) / 2f, -badgeH + (badgeH - font.fontHeight) / 2f + 2,
                0xFFFFFFFF, false, pose, immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0
        );

        String distanceText = Math.round(distance) + "m";
        font.draw(distanceText, -font.getWidth(distanceText) / 2f, tipH + 2, 0xFFFFFFFF,
                false, pose, immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);

        immediate.draw();
    }

    private static MatrixStack buildMatrices(BlockPos pos, Vec3d cameraPos, Camera camera, float scale) {
        Vec3d center = Vec3d.ofCenter(pos);
        MatrixStack matrices = new MatrixStack();
        matrices.translate(center.x - cameraPos.x, center.y - cameraPos.y + 0.5, center.z - cameraPos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw() + 180));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-camera.getPitch()));
        matrices.scale(scale, -scale, scale);
        return matrices;
    }
}