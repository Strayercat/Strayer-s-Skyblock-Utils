package com.skyblockutils.features.chat;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class MessageFeed {
    private static final int LIFETIME_TICKS = 100;

    public record HistoryEntry(ChatLayout.ParsedMessage message, String searchText) {}

    public static final class Handle {
        private HistoryEntry entry;
        private final Live live;

        private Handle(HistoryEntry entry, Live live) {
            this.entry = entry;
            this.live = live;
        }
    }

    private static final class Live {
        private ChatLayout.ParsedMessage message;
        private ChatLayout.ParsedMessage base;
        private int ticks = 0;

        private Live(ChatLayout.ParsedMessage message) {
            this.message = message;
            this.base = message;
        }
    }

    private final int maxHistory;
    private final BooleanSupplier duplicateCounter;
    private final List<Live> live = new ArrayList<>();
    private final List<HistoryEntry> history = new ArrayList<>();
    private Consumer<HistoryEntry> listener = _ -> {};

    private String lastClean = null;
    private ChatLayout.ParsedMessage lastBase = null;
    private int duplicateCount = 1;

    public MessageFeed(int maxHistory, BooleanSupplier duplicateCounter) {
        this.maxHistory = maxHistory;
        this.duplicateCounter = duplicateCounter;
    }

    public void onAdded(Consumer<HistoryEntry> listener) {
        this.listener = listener;
    }

    public Handle add(Component message) {
        return add(message, TextUtils.clean(message));
    }

    public Handle add(Component message, String cleanMessage) {
        if (duplicateCounter.getAsBoolean() && cleanMessage.equals(lastClean) && lastBase != null && !history.isEmpty()) {
            addDuplicate(cleanMessage);
            return null;
        }

        ChatLayout.ParsedMessage parsed = ChatLayout.parse(message);
        lastClean = cleanMessage;
        lastBase = parsed;
        duplicateCount = 1;
        HistoryEntry entry = new HistoryEntry(parsed, cleanMessage.toLowerCase(Locale.ROOT));
        Live chat = new Live(parsed);
        live.add(chat);
        history.add(entry);
        if (history.size() > maxHistory) history.removeFirst();
        listener.accept(entry);
        return new Handle(entry, chat);
    }

    public void replace(Handle handle, Component message, String cleanMessage) {
        ChatLayout.ParsedMessage parsed = ChatLayout.parse(message);
        HistoryEntry replaced = new HistoryEntry(parsed, cleanMessage.toLowerCase(Locale.ROOT));
        int index = history.indexOf(handle.entry);
        if (index != -1) history.set(index, replaced);
        if (lastBase == handle.entry.message()) lastBase = parsed;
        handle.entry = replaced;
        handle.live.message = parsed;
        handle.live.base = parsed;
    }

    private void addDuplicate(String cleanMessage) {
        duplicateCount++;
        ChatLayout.ParsedMessage counted = withCounter(lastBase, duplicateCount);
        history.set(history.size() - 1, new HistoryEntry(counted, cleanMessage.toLowerCase(Locale.ROOT)));

        Live latest = live.isEmpty() ? null : live.getLast();
        if (latest != null && latest.base == lastBase) {
            latest.message = counted;
            latest.ticks = 0;
        } else {
            Live chat = new Live(counted);
            chat.base = lastBase;
            live.add(chat);
        }
    }

    public void tick() {
        live.removeIf(chat -> ++chat.ticks > LIFETIME_TICKS);
    }

    public void clearLive() {
        live.clear();
    }

    public void clear() {
        live.clear();
        history.clear();
        lastClean = null;
        lastBase = null;
        duplicateCount = 1;
    }

    public List<ChatLayout.ParsedMessage> activeMessages() {
        List<ChatLayout.ParsedMessage> messages = new ArrayList<>(live.size());
        for (Live chat : live) messages.add(chat.message);
        return messages;
    }

    public List<HistoryEntry> history() {
        return Collections.unmodifiableList(history);
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
}
