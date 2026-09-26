package com.skyblockutils.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.skyblockutils.utils.TabListIndicator;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @ModifyReturnValue(method = "getNameTag", at = @At("RETURN"))
    private @Nullable Component ssu$addNametagBadge(@Nullable Component original, Entity entity) {
        if (original == null || !(entity instanceof Player player)) return original;
        if (!TabListIndicator.isUser(player.getName().getString())) return original;
        return TabListIndicator.withNametagBadge(original);
    }
}