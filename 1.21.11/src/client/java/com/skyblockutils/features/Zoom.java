package com.skyblockutils.features;

import com.skyblockutils.utils.ZoomState;
import net.minecraft.client.MinecraftClient;

public class Zoom {
    public static void enter(MinecraftClient client) {
        ZoomState.previousCinematic = client.options.smoothCameraEnabled;
        client.options.smoothCameraEnabled = true;
        ZoomState.isZooming = true;
    }

    public static void exit(MinecraftClient client) {
        client.options.smoothCameraEnabled = ZoomState.previousCinematic;
        ZoomState.reset();
        ZoomState.isZooming = false;
    }
}