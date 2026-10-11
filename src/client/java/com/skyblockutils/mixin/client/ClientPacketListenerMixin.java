package com.skyblockutils.mixin.client;

import com.skyblockutils.features.coop.CoopListParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleOpenScreen", at = @At("HEAD"), cancellable = true)
    private void ssu$hideCoopMenu(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return;
        if (CoopListParser.onOpenScreen(packet.getContainerId(), packet.getTitle())) ci.cancel();
    }

    @Inject(method = "handleContainerContent", at = @At("HEAD"), cancellable = true)
    private void ssu$readCoopMenu(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return;
        if (CoopListParser.onContainerContent(packet.containerId(), packet.items())) ci.cancel();
    }

    @Inject(method = "handleSoundEvent", at = @At("HEAD"), cancellable = true)
    private void ssu$muteCoopLeverClick(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return;
        if (!CoopListParser.shouldMuteSounds()) return;
        if (packet.getSound().value().location().getPath().equals("block.lever.click")) ci.cancel();
    }

    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"), cancellable = true)
    private void ssu$dropCoopSlots(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return;
        if (CoopListParser.isHiddenContainer(packet.getContainerId())) ci.cancel();
    }
}
