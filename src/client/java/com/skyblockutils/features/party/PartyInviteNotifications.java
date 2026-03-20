package com.skyblockutils.features.party;

import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.config.ModConfig;
import net.minecraft.client.MinecraftClient;

import java.util.regex.Pattern;

public class PartyInviteNotifications {
    private static final Pattern USER_SENT_MESSAGE_PATTERN = Pattern.compile("^\\[\\d{1,3}]\\s.*?\\s(?:\\[[A-Z]+\\+])?\\s.+: .+$");

    public static boolean handleNotifications(String message) {
        assert MinecraftClient.getInstance().player != null;
        if (USER_SENT_MESSAGE_PATTERN.matcher(message).find() || message.startsWith("Party >") || message.contains(">"))
            return true;
        if (!ModConfig.INSTANCE.partyInviteNotifications) return true;
        if (message.contains("has invited you to join their party!")) {
            String username = message.replaceAll("-", "").replaceAll("\\[[^]]*] ?", "").split(" ")[0];
            OnScreenNotification.renderNotification("PARTY INVITE", username + " is inviting you to their party. Click here to join.\nRight click to dismiss.", 1200);
            return false;
        }
        return true;
    }
}