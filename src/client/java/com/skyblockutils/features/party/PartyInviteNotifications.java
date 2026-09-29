package com.skyblockutils.features.party;

import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PartyInviteNotifications {
    private static final Pattern USER_SENT_MESSAGE_PATTERN = Pattern.compile("^\\[\\d{1,3}]\\s.*?\\s(?:\\[[A-Z]+\\+])?\\s.+: .+$");
    private static final Pattern EXPIRED_PATTERN = Pattern.compile("The party invite from .+ has expired\\.");
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("^-{5,}$");

    private static final long EXPIRE_WINDOW_START = 50000L;
    private static final long EXPIRE_WINDOW_END = 80000L;

    private static final List<Long> inviteTimestamps = new ArrayList<>();
    private static Component pendingSeparator = null;
    private static boolean awaitingClosingSeparator = false;

    public static List<String> previousInvites = new ArrayList<>();

    public static boolean handleNotifications(Component rawMessage) {
        long now = System.currentTimeMillis();
        inviteTimestamps.removeIf(t -> t + EXPIRE_WINDOW_END < now);

        String message = ChatFormatting.stripFormatting(rawMessage.getString());
        String trimmed = message.trim();

        assert Minecraft.getInstance().player != null;
        if (USER_SENT_MESSAGE_PATTERN.matcher(message).find() || message.startsWith("Party >") || message.contains(">")) {
            return flushPendingSeparator();
        }
        if (!ModConfig.INSTANCE.partyInviteNotifications) return flushPendingSeparator();

        if (message.contains("has invited you to join their party!")) {
            flushPendingSeparator();

            String username = message.replaceAll("-", "").replaceAll("\\[[^]]*] ?", "").trim().split("\\s+")[0];
            OnScreenNotification.builder()
                    .title("PARTY INVITE")
                    .subtitle(username + " is inviting you to their party.\nClick here to join.")
                    .tickTime(1300)
                    .send();

            previousInvites.remove(username);
            previousInvites.add(username);

            inviteTimestamps.add(now);
            return false;
        }

        if (EXPIRED_PATTERN.matcher(message).find()) {
            pendingSeparator = null;
            consumeInvite();
            awaitingClosingSeparator = !trimmed.endsWith("-");
            return false;
        }

        boolean isSeparator = SEPARATOR_PATTERN.matcher(trimmed).matches();

        if (awaitingClosingSeparator) {
            awaitingClosingSeparator = false;
            if (isSeparator) return false;
        }

        if (isSeparator && expiryExpected(now)) {
            flushPendingSeparator();
            pendingSeparator = rawMessage;
            return false;
        }

        return flushPendingSeparator();
    }

    private static boolean expiryExpected(long now) {
        for (long t : inviteTimestamps) {
            long age = now - t;
            if (age >= EXPIRE_WINDOW_START && age <= EXPIRE_WINDOW_END) return true;
        }
        return false;
    }

    private static void consumeInvite() {
        if (!inviteTimestamps.isEmpty()) inviteTimestamps.removeFirst();
    }

    private static boolean flushPendingSeparator() {
        if (pendingSeparator != null) {
            Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(pendingSeparator);
            pendingSeparator = null;
        }
        return true;
    }
}