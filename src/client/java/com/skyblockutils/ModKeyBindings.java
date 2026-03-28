package com.skyblockutils;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.minecraft.util.Identifier;

public class ModKeyBindings {
    public static Category ssuCategory = Category.create(Identifier.tryParse("ssu"));

    public static final KeyBinding CORLEONE_TIMER_KEY =
            KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.ssu.corlTimer",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_T,
                    ssuCategory
            ));

    public static final KeyBinding AUTOFISH_KEY =
            KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.ssu.autofish",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_R,
                    ssuCategory
            ));

    public static final KeyBinding HUD_KEY =
            KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.ssu.hud",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_Z,
                    ssuCategory
            ));

    public static final KeyBinding PRINT_COORDINATES_KEY =
            KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.ssu.sendCoordinates",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_HOME,
                    ssuCategory
            ));

    public static final KeyBinding CHAT_PEEK_KEY =
            KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.ssu.chatPeek",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT_ALT,
                    ssuCategory
            ));

    public static final KeyBinding ZOOM_KEY =
            KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.ssu.zoom",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_C,
                    ssuCategory
            ));

    public static final KeyBinding PUFF_TIMER_KEY =
            KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.ssu.puffTimer",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_Y,
                    ssuCategory
            ));

    public static void init() {}
}