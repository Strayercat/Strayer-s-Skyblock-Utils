package com.skyblockutils.mixin.client;

import com.skyblockutils.utils.SSUIndicator;
import com.skyblockutils.utils.SSURenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Unique
    private static final float SSU_HEAD_HEIGHT_WITH_HAT = 9.0F / 16.0F;

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void ssu$applySpecialGemModel(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        String name = entity instanceof Player player ? player.getName().getString() : null;
        boolean special = name != null && SSUIndicator.isSpecialUser(name);
        ((SSURenderState) state).ssu$setBigHead(special);
        ((SSURenderState) state).ssu$setGroucho(name != null && SSUIndicator.isVipUser(name));
        if (!special) return;
        float scale = SSURenderState.SPECIAL_SCALE;
        state.scale *= scale;
        if (state.nameTagAttachment != null) {
            float headGrowth = (SSURenderState.BIG_HEAD_SCALE - 1.0F) * SSU_HEAD_HEIGHT_WITH_HAT * scale;
            state.nameTagAttachment = state.nameTagAttachment.scale(scale).add(0.0, headGrowth, 0.0);
        }
    }
}