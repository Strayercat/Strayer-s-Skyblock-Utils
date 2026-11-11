package com.skyblockutils.mixin.client;

import com.skyblockutils.features.chat.emoji.EmojiPicker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
    @Shadow
    private @Nullable Runnable deferredTooltip;

    @Shadow
    private @Nullable Style hoveredTextStyle;

    @Shadow
    private @Nullable Style clickableTextStyle;

    @Inject(method = "extractDeferredElements", at = @At("HEAD"))
    private void ssu$suppressHoverBehindPicker(int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!EmojiPicker.suppressHover) return;
        EmojiPicker.suppressHover = false;
        deferredTooltip = null;
        hoveredTextStyle = null;
        clickableTextStyle = null;
    }
}
