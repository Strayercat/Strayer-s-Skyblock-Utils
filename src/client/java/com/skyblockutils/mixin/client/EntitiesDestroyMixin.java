package com.skyblockutils.mixin.client;

import com.skyblockutils.features.PuffTracker;
import com.skyblockutils.features.hud.BestiaryHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class EntitiesDestroyMixin {

    @Inject(at = @At("HEAD"), method = "handleRemoveEntities")
    private void onEntitiesDestroy(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        boolean main = Minecraft.getInstance().isSameThread();
        for (int entityId : packet.getEntityIds()) {
            PuffTracker.handleEntityDespawn(entityId);
            if (main) BestiaryHud.onEntityRemoved(entityId);
        }
    }
}