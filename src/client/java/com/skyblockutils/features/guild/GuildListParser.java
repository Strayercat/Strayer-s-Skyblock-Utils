package com.skyblockutils.features.guild;

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
    private static final Pattern MANUAL_COMMAND = Pattern.compile("^(g|guild) (list|online|members|l|o)\\b.*");
    private static final Pattern MEMBER = Pattern.compile("^(?:\\[[^]]+] )*(\\w{3,16})$");
    private static final Pattern RANK_HEADER = Pattern.compile("^-- .+ --$");
    private static final String NOT_IN_GUILD = "You must be in a guild";

    public static boolean onJoinCommandHandled = false;
    public static final List<String> members = new ArrayList<>();

    private static long lastRequest = 0;
    private static long manualUntil = 0;
    private static boolean sendingOwnCommand = false;

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
    }

    public static void tick() {
        if (capture.isExpecting() || PartyListParser.isExpecting()) return;
        long now = System.currentTimeMillis();
        if (onJoinCommandHandled && now - lastRequest < REFRESH_MS) return;
        if (Minecraft.getInstance().getConnection() == null) return;

        onJoinCommandHandled = true;
        lastRequest = now;
        capture.expect();
        sendingOwnCommand = true;
        Minecraft.getInstance().getConnection().sendCommand("guild online");
        sendingOwnCommand = false;
    }

    public static void onCommandSent(String command) {
        if (sendingOwnCommand) return;
        if (!MANUAL_COMMAND.matcher(command.toLowerCase(Locale.ROOT).trim()).matches()) return;

        manualUntil = System.currentTimeMillis() + MANUAL_WINDOW_MS;
        capture.cancel();
    }

    public static boolean handleMessage(Component message) {
        if (System.currentTimeMillis() < manualUntil) return true;
        return capture.handle(message);
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
        members.clear();
        if (lines.stream().noneMatch(line -> line.startsWith(NOT_IN_GUILD))) {
            members.addAll(parseLines(lines));
        }
        SSUIndicator.setGuildMembers(members);
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
