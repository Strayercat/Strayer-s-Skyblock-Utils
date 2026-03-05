package com.skyblockutils.mixin.client;

import com.skyblockutils.ModFunctions;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class WorldTimeMixin {
    @Unique
    private long lastTimeUpdate = -1;
    @Unique
    private long lastWorldTime = -1;
    @Unique
    private final float[] tpsSamples = new float[10];
    @Unique
    private int sampleIndex = 0;

    @Inject(method = "onWorldTimeUpdate", at = @At("HEAD"))
    private void onWorldTimeUpdate(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        long currentWorldTime = packet.time();

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
