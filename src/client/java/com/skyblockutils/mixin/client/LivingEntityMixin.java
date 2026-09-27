package com.skyblockutils.mixin.client;

import com.skyblockutils.config.ModConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(at = @At("HEAD"), method = "swing(Lnet/minecraft/world/InteractionHand;)V", cancellable = true)
    private void cancelSwing(InteractionHand hand, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (ModConfig.INSTANCE.cancelSwings && self instanceof LocalPlayer)
            ci.cancel();
    }
}