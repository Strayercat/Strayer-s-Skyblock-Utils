package com.skyblockutils.mixin.client;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.party.PartyInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowingMixin {
    @Inject(at = @At("HEAD"), method = "isCurrentlyGlowing", cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof Player player)) return;

        boolean shouldGlow = ModConfig.INSTANCE.getGlowingPlayers().stream()
                .anyMatch(gp -> gp.getUuid().toString().equalsIgnoreCase(player.getUUID().toString()));
        if (shouldGlow) {
            cir.setReturnValue(true);
            return;
        }

        if (ModConfig.INSTANCE.partyGlow) {
            String name = player.getGameProfile().name();
            if (PartyInfo.members.stream().anyMatch(m -> m.equalsIgnoreCase(name))
                    || name.equalsIgnoreCase(PartyInfo.leader)) {
                cir.setReturnValue(true);
            }
        }
    }
}