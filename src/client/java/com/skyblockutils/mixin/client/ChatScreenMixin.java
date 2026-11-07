package com.skyblockutils.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.skyblockutils.features.chat.SsuChatStyle;
import com.skyblockutils.features.chat.emoji.EmojiPicker;
import com.skyblockutils.utils.RoundedRect;
import com.skyblockutils.features.chat.emoji.EmojiRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Unique
    private static final int MAX_MESSAGE_LENGTH = 256;

    @Shadow
    protected EditBox input;

    @Unique
    private final EmojiPicker ssu$emojiPicker = new EmojiPicker();

    @Unique
    private boolean ssu$replacing = false;

    protected ChatScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 1)
    private void ssu$shiftInput(CallbackInfo ci) {
        this.input.setX(this.input.getX() + EmojiPicker.INPUT_OFFSET);
        this.input.setWidth(this.input.getWidth() - EmojiPicker.INPUT_OFFSET);
        if (SsuChatStyle.inputEnabled()) {
            this.input.setX(this.input.getX() + SsuChatStyle.INPUT_PADDING);
            this.input.setY(this.height - SsuChatStyle.INPUT_TEXT_FROM_BOTTOM);
            this.input.setWidth(this.width - SsuChatStyle.INPUT_RIGHT_INSET - this.input.getX());
        }
        this.input.addFormatter((text, _) -> EmojiRegistry.formatInput(text));
        EmojiPicker.bind(ssu$emojiPicker, this.input);
        ssu$limitLength();
    }

    @Inject(method = "onEdited", at = @At("TAIL"), require = 1)
    private void ssu$onEdited(String value, CallbackInfo ci) {
        if (!ssu$replacing) ssu$replaceTypedShortcode();
        ssu$limitLength();
    }

    @Unique
    private void ssu$replaceTypedShortcode() {
        String value = this.input.getValue();
        int cursor = this.input.getCursorPosition();
        EmojiRegistry.Emoji emoji = EmojiRegistry.shortcodeEndingAt(value, cursor);
        if (emoji == null) return;

        int start = cursor - emoji.shortcode().length();
        int newCursor = start + emoji.text().length();

        ssu$replacing = true;
        try {
            this.input.setValue(value.substring(0, start) + emoji.text() + value.substring(cursor));
            this.input.setCursorPosition(newCursor);
            this.input.setHighlightPos(newCursor);
        } finally {
            ssu$replacing = false;
        }
    }

    @Unique
    private void ssu$insertEmoji(String emoji) {
        String value = this.input.getValue();
        String selected = this.input.getHighlighted();
        int lengthAfter = value.length() - selected.length() + EmojiRegistry.extraLength(value) - EmojiRegistry.extraLength(selected)
                + emoji.length() + EmojiRegistry.extraLength(emoji);
        if (lengthAfter > MAX_MESSAGE_LENGTH) return;
        this.input.insertText(emoji);
    }

    @Unique
    private void ssu$limitLength() {
        this.input.setMaxLength(MAX_MESSAGE_LENGTH - EmojiRegistry.extraLength(this.input.getValue()));
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V", ordinal = 0)
    )
    private void ssu$styleInputBackground(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int col, Operation<Void> original) {
        if (!SsuChatStyle.inputEnabled()) {
            original.call(graphics, x0, y0, x1, y1, col);
            return;
        }
        RoundedRect.panel(graphics, x0, y0 - SsuChatStyle.INPUT_EXTRA_HEIGHT, x1, y1, SsuChatStyle.INPUT_RADIUS, col, SsuChatStyle.accent());
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 1)
    private void ssu$renderEmojiPicker(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        ssu$emojiPicker.renderButton(graphics, mouseX, mouseY, this.height);
        ssu$emojiPicker.render(graphics, mouseX, mouseY, this.height);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 1)
    private void ssu$emojiClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) return;
        if (ssu$emojiPicker.mouseClicked(event.x(), event.y(), this.height, this::ssu$insertEmoji)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, require = 1)
    private void ssu$emojiScroll(double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (ssu$emojiPicker.mouseScrolled(x, y, scrollY, this.height)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 1)
    private void ssu$emojiPickerKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ssu$emojiPicker.keyPressed(event.key(), event.hasControlDownWithQuirk(), this::ssu$insertEmoji)) {
            cir.setReturnValue(true);
        }
    }
}
