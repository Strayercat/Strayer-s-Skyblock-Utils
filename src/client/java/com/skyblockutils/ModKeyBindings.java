package com.skyblockutils;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.minecraft.util.Identifier;

public class ModKeyBindings {
    public static Category ssuCategory = Category.create(Identifier.tryParse("ssu"));

    public static final KeyBinding RUN_T_FUNCTION_KEY =
            KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.ssu.corlTimer",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_T,
                    ssuCategory
            ));

    public static final KeyBinding RUN_R_FUNCTION_KEY =
            KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.ssu.autofish",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_R,
                    ssuCategory
            ));

    public static final KeyBinding RUN_Z_FUNCTION_KEY =
            KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.ssu.dungeonHud",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_Z,
                    ssuCategory
            ));

    public static final KeyBinding RUN_HOME_FUNCTION_KEY =
            KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.ssu.sendCoordinates",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_HOME,
                    ssuCategory
            ));

    public static void init() {}
}