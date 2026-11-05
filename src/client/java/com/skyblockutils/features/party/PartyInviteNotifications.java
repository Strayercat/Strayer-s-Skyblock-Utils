package com.skyblockutils.features.party;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.ChatBlockRouter;
import com.skyblockutils.utils.OnScreenNotification;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PartyInviteNotifications {
    private static final String INVITE_TEXT = "has invited you to join their party!";
    private static final Pattern EXPIRED_PATTERN = Pattern.compile("^The party invite from .+ has expired\\.$");

    public static List<String> previousInvites = new ArrayList<>();

    public static final ChatBlockRouter.Handler BLOCK_HANDLER = new ChatBlockRouter.Handler() {
        @Override
        public boolean isActive() {
            return ModConfig.INSTANCE.partyInviteNotifications;
        }

        @Override
        public boolean matches(String header) {
            return (header.endsWith(INVITE_TEXT) && !header.contains(":")) || EXPIRED_PATTERN.matcher(header).matches();
        }

        @Override
        public void accept(ChatBlockRouter.Block block) {
            String header = block.headerText();
            if (!header.endsWith(INVITE_TEXT)) return;

            String username = header.replaceAll("\\[[^]]*] ?", "").trim().split("\\s+")[0];
            OnScreenNotification.builder()
                    .title("PARTY INVITE")
                    .subtitle(username + " is inviting you to their party.\nClick here to join.")
                    .tickTime(1300)
                    .send();

            previousInvites.remove(username);
            previousInvites.add(username);
        }
    };
}
