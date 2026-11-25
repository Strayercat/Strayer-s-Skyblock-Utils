package com.skyblockutils.features.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class NamePromptScreen extends Screen {
    private final Screen parent;
    private final String initial;
    private final Consumer<String> onConfirm;
    private EditBox box;

    public NamePromptScreen(Screen parent, String title, String initial, Consumer<String> onConfirm) {
        super(Component.literal(title));
        this.parent = parent;
        this.initial = initial;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 20;
        box = new EditBox(this.font, x, y, 200, 20, Component.empty());
        box.setMaxLength(64);
        box.setValue(initial);
        box.setHint(Component.literal("Layout name"));
        this.addRenderableWidget(box);
        this.setInitialFocus(box);
        this.addRenderableWidget(Button.builder(Component.literal("Save"), _ -> confirm()).bounds(x, y + 26, 98, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), _ -> onClose()).bounds(x + 102, y + 26, 98, 20).build());
    }

    private void confirm() {
        String name = box.getValue().trim();
        if (name.isEmpty()) return;
        this.minecraft.setScreenAndShow(parent);
        onConfirm.accept(name);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (event.isConfirmation()) {
            confirm();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 36, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
