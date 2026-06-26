package com.skyblockutils.features.party;

import com.skyblockutils.utils.PlayerLookup;
import net.minecraft.client.MinecraftClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PartyListParser {

    public static boolean expectingPartyList = false;
    public static boolean onJoinCommandHandled = false;
    private static boolean reading = false;
    private static final List<String> buffer = new ArrayList<>();

    public static void handleOnJoinCommand() {
        if (onJoinCommandHandled) return;
        expectingPartyList = true;
        Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("party list");
        onJoinCommandHandled = true;
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
    }
}