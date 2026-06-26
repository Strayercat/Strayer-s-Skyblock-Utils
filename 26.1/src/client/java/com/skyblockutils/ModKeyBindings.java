package com.skyblockutils;

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public class ModKeyBindings {
    public static final KeyMapping.Category SSU_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ssu", "keybinds"));

    public static final KeyMapping CORLEONE_TIMER_KEY = new KeyMapping(
            "key.ssu.corlTimer",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            SSU_CATEGORY);

    public static final KeyMapping AUTOFISH_KEY = new KeyMapping(
            "key.ssu.autofish",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            SSU_CATEGORY);

    public static final KeyMapping HUD_KEY = new KeyMapping(
            "key.ssu.hud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            SSU_CATEGORY);

    public static final KeyMapping PRINT_COORDINATES_KEY = new KeyMapping(
            "key.ssu.sendCoordinates",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_HOME,
            SSU_CATEGORY);

    public static final KeyMapping CHAT_PEEK_KEY = new KeyMapping(
            "key.ssu.chatPeek",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            SSU_CATEGORY);

    public static final KeyMapping ZOOM_KEY = new KeyMapping(
            "key.ssu.zoom",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            SSU_CATEGORY);

    public static final KeyMapping PUFF_TIMER_KEY = new KeyMapping("key.ssu.puffTimer",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            SSU_CATEGORY);

    public static void init() {
        KeyMappingHelper.registerKeyMapping(CORLEONE_TIMER_KEY);
        KeyMappingHelper.registerKeyMapping(AUTOFISH_KEY);
        KeyMappingHelper.registerKeyMapping(HUD_KEY);
        KeyMappingHelper.registerKeyMapping(PRINT_COORDINATES_KEY);
        KeyMappingHelper.registerKeyMapping(CHAT_PEEK_KEY);
        KeyMappingHelper.registerKeyMapping(ZOOM_KEY);
        KeyMappingHelper.registerKeyMapping(PUFF_TIMER_KEY);
    }
}