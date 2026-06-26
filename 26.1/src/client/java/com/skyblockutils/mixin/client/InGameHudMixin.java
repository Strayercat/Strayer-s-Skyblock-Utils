package com.skyblockutils.mixin.client;

import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.OnScreenNotification;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Gui.class, priority = 5000)
public class InGameHudMixin {
    @Inject(at = @At("TAIL"), method = "extractRenderState")
    private void onRenderEnd(GuiGraphicsExtractor context, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        OnScreenNotification.render(context, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    @Inject(at = @At("HEAD"), method = "extractScoreboardSidebar", cancellable = true)
    private void hideSidebarMixin(GuiGraphicsExtractor context, DeltaTracker deltaTracker, CallbackInfo ci) {
        if ((ModConfig.INSTANCE.sideBarInHud || ModConfig.INSTANCE.customSidebar) && StrayersSkyblockUtilsClient.isInSkyblock)
            ci.cancel();
    }
}