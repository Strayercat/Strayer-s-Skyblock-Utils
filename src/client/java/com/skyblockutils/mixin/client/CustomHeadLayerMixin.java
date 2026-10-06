package com.skyblockutils.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.skyblockutils.utils.SSURenderState;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerMixin {
    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/HeadedModel;translateToHead(Lcom/mojang/blaze3d/vertex/PoseStack;)V", shift = At.Shift.AFTER),
            require = 1
    )
    private void ssu$scaleWithBigHead(PoseStack poseStack, SubmitNodeCollector collector, int light, LivingEntityRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (!(((RenderLayer<?, ?>) (Object) this).getParentModel() instanceof HeadedModel headed)) return;
        ModelPart head = headed.getHead();
        float target = ((SSURenderState) state).ssu$isBigHead() ? SSURenderState.BIG_HEAD_SCALE : 1.0F;
        if (head.xScale == target && head.yScale == target && head.zScale == target) return;
        poseStack.scale(target / head.xScale, target / head.yScale, target / head.zScale);
    }
}
