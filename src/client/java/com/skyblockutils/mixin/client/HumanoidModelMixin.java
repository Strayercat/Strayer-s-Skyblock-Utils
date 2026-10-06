package com.skyblockutils.mixin.client;

import com.skyblockutils.utils.SSURenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin {
    @Shadow
    @Final
    public ModelPart head;

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("TAIL")
    )
    private void ssu$bigHead(HumanoidRenderState state, CallbackInfo ci) {
        if (!((SSURenderState) state).ssu$isBigHead()) return;
        head.xScale = SSURenderState.BIG_HEAD_SCALE;
        head.yScale = SSURenderState.BIG_HEAD_SCALE;
        head.zScale = SSURenderState.BIG_HEAD_SCALE;
    }
}