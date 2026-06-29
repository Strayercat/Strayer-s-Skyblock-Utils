package com.skyblockutils.mixin.client;

import com.skyblockutils.features.hud.SsuHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public class BossBarHudMixin {
    @Inject(
            at = @At("HEAD"),
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
            cancellable = true
    )
    private void onExtractRenderState(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!SsuHud.showBossBar) {
            ci.cancel();
        }
    }
}