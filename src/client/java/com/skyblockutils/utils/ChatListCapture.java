package com.skyblockutils.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ChatListCapture {
    private static final long EXPECT_TIMEOUT_MS = 5_000;
    private static final long HOLD_TIMEOUT_MS = 750;
    private static final long READ_TIMEOUT_MS = 1_500;
    private static final long TRAILING_GRACE_MS = 1_000;

    private final Predicate<String> isStart;
    private final Predicate<String> isLine;
    private final Consumer<List<String>> onComplete;
    private final boolean swallowTrailingSeparator;

    private boolean expecting = false;
    private boolean reading = false;
    private long expectingSince = 0;
    private long lastLineAt = 0;
    private long heldAt = 0;
    private long finishedAt = 0;
    private Component heldSeparator;
    private final List<String> lines = new ArrayList<>();

    public ChatListCapture(Predicate<String> isStart, Predicate<String> isLine, Consumer<List<String>> onComplete, boolean swallowTrailingSeparator) {
        this.isStart = isStart;
        this.isLine = isLine;
        this.onComplete = onComplete;
        this.swallowTrailingSeparator = swallowTrailingSeparator;
    }

    public boolean isExpecting() {
        return expecting;
    }

    public void expect() {
        releaseHeld();
        reset();
        expecting = true;
        expectingSince = System.currentTimeMillis();
    }

    public void cancel() {
        releaseHeld();
        reset();
    }

    public void tick() {
        long now = System.currentTimeMillis();
        if (heldSeparator != null && now - heldAt > HOLD_TIMEOUT_MS) releaseHeld();

        if (reading) {
            if (now - lastLineAt > READ_TIMEOUT_MS) complete();
        } else if (expecting && heldSeparator == null && now - expectingSince > EXPECT_TIMEOUT_MS) {
            reset();
        }
    }

    public boolean handle(Component message) {
        long now = System.currentTimeMillis();
        boolean separator = ChatSeparator.is(message);

        if (!expecting) {
            if (separator && swallowTrailingSeparator && now - finishedAt < TRAILING_GRACE_MS) {
                finishedAt = 0;
                return false;
            }
            return true;
        }

        String text = message.getString().replaceAll("§.", "").trim();

        if (text.contains("\n")) {
            List<String> block = splitBlock(text);
            if (!block.isEmpty() && isStart.test(block.getFirst())) {
                heldSeparator = null;
                lines.clear();
                lines.addAll(block);
                complete();
                return false;
            }
            releaseHeld();
            return true;
        }

        if (reading) {
            if (separator) {
                complete();
                return false;
            }
            if (isStart.test(text) || isLine.test(text)) {
                lines.add(text);
                lastLineAt = now;
                return false;
            }
            return true;
        }

        if (separator) {
            releaseHeld();
            heldSeparator = message;
            heldAt = now;
            return false;
        }

        if (isStart.test(text)) {
            heldSeparator = null;
            reading = true;
            lines.clear();
            lines.add(text);
            lastLineAt = now;
            return false;
        }

        releaseHeld();
        return true;
    }

    private void complete() {
        List<String> result = List.copyOf(lines);
        reset();
        finishedAt = System.currentTimeMillis();
        onComplete.accept(result);
    }

    private void reset() {
        expecting = false;
        reading = false;
        heldSeparator = null;
        lines.clear();
    }

    private void releaseHeld() {
        if (heldSeparator == null) return;
        Component separator = heldSeparator;
        heldSeparator = null;
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(separator);
    }

    private static List<String> splitBlock(String text) {
        List<String> block = new ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (isSeparatorText(trimmed)) continue;
            if (block.isEmpty() && trimmed.isEmpty()) continue;
            block.add(trimmed);
        }
        return block;
    }

    private static boolean isSeparatorText(String text) {
        return text.length() >= 5 && text.chars().allMatch(c -> c == '-' || c == '▬');
    }
}
