package com.skyblockutils.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelPart.class)
public class ModelPartMixin {
    @Shadow
    public float xScale;

    @Shadow
    public float yScale;

    @Shadow
    public float zScale;

    @ModifyReturnValue(method = "storePose", at = @At("RETURN"))
    private PartPose ssu$keepScale(PartPose pose) {
        if (xScale == 1.0F && yScale == 1.0F && zScale == 1.0F) return pose;
        return new PartPose(pose.x(), pose.y(), pose.z(), pose.xRot(), pose.yRot(), pose.zRot(), xScale, yScale, zScale);
    }
}
