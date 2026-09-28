package com.skyblockutils.features.chat;

import com.skyblockutils.mixin.client.ChatComponentAccessor;
import com.skyblockutils.utils.SSUIndicator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatModifications {
    private static final Pattern CHAT_SENDER = Pattern.compile(
            "^(?:(Guild|Party|Officer|Co-op) > |(From) )?(?:\\[\\d+] )?(?:[^\\w\\s\\[]+ )?(?:\\[([^]]+)] )?(\\w{3,16})(?: \\[[^]]+])?: ");

    public static String fancyEmotes(String message) {
        return message.replace("<3", "❤")
                .replace("\\o/", "¯\\_(ツ)_/¯")
                .replace("o/", "( ﾟ◡ﾟ)/")
                .replace("O/", "( ﾟ◡ﾟ)/")
                .replace(":skull:", "☠");
    }

    public static Component withChatBadge(Component message) {
        String text = message.getString().replaceAll("§.", "");
        if (SSUIndicator.hasBadge(text)) return message;

        Matcher m = CHAT_SENDER.matcher(text);
        if (!m.find()) return message;
        if ("NPC".equals(m.group(3))) return message;

        String sender = m.group(4);
        if (!SSUIndicator.isUser(sender)) {
            if (m.group(1) != null || m.group(2) != null) SSUIndicator.onChatSender(sender);
            return message;
        }

        return badge(message, sender);
    }

    public static void badgeExistingMessages(String senderKey) {
        Minecraft client = Minecraft.getInstance();
        ChatComponentAccessor chat = (ChatComponentAccessor) client.gui.hud.getChat();
        List<GuiMessage> messages = chat.getAllMessages();

        boolean changed = false;
        for (int i = 0; i < messages.size(); i++) {
            GuiMessage message = messages.get(i);
            if (!senderKey.equals(senderOf(message.content()))) continue;

            messages.set(i, new GuiMessage(message.addedTime(), badge(message.content(), senderKey), message.signature(), GuiMessageSource.SYSTEM_SERVER, message.tag()));
            changed = true;
        }

        if (changed) chat.invokeRefreshTrimmedMessages();
    }

    public static Component fitToChat(Component message) {
        ChatLayout.ParsedMessage parsed = ChatLayout.parse(message);
        if (!parsed.hasLayoutLines()) return message;
        return ChatLayout.toChatComponent(parsed, ChatLayout.wrapWidth(), Minecraft.getInstance().font);
    }

    private static String senderOf(Component message) {
        String text = message.getString().replaceAll("§.", "");
        if (SSUIndicator.hasBadge(text)) return null;

        Matcher m = CHAT_SENDER.matcher(text);
        if (!m.find() || "NPC".equals(m.group(3))) return null;
        return m.group(4).toLowerCase(Locale.ROOT);
    }

    private static Component badge(Component message, String sender) {
        MutableComponent out = Component.literal(SSUIndicator.glyphFor(sender) + " ").withColor(0xFFFFFF);
        return out.append(message);
    }
}
