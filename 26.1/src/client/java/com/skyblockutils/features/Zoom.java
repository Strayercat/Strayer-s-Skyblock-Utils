package com.skyblockutils.features;

import com.skyblockutils.utils.ZoomState;
import net.minecraft.client.Minecraft;

public class Zoom {
    public static void enter(Minecraft client) {
        ZoomState.previousCinematic = client.options.smoothCamera;
        client.options.smoothCamera = true;
        ZoomState.isZooming = true;
    }

    public static void exit(Minecraft client) {
        client.options.smoothCamera = ZoomState.previousCinematic;
        ZoomState.reset();
        ZoomState.isZooming = false;
    }
}