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
public abstract class EntityColorMixin {
    @Inject(at = @At("HEAD"), method = "getTeamColor", cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof Player player)) return;

        ModConfig.INSTANCE.getGlowingPlayers().stream()
                .filter(gp -> gp.getUuid().equals(player.getUUID()))
                .findFirst()
                .ifPresent(matchedPlayer -> cir.setReturnValue(matchedPlayer.getColor()));

        if (ModConfig.INSTANCE.partyGlow) {
            String name = player.getGameProfile().name();
            if (PartyInfo.members.stream().anyMatch(m -> m.equalsIgnoreCase(name))
                    || name.equalsIgnoreCase(PartyInfo.leader)) {
                cir.setReturnValue(0x313E9E);
            }
        }
    }
}