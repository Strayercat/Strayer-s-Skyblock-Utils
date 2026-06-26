package com.skyblockutils.mixin.client;

import com.skyblockutils.ModKeyBindings;
import com.skyblockutils.features.hud.ScreenshotManager;
import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.utils.ZoomState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {

    @Inject(at = @At("HEAD"), method = "onButton", cancellable = true)
    private void onMouseButton(long handle, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (action == 1) {
            Minecraft client = Minecraft.getInstance();
            int sw = client.getWindow().getGuiScaledWidth();
            int sh = client.getWindow().getGuiScaledHeight();

            if (handle == client.getWindow().handle()) {
                double mouseX = client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / client.getWindow().getWidth();
                double mouseY = client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / client.getWindow().getHeight();

                int button = buttonInfo.button();


                boolean consumedByNotification = OnScreenNotification.handleNotificationClicks((int) mouseX, (int) mouseY, button, sw, sh);

                if (button == 0) ScreenshotManager.onMouseClick(mouseX, mouseY);

                if (consumedByNotification) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "onScroll", cancellable = true)
    private void onScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();

        if (ModKeyBindings.CHAT_PEEK_KEY.isDown() && client.gui.screen() == null) {
            client.gui.hud.getChat().scrollChat((int) (yoffset * 7));
            ci.cancel();
            return;
        }

        if (ZoomState.isZooming) {
            ZoomState.scroll(yoffset);
            ci.cancel();
        }
    }
}