package com.skyblockutils.mixin.client;

import com.skyblockutils.features.inventory.InventoryButtons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<@NotNull InventoryMenu> {
    protected InventoryScreenMixin(InventoryMenu menu, RecipeBookComponent<?> recipeBookComponent, Inventory inventory, Component title) {
        super(menu, recipeBookComponent, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void ssu$removeRecipeButton(CallbackInfo ci) {
        if (!InventoryButtons.active()) return;
        ScreenPosition position = getRecipeBookButtonPosition();
        for (GuiEventListener child : List.copyOf(children())) {
            if (child instanceof ImageButton button && button.getX() == position.x() && button.getY() == position.y()) {
                removeWidget(button);
            }
        }
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void ssu$hideOffhand(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (InventoryButtons.active()) InventoryButtons.coverOffhand(graphics, leftPos, topPos);
    }

    @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
    private void ssu$labels(GuiGraphicsExtractor graphics, int xm, int ym, CallbackInfo ci) {
        if (InventoryButtons.active()) ci.cancel();
    }

    @Inject(method = "extractRenderState*", at = @At("TAIL"))
    private void ssu$tooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!InventoryButtons.active()) return;
        InventoryButtons.extractTooltip(graphics, font, leftPos, topPos, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (InventoryButtons.active() && event.button() == 0
                && InventoryButtons.click(this, leftPos, topPos, event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}