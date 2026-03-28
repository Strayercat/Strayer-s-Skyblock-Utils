package com.skyblockutils.features;

import com.skyblockutils.utils.ZoomState;
import net.minecraft.client.MinecraftClient;

public class Zoom {
    public static void handleZoom(MinecraftClient client) {
        if (!ZoomState.isZooming) {
            ZoomState.previousCinematic = client.options.smoothCameraEnabled;
            client.options.smoothCameraEnabled = true;
            ZoomState.isZooming = true;
        } else {
            client.options.smoothCameraEnabled = ZoomState.previousCinematic;
            ZoomState.reset();
            ZoomState.isZooming = false;
        }
    }
}