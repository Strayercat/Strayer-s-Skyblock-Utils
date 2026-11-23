package com.skyblockutils.features.chat;

import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.hud.SeparatedChatHud;
import com.skyblockutils.utils.TextUtils;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class SeparatedChat {
    public static final MessageFeed FEED = new MessageFeed(300, () -> ModConfig.INSTANCE.systemDuplicateCounter);
    private static final Pattern USER_SENT_MESSAGE_PATTERN = Pattern.compile("^\\[\\d{1,4}] .+?: .*$");
    private static final Pattern USER_SHOW_PATTERN = Pattern.compile(".+? (?:is holding|is wearing|is friends with a|has) \\[.+]$");
    private static final Pattern LEVEL_PREFIX_PATTERN = Pattern.compile("^\\[\\d{1,4}] .+");
    private static final List<String> USER_CHANNEL_PREFIXES = List.of("Party > ", "Guild > ", "Officer > ", "Co-op > ", "From ", "To ");
    private static final long REWRITE_WINDOW_MS = 2_000;
    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{L}\\p{N}]");

    private record Recent(String key, long at, MessageFeed.Handle handle) {}

    private static final List<Recent> recent = new ArrayList<>();

    private static String incomingKey = null;
    private static boolean incomingSeparates = false;
    private static boolean incomingRouted = false;

    public static void beginIncoming(Component message, boolean overlay) {
        incomingRouted = false;
        if (overlay) {
            incomingKey = null;
            incomingSeparates = false;
            return;
        }
        String cleanMessage = TextUtils.clean(message);
        incomingKey = rewriteKey(cleanMessage);
        incomingSeparates = StrayersSkyblockUtilsClient.isInSkyblock && separates(cleanMessage);
    }

    public static void endIncoming() {
        incomingKey = null;
        incomingSeparates = false;
        incomingRouted = false;
    }

    public static boolean handleMessage(Component message, boolean overlay) {
        if (overlay) return true;
        if (!SeparatedChatHud.separatingHere()) return true;
        String cleanMessage = TextUtils.clean(message);
        if (!separates(cleanMessage)) return true;
        if (incomingRouted && rewriteKey(cleanMessage).equals(incomingKey)) return false;

        add(message, cleanMessage);
        return false;
    }

    private static boolean separates(String cleanMessage) {
        if (!SeparatedChatHud.separatingHere()) return false;
        if (isUserMessage(cleanMessage)) return false;
        return !cleanMessage.matches("You are now in the .* channel") && !cleanMessage.matches("Friend > .* (left|joined)\\.");
    }

    public static void addMessage(Component message) {
        add(message, TextUtils.clean(message));
    }

    private static void add(Component message, String cleanMessage) {
        MessageFeed.Handle handle = FEED.add(message, cleanMessage);
        if (handle == null) return;
        long now = System.currentTimeMillis();
        recent.removeIf(r -> now - r.at() > REWRITE_WINDOW_MS);
        String key = rewriteKey(cleanMessage);
        if (!key.isEmpty()) recent.add(new Recent(key, now, handle));
    }

    public static boolean replaceRewrite(Component message) {
        if (!SeparatedChatHud.separatingHere() || (recent.isEmpty() && !incomingSeparates)) return false;
        long now = System.currentTimeMillis();
        recent.removeIf(r -> now - r.at() > REWRITE_WINDOW_MS);
        String cleanMessage = TextUtils.clean(message);
        String key = rewriteKey(cleanMessage);
        if (key.isEmpty()) return false;

        for (int i = recent.size() - 1; i >= 0; i--) {
            Recent match = recent.get(i);
            if (!match.key().equals(key)) continue;
            recent.remove(i);
            FEED.replace(match.handle(), message, cleanMessage);
            return true;
        }

        if (incomingSeparates && key.equals(incomingKey)) {
            add(message, cleanMessage);
            incomingRouted = true;
            return true;
        }
        return false;
    }

    private static String rewriteKey(String cleanMessage) {
        return NON_ALNUM.matcher(cleanMessage).replaceAll("").toLowerCase(Locale.ROOT);
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
            FEED.clearLive();
            recent.clear();
            return;
        }
        FEED.tick();
    }
}
