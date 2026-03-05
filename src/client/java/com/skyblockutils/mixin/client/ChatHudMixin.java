package com.skyblockutils.mixin.client;

import com.skyblockutils.utils.OnScreenNotification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = net.minecraft.client.gui.hud.ChatHud.class, priority = 5000)
public class ChatHudMixin {
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("TAIL"))
    private void onRenderEnd(DrawContext context, TextRenderer textRenderer, int currentTick, int mouseX, int mouseY, boolean focused, boolean isChatFocused, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        OnScreenNotification.render(context, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
    }
}
