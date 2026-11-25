package com.skyblockutils.mixin.client;

import com.skyblockutils.ModKeyBindings;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.features.inventory.ButtonIcons;
import com.skyblockutils.features.inventory.InventoryButtons;
import com.skyblockutils.features.wiki.WikiLookup;
import com.skyblockutils.utils.ClipboardUtils;
import com.skyblockutils.utils.OnScreenNotification;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void ssu$inventoryButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!((Object) this instanceof InventoryScreen) || !InventoryButtons.active()) return;
        InventoryButtons.extractPanel(graphics, Minecraft.getInstance().font, leftPos, topPos, mouseX, mouseY);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void ssu$copyIcon(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!ModKeyBindings.COPY_ICON_KEY.matches(event)) return;
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return;
        ClipboardUtils.copyTextToClipboard(ButtonIcons.specOf(hoveredSlot.getItem()));
        OnScreenNotification.builder()
                .title("Icon Copied")
                .subtitle("Paste it in the button editor's icon picker")
                .tickTime(60)
                .send();
        cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void ssu$openWiki(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!StrayersSkyblockUtilsClient.isInSkyblock || !ModKeyBindings.WIKI_KEY.matches(event)) return;
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return;
        WikiLookup.open(hoveredSlot.getItem(), (AbstractContainerScreen<?>) (Object) this);
        cir.setReturnValue(true);
    }
}
