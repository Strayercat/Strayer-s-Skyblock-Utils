package com.skyblockutils.features.hud;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.ModKeyBindings;
import com.skyblockutils.features.chat.ChatLayout;
import com.skyblockutils.features.chat.MessageFeed;
import com.skyblockutils.features.hud.layout.HudElement;
import com.skyblockutils.features.hud.layout.HudLayoutEntry;
import com.skyblockutils.utils.ClipboardUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class LogHud {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static boolean enabled = false;

    public static final MessageFeed FEED = new MessageFeed(500, () -> true);
    public static final MessageHud HUD = new MessageHud(
            "log_hud", "SSU Log", "SSU Log", "No logs yet", "Log copied to clipboard",
            FEED, () -> ModKeyBindings.LOG_HISTORY_KEY,
            () -> enabled, () -> true,
            () -> new HudLayoutEntry(HudLayoutEntry.Anchor.START, HudLayoutEntry.Anchor.START, 0, 0, 1f));
    public static final HudElement ELEMENT = HUD.element;

    private LogHud() {
    }

    public static void toggle() {
        enabled = !enabled;
        ModFunctions.sendSystemMessage(enabled ? "§aLog HUD shown" : "§cLog HUD hidden", false);
    }

    public static void addLog(String message) {
        addLog(Component.literal(message));
    }

    public static void addLog(Component message) {
        Component line = Component.empty()
                .append(Component.literal(LocalTime.now().format(TIME) + " ").withColor(0xFF555555))
                .append(message);
        Minecraft client = Minecraft.getInstance();
        if (client.isSameThread()) FEED.add(line);
        else client.execute(() -> FEED.add(line));
    }

    public static void addLog(String level, String logger, String message) {
        String color = switch (level) {
            case "ERROR", "FATAL" -> "§c";
            case "WARN" -> "§e";
            case "DEBUG", "TRACE" -> "§8";
            default -> "§f";
        };
        String source = logger.startsWith("SSU ") ? logger.substring(4) : logger;
        addLog(color + "[" + source + "] " + message);
    }

    public static void copy() {
        List<String> lines = new ArrayList<>();
        for (MessageFeed.HistoryEntry entry : FEED.history()) {
            for (ChatLayout.ParsedLine line : entry.message().lines()) {
                if (line.type() == ChatLayout.LineType.SEPARATOR) continue;
                String text = line.component().getString().replaceAll("§.", "").strip();
                if (!text.isEmpty()) lines.add(text);
            }
        }
        if (lines.isEmpty()) {
            ModFunctions.sendSystemMessage("§cThe log is empty", false);
            return;
        }
        ClipboardUtils.copyTextToClipboard(String.join("\n", lines));
        ModFunctions.sendSystemMessage("§aCopied " + lines.size() + " log lines to your clipboard", false);
    }

    public static void clear() {
        FEED.clear();
    }

    public static void tick() {
        if (!enabled) {
            FEED.clearLive();
            return;
        }
        FEED.tick();
        HUD.tick();
    }

    public static void render(GuiGraphicsExtractor graphics) {
        HUD.render(graphics);
    }
}
