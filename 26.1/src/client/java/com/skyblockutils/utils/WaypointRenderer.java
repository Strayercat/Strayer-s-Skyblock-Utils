package com.skyblockutils.utils;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

    public static void render(LevelRenderContext context, Waypoint waypoint, double maxDistance) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        Camera camera = context.gameRenderer().getMainCamera();
        Vec3 cameraPos = camera.position();
        MultiBufferSource.BufferSource immediate = client.renderBuffers().bufferSource();
        Font font = client.font;

        double effectiveMax = maxDistance < 0 ? Double.MAX_VALUE : maxDistance;

        Vec3 center = Vec3.atCenterOf(waypoint.pos());
        double distance = cameraPos.distanceTo(center);
        if (distance > effectiveMax) return;

        switch (waypoint.style()) {
            case TEXT -> renderText(waypoint, distance, cameraPos, camera, font, immediate);
            case BADGE -> renderBadge(waypoint, distance, cameraPos, camera, font, immediate);
        }
    }

    private static void renderText(Waypoint waypoint, double distance, Vec3 cameraPos, Camera camera, Font font, MultiBufferSource.BufferSource immediate) {
        float scale = (float) Math.max(distance / 10f, 1f) * 0.025f;

        PoseStack matrices = buildMatrices(waypoint.pos(), cameraPos, camera, scale);

        int lineHeight = font.lineHeight + 1;
        int r = (waypoint.color() >> 16) & 0xFF;
        int g = (waypoint.color() >> 8) & 0xFF;
        int b = waypoint.color() & 0xFF;
        int labelColor = (0xFF << 24) | (r << 16) | (g << 8) | b;

        String label = waypoint.label();
        String distanceText = Math.round(distance) + "m";

        font.drawInBatch(label, -font.width(label) / 2f, 0, labelColor, false, matrices.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
        font.drawInBatch(distanceText, -font.width(distanceText) / 2f, lineHeight, 0xFFFFFFFF, false, matrices.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

        immediate.endBatch();
    }

    static final RenderPipeline BADGE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation("waypoint_badge")
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                    .withCull(false)
                    .build()
    );

    private static final RenderType BADGE_BACKGROUND = RenderType.create(
            "waypoint_badge_background",
            RenderSetup.builder(BADGE_PIPELINE).createRenderSetup()
    );

    private static void renderBadge(Waypoint waypoint, double distance, Vec3 cameraPos, Camera camera, Font font, MultiBufferSource.BufferSource immediate) {
        float scale = (float) Math.max(distance / 10f, 1f) * 0.035f;
        PoseStack matrices = buildMatrices(waypoint.pos(), cameraPos, camera, scale);
        var pose = matrices.last().pose();

        int r = (waypoint.color() >> 16) & 0xFF;
        int g = (waypoint.color() >> 8) & 0xFF;
        int b = waypoint.color() & 0xFF;
        int bgColor = (0xCC << 24) | (r << 16) | (g << 8) | b;
        int outlineColor = (0xCC << 24);

        int badgeW = 7;
        int badgeH = 14;
        int tipH = 5;
        int o = 1;

        VertexConsumer outline = immediate.getBuffer(BADGE_BACKGROUND);
        outline.addVertex(pose, -badgeW - o, -badgeH - o, 0).setColor(outlineColor);
        outline.addVertex(pose, badgeW + o, -badgeH - o, 0).setColor(outlineColor);
        outline.addVertex(pose, badgeW + o, o, 0).setColor(outlineColor);
        outline.addVertex(pose, -badgeW - o, o, 0).setColor(outlineColor);
        outline.addVertex(pose, -badgeW - o, o, 0).setColor(outlineColor);
        outline.addVertex(pose, badgeW + o, o, 0).setColor(outlineColor);
        outline.addVertex(pose, 0, tipH + o, 0).setColor(outlineColor);
        outline.addVertex(pose, 0, tipH + o, 0).setColor(outlineColor);
        immediate.endBatch(BADGE_BACKGROUND);

        VertexConsumer fill = immediate.getBuffer(BADGE_BACKGROUND);
        fill.addVertex(pose, -badgeW, -badgeH, 0).setColor(bgColor);
        fill.addVertex(pose, badgeW, -badgeH, 0).setColor(bgColor);
        fill.addVertex(pose, badgeW, 0, 0).setColor(bgColor);
        fill.addVertex(pose, -badgeW, 0, 0).setColor(bgColor);
        fill.addVertex(pose, -badgeW, 0, 0).setColor(bgColor);
        fill.addVertex(pose, badgeW, 0, 0).setColor(bgColor);
        fill.addVertex(pose, 0, tipH, 0).setColor(bgColor);
        fill.addVertex(pose, 0, tipH, 0).setColor(bgColor);
        immediate.endBatch(BADGE_BACKGROUND);

        String letter = String.valueOf(waypoint.badgeLetter());
        font.drawInBatch(
                Component.literal(letter).getVisualOrderText(),
                -font.width(letter) / 2f, -badgeH / 2f - font.lineHeight / 2f + 2,
                0xFFFFFFFF, false, pose, immediate, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0
        );

        String distanceText = Math.round(distance) + "m";
        font.drawInBatch(distanceText, -font.width(distanceText) / 2f, tipH + 2, 0xFFFFFFFF,
                false, pose, immediate, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

        immediate.endBatch();
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