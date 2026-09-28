package com.skyblockutils.mixin.client;

import com.skyblockutils.utils.SSUIndicator;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = {
        "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess",
        "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess"
})
public class ChatGlyphAnimationMixin {
    @ModifyVariable(method = "handleMessage", at = @At("HEAD"), argsOnly = true, name = "message")
    private FormattedCharSequence ssu$animateSpecialGlyph(FormattedCharSequence message) {
        return SSUIndicator.animateSpecialGlyphs(message);
    }
}
