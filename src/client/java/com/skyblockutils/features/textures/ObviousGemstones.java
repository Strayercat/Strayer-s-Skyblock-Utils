package com.skyblockutils.features.textures;

import com.skyblockutils.hypixel.HypixelLocation;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.mixin.client.ModelManagerAccessor;
import com.skyblockutils.utils.SideBarUtils;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class ObviousGemstones {
    public enum Gem {
        RUBY("red", Blocks.STAINED_GLASS.red(), Blocks.STAINED_GLASS_PANE.red()),
        AMBER("orange", Blocks.STAINED_GLASS.orange(), Blocks.STAINED_GLASS_PANE.orange()),
        SAPPHIRE("light_blue", Blocks.STAINED_GLASS.lightBlue(), Blocks.STAINED_GLASS_PANE.lightBlue()),
        JADE("lime", Blocks.STAINED_GLASS.lime(), Blocks.STAINED_GLASS_PANE.lime()),
        AMETHYST("purple", Blocks.STAINED_GLASS.purple(), Blocks.STAINED_GLASS_PANE.purple()),
        TOPAZ("yellow", Blocks.STAINED_GLASS.yellow(), Blocks.STAINED_GLASS_PANE.yellow()),
        JASPER("magenta", Blocks.STAINED_GLASS.magenta(), Blocks.STAINED_GLASS_PANE.magenta()),
        OPAL("white", Blocks.STAINED_GLASS.white(), Blocks.STAINED_GLASS_PANE.white()),
        AQUAMARINE("blue", Blocks.STAINED_GLASS.blue(), Blocks.STAINED_GLASS_PANE.blue()),
        CITRINE("brown", Blocks.STAINED_GLASS.brown(), Blocks.STAINED_GLASS_PANE.brown()),
        ONYX("black", Blocks.STAINED_GLASS.black(), Blocks.STAINED_GLASS_PANE.black()),
        PERIDOT("green", Blocks.STAINED_GLASS.green(), Blocks.STAINED_GLASS_PANE.green());

        final Block glass;
        final Block pane;
        final Identifier vanillaGlass;
        final Identifier vanillaPaneTop;
        final Identifier gemTexture;
        final Identifier gemPaneTexture;

        Gem(String color, Block glass, Block pane) {
            String name = name().toLowerCase(java.util.Locale.ROOT);
            this.glass = glass;
            this.pane = pane;
            this.vanillaGlass = Identifier.fromNamespaceAndPath("minecraft", "block/" + color + "_stained_glass");
            this.vanillaPaneTop = Identifier.fromNamespaceAndPath("minecraft", "block/" + color + "_stained_glass_pane_top");
            this.gemTexture = Identifier.fromNamespaceAndPath("skyblockutils", "block/" + name);
            this.gemPaneTexture = Identifier.fromNamespaceAndPath("skyblockutils", "block/" + name + "_pane");
        }
    }

    private static final Set<Gem> CRYSTAL_HOLLOWS = Collections.unmodifiableSet(EnumSet.of(
            Gem.TOPAZ, Gem.JADE, Gem.AMBER, Gem.RUBY, Gem.SAPPHIRE, Gem.AMETHYST, Gem.JASPER));
    private static final Set<Gem> TUNNELS = Collections.unmodifiableSet(EnumSet.of(
            Gem.AQUAMARINE, Gem.CITRINE, Gem.ONYX, Gem.PERIDOT));
    private static final Set<Gem> CRIMSON_ISLE = Collections.unmodifiableSet(EnumSet.of(Gem.OPAL));
    private static final Set<Gem> ALL = Collections.unmodifiableSet(EnumSet.allOf(Gem.class));
    private static final Set<Gem> NONE = Collections.unmodifiableSet(EnumSet.noneOf(Gem.class));

    private static final int REBUILD_STEP = 3;

    private static final Map<Identifier, TextureAtlasSprite> spriteCache = new ConcurrentHashMap<>();
    private static volatile Set<Gem> activeGems = NONE;

    private ObviousGemstones() {
    }

    public static void register() {
        Map<Block, Gem> glassBlocks = new IdentityHashMap<>();
        Map<Block, Gem> paneBlocks = new IdentityHashMap<>();
        for (Gem gem : Gem.values()) {
            glassBlocks.put(gem.glass, gem);
            paneBlocks.put(gem.pane, gem);
        }

        ModelLoadingPlugin.register(context -> {
            spriteCache.clear();
            context.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, ctx) -> {
                Block block = ctx.state().getBlock();
                Gem gem = glassBlocks.get(block);
                if (gem != null) return new GemstoneModel(model, gem);
                gem = paneBlocks.get(block);
                if (gem != null) return new GemstoneModel(model, gem);
                return model;
            });
        });
    }

    public static void tick(Minecraft client) {
        Set<Gem> wanted = computeActiveGems();
        if (wanted.equals(activeGems)) return;
        activeGems = wanted;
        rebuildLoadedSections(client);
    }

    private static Set<Gem> computeActiveGems() {
        if (!ModConfig.INSTANCE.obviousGemstones || Minecraft.getInstance().level == null) return NONE;

        if (!HypixelLocation.isSkyBlock()) return NONE;
        if (HypixelLocation.isInMineshaft()) return ALL;

        return switch (HypixelLocation.map()) {
            case "Crystal Hollows" -> CRYSTAL_HOLLOWS;
            case "Crimson Isle" -> CRIMSON_ISLE;
            case "Dwarven Mines" -> SideBarUtils.location.equals("Glacite Tunnels") ? TUNNELS : NONE;
            default -> NONE;
        };
    }

    private static void rebuildLoadedSections(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || client.player == null) return;

        int radius = client.options.getEffectiveRenderDistance() + 1;
        int centerX = SectionPos.blockToSectionCoord(client.player.getBlockX());
        int centerZ = SectionPos.blockToSectionCoord(client.player.getBlockZ());

        for (int x = centerX - radius; x <= centerX + radius; x += REBUILD_STEP) {
            for (int z = centerZ - radius; z <= centerZ + radius; z += REBUILD_STEP) {
                for (int y = level.getMinSectionY(); y <= level.getMaxSectionY(); y += REBUILD_STEP) {
                    level.setSectionDirtyWithNeighbors(x, y, z);
                }
            }
        }
    }

    private static TextureAtlasSprite sprite(Identifier id) {
        return spriteCache.computeIfAbsent(id, key -> ((ModelManagerAccessor) Minecraft.getInstance().getModelManager())
                .getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(key));
    }

    private static final class GemstoneModel implements BlockStateModel {
        private final BlockStateModel delegate;
        private final Gem gem;
        private final Map<BlockStateModelPart, GemstonePart> parts = new ConcurrentHashMap<>();

        GemstoneModel(BlockStateModel delegate, Gem gem) {
            this.delegate = delegate;
            this.gem = gem;
        }

        private boolean active() {
            return activeGems.contains(gem);
        }

        @Override
        public void collectParts(@NotNull RandomSource random, @NotNull List<BlockStateModelPart> output) {
            if (!active()) {
                delegate.collectParts(random, output);
                return;
            }
            List<BlockStateModelPart> original = new ArrayList<>();
            delegate.collectParts(random, original);
            for (BlockStateModelPart part : original) output.add(parts.computeIfAbsent(part, p -> new GemstonePart(p, gem)));
        }

        @Override
        public void emitQuads(@NotNull QuadEmitter emitter, @NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull RandomSource random,
                              @NotNull Predicate<@Nullable Direction> cullTest) {
            if (active()) {
                BlockStateModel.super.emitQuads(emitter, level, pos, state, random, cullTest);
            } else {
                delegate.emitQuads(emitter, level, pos, state, random, cullTest);
            }
        }

        @Override
        public @Nullable Object createGeometryKey(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull RandomSource random) {
            return active() ? null : delegate.createGeometryKey(level, pos, state, random);
        }

        @Override
        public Material.@NotNull Baked particleMaterial() {
            return delegate.particleMaterial();
        }

        @Override
        public Material.@NotNull  Baked particleMaterial(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state) {
            return delegate.particleMaterial(level, pos, state);
        }

        @Override
        public @BakedQuad.MaterialFlags int materialFlags() {
            return delegate.materialFlags();
        }

        @Override
        public @BakedQuad.MaterialFlags int materialFlags(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull RandomSource random) {
            return delegate.materialFlags(level, pos, state, random);
        }
    }

    private static final class GemstonePart implements BlockStateModelPart {
        private final BlockStateModelPart delegate;
        private final Gem gem;
        private final List<BakedQuad> generalQuads;
        private final Map<Direction, List<BakedQuad>> sideQuads = new EnumMap<>(Direction.class);

        GemstonePart(BlockStateModelPart delegate, Gem gem) {
            this.delegate = delegate;
            this.gem = gem;
            this.generalQuads = retexture(delegate.getQuads(null));
            for (Direction direction : Direction.values()) sideQuads.put(direction, retexture(delegate.getQuads(direction)));
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null ? generalQuads : sideQuads.get(direction);
        }

        private List<BakedQuad> retexture(List<BakedQuad> quads) {
            if (quads.isEmpty()) return quads;
            TextureAtlasSprite glass = sprite(gem.vanillaGlass);
            TextureAtlasSprite paneTop = sprite(gem.vanillaPaneTop);
            List<BakedQuad> result = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                TextureAtlasSprite current = quad.materialInfo().sprite();
                if (current == paneTop) result.add(remap(quad, sprite(gem.gemPaneTexture)));
                else if (current == glass) result.add(remap(quad, sprite(gem.gemTexture)));
                else result.add(quad);
            }
            return result;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return delegate.useAmbientOcclusion();
        }

        @Override
        public Material.@NotNull Baked particleMaterial() {
            return delegate.particleMaterial();
        }

        @Override
        public @BakedQuad.MaterialFlags int materialFlags() {
            return delegate.materialFlags();
        }
    }

    private static BakedQuad remap(BakedQuad quad, TextureAtlasSprite target) {
        TextureAtlasSprite source = quad.materialInfo().sprite();
        boolean uHigh;
        if (fits(quad, source, true)) uHigh = true;
        else if (fits(quad, source, false)) uHigh = false;
        else return quad;

        long[] uvs = new long[4];
        for (int i = 0; i < 4; i++) {
            long packed = quad.packedUV(i);
            float u = uHigh ? high(packed) : low(packed);
            float v = uHigh ? low(packed) : high(packed);
            float newU = target.getU(fraction(u, source.getU(0f), source.getU(1f)));
            float newV = target.getV(fraction(v, source.getV(0f), source.getV(1f)));
            uvs[i] = uHigh ? pack(newU, newV) : pack(newV, newU);
        }

        BakedQuad.MaterialInfo info = quad.materialInfo();
        BakedQuad.MaterialInfo newInfo = new BakedQuad.MaterialInfo(target, info.layer(), info.itemRenderType(),
                info.tintIndex(), info.shade(), info.lightEmission());
        return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                uvs[0], uvs[1], uvs[2], uvs[3], quad.direction(), newInfo);
    }

    private static boolean fits(BakedQuad quad, TextureAtlasSprite sprite, boolean uHigh) {
        for (int i = 0; i < 4; i++) {
            long packed = quad.packedUV(i);
            float u = uHigh ? high(packed) : low(packed);
            float v = uHigh ? low(packed) : high(packed);
            if (within(u, sprite.getU(0f), sprite.getU(1f)) || within(v, sprite.getV(0f), sprite.getV(1f))) return false;
        }
        return true;
    }

    private static boolean within(float value, float a, float b) {
        float epsilon = 1.0E-4f;
        return !(value >= Math.min(a, b) - epsilon) || !(value <= Math.max(a, b) + epsilon);
    }

    private static float fraction(float value, float start, float end) {
        return end == start ? 0f : (value - start) / (end - start);
    }

    private static float high(long packed) {
        return Float.intBitsToFloat((int) (packed >>> 32));
    }

    private static float low(long packed) {
        return Float.intBitsToFloat((int) packed);
    }

    private static long pack(float high, float low) {
        return ((long) Float.floatToRawIntBits(high) << 32) | (Float.floatToRawIntBits(low) & 0xFFFFFFFFL);
    }
}
