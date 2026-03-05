package com.skyblockutils.mixin.client;

import com.skyblockutils.utils.OnScreenNotification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        if (action == InputUtil.GLFW_PRESS) {
            MinecraftClient client = MinecraftClient.getInstance();
            Window win = client.getWindow();
            if (window == win.getHandle()) {
                double mouseX = Mouse.scaleX(win, client.mouse.getX());
                double mouseY = Mouse.scaleY(win, client.mouse.getY());
                OnScreenNotification.handleNotificationClicks((int) mouseX, (int) mouseY, input.button());
            }
        }
    }
}
