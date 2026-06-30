package com.skyblockutils.features.party;

import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.config.ModConfig;
import net.minecraft.client.Minecraft;

import java.util.regex.Pattern;

public class PartyInviteNotifications {
    private static final Pattern USER_SENT_MESSAGE_PATTERN = Pattern.compile("^\\[\\d{1,3}]\\s.*?\\s(?:\\[[A-Z]+\\+])?\\s.+: .+$");

    public static boolean handleNotifications(String message) {
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
            return false;
        }
        return true;
    }
}