package com.skyblockutils.features.party;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.features.guild.GuildListParser;
import com.skyblockutils.utils.ChatListCapture;
import com.skyblockutils.utils.PlayerLookup;
import com.skyblockutils.utils.SSUIndicator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class PartyListParser {
    private static final long REFRESH_MS = 10 * 60_000;
    private static final long MANUAL_WINDOW_MS = 3_000;
    private static final Pattern MANUAL_COMMAND = Pattern.compile("^((p|party) (list|l)|pl)\\b.*");
    private static final Pattern PARTY_LINE = Pattern.compile("^Party (Members \\(\\d+\\)|Leader:|Moderators:|Members:).*");

    public static boolean onJoinCommandHandled = false;
    private static long lastRequest = 0;
    private static long manualUntil = 0;
    private static boolean sendingOwnCommand = false;
    private static boolean requestPending = false;
    private static ClientLevel lastLevel;
    private static boolean awaitingResponse = false;

    private static final ChatListCapture capture = new ChatListCapture(
            PartyListParser::isStartLine,
            PartyListParser::isListLine,
            PartyListParser::onListReceived,
            false
    );

    public static boolean isExpecting() {
        return capture.isExpecting();
    }

    public static void tickCapture() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != lastLevel) {
            lastLevel = level;
            if (level != null) onJoinCommandHandled = false;
        }

        capture.tick();
        if (awaitingResponse && !capture.isExpecting()) {
            awaitingResponse = false;
            onJoinCommandHandled = false;
        }
        if (requestPending && !GuildListParser.isExpecting()) requestList();
    }

    public static void handleOnJoinCommand() {
        if (capture.isExpecting() || GuildListParser.isExpecting()) return;
        if (onJoinCommandHandled && System.currentTimeMillis() - lastRequest < REFRESH_MS) return;
        if (ModFunctions.isWorldLoaded()) return;

        onJoinCommandHandled = true;
        requestList();
    }

    public static void requestList() {
        if (Minecraft.getInstance().getConnection() == null) return;
        if (GuildListParser.isExpecting()) {
            requestPending = true;
            return;
        }

        requestPending = false;
        lastRequest = System.currentTimeMillis();
        awaitingResponse = true;
        capture.expect();
        sendingOwnCommand = true;
        Minecraft.getInstance().getConnection().sendCommand("party list");
        sendingOwnCommand = false;
    }

    public static void onCommandSent(String command) {
        if (sendingOwnCommand) return;
        if (!MANUAL_COMMAND.matcher(command.toLowerCase(Locale.ROOT).trim()).matches()) return;

        manualUntil = System.currentTimeMillis() + MANUAL_WINDOW_MS;
        awaitingResponse = false;
        capture.cancel();
    }

    public static boolean handleMessage(Component message) {
        if (System.currentTimeMillis() < manualUntil) return true;
        return capture.handle(message);
    }

    private static boolean isNotInParty(String text) {
        return text.startsWith("You are not currently in a party") || text.startsWith("You are not in a party");
    }

    private static boolean isStartLine(String text) {
        return PARTY_LINE.matcher(text).matches() || isNotInParty(text);
    }

    private static boolean isListLine(String text) {
        return text.isEmpty() || isStartLine(text);
    }

    private static void onListReceived(List<String> lines) {
        awaitingResponse = false;
        parseLines(lines);
        getMemberUuids();
    }

    private static void parseLines(List<String> lines) {
        PartyInfo.members.clear();

        if (lines.stream().noneMatch(line -> PARTY_LINE.matcher(line).matches())) {
            resetPartyInfo();
            return;
        }

        for (String line : lines) {
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
                .thenRun(() -> Minecraft.getInstance().execute(() -> {
                    for (CompletableFuture<UUID> future : futures) {
                        UUID uuid = future.join();
                        if (uuid != null) {
                            PartyInfo.memberUuids.add(uuid);
                        }
                    }
                }));
    }

    private static void resetPartyInfo() {
        PartyInfo.isInParty = false;
        PartyInfo.leader = null;
        PartyInfo.members.clear();
        SSUIndicator.setPartyMembers(List.of());
    }
}
