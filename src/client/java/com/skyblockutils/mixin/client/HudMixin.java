package com.skyblockutils.mixin.client;

import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.features.hud.CustomSidebar;
import com.skyblockutils.features.hud.SsuHud;
import com.skyblockutils.features.hud.layout.HudManager;
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
        if (!StrayersSkyblockUtilsClient.isInSkyblock) return;
        String location = HudManager.currentLocation();
        boolean customSidebar = CustomSidebar.ELEMENT.isEnabled() && HudManager.isVisible(CustomSidebar.ELEMENT, location);
        boolean sidebarInHud = SsuHud.ELEMENT.isEnabled() && SsuHud.ELEMENT.show("sidebar");
        if (customSidebar || sidebarInHud) ci.cancel();
    }
}
