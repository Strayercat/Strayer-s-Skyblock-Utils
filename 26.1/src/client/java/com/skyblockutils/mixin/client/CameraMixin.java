package com.skyblockutils.mixin.client;

import com.skyblockutils.utils.ZoomState;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {
    @Inject(at = @At("RETURN"), method = "calculateFov", cancellable = true)
    private void modifyFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (ZoomState.isZooming) {
            cir.setReturnValue(ZoomState.fov);
        }
    }
}