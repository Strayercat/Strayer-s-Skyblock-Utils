package com.skyblockutils.mixin.client;

import com.skyblockutils.features.chat.emoji.EmojiPicker;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EditBox.class)
public class EditBoxMixin {
    @Unique
    private static final int KEY_BACKSPACE = 259;

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true, require = 1)
    private void ssu$emojiSearchChar(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        EmojiPicker picker = EmojiPicker.capturing((EditBox) (Object) this);
        if (picker != null && event.isAllowedChatCharacter()) {
            cir.setReturnValue(picker.charTyped(event.codepointAsString()));
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 1)
    private void ssu$emojiSearchBackspace(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.key() != KEY_BACKSPACE) return;
        EmojiPicker picker = EmojiPicker.capturing((EditBox) (Object) this);
        if (picker != null) {
            picker.keyPressed(KEY_BACKSPACE, event.hasControlDownWithQuirk(), _ -> {});
            cir.setReturnValue(true);
        }
    }
}
