package com.skyblockutils.mixin.client;

import com.skyblockutils.config.ModConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowingMixin {

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;

        if (entity instanceof PlayerEntity) {
            // Log the IDs to the console so we can see the mismatch
            // System.out.println("Checking: " + entity.getUuidAsString());

            boolean shouldGlow = ModConfig.INSTANCE.getGlowingPlayers().stream()
                    .anyMatch(gp -> gp.getUuid().toString().equalsIgnoreCase(entity.getUuid().toString()));

            if (shouldGlow) {
                cir.setReturnValue(true);
            }
        }
    }
}