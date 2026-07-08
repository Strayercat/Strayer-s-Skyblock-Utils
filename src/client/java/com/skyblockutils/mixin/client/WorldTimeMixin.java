package com.skyblockutils.mixin.client;

import com.skyblockutils.ModFunctions;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class WorldTimeMixin {
    @Unique private long lastTimeUpdate = -1;
    @Unique private long lastWorldTime = -1;

    @Inject(at = @At("HEAD"), method = "handleSetTime")
    private void onWorldTimeUpdate(ClientboundSetTimePacket packet, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        long currentWorldTime = packet.gameTime();

        if (lastTimeUpdate != -1 && lastWorldTime != -1) {
            long ticksPassed = currentWorldTime - lastWorldTime;
            long msPassed = now - lastTimeUpdate;

            if (ticksPassed > 0 && msPassed > 0) {
                ModFunctions.tps = Math.min(20f, ticksPassed / (msPassed / 1000f));
            }
        }

        lastTimeUpdate = now;
        lastWorldTime = currentWorldTime;
    }
}