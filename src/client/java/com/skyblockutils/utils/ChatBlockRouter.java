package com.skyblockutils.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class ChatBlockRouter {
    public interface Handler {
        boolean isActive();

        boolean matches(String header);

        void accept(Block block);
    }

    public record Block(Component header, List<Component> lines) {
        public String headerText() {
            return clean(header);
        }

        public List<String> texts() {
            return lines.stream().map(ChatBlockRouter::clean).toList();
        }

        public List<String> allTexts() {
            List<String> all = new ArrayList<>();
            all.add(headerText());
            all.addAll(texts());
            return all;
        }
    }

    private enum State { IDLE, HELD, READING }

    private static final long HOLD_MS = 750;
    private static final long READ_MS = 1_500;

    private static final List<Handler> handlers = new ArrayList<>();
    private static Consumer<Component> releaseSink = message -> Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(message);

    private static State state = State.IDLE;
    private static long lastMessageAt = 0;
    private static Component heldSeparator;
    private static Component header;
    private static Handler current;
    private static final List<Component> buffer = new ArrayList<>();

    private ChatBlockRouter() {
    }

    public static void register(Handler... toRegister) {
        handlers.addAll(List.of(toRegister));
    }

    public static void setReleaseSink(Consumer<Component> sink) {
        releaseSink = sink;
    }

    public static boolean handle(Component message) {
        long now = System.currentTimeMillis();
        String text = clean(message);

        if (text.contains("\n")) {
            flush();
            return !handleInlineBlock(message);
        }

        boolean separator = ChatSeparator.is(message);

        switch (state) {
            case IDLE -> {
                if (!separator || noneActive()) return true;
                hold(message, now);
                return false;
            }
            case HELD -> {
                if (separator) {
                    release();
                    if (noneActive()) return true;
                    hold(message, now);
                    return false;
                }
                if (text.isEmpty()) {
                    buffer.add(message);
                    lastMessageAt = now;
                    return false;
                }
                Handler handler = find(text);
                if (handler == null) {
                    release();
                    return true;
                }
                current = handler;
                header = message;
                buffer.clear();
                heldSeparator = null;
                state = State.READING;
                lastMessageAt = now;
                return false;
            }
            case READING -> {
                if (separator) {
                    finish();
                    return false;
                }
                buffer.add(message);
                lastMessageAt = now;
                return false;
            }
        }
        return true;
    }

    public static void tick() {
        if (state == State.IDLE) return;
        long elapsed = System.currentTimeMillis() - lastMessageAt;
        if (state == State.HELD && elapsed > HOLD_MS) release();
        else if (state == State.READING && elapsed > READ_MS) finish();
    }

    public static void reset() {
        flush();
    }

    private static boolean handleInlineBlock(Component message) {
        List<Component> lines = new ArrayList<>();
        for (Component line : splitLines(message)) {
            String lineText = clean(line);
            if (ChatSeparator.is(line)) continue;
            if (lines.isEmpty() && lineText.isEmpty()) continue;
            lines.add(line);
        }
        if (lines.isEmpty()) return false;

        Handler handler = find(clean(lines.getFirst()));
        if (handler == null) return false;

        handler.accept(new Block(lines.getFirst(), List.copyOf(lines.subList(1, lines.size()))));
        return true;
    }

    private static Handler find(String headerText) {
        for (Handler handler : handlers) {
            if (handler.isActive() && handler.matches(headerText)) return handler;
        }
        return null;
    }

    private static boolean noneActive() {
        for (Handler handler : handlers) {
            if (handler.isActive()) return false;
        }
        return true;
    }

    private static void hold(Component separator, long now) {
        heldSeparator = separator;
        buffer.clear();
        header = null;
        current = null;
        state = State.HELD;
        lastMessageAt = now;
    }

    private static void finish() {
        Handler handler = current;
        Block block = new Block(header, List.copyOf(buffer));
        clear();
        if (handler != null) handler.accept(block);
    }

    private static void release() {
        List<Component> released = new ArrayList<>();
        if (heldSeparator != null) released.add(heldSeparator);
        released.addAll(buffer);
        clear();
        released.forEach(releaseSink);
    }

    private static void flush() {
        if (state == State.HELD) release();
        else if (state == State.READING) finish();
    }

    private static void clear() {
        state = State.IDLE;
        heldSeparator = null;
        header = null;
        current = null;
        buffer.clear();
    }

    public static List<Component> splitLines(Component message) {
        List<Component> lines = new ArrayList<>();
        List<Component> parts = new ArrayList<>();
        message.visit((style, segment) -> {
            String[] split = segment.split("\n", -1);
            for (int i = 0; i < split.length; i++) {
                if (i > 0) {
                    lines.add(join(parts));
                    parts.clear();
                }
                if (!split[i].isEmpty()) parts.add(Component.literal(split[i]).setStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        lines.add(join(parts));
        return lines;
    }

    private static Component join(List<Component> parts) {
        MutableComponent joined = Component.empty();
        for (Component part : parts) joined.append(part);
        return joined;
    }

    public static String clean(Component message) {
        return message.getString().replaceAll("§.", "").trim();
    }
}
