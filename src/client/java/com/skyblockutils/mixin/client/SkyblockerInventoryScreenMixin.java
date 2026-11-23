package com.skyblockutils.mixin.client;

import com.skyblockutils.features.inventory.InventoryButtons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "de.hysky.skyblocker.skyblock.item.SkyblockInventoryScreen", remap = false)
public class SkyblockerInventoryScreenMixin {
    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "extractBackground", at = @At("RETURN"), require = 0)
    private void ssu$drawAfterSkyblocker(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        InventoryButtons.extractDeferred(graphics, mouseX, mouseY);
    }
}