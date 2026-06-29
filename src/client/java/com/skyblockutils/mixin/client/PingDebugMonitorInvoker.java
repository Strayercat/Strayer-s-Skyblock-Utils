package com.skyblockutils.mixin.client;

import net.minecraft.client.multiplayer.PingDebugMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PingDebugMonitor.class)
public interface PingDebugMonitorInvoker {
    @Invoker("tick")
    void ssu$tick();
}