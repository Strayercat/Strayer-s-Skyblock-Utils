package com.skyblockutils.features.guild;

import com.skyblockutils.features.party.PartyListParser;
import com.skyblockutils.utils.SSUIndicator;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuildListParser {
    private static final long REFRESH_MS = 10 * 60_000;
    private static final long EXPECT_TIMEOUT_MS = 5_000;
    private static final long TRAILING_GRACE_MS = 1_000;
    private static final Pattern MEMBER = Pattern.compile("^(?:\\[[^]]+] )*(\\w{3,16})$");

    public static boolean onJoinCommandHandled = false;
    public static final List<String> members = new ArrayList<>();

    private static boolean expecting = false;
    private static boolean reading = false;
    private static boolean sawFooter = false;
    private static long expectingSince = 0;
    private static long lastRequest = 0;
    private static long finishedAt = 0;
    private static final List<String> buffer = new ArrayList<>();

    public static void tick() {
        long now = System.currentTimeMillis();
        if (expecting && now - expectingSince > EXPECT_TIMEOUT_MS) {
            expecting = false;
            reading = false;
            buffer.clear();
        }
        if (expecting || PartyListParser.expectingPartyList) return;
        if (onJoinCommandHandled && now - lastRequest < REFRESH_MS) return;

        if (Minecraft.getInstance().getConnection() == null) return;
        onJoinCommandHandled = true;
        expecting = true;
        reading = false;
        sawFooter = false;
        buffer.clear();
        expectingSince = now;
        lastRequest = now;
        Minecraft.getInstance().getConnection().sendCommand("guild online");
    }

    public static boolean handleMessage(String message) {
        boolean isSeparator = message.startsWith("-----");

        if (!expecting) {
            return !(isSeparator && System.currentTimeMillis() - finishedAt < TRAILING_GRACE_MS);
        }

        if (isNotInGuild(message)) {
            finish(List.of());
            return false;
        }

        if (isSeparator) {
            if (!reading) {
                reading = true;
            } else if (sawFooter) {
                parseBuffer();
            }
            return false;
        }

        if (reading) {
            buffer.add(message);
            if (message.trim().startsWith("Online Members:")) sawFooter = true;
            return false;
        }

        return true;
    }

    private static void parseBuffer() {
        if (buffer.stream().noneMatch(line -> line.trim().startsWith("Guild Name:"))) {
            reading = false;
            sawFooter = false;
            buffer.clear();
            return;
        }

        List<String> parsed = new ArrayList<>();
        for (String line : buffer) {
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

        finish(parsed);
    }

    private static boolean isNotInGuild(String message) {
        return message.trim().startsWith("You need to be in a guild");
    }

    private static void finish(List<String> parsed) {
        expecting = false;
        reading = false;
        sawFooter = false;
        finishedAt = System.currentTimeMillis();
        buffer.clear();
        members.clear();
        members.addAll(parsed);
        SSUIndicator.setGuildMembers(members);
    }
}
