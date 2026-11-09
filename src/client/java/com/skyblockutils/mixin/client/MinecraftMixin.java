package com.skyblockutils.mixin.client;

import com.skyblockutils.features.hud.BestiaryHud;
import com.skyblockutils.features.mining.GemstonePaneFix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow @Nullable public HitResult hitResult;
    @Shadow @Nullable public ClientLevel level;

    @Inject(method = "continueAttack", at = @At("HEAD"))
    private void onBlockBreaking(boolean down, CallbackInfo ci) {
        if (!down || this.level == null) return;
        if (this.hitResult instanceof BlockHitResult blockHitResult && this.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            Block block = this.level.getBlockState(blockPos).getBlock();

            if (BuiltInRegistries.BLOCK.getKey(block).toString().contains("stained_glass")) {
                GemstonePaneFix.latestGemBreaking = blockPos;
            }
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void ssu$trackAttackAction(CallbackInfoReturnable<Boolean> cir) {
        BestiaryHud.onPlayerAction();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void ssu$trackUseAction(CallbackInfo ci) {
        BestiaryHud.onPlayerAction();
    }
}
