package com.skyblockutils.mixin.client;

import com.skyblockutils.features.coop.CoopListParser;
import com.skyblockutils.features.hud.BestiaryHud;
import com.skyblockutils.render.HeadRestorer;
import com.skyblockutils.utils.DamageLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
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
        String sound = packet.getSound().value().location().getPath();
        if (sound.equals("entity.experience_orb.pickup")) BestiaryHud.onXpSound();
        if (!CoopListParser.shouldMuteSounds()) return;
        if (sound.equals("block.lever.click")) ci.cancel();
    }

    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"), cancellable = true)
    private void ssu$dropCoopSlots(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return;
        if (CoopListParser.isHiddenContainer(packet.getContainerId())) ci.cancel();
    }

    @Inject(method = "handleContainerClose", at = @At("HEAD"), cancellable = true)
    private void ssu$keepScreenOnHiddenClose(ClientboundContainerClosePacket packet, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return;
        if (CoopListParser.isHiddenContainer(packet.getContainerId())) ci.cancel();
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void ssu$trackEntityDeath(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (!client.isSameThread() || client.level == null || packet.getEventId() != 3) return;
        Entity entity = packet.getEntity(client.level);
        if (entity == null) return;
        DamageLogger.onDeath(entity);
        BestiaryHud.onEntityDeath(entity);
    }

    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    private void ssu$logDamage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return;
        DamageLogger.onDamage(packet);
    }

    @Inject(method = "handleBlockEntityData", at = @At("TAIL"))
    private void ssu$cacheHeadData(ClientboundBlockEntityDataPacket packet, CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || packet.getType() != BlockEntityTypes.SKULL) return;
        HeadRestorer.onSkullData(level, packet.getPos(), packet.getTag());
    }
}
