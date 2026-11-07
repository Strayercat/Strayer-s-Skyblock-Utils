package com.skyblockutils.render;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.utils.SideBarUtils;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class SkullRefresher {
    private static final int DELAY_TICKS = 5;
    private static final Long2IntOpenHashMap PENDING = new Long2IntOpenHashMap();
    private static final int LOCATION_TIMEOUT_TICKS = 100;
    private static boolean wasScanning = false;
    private static @Nullable ClientLevel lastLevel;
    private static @Nullable String staleLocation;
    private static int staleTicks = 0;

    private SkullRefresher() {
    }

    public static void tick(Minecraft client) {
        EggDetector.tick(client);
        trackLevel(client.level);
        if (staleLocation != null && (!SideBarUtils.location.equals(staleLocation) || --staleTicks <= 0)) staleLocation = null;

        boolean scanning = canScan(client.level);
        if (scanning && !wasScanning) rescanLoadedChunks(client);
        wasScanning = scanning;

        if (PENDING.isEmpty()) return;
        if (client.level == null) {
            PENDING.clear();
            return;
        }
        ObjectIterator<Long2IntMap.Entry> it = PENDING.long2IntEntrySet().iterator();
        while (it.hasNext()) {
            Long2IntMap.Entry e = it.next();
            int left = e.getIntValue() - 1;
            if (left > 0) {
                e.setValue(left);
                continue;
            }
            long sec = e.getLongKey();
            client.level.setSectionDirtyWithNeighbors(SectionPos.x(sec), SectionPos.y(sec), SectionPos.z(sec));
            it.remove();
        }
    }

    private static void rescanLoadedChunks(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || client.player == null) return;

        int radius = client.options.getEffectiveRenderDistance();
        int centerX = SectionPos.blockToSectionCoord(client.player.getBlockX());
        int centerZ = SectionPos.blockToSectionCoord(client.player.getBlockZ());

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(centerX + dx, centerZ + dz, false);
                if (chunk == null) continue;
                for (BlockEntity be : List.copyOf(chunk.getBlockEntities().values())) handleBELoad(be);
                handleChunkLoad(level, chunk);
            }
        }
    }

    public static void handleChunkLoad(ClientLevel level, LevelChunk chunk) {
        if (!canScan(level)) return;
        LevelChunkSection[] sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(EggDetector::isPlayerHead)) continue;
            int baseY = SectionPos.sectionToBlockCoord(chunk.getMinSectionY() + i);
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (!EggDetector.isPlayerHead(section.getBlockState(x, y, z))) continue;
                        pos.set(baseX + x, baseY + y, baseZ + z);
                        if (chunk.getBlockEntities().containsKey(pos)) continue;
                        if (level.getBlockEntity(pos.immutable()) instanceof SkullBlockEntity skull) {
                            if (skull.getOwnerProfile() == null) ((SkullOwnerAccess) skull).ssu$setOwner(EggDetector.EGG_PROFILE);
                            handleBELoad(skull);
                        }
                    }
                }
            }
        }
    }

    public static void handleBELoad(BlockEntity be) {
        if (!(be instanceof SkullBlockEntity skull)) return;
        if (!(be.getLevel() instanceof ClientLevel level) || !canScan(level)) return;
        EggDetector.check(skull);
        PENDING.put(SectionPos.asLong(be.getBlockPos()), DELAY_TICKS);
    }

    private static void trackLevel(@Nullable ClientLevel level) {
        if (level == lastLevel) return;
        lastLevel = level;
        staleLocation = SideBarUtils.location;
        staleTicks = LOCATION_TIMEOUT_TICKS;
        wasScanning = false;
        PENDING.clear();
    }

    private static boolean canScan(@Nullable ClientLevel level) {
        if (level == null || !StrayersSkyblockUtilsClient.isOnHypixel) return false;
        trackLevel(level);
        if (staleLocation != null) return false;
        String location = SideBarUtils.location;
        return !location.isEmpty() && ModFunctions.mapLocationToGeneralArea(location).isEmpty();
    }

    public static void handleBEUnload(BlockEntity be) {
        if (be instanceof SkullBlockEntity) EggDetector.remove(be);
    }
}