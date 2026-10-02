package com.skyblockutils.features.guild;

import com.skyblockutils.features.party.PartyListParser;
import com.skyblockutils.utils.ChatSeparator;
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
    private static final long EXPECT_TIMEOUT_MS = 5_000;
    private static final long TRAILING_GRACE_MS = 1_000;
    private static final long MANUAL_WINDOW_MS = 3_000;
    private static final Pattern MANUAL_COMMAND = Pattern.compile("^(g|guild) (list|online|members|l|o)\\b.*");
    private static final Pattern MEMBER = Pattern.compile("^(?:\\[[^]]+] )*(\\w{3,16})$");

    public static boolean onJoinCommandHandled = false;
    public static final List<String> members = new ArrayList<>();

    private static boolean expecting = false;
    private static boolean reading = false;
    private static long expectingSince = 0;
    private static long lastRequest = 0;
    private static long finishedAt = 0;
    private static long manualUntil = 0;
    private static boolean sendingOwnCommand = false;
    private static Component heldSeparator;
    private static final List<Component> rawBuffer = new ArrayList<>();
    private static final List<String> buffer = new ArrayList<>();

    public static void tick() {
        long now = System.currentTimeMillis();
        if (expecting && !reading && now - expectingSince > EXPECT_TIMEOUT_MS) {
            replayHeld();
            expecting = false;
        }
        if (expecting || PartyListParser.expectingPartyList) return;
        if (onJoinCommandHandled && now - lastRequest < REFRESH_MS) return;

        if (Minecraft.getInstance().getConnection() == null) return;
        onJoinCommandHandled = true;
        expecting = true;
        clearHeld();
        expectingSince = now;
        lastRequest = now;
        sendingOwnCommand = true;
        Minecraft.getInstance().getConnection().sendCommand("guild online");
        sendingOwnCommand = false;
    }

    public static void onCommandSent(String command) {
        if (sendingOwnCommand) return;
        if (!MANUAL_COMMAND.matcher(command.toLowerCase(Locale.ROOT).trim()).matches()) return;

        manualUntil = System.currentTimeMillis() + MANUAL_WINDOW_MS;
        if (expecting) {
            replayHeld();
            expecting = false;
        }
    }

    public static boolean handleMessage(Component message) {
        if (System.currentTimeMillis() < manualUntil) return true;

        String text = message.getString().replaceAll("§.", "").trim();
        boolean isSeparator = ChatSeparator.is(message);

        if (!expecting) {
            return !(isSeparator && System.currentTimeMillis() - finishedAt < TRAILING_GRACE_MS);
        }

        if (text.startsWith("You must be in a guild")) {
            clearHeld();
            finish(List.of());
            return false;
        }

        if (!reading) {
            if (isSeparator) {
                if (heldSeparator != null) replayHeld();
                heldSeparator = message;
                return false;
            }

            if (text.startsWith("Guild Name:")) {
                reading = true;
                buffer.clear();
                rawBuffer.clear();
                buffer.add(text);
                rawBuffer.add(message);
                return false;
            }

            if (heldSeparator != null) replayHeld();
            return true;
        }

        buffer.add(text);
        rawBuffer.add(message);
        if (text.startsWith("Online Members:")) finish(parseLines());
        return false;
    }

    private static List<String> parseLines() {
        List<String> parsed = new ArrayList<>();
        for (String line : GuildListParser.buffer) {
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

    private static void replayHeld() {
        var chat = Minecraft.getInstance().gui.hud.getChat();
        if (heldSeparator != null) chat.addClientSystemMessage(heldSeparator);
        for (Component line : rawBuffer) chat.addClientSystemMessage(line);
        clearHeld();
    }

    private static void clearHeld() {
        reading = false;
        heldSeparator = null;
        buffer.clear();
        rawBuffer.clear();
    }

    private static void finish(List<String> parsed) {
        expecting = false;
        clearHeld();
        finishedAt = System.currentTimeMillis();
        members.clear();
        members.addAll(parsed);
        SSUIndicator.setGuildMembers(members);
    }
}
