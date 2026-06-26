package com.skyblockutils.mixin.client;

import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.OnScreenNotification;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Gui.class, priority = 5000)
public class InGameHudMixin {

    @Shadow
    private net.minecraft.client.renderer.state.gui.GuiRenderState guiRenderState;

    @Inject(at = @At("TAIL"), method = "extractRenderState")
    private void onRenderEnd(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        int xMouse = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int yMouse = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        GuiGraphicsExtractor context = new GuiGraphicsExtractor(mc, guiRenderState, xMouse, yMouse);
        OnScreenNotification.render(context, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }
}