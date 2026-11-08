package com.skyblockutils.render;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.skyblockutils.hypixel.HypixelLocation;
import com.skyblockutils.mixin.client.SkullBlockEntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.storage.TagValueInput;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class HeadRestorer {
    private static final String EGG_TEXTURE_URL = "http://textures.minecraft.net/texture/98c5530c2c856e30b11fb22c711117b073fd8333ae06c9fd1c697c4af4a0ccc9";
    private static final ResolvableProfile EGG_PROFILE = createEggProfile();

    private static final Set<BlockPos> AWAITING = new HashSet<>();
    private static @Nullable ClientLevel trackedLevel;

    private HeadRestorer() {
    }

    public static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null) {
            AWAITING.clear();
            return;
        }
        track(level);
        if (!HypixelLocation.isKnown() || AWAITING.isEmpty()) return;

        List<BlockPos> heads = new ArrayList<>(AWAITING);
        AWAITING.clear();
        if (!HypixelLocation.isOnPrivateIsland()) return;
        for (BlockPos pos : heads) applyEgg(level, pos);
    }

    public static void onSkullData(ClientLevel level, BlockPos pos, CompoundTag tag) {
        if (level.getBlockEntity(pos) instanceof SkullBlockEntity skull && skull.getOwnerProfile() == EGG_PROFILE) {
            skull.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
            markDirty(level, pos);
        }
    }

    public static void onChunkLoad(ClientLevel level, LevelChunk chunk) {
        track(level);

        LevelChunkSection[] sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(HeadRestorer::isPlayerHead)) continue;
            int baseY = SectionPos.sectionToBlockCoord(chunk.getMinSectionY() + i);
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (!isPlayerHead(section.getBlockState(x, y, z))) continue;
                        pos.set(baseX + x, baseY + y, baseZ + z);
                        BlockEntity existing = chunk.getBlockEntities().get(pos);
                        if (existing != null && (!(existing instanceof SkullBlockEntity skull) || skull.getOwnerProfile() != null)) continue;
                        AWAITING.add(pos.immutable());
                    }
                }
            }
        }
    }

    private static void applyEgg(ClientLevel level, BlockPos pos) {
        if (!isPlayerHead(level.getBlockState(pos))) return;
        LevelChunk chunk = level.getChunkAt(pos);
        if (!(chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE) instanceof SkullBlockEntity skull)) return;
        if (skull.getOwnerProfile() != null) return;
        ((SkullBlockEntityAccessor) skull).ssu$setOwner(EGG_PROFILE);
        markDirty(level, pos);
    }

    private static void markDirty(ClientLevel level, BlockPos pos) {
        level.setSectionDirtyWithNeighbors(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    private static void track(ClientLevel level) {
        if (level == trackedLevel) return;
        trackedLevel = level;
        AWAITING.clear();
    }

    private static boolean isPlayerHead(BlockState state) {
        return state.is(Blocks.PLAYER_HEAD) || state.is(Blocks.PLAYER_WALL_HEAD);
    }

    private static ResolvableProfile createEggProfile() {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + EGG_TEXTURE_URL + "\"}}}";
        String value = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", value)));
        UUID id = UUID.nameUUIDFromBytes(EGG_TEXTURE_URL.getBytes(StandardCharsets.UTF_8));
        return ResolvableProfile.createResolved(new GameProfile(id, "SSUEgg", properties));
    }
}
