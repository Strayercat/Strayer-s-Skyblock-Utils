package com.skyblockutils.mixin.client;

import com.skyblockutils.features.inventory.InventoryButtons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {
    @Shadow
    public int index;

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void ssu$hideCraftingAndOffhand(CallbackInfoReturnable<Boolean> cir) {
        if (index >= InventoryMenu.CRAFT_SLOT_END && index != InventoryMenu.SHIELD_SLOT) return;
        if (!InventoryButtons.active()) return;
        if (!(Minecraft.getInstance().gui.screen() instanceof InventoryScreen screen)) return;
        Slot self = (Slot) (Object) this;
        InventoryMenu menu = screen.getMenu();
        if (index < menu.slots.size() && menu.slots.get(index) == self) cir.setReturnValue(false);
    }
}