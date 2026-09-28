package com.skyblockutils.features.chat;

import com.skyblockutils.features.hud.SeparatedChatHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
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
    private EditBox searchBox;

    public SystemChatScreen() {
        super(Component.literal("System Chat"));
    }

    @Override
    protected void init() {
        int[] bounds = SeparatedChatHud.searchBarBounds(this.width);
        this.searchBox = new EditBox(this.font, bounds[0] + 2, bounds[1] + 2, bounds[2] - 4, bounds[3], Component.literal("Search"));
        this.searchBox.setMaxLength(100);
        this.searchBox.setBordered(false);
        this.searchBox.setCanLoseFocus(false);
        this.searchBox.setHint(Component.literal("Search..."));
        this.searchBox.setValue(SeparatedChatHud.searchQuery());
        this.searchBox.setResponder(value -> {
            SeparatedChatHud.setSearch(value);
            this.updateSearchColor();
        });
        this.updateSearchColor();
        this.addRenderableWidget(this.searchBox);
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.searchBox);
    }

    private void updateSearchColor() {
        this.searchBox.setTextColor(SeparatedChatHud.hasNoMatch() ? 0xFFFF5555 : 0xFFE0E0E0);
    }

    @Override
    public void extractBackground(final @NotNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
    }

    @Override
    public void extractRenderState(final @NotNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        SeparatedChatHud.renderPinned(graphics);
        int[] bounds = SeparatedChatHud.searchBarBounds(this.width);
        graphics.fill(bounds[0], bounds[1], bounds[0] + bounds[2], bounds[1] + bounds[3], this.minecraft.options.getBackgroundColor(Integer.MIN_VALUE));
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (event.isConfirmation()) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() == 0 && !this.searchBox.isMouseOver(event.x(), event.y())) {
            boolean insert = this.minecraft.hasShiftDown();
            Style clicked = SeparatedChatHud.findStyleAt(event.x(), event.y(), insert);
            if (clicked != null && this.handleComponentClicked(clicked, insert)) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        SeparatedChatHud.scrollHistory(Mth.clamp(scrollY, -1.0, 1.0));
        return true;
    }

    @Override
    public void removed() {
        SeparatedChatHud.resetPinnedState();
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