package com.skyblockutils.mixin.client;

import com.skyblockutils.features.hud.ScreenshotManager;
import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.utils.ZoomState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(at = @At("HEAD"), method = "renderItemInHand", cancellable = true)
    private void cancelHand(CameraRenderState cameraState, float deltaPartialTick, Matrix4fc modelViewMatrix, CallbackInfo ci) {
        if (ZoomState.isZooming) {
            ci.cancel();
        }
    }

    @Inject(at = @At("TAIL"), method = "extract")
    private void ssu$extractTopLayer(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isGameLoadFinished()) return;

        GuiRenderState guiRenderState = ((GameRenderer) (Object) this).gameRenderState().guiRenderState;
        guiRenderState.nextStratum();

        int mouseX = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int mouseY = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(mc, guiRenderState, mouseX, mouseY);

        ScreenshotManager.buildScreenshotHud(graphics);
        OnScreenNotification.render(graphics, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }
}
