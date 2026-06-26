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
    @Unique private final float[] tpsSamples = new float[10];
    @Unique private int sampleIndex = 0;

    @Inject(at = @At("HEAD"), method = "handleSetTime")
    private void onWorldTimeUpdate(ClientboundSetTimePacket packet, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        long currentWorldTime = packet.gameTime();

        if (lastTimeUpdate != -1 && lastWorldTime != -1) {
            long ticksPassed = currentWorldTime - lastWorldTime;
            long msPassed = now - lastTimeUpdate;

            if (ticksPassed > 0 && msPassed > 0) {
                float sample = Math.min(20f, ticksPassed / (msPassed / 1000f));
                tpsSamples[sampleIndex % tpsSamples.length] = sample;
                sampleIndex++;

                float sum = 0;
                int count = Math.min(sampleIndex, tpsSamples.length);
                for (int i = 0; i < count; i++) sum += tpsSamples[i];
                ModFunctions.tps = sum / count;
            }
        }

        lastTimeUpdate = now;
        lastWorldTime = currentWorldTime;
    }
}