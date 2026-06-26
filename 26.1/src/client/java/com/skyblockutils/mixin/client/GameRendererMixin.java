package com.skyblockutils.mixin.client;

import com.skyblockutils.utils.ZoomState;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(at = @At("HEAD"), method = "renderItemInHand", cancellable = true)
    private void cancelHand(CameraRenderState cameraRenderState, float tickProgress, Matrix4fc positionMatrix, CallbackInfo ci) {
        if (ZoomState.isZooming) {
            ci.cancel();
        }
    }
}