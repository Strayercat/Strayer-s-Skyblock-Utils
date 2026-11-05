package com.skyblockutils.features.guild;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.features.party.PartyListParser;
import com.skyblockutils.utils.ChatListCapture;
import com.skyblockutils.utils.SSUIndicator;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuildListParser {
    private static final long REFRESH_MS = 10 * 60_000;
    private static final long MANUAL_WINDOW_MS = 3_000;
    private static final long RETRY_MS = 30_000;
    private static final long CHANGE_DELAY_MS = 1_000;
    private static final Pattern MEMBERSHIP_CHANGE = Pattern.compile("^(?:\\[[^]]+] )*\\w{3,16} (?:joined the guild!|left the guild!|was kicked from the guild.*|disbanded the guild\\.)$");
    private static final Pattern SELF_REMOVED = Pattern.compile("^(?:You left the guild.*|You were kicked from the guild.*|(?:\\[[^]]+] )*\\w{3,16} disbanded the guild\\.)$");
    private static final Pattern MANUAL_COMMAND = Pattern.compile("^(g|guild) (list|online|members|l|o)\\b.*");
    private static final Pattern MEMBER = Pattern.compile("^(?:\\[[^]]+] )*(\\w{3,16})$");
    private static final Pattern RANK_HEADER = Pattern.compile("^-- .+ --$");
    private static final String NOT_IN_GUILD = "You must be in a guild";

    public static boolean onJoinCommandHandled = false;
    public static final List<String> members = new ArrayList<>();

    private static long lastRequest = 0;
    private static long manualUntil = 0;
    private static boolean sendingOwnCommand = false;
    private static boolean awaitingResponse = false;
    private static long rescanAt = 0;
    private static boolean reportNext = false;

    private static final ChatListCapture capture = new ChatListCapture(
            GuildListParser::isStartLine,
            GuildListParser::isListLine,
            GuildListParser::onListReceived,
            true
    );

    public static boolean isExpecting() {
        return capture.isExpecting();
    }

    public static void tickCapture() {
        capture.tick();
        if (awaitingResponse && !capture.isExpecting()) {
            awaitingResponse = false;
            rescanAt = System.currentTimeMillis() + RETRY_MS;
            if (reportNext) {
                reportNext = false;
                ModFunctions.sendSystemMessage("§c[Dev] Guild scan got no response", false);
            }
        }
    }

    public static void tick() {
        if (capture.isExpecting() || PartyListParser.isExpecting()) return;
        if (ModFunctions.isWorldLoaded()) return;
        if (Minecraft.getInstance().getConnection() == null) return;

        long now = System.currentTimeMillis();
        boolean rescanDue = rescanAt > 0 && now >= rescanAt;
        if (onJoinCommandHandled && !rescanDue && now - lastRequest < REFRESH_MS) return;

        onJoinCommandHandled = true;
        requestList();
    }

    public static void requestList() {
        if (Minecraft.getInstance().getConnection() == null) return;
        rescanAt = 0;
        lastRequest = System.currentTimeMillis();
        awaitingResponse = true;
        capture.expect();
        sendingOwnCommand = true;
        try {
            Minecraft.getInstance().getConnection().sendCommand("guild online");
        } finally {
            sendingOwnCommand = false;
        }
    }

    public static void devScan() {
        if (Minecraft.getInstance().getConnection() == null) return;
        if (capture.isExpecting() || PartyListParser.isExpecting()) {
            ModFunctions.sendSystemMessage("§e[Dev] A party/guild scan is already in progress", false);
            return;
        }
        reportNext = true;
        ModFunctions.sendSystemMessage("§7[Dev] Scanning guild...", false);
        requestList();
    }

    public static void onCommandSent(String command) {
        if (sendingOwnCommand) return;
        if (!MANUAL_COMMAND.matcher(command.toLowerCase(Locale.ROOT).trim()).matches()) return;

        manualUntil = System.currentTimeMillis() + MANUAL_WINDOW_MS;
        awaitingResponse = false;
        capture.cancel();
    }

    public static boolean handleMessage(Component message) {
        checkMembershipChange(message);
        if (System.currentTimeMillis() < manualUntil) return true;
        return capture.handle(message);
    }

    private static void checkMembershipChange(Component message) {
        for (String line : message.getString().replaceAll("§.", "").split("\n")) {
            String text = line.trim();
            if (SELF_REMOVED.matcher(text).matches()) {
                members.clear();
                SSUIndicator.setGuildMembers(members);
                rescanAt = 0;
                return;
            }
            if (MEMBERSHIP_CHANGE.matcher(text).matches()) {
                rescanAt = System.currentTimeMillis() + CHANGE_DELAY_MS;
                return;
            }
        }
    }

    private static boolean isStartLine(String text) {
        return text.startsWith("Guild Name:") || text.startsWith(NOT_IN_GUILD);
    }

    private static boolean isListLine(String text) {
        if (text.isEmpty()
                || RANK_HEADER.matcher(text).matches()
                || text.startsWith("Total Members:")
                || text.startsWith("Online Members:")
                || text.startsWith("Offline Members:")) return true;

        if (!text.contains("●")) return false;
        for (String part : text.split("●")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && !MEMBER.matcher(trimmed).matches()) return false;
        }
        return true;
    }

    private static void onListReceived(List<String> lines) {
        awaitingResponse = false;
        members.clear();
        if (lines.stream().noneMatch(line -> line.startsWith(NOT_IN_GUILD))) {
            members.addAll(parseLines(lines));
        }
        SSUIndicator.setGuildMembers(members);
        if (reportNext) {
            reportNext = false;
            if (members.isEmpty() && lines.stream().anyMatch(line -> line.startsWith(NOT_IN_GUILD))) {
                ModFunctions.sendSystemMessage("§a[Dev] Guild scan: not in a guild", false);
            } else {
                ModFunctions.sendSystemMessage("§a[Dev] Guild scan: " + members.size() + " online member(s): " + String.join(", ", members), false);
            }
        }
    }

    private static List<String> parseLines(List<String> lines) {
        List<String> parsed = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()
                    || line.startsWith("Guild Name:")
                    || line.startsWith("--")
                    || line.startsWith("Total Members:")
                    || line.startsWith("Online Members:")
                    || line.startsWith("Offline Members:")) continue;

            for (String part : line.split("●")) {
                Matcher m = MEMBER.matcher(part.trim());
                if (m.matches()) parsed.add(m.group(1));
            }
        }
        return parsed;
    }
}
