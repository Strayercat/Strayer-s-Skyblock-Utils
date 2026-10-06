package com.skyblockutils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.skyblockutils.utils.SSURenderState;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class GrouchoMaskLayer extends RenderLayer<@NotNull AvatarRenderState, @NotNull PlayerModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("skyblockutils", "textures/entity/groucho_mask.png");

    private final PlayerModel model;

    public GrouchoMaskLayer(RenderLayerParent<@NotNull AvatarRenderState, @NotNull PlayerModel> renderer) {
        super(renderer);
        this.model = new PlayerModel(createLayer().bakeRoot(), false);
    }

    public static void register() {
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((_, renderer, helper, _) -> {
            if (renderer instanceof AvatarRenderer<?> avatar) helper.register(new GrouchoMaskLayer(avatar));
        });
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        PartDefinition root = mesh.getRoot().clearRecursively();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("groucho_glasses",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(1.1F)),
                PartPose.ZERO);
        head.addOrReplaceChild("groucho_nose",
                CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, -4.0F, -7.0F, 2.0F, 3.0F, 2.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void submit(@NotNull PoseStack poseStack, @NotNull SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        if (!((SSURenderState) state).ssu$isGroucho() || state.isInvisible) return;
        int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        submitNodeCollector.submitModel(
                this.model, state, poseStack, RenderTypes.entityCutout(TEXTURE), lightCoords, overlayCoords, state.outlineColor, null
        );
    }
}
