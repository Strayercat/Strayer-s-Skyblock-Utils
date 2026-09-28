package com.skyblockutils.features.party;

import com.skyblockutils.utils.PlayerLookup;
import com.skyblockutils.utils.SSUIndicator;
import net.minecraft.client.Minecraft;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PartyListParser {
    private static final long REFRESH_MS = 10 * 60_000;
    private static final long EXPECT_TIMEOUT_MS = 5_000;

    public static boolean expectingPartyList = false;
    public static boolean onJoinCommandHandled = false;
    private static boolean reading = false;
    private static long expectingSince = 0;
    private static long lastRequest = 0;
    private static final List<String> buffer = new ArrayList<>();

    public static void handleOnJoinCommand() {
        long now = System.currentTimeMillis();
        if (expectingPartyList && !reading && now - expectingSince > EXPECT_TIMEOUT_MS) expectingPartyList = false;
        if (expectingPartyList) return;
        if (onJoinCommandHandled && now - lastRequest < REFRESH_MS) return;

        onJoinCommandHandled = true;
        requestList();
    }

    public static void requestList() {
        if (Minecraft.getInstance().getConnection() == null) return;
        long now = System.currentTimeMillis();
        expectingPartyList = true;
        expectingSince = now;
        lastRequest = now;
        Minecraft.getInstance().getConnection().sendCommand("party list");
    }

    public static boolean handleMessage(String message) {
        if (!expectingPartyList) return true;

        if (message.startsWith("-----")) {
            if (!reading) {
                reading = true;
                buffer.clear();
            } else {
                reading = false;
                parseBuffer();
                getMemberUuids();
                expectingPartyList = false;
            }
            return false;
        }

        if (reading) {
            buffer.add(message);
            return false;
        }

        return true;
    }

    private static void parseBuffer() {
        PartyInfo.members.clear();

        if (buffer.size() == 1) {
            resetPartyInfo();
            return;
        }

        for (String line : buffer) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("Party Leader:")) {
                PartyInfo.leader = extractName(line);
            }

            if (line.startsWith("Party Moderators:")) {
                PartyInfo.members.addAll(extractMultipleNames(line));
            }

            if (line.startsWith("Party Members:")) {
                PartyInfo.members.addAll(extractMultipleNames(line));
            }
        }

        PartyInfo.isInParty = !PartyInfo.members.isEmpty();

        List<String> watched = new ArrayList<>(PartyInfo.members);
        if (PartyInfo.leader != null) watched.add(PartyInfo.leader);
        SSUIndicator.setPartyMembers(watched);
    }

    private static String extractName(String line) {
        String cleaned = line.replaceFirst("Party Leader: ", "");
        cleaned = cleaned.replaceAll("\\[[^]]+] ", "");
        cleaned = cleaned.replace("●", "").trim();
        return cleaned;
    }

    private static List<String> extractMultipleNames(String line) {
        String cleaned = line
                .replaceFirst("Party (Members|Moderators): ", "")
                .replaceAll("\\[[^]]+] ", "")
                .replace("●", "");

        String[] split = cleaned.split("\\s+");

        List<String> names = new ArrayList<>();
        for (String s : split) {
            if (!s.isBlank()) {
                names.add(s.trim());
            }
        }
        return names;
    }

    private static void getMemberUuids() {
        PartyInfo.memberUuids.clear();

        List<CompletableFuture<UUID>> futures = new ArrayList<>();
        for (String member : PartyInfo.members) {
            futures.add(PlayerLookup.getUuidOffline(member));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    for (CompletableFuture<UUID> future : futures) {
                        UUID uuid = future.join();
                        if (uuid != null) {
                            PartyInfo.memberUuids.add(uuid);
                        }
                    }
                });
    }

    private static void resetPartyInfo() {
        PartyInfo.isInParty = false;
        PartyInfo.leader = null;
        PartyInfo.members.clear();
        SSUIndicator.setPartyMembers(List.of());
    }
}