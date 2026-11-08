package com.skyblockutils.hypixel;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.features.party.PartyTracker;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket;
import net.minecraft.client.Minecraft;

public final class HypixelApi {
    private HypixelApi() {
    }

    public static void init() {
        HypixelModAPI.getInstance().createHandler(ClientboundHelloPacket.class, _ -> onMainThread(() -> {
            if (StrayersSkyblockUtilsClient.isOnHypixel) return;
            StrayersSkyblockUtilsClient.isOnHypixel = true;
            ModFunctions.connectionEventDataReset("Join");
        }));
        HypixelLocation.init();
        PartyTracker.init();
    }

    public static void onMainThread(Runnable task) {
        Minecraft client = Minecraft.getInstance();
        if (client.isSameThread()) task.run();
        else client.execute(task);
    }
}
