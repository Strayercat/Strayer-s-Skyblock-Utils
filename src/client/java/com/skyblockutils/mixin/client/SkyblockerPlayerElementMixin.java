package com.skyblockutils.mixin.client;

import com.skyblockutils.utils.TabListIndicator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "de.hysky.skyblocker.skyblock.tabhud.widget.element.PlayerElement", remap = false)
public class SkyblockerPlayerElementMixin {
    @Shadow
    @Final
    private int iconDim;

    @Unique
    private String ssu$playerName;

    @Inject(
            method = "<init>(Lnet/minecraft/client/multiplayer/PlayerInfo;Lnet/minecraft/network/chat/Component;Z)V",
            at = @At("RETURN"),
            require = 0
    )
    private void ssu$captureName(PlayerInfo ple, Component name, boolean large, CallbackInfo ci) {
        this.ssu$playerName = TabListIndicator.extractName(ple);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void ssu$drawBadge(GuiGraphicsExtractor graphics, int x, int y, CallbackInfo ci) {
        if (TabListIndicator.isUser(ssu$playerName)) TabListIndicator.drawBadge(graphics, x, y, iconDim);
    }
}