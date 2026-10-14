package com.skyblockutils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.skyblockutils.utils.SSUIndicator;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PawPrints {
    private static final RenderType TYPE = RenderTypes.text(Identifier.fromNamespaceAndPath("skyblockutils", "textures/entity/paw_print.png"));

    private static final int COLOR = 0xA855F7;
    private static final int LIGHT = 0xF000F0;
    private static final double STRIDE = 0.55;
    private static final double SIDE_OFFSET = 0.12;
    private static final double TELEPORT_DISTANCE = 4.0;
    private static final float SIZE = 0.28F;
    private static final float LIFT = 0.015F;
    private static final long LIFETIME_MS = 4_000;
    private static final long FADE_MS = 1_500;
    private static final double MAX_RENDER_DISTANCE_SQ = 48 * 48;
    private static final int MAX_PRINTS = 400;

    private static final Deque<Print> prints = new ArrayDeque<>();
    private static final Map<UUID, Tracker> trackers = new HashMap<>();

    private PawPrints() {
    }

    public static void tick(Minecraft client) {
        long now = System.currentTimeMillis();
        while (!prints.isEmpty() && now - prints.peekFirst().time() > LIFETIME_MS) prints.pollFirst();

        if (client.level == null) {
            prints.clear();
            trackers.clear();
            return;
        }

        Set<UUID> seen = new HashSet<>();
        for (Player player : client.level.players()) {
            if (!SSUIndicator.isVipUser(player.getName().getString())) continue;
            UUID id = player.getUUID();
            seen.add(id);

            Tracker tracker = trackers.get(id);
            if (tracker == null) {
                trackers.put(id, new Tracker(player.getX(), player.getZ()));
                continue;
            }

            double dx = player.getX() - tracker.x;
            double dz = player.getZ() - tracker.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > TELEPORT_DISTANCE || !player.onGround()) {
                tracker.reset(player.getX(), player.getZ());
                continue;
            }
            if (dist < STRIDE || player.isInvisible() || player.isSpectator()) continue;

            double fx = dx / dist;
            double fz = dz / dist;
            double side = tracker.left ? -SIDE_OFFSET : SIDE_OFFSET;
            prints.addLast(new Print(
                    player.getX() - fz * side,
                    player.getY() + LIFT,
                    player.getZ() + fx * side,
                    (float) fx, (float) fz, now));
            if (prints.size() > MAX_PRINTS) prints.pollFirst();

            tracker.left = !tracker.left;
            tracker.reset(player.getX(), player.getZ());
        }
        trackers.keySet().retainAll(seen);
    }

    public static void onWorldRender(LevelRenderContext context) {
        if (prints.isEmpty()) return;
        Vec3 cam = context.gameRenderer().mainCamera().position();
        long now = System.currentTimeMillis();

        context.submitNodeCollector().submitCustomGeometry(new PoseStack(), TYPE, (pose, buffer) -> {
            for (Print print : prints) {
                double rx = print.x() - cam.x;
                double ry = print.y() - cam.y;
                double rz = print.z() - cam.z;
                if (rx * rx + ry * ry + rz * rz > MAX_RENDER_DISTANCE_SQ) continue;

                long age = now - print.time();
                float alpha = age < LIFETIME_MS - FADE_MS ? 1.0F : Math.max(0.0F, (LIFETIME_MS - age) / (float) FADE_MS);
                if (alpha <= 0.1F) continue;
                int color = ((int) (alpha * 255.0F) << 24) | COLOR;

                float half = SIZE / 2.0F;
                float fx = print.fx() * half;
                float fz = print.fz() * half;
                float sx = -print.fz() * half;
                float sz = print.fx() * half;
                float x = (float) rx;
                float y = (float) ry;
                float z = (float) rz;

                vertex(buffer, pose, x + fx - sx, y, z + fz - sz, color, 0.0F, 0.0F);
                vertex(buffer, pose, x - fx - sx, y, z - fz - sz, color, 0.0F, 1.0F);
                vertex(buffer, pose, x - fx + sx, y, z - fz + sz, color, 1.0F, 1.0F);
                vertex(buffer, pose, x + fx + sx, y, z + fz + sz, color, 1.0F, 0.0F);
            }
        });
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, int color, float u, float v) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setLight(LIGHT);
    }

    private record Print(double x, double y, double z, float fx, float fz, long time) {
    }

    private static final class Tracker {
        private double x;
        private double z;
        private boolean left;

        private Tracker(double x, double z) {
            this.x = x;
            this.z = z;
        }

        private void reset(double x, double z) {
            this.x = x;
            this.z = z;
        }
    }
}
