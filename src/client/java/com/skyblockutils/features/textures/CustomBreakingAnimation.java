package com.skyblockutils.features.textures;

import com.skyblockutils.config.ModConfig;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.stream.IntStream;

public final class CustomBreakingAnimation {
    private static final List<RenderType> VANILLA = List.copyOf(ModelBakery.DESTROY_TYPES);
    private static final List<RenderType> CUSTOM = IntStream.range(0, ModelBakery.DESTROY_STAGE_COUNT)
            .mapToObj(i -> RenderTypes.crumbling(Identifier.fromNamespaceAndPath("skyblockutils", "textures/block/destroy_stage_" + i + ".png")))
            .toList();
    private static boolean applied = false;

    private CustomBreakingAnimation() {
    }

    public static void tick() {
        boolean wanted = ModConfig.INSTANCE.customBreakingAnimation;
        if (wanted == applied) return;
        List<RenderType> source = wanted ? CUSTOM : VANILLA;
        for (int i = 0; i < source.size(); i++) ModelBakery.DESTROY_TYPES.set(i, source.get(i));
        applied = wanted;
    }
}
