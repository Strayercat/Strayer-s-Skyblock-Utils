package com.skyblockutils.mixin.client;

import com.skyblockutils.utils.OnScreenNotification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = net.minecraft.client.gui.hud.InGameHud.class, priority = 5000)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderEnd(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        OnScreenNotification.render(context, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
    }
}
