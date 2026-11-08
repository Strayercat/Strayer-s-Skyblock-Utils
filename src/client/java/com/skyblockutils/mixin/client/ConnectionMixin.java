package com.skyblockutils.mixin.client;

import com.skyblockutils.features.mining.GemstonePaneFix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Unique
    private static BlockPos lastUpdatedToFullConnectedPane = null;

    @Inject(method = "genericsFtw", at = @At("HEAD"))
    private static <T extends PacketListener> void onHandlePacket(Packet<@NotNull T> packet, PacketListener listener, CallbackInfo ci) {
        if (GemstonePaneFix.latestGemBreaking == null) return;
        if (!(packet instanceof ClientboundBlockUpdatePacket blockUpdatePacket)) return;
        if (!GemstonePaneFix.posEquals(GemstonePaneFix.latestGemBreaking, blockUpdatePacket.getPos())) return;
        if (!blockUpdatePacket.getBlockState().isAir()) return;

        BlockPos brokenPos = blockUpdatePacket.getPos();
        Minecraft minecraft = Minecraft.getInstance();

        minecraft.execute(() -> {
            ClientLevel level = minecraft.level;
            if (level == null) return;

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighborPos = brokenPos.relative(direction);
                BlockState neighborState = level.getBlockState(neighborPos);
                if (!(neighborState.getBlock() instanceof IronBarsBlock)) continue;

                BooleanProperty nProperty = CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(direction.getOpposite());
                BlockState nState = neighborState.setValue(nProperty, false);

                if (GemstonePaneFix.isDisconnectedPane(nState, direction.getOpposite())) {
                    lastUpdatedToFullConnectedPane = neighborPos;
                    level.setBlockAndUpdate(neighborPos, GemstonePaneFix.createFullConnectedPane(neighborState));
                } else if (!neighborPos.equals(lastUpdatedToFullConnectedPane)) {
                    level.setBlockAndUpdate(neighborPos, nState);
                }
            }
        });
    }
}