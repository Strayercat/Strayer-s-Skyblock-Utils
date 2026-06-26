package com.skyblockutils.mixin.client;

import com.skyblockutils.features.hud.SsuHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossEvent.BossBarOverlay.class)
public class BossBarHudMixin {
    @Inject(method = "setOverlay", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphicsExtractor context, CallbackInfo ci) {
        if (!SsuHud.showBossBar) {
            ci.cancel();
        }
    }
}
