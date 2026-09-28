package com.skyblockutils.features.chat;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.hud.SeparatedChatHud;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class SeparatedChat {
    private static final List<SystemChat> systemChats = new ArrayList<>();
    private static final List<HistoryEntry> history = new ArrayList<>();
    private static final int MESSAGE_TIME_TICK = 100;
    private static final int MAX_HISTORY = 300;
    private static final Pattern USER_SENT_MESSAGE_PATTERN = Pattern.compile("^\\[\\d{1,4}] .+?: .*$");
    private static final Pattern USER_SHOW_PATTERN = Pattern.compile(".+? (?:is holding|is wearing|is friends with a|has) \\[.+]$");
    private static final Pattern LEVEL_PREFIX_PATTERN = Pattern.compile("^\\[\\d{1,4}] .+");
    private static final List<String> USER_CHANNEL_PREFIXES = List.of("Party > ", "Guild > ", "Officer > ", "Co-op > ", "From ", "To ");

    public record HistoryEntry(ChatLayout.ParsedMessage message, String searchText) {}

    private static class SystemChat {
        private final ChatLayout.ParsedMessage message;
        private int tickTime = 0;

        SystemChat(ChatLayout.ParsedMessage message) {
            this.message = message;
        }

        void tick() {
            tickTime++;
        }

        boolean expired() {
            return tickTime > MESSAGE_TIME_TICK;
        }
    }

    public static boolean handleMessage(Component message, boolean overlay) {
        if (overlay) return true;
        if (!ModConfig.INSTANCE.separateMessage) return true;
        String cleanMessage = clean(message);
        if (isUserMessage(cleanMessage)) return true;
        if (cleanMessage.matches("You are now in the .* channel") || cleanMessage.matches("Friend > .* (left|joined)\\.")) return true;

        add(message, cleanMessage);
        return false;
    }

    public static void addMessage(Component message) {
        add(message, clean(message));
    }

    private static void add(Component message, String cleanMessage) {
        ChatLayout.ParsedMessage parsed = ChatLayout.parse(message);
        HistoryEntry entry = new HistoryEntry(parsed, cleanMessage.toLowerCase(Locale.ROOT));
        systemChats.add(new SystemChat(parsed));
        history.add(entry);
        if (history.size() > MAX_HISTORY) history.removeFirst();

        SeparatedChatHud.onMessageAdded(entry);
    }

    private static String clean(Component message) {
        return message.getString().replaceAll("§.", "").trim();
    }

    private static boolean isUserMessage(String clean) {
        if (USER_SENT_MESSAGE_PATTERN.matcher(clean).matches()) return true;
        if (LEVEL_PREFIX_PATTERN.matcher(clean).matches() && USER_SHOW_PATTERN.matcher(clean).matches()) return true;

        for (String prefix : USER_CHANNEL_PREFIXES) {
            if (!clean.startsWith(prefix)) continue;
            if (clean.contains(": ")) return true;
            if (USER_SHOW_PATTERN.matcher(clean.substring(prefix.length())).matches()) return true;
        }
        return false;
    }

    public static void tickAllMessages() {
        if (!ModConfig.INSTANCE.separateMessage) {
            systemChats.clear();
            return;
        }

        Iterator<SystemChat> it = systemChats.iterator();
        while (it.hasNext()) {
            SystemChat chat = it.next();
            chat.tick();
            if (chat.expired()) it.remove();
        }
    }

    public static List<ChatLayout.ParsedMessage> activeMessages() {
        List<ChatLayout.ParsedMessage> messages = new ArrayList<>(systemChats.size());
        for (SystemChat chat : systemChats) messages.add(chat.message);
        return messages;
    }

    public static List<HistoryEntry> history() {
        return Collections.unmodifiableList(history);
    }
}