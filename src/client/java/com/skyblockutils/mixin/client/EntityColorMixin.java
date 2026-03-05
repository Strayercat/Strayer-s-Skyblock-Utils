package com.skyblockutils.mixin.client;

import com.skyblockutils.config.ModConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityColorMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            // Find the specific player in your config
            ModConfig.INSTANCE.getGlowingPlayers().stream()
                    .filter(gp -> gp.getUuid().equals(player.getUuid()))
                    .findFirst()
                    .ifPresent(matchedPlayer -> cir.setReturnValue(matchedPlayer.getColor()));
        }
    }
}
