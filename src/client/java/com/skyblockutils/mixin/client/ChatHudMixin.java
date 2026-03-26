package com.skyblockutils.mixin.client;

import com.skyblockutils.ModKeyBindings;
import com.skyblockutils.utils.OnScreenNotification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Inject(method = "isChatHidden", at = @At("RETURN"), cancellable = true)
    private void forceVisible(CallbackInfoReturnable<Boolean> cir) {
        if (ModKeyBindings.CHAT_PEEK_KEY.isPressed()) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
            method = "getHeight()I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;isChatFocused()Z")
    )
    private boolean forceFocusedForHeight(ChatHud instance) {
        return ModKeyBindings.CHAT_PEEK_KEY.isPressed() || instance.isChatFocused();
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V"),
            index = 3
    )
    private boolean forceExpanded(boolean expanded) {
        return ModKeyBindings.CHAT_PEEK_KEY.isPressed() || expanded;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("TAIL"))
    private void onRenderEnd(DrawContext context, TextRenderer textRenderer, int currentTick, int mouseX, int mouseY, boolean interactable, boolean bl, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        OnScreenNotification.render(context, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
    }
}