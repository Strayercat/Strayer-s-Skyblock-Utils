package com.skyblockutils.mixin.client;

import com.skyblockutils.features.coop.CoopListParser;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "closeContainer", at = @At("HEAD"), cancellable = true)
    private void ssu$holdInventoryClose(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (!CoopListParser.isBusy()) return;
        if (self.containerMenu != self.inventoryMenu) return;
        self.clientSideCloseContainer();
        ci.cancel();
    }
}
