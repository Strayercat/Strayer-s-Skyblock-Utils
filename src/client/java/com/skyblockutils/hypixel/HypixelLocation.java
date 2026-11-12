package com.skyblockutils.hypixel;

import com.skyblockutils.ModFunctions;
import net.hypixel.data.type.GameType;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jspecify.annotations.Nullable;

public final class HypixelLocation {
    private static @Nullable ClientboundLocationPacket current;
    private static @Nullable ClientLevel packetLevel;

    private HypixelLocation() {
    }

    public static void init() {
        HypixelModAPI api = HypixelModAPI.getInstance();
        api.subscribeToEventPacket(ClientboundLocationPacket.class);
        api.createHandler(ClientboundLocationPacket.class, packet -> HypixelApi.onMainThread(() -> {
            boolean serverChanged = current == null || !current.getServerName().equals(packet.getServerName());
            current = packet;
            if (serverChanged) ModFunctions.serverChangeDataReset();
            packetLevel = Minecraft.getInstance().level;
        }));
    }

    public static boolean isKnown() {
        ClientLevel level = Minecraft.getInstance().level;
        return current != null && level != null && packetLevel == level;
    }

    public static @Nullable ClientboundLocationPacket packet() {
        return isKnown() ? current : null;
    }

    public static boolean isSkyBlock() {
        ClientboundLocationPacket packet = packet();
        return packet != null && packet.getServerType().orElse(null) == GameType.SKYBLOCK;
    }

    public static String mode() {
        ClientboundLocationPacket packet = packet();
        return packet == null ? "" : packet.getMode().orElse("");
    }

    public static String map() {
        ClientboundLocationPacket packet = packet();
        return packet == null ? "" : packet.getMap().orElse("");
    }

    public static boolean isInMineshaft() {
        return isSkyBlock() && mode().equals("mineshaft");
    }

    public static boolean isOnPrivateIsland() {
        return isSkyBlock() && mode().equals("dynamic");
    }

    public static String describe() {
        ClientboundLocationPacket packet = current;
        if (packet == null) return "no packet received";
        return (isKnown() ? "" : "(stale, from a previous world) ") + "server=" + packet.getServerName()
                + " type=" + packet.getServerType().map(Object::toString).orElse("-")
                + " lobby=" + packet.getLobbyName().orElse("-")
                + " mode=" + packet.getMode().orElse("-")
                + " map=" + packet.getMap().orElse("-");
    }

    public static void reset() {
        current = null;
        packetLevel = null;
    }
}
