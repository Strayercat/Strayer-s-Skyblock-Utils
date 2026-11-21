package com.skyblockutils.mixin.client;

import com.skyblockutils.ModKeyBindings;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.features.wiki.WikiLookup;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void ssu$openWiki(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!StrayersSkyblockUtilsClient.isInSkyblock || !ModKeyBindings.WIKI_KEY.matches(event)) return;
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return;
        WikiLookup.open(hoveredSlot.getItem(), (AbstractContainerScreen<?>) (Object) this);
        cir.setReturnValue(true);
    }
}
