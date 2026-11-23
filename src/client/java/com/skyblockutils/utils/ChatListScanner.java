package com.skyblockutils.utils;

import com.skyblockutils.ModFunctions;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public abstract class ChatListScanner {
    private static final long REFRESH_MS = 10 * 60_000;
    private static final long MANUAL_WINDOW_MS = 3_000;
    private static final long EXPECT_MS = 5_000;

    private final String name;
    private final String command;
    private final Pattern manualCommand;

    protected boolean requested = false;
    private long lastRequest = 0;
    private long manualUntil = 0;
    private long expectingUntil = 0;
    private boolean sendingOwnCommand = false;
    private boolean awaitingResponse = false;
    private boolean reportNext = false;

    public final ChatBlockRouter.Handler blockHandler = new ChatBlockRouter.Handler() {
        @Override
        public boolean isActive() {
            long now = System.currentTimeMillis();
            return now < expectingUntil && now >= manualUntil;
        }

        @Override
        public boolean matches(String header) {
            return isStartLine(header);
        }

        @Override
        public void accept(ChatBlockRouter.Block block) {
            List<String> lines = block.allTexts();
            expectingUntil = 0;
            awaitingResponse = false;
            parse(lines);
            if (reportNext) {
                reportNext = false;
                ModFunctions.sendSystemMessage("§a[Dev] " + name + " scan: " + summary(lines), false);
            }
        }
    };

    protected ChatListScanner(String name, String command, Pattern manualCommand) {
        this.name = name;
        this.command = command;
        this.manualCommand = manualCommand;
    }

    protected abstract boolean isStartLine(String text);

    protected abstract void parse(List<String> lines);

    protected abstract String summary(List<String> lines);

    protected boolean refreshDue(long now) {
        return false;
    }

    protected void onTimeout() {
    }

    protected void beforeRequest() {
    }

    public boolean isExpecting() {
        return System.currentTimeMillis() < expectingUntil;
    }

    public void tickCapture() {
        if (!awaitingResponse || isExpecting()) return;
        awaitingResponse = false;
        onTimeout();
        if (reportNext) {
            reportNext = false;
            ModFunctions.sendSystemMessage("§c[Dev] " + name + " scan got no response", false);
        }
    }

    public void tick() {
        if (isExpecting() || ModFunctions.isWorldNotReady()) return;
        long now = System.currentTimeMillis();
        if (requested && !refreshDue(now) && now - lastRequest < REFRESH_MS) return;
        requested = true;
        requestList();
    }

    public void requestList() {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;
        beforeRequest();
        lastRequest = System.currentTimeMillis();
        awaitingResponse = true;
        expectingUntil = lastRequest + EXPECT_MS;
        sendingOwnCommand = true;
        try {
            connection.sendCommand(command);
        } finally {
            sendingOwnCommand = false;
        }
    }

    public void devScan() {
        if (Minecraft.getInstance().getConnection() == null) return;
        if (isExpecting()) {
            ModFunctions.sendSystemMessage("§e[Dev] " + name + " scan already in progress", false);
            return;
        }
        reportNext = true;
        ModFunctions.sendSystemMessage("§7[Dev] Scanning " + name.toLowerCase(Locale.ROOT) + "...", false);
        requestList();
    }

    public void onCommandSent(String sent) {
        if (sendingOwnCommand) return;
        if (!manualCommand.matcher(sent.toLowerCase(Locale.ROOT).trim()).matches()) return;
        manualUntil = System.currentTimeMillis() + MANUAL_WINDOW_MS;
        awaitingResponse = false;
        expectingUntil = 0;
    }

    public void reset() {
        requested = false;
        awaitingResponse = false;
        reportNext = false;
        expectingUntil = 0;
    }
}
