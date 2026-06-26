package com.skyblockutils.mixin.client;

import com.skyblockutils.ModKeyBindings;
import com.skyblockutils.utils.OnScreenNotification;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatHudMixin {

    @Shadow
    public void resetChatScroll() {}

    @Unique
    private boolean wasPeeking = false;

    @ModifyArg(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V"
            ),
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            index = 3
    )
    private ChatComponent.DisplayMode forceExpanded(ChatComponent.DisplayMode displayMode) {
        if (ModKeyBindings.CHAT_PEEK_KEY.isDown() && displayMode == ChatComponent.DisplayMode.BACKGROUND) {
            return ChatComponent.DisplayMode.FOREGROUND;
        }
        return displayMode;
    }

    @Redirect(
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;isChatFocused()Z"),
            method = "getHeight()I"
    )
    private boolean forceHeightFocused(ChatComponent instance) {
        return ModKeyBindings.CHAT_PEEK_KEY.isDown() || instance.isChatFocused();
    }

    @Inject(
            at = @At("TAIL"),
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V"
    )
    private void onRenderEnd(GuiGraphicsExtractor context, Font font, int ticks, int mouseX, int mouseY, ChatComponent.DisplayMode displayMode, boolean changeCursorOnInsertions, CallbackInfo ci) {
        boolean isPeeking = ModKeyBindings.CHAT_PEEK_KEY.isDown();

        if (wasPeeking && !isPeeking) {
            resetChatScroll();
        }
        wasPeeking = isPeeking;

        Minecraft mc = Minecraft.getInstance();
        OnScreenNotification.render(context, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    @Inject(
            at = @At("HEAD"),
            method = "addClientSystemMessage(Lnet/minecraft/network/chat/Component;)V",
            cancellable = true
    )
    private void onAddClientSystemMessage(Component message, CallbackInfo ci) {
        filterScreenshotMessage(message, ci);
    }

    @Inject(
            at = @At("HEAD"),
            method = "addServerSystemMessage(Lnet/minecraft/network/chat/Component;)V",
            cancellable = true
    )
    private void onAddServerSystemMessage(Component message, CallbackInfo ci) {
        filterScreenshotMessage(message, ci);
    }

    @Unique
    private static void filterScreenshotMessage(Component message, CallbackInfo ci) {
        String clean = message.getString().replaceAll("§.", "").trim();
        if (clean.startsWith("Saved screenshot as")) {
            ci.cancel();
        }
    }
}