package com.skyblockutils.render;

import com.skyblockutils.StrayersSkyblockUtilsClient;
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

public final class SkullRefresher {
    private static final int DELAY_TICKS = 5;
    private static final Long2IntOpenHashMap PENDING = new Long2IntOpenHashMap();

    private SkullRefresher() {
    }

    public static void tick(Minecraft client) {
        EggDetector.tick(client);
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

    public static void handleChunkLoad(ClientLevel level, LevelChunk chunk) {
        if (!StrayersSkyblockUtilsClient.isOnHypixel) return;
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
        if (!StrayersSkyblockUtilsClient.isOnHypixel) return;
        if (!(be instanceof SkullBlockEntity skull)) return;
        EggDetector.check(skull);
        PENDING.put(SectionPos.asLong(be.getBlockPos()), DELAY_TICKS);
    }

    public static void handleBEUnload(BlockEntity be) {
        if (be instanceof SkullBlockEntity) EggDetector.remove(be);
    }
}
