package com.skyblockutils.features.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;

public class GemstonePaneFix {
    public static BlockPos latestGemBreaking = null;

    public static boolean posEquals(BlockPos pos1, BlockPos pos2) {
        if (pos1 == null || pos2 == null) return false;
        return pos1.getX() == pos2.getX() && pos1.getY() == pos2.getY() && pos1.getZ() == pos2.getZ();
    }

    public static boolean isDisconnectedPane(BlockState state, Direction toIgnore) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction == toIgnore) continue;
            if (state.getValue(CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                return false;
            }
        }
        return true;
    }

    public static BlockState createFullConnectedPane(BlockState state) {
        BlockState newState = state;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            newState = newState.setValue(CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(direction), true);
        }
        return newState;
    }
}