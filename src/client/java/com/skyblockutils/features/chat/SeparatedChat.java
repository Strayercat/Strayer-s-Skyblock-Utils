package com.skyblockutils.features.chat;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.hud.SeparatedChatHud;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

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
    private static final long REWRITE_WINDOW_MS = 2_000;
    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{L}\\p{N}]");

    public record HistoryEntry(ChatLayout.ParsedMessage message, String searchText) {}

    private static String lastClean = null;
    private static ChatLayout.ParsedMessage lastBase = null;
    private static int duplicateCount = 1;

    private record Recent(String key, long at, HistoryEntry entry, SystemChat chat) {}

    private static final List<Recent> recent = new ArrayList<>();

    private static class SystemChat {
        private ChatLayout.ParsedMessage message;
        private int tickTime = 0;
        private ChatLayout.ParsedMessage base;

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
        if (ModConfig.INSTANCE.systemDuplicateCounter && cleanMessage.equals(lastClean) && lastBase != null && !history.isEmpty()) {
            addDuplicate(cleanMessage);
            return;
        }

        ChatLayout.ParsedMessage parsed = ChatLayout.parse(message);
        lastClean = cleanMessage;
        lastBase = parsed;
        duplicateCount = 1;
        HistoryEntry entry = new HistoryEntry(parsed, cleanMessage.toLowerCase(Locale.ROOT));
        SystemChat chat = new SystemChat(parsed);
        chat.base = parsed;
        systemChats.add(chat);
        history.add(entry);
        if (history.size() > MAX_HISTORY) history.removeFirst();
        remember(cleanMessage, entry, chat);

        SeparatedChatHud.onMessageAdded(entry);
    }

    private static void remember(String cleanMessage, HistoryEntry entry, SystemChat chat) {
        long now = System.currentTimeMillis();
        recent.removeIf(r -> now - r.at() > REWRITE_WINDOW_MS);
        String key = rewriteKey(cleanMessage);
        if (!key.isEmpty()) recent.add(new Recent(key, now, entry, chat));
    }

    public static boolean replaceRewrite(Component message) {
        if (!ModConfig.INSTANCE.separateMessage || recent.isEmpty()) return false;
        long now = System.currentTimeMillis();
        recent.removeIf(r -> now - r.at() > REWRITE_WINDOW_MS);
        String cleanMessage = clean(message);
        String key = rewriteKey(cleanMessage);
        if (key.isEmpty()) return false;

        for (int i = recent.size() - 1; i >= 0; i--) {
            Recent match = recent.get(i);
            if (!match.key().equals(key)) continue;
            recent.remove(i);

            ChatLayout.ParsedMessage parsed = ChatLayout.parse(message);
            int index = history.indexOf(match.entry());
            if (index != -1) history.set(index, new HistoryEntry(parsed, cleanMessage.toLowerCase(Locale.ROOT)));
            match.chat().message = parsed;
            match.chat().base = parsed;
            if (lastBase == match.entry().message()) lastBase = parsed;
            return true;
        }
        return false;
    }

    private static String rewriteKey(String cleanMessage) {
        return NON_ALNUM.matcher(cleanMessage).replaceAll("").toLowerCase(Locale.ROOT);
    }

    private static void addDuplicate(String cleanMessage) {
        duplicateCount++;
        ChatLayout.ParsedMessage counted = withCounter(lastBase, duplicateCount);
        history.set(history.size() - 1, new HistoryEntry(counted, cleanMessage.toLowerCase(Locale.ROOT)));

        SystemChat latest = systemChats.isEmpty() ? null : systemChats.getLast();
        if (latest != null && latest.base == lastBase) {
            latest.message = counted;
            latest.tickTime = 0;
        } else {
            SystemChat chat = new SystemChat(counted);
            chat.base = lastBase;
            systemChats.add(chat);
        }
    }

    private static ChatLayout.ParsedMessage withCounter(ChatLayout.ParsedMessage base, int count) {
        List<ChatLayout.ParsedLine> lines = new ArrayList<>(base.lines());
        for (int i = lines.size() - 1; i >= 0; i--) {
            ChatLayout.ParsedLine line = lines.get(i);
            if (line.type() == ChatLayout.LineType.SEPARATOR) continue;
            lines.set(i, new ChatLayout.ParsedLine(line.type(), line.component().copy().append(counterComponent(count)),
                    line.style(), line.codes(), line.separatorChar()));
            break;
        }
        return new ChatLayout.ParsedMessage(lines);
    }

    private static Component counterComponent(int count) {
        String text = " " + ModConfig.INSTANCE.systemDuplicateCounterText.replace("$", String.valueOf(count));
        Style base = Style.EMPTY.withColor(ModConfig.INSTANCE.systemDuplicateCounterColor & 0xFFFFFF);
        MutableComponent result = Component.empty();
        Style style = base;
        StringBuilder segment = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                ChatFormatting format = ChatFormatting.getByCode(Character.toLowerCase(text.charAt(i + 1)));
                if (format != null) {
                    if (!segment.isEmpty()) {
                        result.append(Component.literal(segment.toString()).setStyle(style));
                        segment.setLength(0);
                    }
                    style = switch (format) {
                        case RESET -> base;
                        case OBFUSCATED -> style.withObfuscated(true);
                        case BOLD -> style.withBold(true);
                        case STRIKETHROUGH -> style.withStrikethrough(true);
                        case UNDERLINE -> style.withUnderlined(true);
                        case ITALIC -> style.withItalic(true);
                        default -> Style.EMPTY.withColor(format);
                    };
                    i++;
                    continue;
                }
            }
            segment.append(c);
        }
        if (!segment.isEmpty()) result.append(Component.literal(segment.toString()).setStyle(style));
        return result;
    }

    private static String clean(Component message) {
        return message.getString().replaceAll("§.", "").trim();
    }

    private static boolean isUserMessage(String clean) {
        if (clean.startsWith("From stash: ")) return false;
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
            recent.clear();
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