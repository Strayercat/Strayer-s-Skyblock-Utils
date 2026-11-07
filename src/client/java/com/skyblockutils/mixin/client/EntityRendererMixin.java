package com.skyblockutils.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.skyblockutils.render.NameplateRenderer;
import com.skyblockutils.utils.SSUIndicator;
import com.skyblockutils.utils.SSURenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @ModifyReturnValue(method = "getNameTag", at = @At("RETURN"))
    private @Nullable Component ssu$addNametagBadge(@Nullable Component original, Entity entity) {
        if (original == null || !(entity instanceof Player player)) return original;
        String name = player.getName().getString();
        return SSUIndicator.isUser(name) ? NameplateRenderer.mark(original, name) : original;
    }

    @Inject(
            method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",
            at = @At("TAIL")
    )
    private void ssu$markPlateScore(Entity entity, EntityRenderState state, float partialTicks, double nameTagDistance, double belowNameDistance, CallbackInfo ci) {
        if (state.nameTag == null || state.scoreText == null) return;
        String name = NameplateRenderer.markedName(state.nameTag);
        if (name != null) state.scoreText = NameplateRenderer.markScore(state.scoreText, name);
    }

    @WrapOperation(
            method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V")
    )
    private void ssu$keepPlateStacked(PoseStack poseStack, float xo, float yo, float zo, Operation<Void> original,
                                      @Local(argsOnly = true, name = "state") EntityRenderState state) {
        if (state.nameTag != null && NameplateRenderer.markedName(state.nameTag) != null) return;
        original.call(poseStack, xo, yo, zo);
    }

    @WrapOperation(
            method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
    )
    private void ssu$offsetPlateLines(SubmitNodeCollector collector, PoseStack poseStack, @Nullable Vec3 attachment, int offset,
                                      Component text, boolean seeThrough, int lightCoords, CameraRenderState camera,
                                      Operation<Void> original, @Local(argsOnly = true, name = "state") EntityRenderState state) {
        boolean score = NameplateRenderer.isScore(text);
        boolean name = NameplateRenderer.markedName(text) != null;
        if (attachment == null || (!score && !name)) {
            original.call(collector, poseStack, attachment, offset, text, seeThrough, lightCoords, camera);
            return;
        }

        if (score) offset += NameplateRenderer.NAME_SHIFT_WITH_SCORE + NameplateRenderer.SCORE_OFFSET;
        else if (state.scoreText != null) offset += NameplateRenderer.NAME_SHIFT_WITH_SCORE;

        boolean special = state instanceof SSURenderState render && render.ssu$isBigHead();
        float lift = special ? 0.0F : NameplateRenderer.NORMAL_LIFT;
        float scale = NameplateRenderer.SCALE;

        Vector3f anchor = poseStack.last().pose().transformPosition(
                (float) attachment.x, (float) attachment.y + lift, (float) attachment.z, new Vector3f());
        float distance = anchor.length();
        Vector3f toCamera = distance > 1.0E-4F
                ? anchor.mul(-Math.min(NameplateRenderer.TOWARD_CAMERA, distance * 0.5F) / distance)
                : new Vector3f();

        poseStack.pushPose();
        poseStack.last().pose().translateLocal(toCamera);
        poseStack.translate((float) attachment.x, (float) attachment.y + lift, (float) attachment.z);
        poseStack.scale(scale, scale, scale);
        poseStack.translate((float) -attachment.x, (float) -attachment.y, (float) -attachment.z);
        original.call(collector, poseStack, attachment, offset, text, seeThrough, lightCoords, camera);
        poseStack.popPose();
    }
}
