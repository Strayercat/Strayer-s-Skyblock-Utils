package com.skyblockutils.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PingDebugMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPacketListener.class)
public interface PingDebugMonitorAccessor {
    @Accessor("pingDebugMonitor")
    PingDebugMonitor getPingDebugMonitor();
}