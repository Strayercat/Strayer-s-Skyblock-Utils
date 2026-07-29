package com.skyblockutils.features.textures;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.mixin.client.BossHealthOverlayAccessor;
import com.skyblockutils.mixin.client.ModelManagerAccessor;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class F7VoidLava {
    private static final Identifier STILL_ID =
            Identifier.fromNamespaceAndPath("skyblockutils", "block/void_lava_still");
    private static final Identifier FLOW_ID =
            Identifier.fromNamespaceAndPath("skyblockutils", "block/void_lava_flow");

    private static TextureAtlasSprite voidStillSprite;
    private static TextureAtlasSprite voidFlowSprite;

    public static void register() {
        Material vanillaStillMat = new Material(Identifier.fromNamespaceAndPath("minecraft", "block/lava_still"));
        Material vanillaFlowMat = new Material(Identifier.fromNamespaceAndPath("minecraft", "block/lava_flow"));
        FluidModel.Unbaked passthroughModel = new FluidModel.Unbaked(vanillaStillMat, vanillaFlowMat, null, null);

        FluidRenderHandler handler = new FluidRenderHandler() {
            @Override
            public void renderFluid(@NotNull FluidRenderer fluidRenderer, @NotNull BlockPos pos, @NotNull BlockAndTintGetter level,
                                    FluidRenderer.@NotNull Output output, @NotNull BlockState blockState, @NotNull FluidState fluidState) {
                if (!shouldOverride()) {
                    FluidRenderHandler.super.renderFluid(fluidRenderer, pos, level, output, blockState, fluidState);
                    return;
                }
                resolveSprites();
                if (voidStillSprite == null || voidFlowSprite == null) {
                    FluidRenderHandler.super.renderFluid(fluidRenderer, pos, level, output, blockState, fluidState);
                    return;
                }
                FluidModel model = fluidRenderer.fluidModels.get(fluidState);
                tesselateVoidLava(level, pos, output, blockState, fluidState, model);
            }
        };

        FluidRenderingRegistry.register(Fluids.LAVA, Fluids.FLOWING_LAVA, passthroughModel, handler);
    }

    private static void resolveSprites() {
        if (voidStillSprite != null) return;
        AtlasManager atlasManager = ((ModelManagerAccessor) Minecraft.getInstance().getModelManager()).getAtlasManager();
        TextureAtlas atlas = atlasManager.getAtlasOrThrow(AtlasIds.BLOCKS);
        voidStillSprite = atlas.getSprite(STILL_ID);
        voidFlowSprite = atlas.getSprite(FLOW_ID);
    }

    private static boolean shouldOverride() {
        Optional<String> bossBarTitle = ((BossHealthOverlayAccessor) Minecraft.getInstance().gui.hud.getBossOverlay())
                .getEvents().values().stream().findFirst()
                .map(bossBar -> bossBar.getName().getString().replaceAll("§.", ""));

        List<String> allowedTitles = List.of("Maxor", "Storm", "Goldor", "Necron");

        boolean isInF7OrM7 = bossBarTitle.filter(allowedTitles::contains).isPresent();

        return ModConfig.INSTANCE.f7VoidLava && isInF7OrM7;
    }

    private static void tesselateVoidLava(BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output,
                                          BlockState blockState, FluidState fluidState, FluidModel model) {
        BlockState blockStateDown = level.getBlockState(pos.relative(Direction.DOWN));
        FluidState fluidStateDown = blockStateDown.getFluidState();
        BlockState blockStateUp = level.getBlockState(pos.relative(Direction.UP));
        FluidState fluidStateUp = blockStateUp.getFluidState();
        BlockState blockStateNorth = level.getBlockState(pos.relative(Direction.NORTH));
        FluidState fluidStateNorth = blockStateNorth.getFluidState();
        BlockState blockStateSouth = level.getBlockState(pos.relative(Direction.SOUTH));
        FluidState fluidStateSouth = blockStateSouth.getFluidState();
        BlockState blockStateWest = level.getBlockState(pos.relative(Direction.WEST));
        FluidState fluidStateWest = blockStateWest.getFluidState();
        BlockState blockStateEast = level.getBlockState(pos.relative(Direction.EAST));
        FluidState fluidStateEast = blockStateEast.getFluidState();

        boolean renderUp = !fluidStateUp.getType().isSame(fluidState.getType());
        boolean renderDown = FluidRenderer.shouldRenderFace(fluidState, blockState, Direction.DOWN, fluidStateDown);
        boolean renderNorth = FluidRenderer.shouldRenderFace(fluidState, blockState, Direction.NORTH, fluidStateNorth);
        boolean renderSouth = FluidRenderer.shouldRenderFace(fluidState, blockState, Direction.SOUTH, fluidStateSouth);
        boolean renderWest = FluidRenderer.shouldRenderFace(fluidState, blockState, Direction.WEST, fluidStateWest);
        boolean renderEast = FluidRenderer.shouldRenderFace(fluidState, blockState, Direction.EAST, fluidStateEast);

        if (!(renderUp || renderDown || renderEast || renderWest || renderNorth || renderSouth)) return;

        VertexConsumer builder = output.getBuilder(model.layer());
        int tintColor = model.tintSource() != null ? model.tintSource().colorInWorld(blockState, level, pos) : -1;
        CardinalLighting cardinalLighting = level.cardinalLighting();
        Fluid type = fluidState.getType();

        float heightSelf = getHeight(level, type, pos, blockState, fluidState);
        float heightNorthEast, heightNorthWest, heightSouthEast, heightSouthWest;
        if (heightSelf >= 1.0F) {
            heightNorthEast = heightNorthWest = heightSouthEast = heightSouthWest = 1.0F;
        } else {
            float heightNorth = getHeight(level, type, pos.north(), blockStateNorth, fluidStateNorth);
            float heightSouth = getHeight(level, type, pos.south(), blockStateSouth, fluidStateSouth);
            float heightEast = getHeight(level, type, pos.east(), blockStateEast, fluidStateEast);
            float heightWest = getHeight(level, type, pos.west(), blockStateWest, fluidStateWest);
            heightNorthEast = calculateAverageHeight(level, type, heightSelf, heightNorth, heightEast, pos.relative(Direction.NORTH).relative(Direction.EAST));
            heightNorthWest = calculateAverageHeight(level, type, heightSelf, heightNorth, heightWest, pos.relative(Direction.NORTH).relative(Direction.WEST));
            heightSouthEast = calculateAverageHeight(level, type, heightSelf, heightSouth, heightEast, pos.relative(Direction.SOUTH).relative(Direction.EAST));
            heightSouthWest = calculateAverageHeight(level, type, heightSelf, heightSouth, heightWest, pos.relative(Direction.SOUTH).relative(Direction.WEST));
        }

        float x = pos.getX() & 15;
        float y = pos.getY() & 15;
        float z = pos.getZ() & 15;
        float bottomOffs = renderDown ? 0.001F : 0.0F;

        if (renderUp) {
            heightNorthWest -= 0.001F;
            heightSouthWest -= 0.001F;
            heightSouthEast -= 0.001F;
            heightNorthEast -= 0.001F;
            Vec3 flow = fluidState.getFlow(level, pos);
            float u00, u01, u10, u11, v00, v01, v10, v11;
            if (flow.x == 0.0 && flow.z == 0.0) {
                u00 = voidStillSprite.getU0();
                v00 = voidStillSprite.getV0();
                u01 = u00;
                v01 = voidStillSprite.getV1();
                u10 = voidStillSprite.getU1();
                v10 = v01;
                u11 = u10;
                v11 = v00;
            } else {
                float angle = (float) Mth.atan2(flow.z, flow.x) - (float) (Math.PI / 2);
                float s = Mth.sin(angle) * 0.25F;
                float c = Mth.cos(angle) * 0.25F;
                u00 = voidFlowSprite.getU(0.5F + (-c - s));
                v00 = voidFlowSprite.getV(0.5F + (-c + s));
                u01 = voidFlowSprite.getU(0.5F + (-c + s));
                v01 = voidFlowSprite.getV(0.5F + (c + s));
                u10 = voidFlowSprite.getU(0.5F + (c + s));
                v10 = voidFlowSprite.getV(0.5F + (c - s));
                u11 = voidFlowSprite.getU(0.5F + (c - s));
                v11 = voidFlowSprite.getV(0.5F + (-c - s));
            }
            int topLightCoords = getLightCoords(level, pos);
            int topColor = ARGB.scaleRGB(tintColor, cardinalLighting.up());
            addFace(builder,
                    x, y + heightNorthWest, z, u00, v00,
                    x, y + heightSouthWest, z + 1.0F, u01, v01,
                    x + 1.0F, y + heightSouthEast, z + 1.0F, u10, v10,
                    x + 1.0F, y + heightNorthEast, z, u11, v11,
                    topColor, topLightCoords, fluidState.shouldRenderBackwardUpFace(level, pos.above()));
        }

        if (renderDown) {
            float u0 = voidStillSprite.getU0(), u1 = voidStillSprite.getU1();
            float v0 = voidStillSprite.getV0(), v1 = voidStillSprite.getV1();
            int belowLightCoords = getLightCoords(level, pos.below());
            int belowColor = ARGB.scaleRGB(tintColor, cardinalLighting.down());
            addFace(builder,
                    x, y + bottomOffs, z, u0, v0,
                    x + 1.0F, y + bottomOffs, z, u1, v0,
                    x + 1.0F, y + bottomOffs, z + 1.0F, u1, v1,
                    x, y + bottomOffs, z + 1.0F, u0, v1,
                    belowColor, belowLightCoords, false);
        }

        int sideLightCoords = getLightCoords(level, pos);
        for (Direction faceDir : Plane.HORIZONTAL) {
            float hh0, hh1, x0, z0, x1, z1;
            boolean renderCondition;
            switch (faceDir) {
                case NORTH -> {
                    hh0 = heightNorthWest;
                    hh1 = heightNorthEast;
                    x0 = x;
                    x1 = x + 1.0F;
                    z0 = z + 0.001F;
                    z1 = z + 0.001F;
                    renderCondition = renderNorth;
                }
                case SOUTH -> {
                    hh0 = heightSouthEast;
                    hh1 = heightSouthWest;
                    x0 = x + 1.0F;
                    x1 = x;
                    z0 = z + 1.0F - 0.001F;
                    z1 = z + 1.0F - 0.001F;
                    renderCondition = renderSouth;
                }
                case WEST -> {
                    hh0 = heightSouthWest;
                    hh1 = heightNorthWest;
                    x0 = x + 0.001F;
                    x1 = x + 0.001F;
                    z0 = z + 1.0F;
                    z1 = z;
                    renderCondition = renderWest;
                }
                case EAST -> {
                    hh0 = heightNorthEast;
                    hh1 = heightSouthEast;
                    x0 = x + 1.0F - 0.001F;
                    x1 = x + 1.0F - 0.001F;
                    z0 = z;
                    z1 = z + 1.0F;
                    renderCondition = renderEast;
                }
                default -> throw new UnsupportedOperationException();
            }
            if (renderCondition) {
                float u0 = voidFlowSprite.getU(0.0F), u1 = voidFlowSprite.getU(0.5F);
                float v01 = voidFlowSprite.getV((1.0F - hh0) * 0.5F), v02 = voidFlowSprite.getV((1.0F - hh1) * 0.5F);
                float v1 = voidFlowSprite.getV(0.5F);
                float shadeSide = faceDir.getAxis() == Axis.Z ? cardinalLighting.north() : cardinalLighting.west();
                int faceColor = ARGB.scaleRGB(tintColor, cardinalLighting.up() * shadeSide);
                addFace(builder,
                        x0, y + hh0, z0, u0, v01,
                        x1, y + hh1, z1, u1, v02,
                        x1, y + bottomOffs, z1, u1, v1,
                        x0, y + bottomOffs, z0, u0, v1,
                        faceColor, sideLightCoords, true);
            }
        }
    }

    private static void addFace(VertexConsumer builder,
                                float x0, float y0, float z0, float u0, float v0,
                                float x1, float y1, float z1, float u1, float v1,
                                float x2, float y2, float z2, float u2, float v2,
                                float x3, float y3, float z3, float u3, float v3,
                                int color, int lightCoords, boolean addBackFace) {
        vertex(builder, x0, y0, z0, color, u0, v0, lightCoords);
        vertex(builder, x1, y1, z1, color, u1, v1, lightCoords);
        vertex(builder, x2, y2, z2, color, u2, v2, lightCoords);
        vertex(builder, x3, y3, z3, color, u3, v3, lightCoords);
        if (addBackFace) {
            vertex(builder, x0, y0, z0, color, u0, v0, lightCoords);
            vertex(builder, x3, y3, z3, color, u3, v3, lightCoords);
            vertex(builder, x2, y2, z2, color, u2, v2, lightCoords);
            vertex(builder, x1, y1, z1, color, u1, v1, lightCoords);
        }
    }

    private static void vertex(VertexConsumer builder, float x, float y, float z, int color, float u, float v, int lightCoords) {
        builder.addVertex(x, y, z, color, u, v, OverlayTexture.NO_OVERLAY, lightCoords, 0.0F, 1.0F, 0.0F);
    }

    private static int getLightCoords(BlockAndTintGetter level, BlockPos pos) {
        return LightCoordsUtil.max(LightCoordsUtil.getLightCoords(level, pos), LightCoordsUtil.getLightCoords(level, pos.above()));
    }

    private static float calculateAverageHeight(BlockAndTintGetter level, Fluid type, float heightSelf, float height2, float height1, BlockPos cornerPos) {
        if (!(height1 >= 1.0F) && !(height2 >= 1.0F)) {
            float[] weighted = new float[2];
            if (height1 > 0.0F || height2 > 0.0F) {
                float heightCorner = getHeight(level, type, cornerPos);
                if (heightCorner >= 1.0F) return 1.0F;
                addWeightedHeight(weighted, heightCorner);
            }
            addWeightedHeight(weighted, heightSelf);
            addWeightedHeight(weighted, height1);
            addWeightedHeight(weighted, height2);
            return weighted[0] / weighted[1];
        }
        return 1.0F;
    }

    private static void addWeightedHeight(float[] weighted, float height) {
        if (height >= 0.8F) {
            weighted[0] += height * 10.0F;
            weighted[1] += 10.0F;
        } else if (height >= 0.0F) {
            weighted[0] += height;
            weighted[1]++;
        }
    }

    private static float getHeight(BlockAndTintGetter level, Fluid fluidType, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return getHeight(level, fluidType, pos, state, state.getFluidState());
    }

    private static float getHeight(BlockAndTintGetter level, Fluid fluidType, BlockPos pos, BlockState state, FluidState fluidState) {
        if (fluidType.isSame(fluidState.getType())) {
            BlockState aboveState = level.getBlockState(pos.above());
            return fluidType.isSame(aboveState.getFluidState().getType()) ? 1.0F : fluidState.getOwnHeight();
        }
        return !state.isSolid() ? 0.0F : -1.0F;
    }
}