package com.skyblockutils.features.chat;

import com.skyblockutils.utils.TabListIndicator;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatModifications {
    private static final Pattern CHAT_SENDER = Pattern.compile(
            "^(?:(?:Guild|Party|Officer|Co-op) > |From )?(?:\\[\\d+] )?(?:[^\\w\\s\\[]+ )?(?:\\[([^]]+)] )?(\\w{3,16})(?: \\[[^]]+])?: ");

    public static String fancyEmotes(String message) {
        return message.replace("<3", "❤")
                .replace("\\o/", "¯\\_(ツ)_/¯")
                .replace("o/", "( ﾟ◡ﾟ)/")
                .replace("O/", "( ﾟ◡ﾟ)/")
                .replace(":skull:", "☠");
    }

    public static Component withChatBadge(Component message) {
        String text = message.getString().replaceAll("§.", "");
        if (text.startsWith(TabListIndicator.BADGE_GLYPH)) return message;

        Matcher m = CHAT_SENDER.matcher(text);
        if (!m.find()) return message;
        if ("NPC".equals(m.group(1))) return message;

        String sender = m.group(2);
        if (!TabListIndicator.isUser(sender)) {
            TabListIndicator.requestCheck(sender);
            return message;
        }

        MutableComponent out = Component.literal(TabListIndicator.BADGE_GLYPH + " ").withColor(0xFFFFFF);
        return out.append(message);
    }
}