package com.skyblockutils.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.skyblockutils.features.textures.CustomBreakingAnimation;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin {
    @WrapOperation(
            method = {"submitBreakingBlockModel", "submitModel"},
            at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;")
    )
    private Object ssu$customDestroyStage(List<RenderType> list, int progress, Operation<Object> original) {
        return CustomBreakingAnimation.replace((RenderType) original.call(list, progress), progress);
    }
}
