package com.skyblockutils.features.party;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.hypixel.HypixelApi;
import com.skyblockutils.utils.PlayerLookup;
import com.skyblockutils.utils.SSUIndicator;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PartyTracker {
    private static final int RETRY_TICKS = 20;
    private static final Map<UUID, String> NAMES = new HashMap<>();
    private static final Set<UUID> LOOKUPS = new HashSet<>();

    private static boolean wanted = false;
    private static boolean awaiting = false;
    private static boolean reportNext = false;
    private static int retryIn = 0;
    private static @Nullable UUID leaderUuid;
    private static final List<UUID> memberUuids = new ArrayList<>();

    private PartyTracker() {
    }

    public static void init() {
        HypixelModAPI.getInstance()
                .createHandler(ClientboundPartyInfoPacket.class, packet -> HypixelApi.onMainThread(() -> onPartyInfo(packet)))
                .onError(reason -> HypixelApi.onMainThread(() -> {
                    awaiting = false;
                    retryIn = RETRY_TICKS;
                    if (reportNext) ModFunctions.sendSystemMessage("§e[Dev] Party info request failed (" + reason + "), retrying", false);
                }));
    }

    public static void request() {
        wanted = true;
    }

    public static void devScan() {
        reportNext = true;
        ModFunctions.sendSystemMessage("§7[Dev] Requesting party info...", false);
        request();
    }

    public static void tick(Minecraft client) {
        if (!wanted || awaiting) return;
        if (retryIn > 0) {
            retryIn--;
            return;
        }
        if (client.getConnection() == null || !StrayersSkyblockUtilsClient.isOnHypixel) return;

        boolean sent;
        try {
            sent = HypixelModAPI.getInstance().sendPacket(new ServerboundPartyInfoPacket());
        } catch (IllegalStateException e) {
            sent = false;
        }
        awaiting = sent;
        if (!sent) retryIn = RETRY_TICKS;
    }

    public static void reset() {
        wanted = false;
        awaiting = false;
        retryIn = 0;
        leaderUuid = null;
        memberUuids.clear();
        apply();
    }

    private static void onPartyInfo(ClientboundPartyInfoPacket packet) {
        wanted = false;
        awaiting = false;
        leaderUuid = packet.getLeader().orElse(null);
        memberUuids.clear();
        if (packet.isInParty()) {
            for (UUID uuid : packet.getMembers()) {
                if (!uuid.equals(leaderUuid)) memberUuids.add(uuid);
            }
        }
        apply();

        if (reportNext) {
            reportNext = false;
            if (!PartyInfo.isInParty) {
                ModFunctions.sendSystemMessage("§a[Dev] Party info: not in a party", false);
            } else {
                ModFunctions.sendSystemMessage("§a[Dev] Party info: leader " + PartyInfo.leader + ", "
                        + PartyInfo.members.size() + " member(s): " + String.join(", ", PartyInfo.members), false);
            }
        }
    }

    private static void apply() {
        PartyInfo.isInParty = leaderUuid != null;
        PartyInfo.leader = leaderUuid == null ? null : name(leaderUuid);
        PartyInfo.members.clear();
        PartyInfo.memberUuids.clear();
        for (UUID uuid : memberUuids) {
            PartyInfo.memberUuids.add(uuid);
            String name = name(uuid);
            if (name != null) PartyInfo.members.add(name);
        }

        List<String> watched = new ArrayList<>(PartyInfo.members);
        if (PartyInfo.leader != null) watched.add(PartyInfo.leader);
        SSUIndicator.setPartyMembers(watched);
    }

    private static @Nullable String name(UUID uuid) {
        String cached = NAMES.get(uuid);
        if (cached != null) return cached;

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(uuid);
        if (info != null) {
            String name = info.getProfile().name();
            NAMES.put(uuid, name);
            return name;
        }

        if (LOOKUPS.add(uuid)) {
            PlayerLookup.getNameByUuid(uuid).thenAccept(name -> HypixelApi.onMainThread(() -> {
                LOOKUPS.remove(uuid);
                if (name == null) return;
                NAMES.put(uuid, name);
                if (uuid.equals(leaderUuid) || memberUuids.contains(uuid)) apply();
            }));
        }
        return null;
    }
}
