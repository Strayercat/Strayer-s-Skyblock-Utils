package com.skyblockutils.mixin.client;

import com.skyblockutils.ModKeyBindings;
import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.utils.ZoomState;
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
                double mouseX = client.mouse.getScaledX(win);
                double mouseY = client.mouse.getScaledY(win);
                OnScreenNotification.handleNotificationClicks((int) mouseX, (int) mouseY, input.button());
            }
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (ModKeyBindings.CHAT_PEEK_KEY.isPressed() && client.currentScreen == null) {
            client.inGameHud.getChatHud().scroll((int) (vertical * 7));
            ci.cancel();
            return;
        }

        if (ZoomState.isZooming) {
            ZoomState.scroll(vertical);
            ci.cancel();
        }
    }
}