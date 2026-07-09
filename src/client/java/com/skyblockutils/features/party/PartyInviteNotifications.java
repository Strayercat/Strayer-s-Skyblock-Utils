package com.skyblockutils.features.party;

import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PartyInviteNotifications {
    private static final Pattern USER_SENT_MESSAGE_PATTERN = Pattern.compile("^\\[\\d{1,3}]\\s.*?\\s(?:\\[[A-Z]+\\+])?\\s.+: .+$");
    private static long partyInviteTimestamp = 0L;
    private static boolean expectingExpired = false;
    private static boolean reading = false;
    private static Component separator = Component.empty();
    private static final List<Component> buffer = new ArrayList<>();

    public static List<String> previousInvites = new ArrayList<>();

    public static boolean handleNotifications(Component rawMessage) {
        if (partyInviteTimestamp + 70000 < System.currentTimeMillis()) expectingExpired = false;

        String message = rawMessage.getString();
        assert Minecraft.getInstance().player != null;
        if (USER_SENT_MESSAGE_PATTERN.matcher(message).find() || message.startsWith("Party >") || message.contains(">"))
            return true;
        if (!ModConfig.INSTANCE.partyInviteNotifications) return true;
        if (message.contains("has invited you to join their party!")) {
            String username = message.replaceAll("-", "").replaceAll("\\[[^]]*] ?", "").split(" ")[0];
            OnScreenNotification.builder()
                    .title("PARTY INVITE")
                    .subtitle(username + " is inviting you to their party.\nClick here to join.")
                    .tickTime(1200)
                    .send();

            previousInvites.remove(username);
            previousInvites.add(username);

            expectingExpired = true;
            partyInviteTimestamp = System.currentTimeMillis();
            return false;
        }

        if (expectingExpired && partyInviteTimestamp + 58000 < System.currentTimeMillis()) {
            if (message.startsWith("-----")) {
                if (!reading) {
                    buffer.clear();
                    separator = rawMessage;
                    reading = true;
                } else {
                    reading = false;

                    if (!buffer.getFirst().getString().matches("The party invite from .* has expired\\.")) {
                        replaySwallowedMessage();
                    } else {
                        expectingExpired = false;
                    }
                }

                return false;
            }

            buffer.add(rawMessage);
            return false;
        }

        return true;
    }

    private static void replaySwallowedMessage() {
        Minecraft client = Minecraft.getInstance();

        client.gui.hud.getChat().addClientSystemMessage(separator);
        for (Component message : buffer) {
            client.gui.hud.getChat().addClientSystemMessage(message);
        }
        client.gui.hud.getChat().addClientSystemMessage(separator);
    }
}