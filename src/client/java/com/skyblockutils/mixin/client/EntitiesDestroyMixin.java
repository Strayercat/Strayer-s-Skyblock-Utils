package com.skyblockutils.mixin.client;

import com.skyblockutils.features.PuffTracker;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class EntitiesDestroyMixin {

    @Inject(method = "onEntitiesDestroy", at = @At("HEAD"))
    private void onEntitiesDestroy(EntitiesDestroyS2CPacket packet, CallbackInfo ci) {
        for (int entityId : packet.getEntityIds()) {
            PuffTracker.handleEntityDespawn(entityId);
        }
    }
}
