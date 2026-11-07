package com.skyblockutils.features.textures;

import com.skyblockutils.config.ModConfig;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.stream.IntStream;

public final class CustomBreakingAnimation {
    private static final List<RenderType> DESTROY_TYPES = IntStream.range(0, ModelBakery.DESTROY_STAGE_COUNT)
            .mapToObj(i -> RenderTypes.crumbling(Identifier.fromNamespaceAndPath("skyblockutils", "textures/block/destroy_stage_" + i + ".png")))
            .toList();

    private CustomBreakingAnimation() {
    }

    public static RenderType replace(RenderType original, int progress) {
        if (!ModConfig.INSTANCE.customBreakingAnimation || progress < 0 || progress >= DESTROY_TYPES.size()) return original;
        return DESTROY_TYPES.get(progress);
    }
}
