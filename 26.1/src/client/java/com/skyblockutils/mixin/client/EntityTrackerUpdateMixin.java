package com.skyblockutils.mixin.client;

import com.skyblockutils.features.PuffTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class EntityTrackerUpdateMixin {

    @Inject(at = @At("TAIL"), method = "handleSetEntityData")
    private void onEntityTrackerUpdate(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        PuffTracker.handleMetadataPacket(packet.id());
    }
}