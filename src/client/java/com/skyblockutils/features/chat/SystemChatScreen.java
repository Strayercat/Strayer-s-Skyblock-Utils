package com.skyblockutils.features.chat;

import com.skyblockutils.ModKeyBindings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class SystemChatScreen extends Screen {
    public SystemChatScreen() {
        super(Component.literal("System Chat"));
    }

    @Override
    public void extractBackground(final @NotNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
    }

    @Override
    public void extractRenderState(final @NotNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        SeparatedChat.renderPinned(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (event.isConfirmation() || ModKeyBindings.SYSTEM_CHAT_HISTORY_KEY.matches(event)) {
            SeparatedChat.ignoreKeyUntilRelease();
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() == 0) {
            boolean insert = this.minecraft.hasShiftDown();
            Style clicked = SeparatedChat.findStyleAt(event.x(), event.y(), insert);
            if (clicked != null && this.handleComponentClicked(clicked, insert)) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        SeparatedChat.scrollHistory(Mth.clamp(scrollY, -1.0, 1.0));
        return true;
    }

    private boolean handleComponentClicked(final Style clicked, final boolean insert) {
        if (insert) {
            if (clicked.getInsertion() == null) return false;
            this.openChat(clicked.getInsertion());
            return true;
        }

        ClickEvent event = clicked.getClickEvent();
        if (event == null) return false;

        if (event instanceof ClickEvent.SuggestCommand(String command)) {
            this.openChat(command);
            return true;
        }

        defaultHandleGameClickEvent(event, this.minecraft, this);
        return true;
    }

    private void openChat(final String text) {
        this.minecraft.gui.setScreen(new ChatScreen(text, false));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isAllowedInPortal() {
        return true;
    }
}