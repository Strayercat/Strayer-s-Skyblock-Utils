package com.skyblockutils.render;

import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.regex.Pattern;

public final class EggClickFilter {
    private static final Pattern FOUND = Pattern.compile("^(\\d+ eggs? remains?|you found .*egg).*", Pattern.CASE_INSENSITIVE);
    private static final String ALREADY_FOUND = "You already found this egg!";
    private static final long CLICK_WINDOW_MS = 1500;
    private static long clickTick = Long.MIN_VALUE;
    private static long clickTime;
    private static int clickId;
    private static int foundClickId = -1;

    private EggClickFilter() {
    }

    public static void handleClick(Level level, BlockHitResult hit) {
        if (!EggDetector.isPlayerHead(level.getBlockState(hit.getBlockPos()))) return;
        long tick = level.getGameTime();
        if (tick == clickTick) return;
        clickTick = tick;
        clickTime = Util.getMillis();
        clickId++;
    }

    public static boolean handleMessage(String message) {
        if (Util.getMillis() - clickTime > CLICK_WINDOW_MS) return true;
        if (FOUND.matcher(message).matches()) {
            foundClickId = clickId;
            return true;
        }
        return !(message.equals(ALREADY_FOUND) && foundClickId == clickId);
    }
}
