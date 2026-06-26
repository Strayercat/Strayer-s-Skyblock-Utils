package com.skyblockutils.utils;

import net.minecraft.client.MinecraftClient;

public class ZoomState {
    public static boolean isZooming = false;
    public static boolean previousCinematic = false;
    public static float fov = 30.0f;

    public static final float MIN_FOV = 5.0f;

    public static float getMaxFov() {
        return MinecraftClient.getInstance().options.getFov().getValue().floatValue();
    }

    public static void scroll(double vertical) {
        fov = Math.clamp(fov + (-vertical > 0 ? 5.0f : -5.0f), MIN_FOV, getMaxFov());
    }

    public static void reset() {
        fov = 30.0f;
    }
}