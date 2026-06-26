package com.skyblockutils.mixin.client;

import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Hud.class, priority = 5000)
public class HudMixin {
    @Inject(at = @At("HEAD"), method = "extractScoreboardSidebar", cancellable = true)
    private void hideSidebarMixin(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if ((ModConfig.INSTANCE.sideBarInHud || ModConfig.INSTANCE.customSidebar) && StrayersSkyblockUtilsClient.isInSkyblock)
            ci.cancel();
    }
}