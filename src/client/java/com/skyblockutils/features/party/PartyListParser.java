package com.skyblockutils.features.party;

import com.skyblockutils.utils.ChatSeparator;
import com.skyblockutils.utils.PlayerLookup;
import com.skyblockutils.utils.SSUIndicator;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class PartyListParser {
    private static final long REFRESH_MS = 10 * 60_000;
    private static final long EXPECT_TIMEOUT_MS = 5_000;
    private static final long MANUAL_WINDOW_MS = 3_000;
    private static final Pattern MANUAL_COMMAND = Pattern.compile("^((p|party) (list|l)|pl)\\b.*");

    public static boolean expectingPartyList = false;
    public static boolean onJoinCommandHandled = false;
    private static boolean reading = false;
    private static long expectingSince = 0;
    private static long lastRequest = 0;
    private static long manualUntil = 0;
    private static boolean sendingOwnCommand = false;
    private static Component separator;
    private static final List<Component> rawBuffer = new ArrayList<>();
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
        sendingOwnCommand = true;
        Minecraft.getInstance().getConnection().sendCommand("party list");
        sendingOwnCommand = false;
    }

    public static void onCommandSent(String command) {
        if (sendingOwnCommand) return;
        if (!MANUAL_COMMAND.matcher(command.toLowerCase(Locale.ROOT).trim()).matches()) return;

        manualUntil = System.currentTimeMillis() + MANUAL_WINDOW_MS;
        if (reading) abort();
        expectingPartyList = false;
    }

    public static boolean handleMessage(Component message) {
        if (!expectingPartyList || System.currentTimeMillis() < manualUntil) return true;
        String text = message.getString().replaceAll("§.", "").trim();

        if (text.contains("\n") && (text.contains("Party Members") || text.contains("not currently in a party") || text.contains("not in a party"))) {
            reading = false;
            rawBuffer.clear();
            buffer.clear();
            for (String line : text.split("\n")) buffer.add(line.trim());
            parseBuffer();
            getMemberUuids();
            expectingPartyList = false;
            return false;
        }

        if (ChatSeparator.is(message)) {
            if (!reading) {
                reading = true;
                separator = message;
                buffer.clear();
                rawBuffer.clear();
            } else {
                reading = false;
                parseBuffer();
                getMemberUuids();
                expectingPartyList = false;
            }
            return false;
        }

        if (!reading) return true;

        if (buffer.isEmpty() && !text.isEmpty() && !isPartyListLine(text)) {
            abort();
            return true;
        }

        buffer.add(text);
        rawBuffer.add(message);
        return false;
    }

    private static boolean isPartyListLine(String text) {
        return text.startsWith("Party ") || text.startsWith("You are not currently in a party") || text.startsWith("You are not in a party");
    }

    private static void abort() {
        reading = false;
        var chat = Minecraft.getInstance().gui.hud.getChat();
        if (separator != null) chat.addClientSystemMessage(separator);
        for (Component line : rawBuffer) chat.addClientSystemMessage(line);
        buffer.clear();
        rawBuffer.clear();
    }

    private static void parseBuffer() {
        PartyInfo.members.clear();

        if (buffer.stream().noneMatch(line -> line.startsWith("Party "))) {
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