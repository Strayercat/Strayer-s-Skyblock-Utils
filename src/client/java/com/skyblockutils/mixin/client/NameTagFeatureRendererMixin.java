package com.skyblockutils.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.skyblockutils.render.NameplateRenderer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(NameTagFeatureRenderer.class)
public class NameTagFeatureRendererMixin {
    @WrapOperation(
            method = "prepareText",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/NameTagFeatureRenderer$Submit;backgroundColor()I")
    )
    private static int ssu$hidePlateBackground(NameTagFeatureRenderer.Submit submit, Operation<Integer> original) {
        return NameplateRenderer.isMarked(submit.text()) ? 0 : original.call(submit);
    }

    @Inject(
            method = "buildGroup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font$PreparedText;visit(Lnet/minecraft/client/gui/Font$GlyphVisitor;)V")
    )
    private void ssu$submitPlate(FeatureFrameContext context, List<NameTagFeatureRenderer.Submit> submits, CallbackInfo ci,
                                 @Local(name = "nameTag") NameTagFeatureRenderer.Submit nameTag) {
        NameplateRenderer.submit((NameTagFeatureRenderer) (Object) this, nameTag);
    }
}
