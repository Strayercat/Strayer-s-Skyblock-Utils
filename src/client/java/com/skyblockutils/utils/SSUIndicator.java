package com.skyblockutils.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockutils.features.chat.ChatModifications;
import com.skyblockutils.features.voice.VoiceChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SSUIndicator {
    private static final URI WS_URI = URI.create("wss://cawcah.duckdns.org/ssu/ws");
    private static final int PROTOCOL = 2;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static final Identifier BADGE_BIG = Identifier.fromNamespaceAndPath("skyblockutils", "ssu_gem_big");

    private static final int SPECIAL_GLYPH_BASE = 0xE7B0;
    private static final int SPECIAL_GLYPH_FRAMES = 20;
    private static final long SPECIAL_GLYPH_FRAME_MS = 200;
    private static final long PRISMATIC_CYCLE_MS = 3_000;

    private static final Pattern SB_TAB_NAME = Pattern.compile("\\[\\d+] (?:[^\\w\\s\\[]+ )?(?:\\[[^]]+] )*(\\w{3,16})");
    private static final Pattern VALID_NAME = Pattern.compile("^\\w{3,16}$");

    private static final int MAX_NAMES_PER_QUERY = 100;
    private static final long[] PROBATION_OFFSETS = {0, 2_000, 5_000, 10_000, 15_000};
    private static final long REVERIFY_MS = 60_000;
    private static final long NEGATIVE_TTL_MS = 60_000;
    private static final long CHAT_RELEVANCE_MS = 10 * 60_000;
    private static final long MIN_BACKOFF = 5_000;
    private static final long MAX_BACKOFF = 5 * 60_000;
    private static final int LEAVE_GRACE_TICKS = 20 * 5;

    private static final Map<String, Long> confirmed = new HashMap<>();
    private static final Map<String, GemColor> colors = new HashMap<>();
    private static final Map<String, Probation> probation = new HashMap<>();
    private static final Set<String> nonUsers = new HashSet<>();
    private static final Map<String, Long> negativeUntil = new HashMap<>();
    private static final Map<String, Long> chatSeen = new HashMap<>();
    private static final Set<String> partyWatch = new HashSet<>();
    private static final Set<String> guildWatch = new HashSet<>();
    private static final Set<String> coopWatch = new HashSet<>();
    private static final Set<String> queued = new LinkedHashSet<>();
    private static final Set<String> inFlight = new HashSet<>();
    private static final Set<String> requeueOnReady = new HashSet<>();
    private static final Deque<List<String>> pendingQueries = new ArrayDeque<>();
    private static Set<String> tab = new HashSet<>();

    private static WebSocket socket;
    private static CompletableFuture<WebSocket> sendChain;
    private static boolean connecting = false;
    private static boolean ready = false;
    private static long nextAttempt = 0;
    private static long backoff = MIN_BACKOFF;
    private static int ticksOutOfWorld = 0;
    private static int ticks = 0;

    public static void tick(Minecraft client) {
        if (client.player == null || client.getConnection() == null) {
            if (++ticksOutOfWorld >= LEAVE_GRACE_TICKS) disconnect();
            return;
        }
        ticksOutOfWorld = 0;

        long now = System.currentTimeMillis();
        if (socket == null && !connecting && now >= nextAttempt) connect(client);

        if (++ticks % 20 != 0) return;
        updateTab(client, now);
        if (ready) flush(now);
    }

    public static String extractName(PlayerInfo info) {
        Component display = info.getTabListDisplayName();
        if (display != null) {
            Matcher m = SB_TAB_NAME.matcher(display.getString());
            if (m.find()) return m.group(1);
        }
        String profileName = info.getProfile().name();
        return profileName != null && VALID_NAME.matcher(profileName).matches() ? profileName : null;
    }

    public static boolean isUser(String name) {
        return name != null && confirmed.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public static void onChatSender(String name) {
        if (name == null || !VALID_NAME.matcher(name).matches()) return;
        String key = name.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        chatSeen.put(key, now);
        if (needsQuery(key, now)) queued.add(key);
    }

    public static void setPartyMembers(Collection<String> names) {
        replaceWatch(partyWatch, names);
    }

    public static void setGuildMembers(Collection<String> names) {
        replaceWatch(guildWatch, names);
    }

    public static void setCoopMembers(Collection<String> names) {
        replaceWatch(coopWatch, names);
    }

    public static boolean isReady() {
        return !ready || socket == null;
    }

    public static boolean sendText(String text) {
        if (isReady()) return false;
        sendChain = sendChain.thenCompose(ws -> ws.sendText(text, true));
        return true;
    }

    public static boolean isSpecialUser(String name) {
        return isUser(name) && colorOf(name) == GemColor.SPECIAL;
    }

    public static boolean isVipUser(String name) {
        return isUser(name) && colorOf(name) == GemColor.PURPLE;
    }

    public static int prismaticColor() {
        float hue = (System.currentTimeMillis() % PRISMATIC_CYCLE_MS) / (float) PRISMATIC_CYCLE_MS;
        return Color.HSBtoRGB(hue, 0.8F, 1.0F) & 0xFFFFFF;
    }

    public static GemColor colorOf(String name) {
        if (name == null) return GemColor.BLUE;
        return colors.getOrDefault(name.toLowerCase(Locale.ROOT), GemColor.BLUE);
    }

    public static void drawBadge(GuiGraphicsExtractor graphics, String name, int headX, int headY, int headSize) {
        int scale = Math.max(1, headSize / 8);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, colorOf(name).sprite, headX + headSize - 4 * scale, headY - scale, 5 * scale, 4 * scale);
    }

    public static Component withNametagBadge(Component name, String playerName) {
        if (hasBadge(name.getString())) return name;
        GemColor color = colorOf(playerName);
        String glyph = color == GemColor.SPECIAL ? Character.toString(currentSpecialGlyph()) : color.glyphString();
        MutableComponent out = Component.literal(glyph + " ").withColor(0xFFFFFF);
        return out.append(name);
    }

    public static String glyphFor(String name) {
        return colorOf(name).glyphString();
    }

    public static boolean hasBadge(String text) {
        return !text.isEmpty() && GemColor.isGemGlyph(text.codePointAt(0));
    }

    public static boolean isSpecialGlyph(int codepoint) {
        return codepoint >= SPECIAL_GLYPH_BASE && codepoint < SPECIAL_GLYPH_BASE + SPECIAL_GLYPH_FRAMES;
    }

    public static int currentSpecialGlyph() {
        return SPECIAL_GLYPH_BASE + (int) ((System.currentTimeMillis() / SPECIAL_GLYPH_FRAME_MS) % SPECIAL_GLYPH_FRAMES);
    }

    public static FormattedCharSequence animateSpecialGlyphs(FormattedCharSequence sequence) {
        return sink -> sequence.accept((index, style, codepoint) ->
                sink.accept(index, style, isSpecialGlyph(codepoint) ? currentSpecialGlyph() : codepoint));
    }

    public static void disconnect() {
        if (socket == null) return;
        WebSocket ws = socket;
        socket = null;
        ready = false;
        nextAttempt = 0;
        backoff = MIN_BACKOFF;

        confirmed.clear();
        colors.clear();
        probation.clear();
        nonUsers.clear();
        negativeUntil.clear();
        chatSeen.clear();
        queued.clear();
        inFlight.clear();
        requeueOnReady.clear();
        pendingQueries.clear();
        tab = new HashSet<>();

        VoiceChat.onSocketClosed();
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "");
    }

    private static void replaceWatch(Set<String> watch, Collection<String> names) {
        long now = System.currentTimeMillis();
        watch.clear();
        for (String name : names) {
            if (name == null || !VALID_NAME.matcher(name).matches()) continue;
            String key = name.toLowerCase(Locale.ROOT);
            watch.add(key);
            if (needsQuery(key, now)) queued.add(key);
        }
    }

    private static boolean needsQuery(String key, long now) {
        if (confirmed.containsKey(key) || probation.containsKey(key) || nonUsers.contains(key) || inFlight.contains(key))
            return false;
        Long until = negativeUntil.get(key);
        return until == null || until <= now;
    }

    private static boolean isRelevant(String key, long now) {
        if (tab.contains(key) || partyWatch.contains(key) || guildWatch.contains(key) || coopWatch.contains(key)) return true;
        Long seen = chatSeen.get(key);
        return seen != null && now - seen <= CHAT_RELEVANCE_MS;
    }

    private static void updateTab(Minecraft client, long now) {
        Set<String> current = new HashSet<>();
        if (client.getConnection() == null) return;
        for (PlayerInfo info : client.getConnection().getOnlinePlayers()) {
            String name = extractName(info);
            if (name != null) current.add(name.toLowerCase(Locale.ROOT));
        }

        for (String key : tab) {
            if (current.contains(key)) continue;
            nonUsers.remove(key);
            probation.remove(key);
        }

        for (String key : current) {
            if (tab.contains(key) || confirmed.containsKey(key) || probation.containsKey(key) || nonUsers.contains(key))
                continue;
            probation.put(key, new Probation(now));
        }

        tab = current;
    }

    private static void flush(long now) {
        confirmed.keySet().removeIf(key -> !isRelevant(key, now));
        colors.keySet().retainAll(confirmed.keySet());
        negativeUntil.values().removeIf(until -> until <= now);
        chatSeen.values().removeIf(seen -> now - seen > CHAT_RELEVANCE_MS);

        Set<String> batch = new LinkedHashSet<>();

        for (Map.Entry<String, Probation> entry : probation.entrySet()) {
            if (batch.size() >= MAX_NAMES_PER_QUERY) break;
            String key = entry.getKey();
            Probation p = entry.getValue();
            if (inFlight.contains(key) || !p.isDue(now)) continue;
            p.advance(now);
            batch.add(key);
        }

        Iterator<String> it = queued.iterator();
        while (it.hasNext() && batch.size() < MAX_NAMES_PER_QUERY) {
            String key = it.next();
            it.remove();
            if (!inFlight.contains(key)) batch.add(key);
        }

        for (Map.Entry<String, Long> entry : confirmed.entrySet()) {
            if (batch.size() >= MAX_NAMES_PER_QUERY) break;
            String key = entry.getKey();
            if (now - entry.getValue() >= REVERIFY_MS && !inFlight.contains(key)) batch.add(key);
        }

        if (!batch.isEmpty()) send(new ArrayList<>(batch));
    }

    private static void send(List<String> names) {
        ByteBuffer buf = ByteBuffer.allocate(names.size() * 4);
        for (String key : names) buf.putInt(hash(key));
        buf.flip();

        inFlight.addAll(names);
        pendingQueries.addLast(names);
        sendChain = sendChain.thenCompose(ws -> ws.sendBinary(buf, true));
    }

    private static int hash(String key) {
        int h = 0x811c9dc5;
        for (int i = 0; i < key.length(); i++) {
            h ^= key.charAt(i);
            h *= 0x01000193;
        }
        return h;
    }

    private static void handleText(Minecraft client, WebSocket ws, String text) {
        JsonObject msg;
        try {
            msg = JsonParser.parseString(text).getAsJsonObject();
        } catch (Exception e) {
            return;
        }

        String type = msg.has("type") ? msg.get("type").getAsString() : "";
        if (type.equals("challenge") && msg.has("serverId")) {
            authenticate(client, ws, msg.get("serverId").getAsString());
        } else if (type.equals("ready") && ws == socket) {
            onReady(System.currentTimeMillis());
        } else if (type.startsWith("voice_") && ws == socket) {
            VoiceChat.handleServerMessage(type, msg);
        }
    }

    private static void handleBinary(WebSocket ws, byte[] bytes) {
        if (ws != socket) return;
        List<String> names = pendingQueries.pollFirst();
        if (names == null) return;

        Map<String, GemColor> hits = new HashMap<>();
        int i = 0;
        while (i < bytes.length) {
            int header = bytes[i] & 0xFF;
            int len = header & 0x1F;
            boolean hasColor = (header & 0x80) != 0;
            int end = i + 1 + len + (hasColor ? 1 : 0);
            if (end > bytes.length) break;
            String name = new String(bytes, i + 1, len, StandardCharsets.US_ASCII);
            hits.put(name, hasColor ? GemColor.byId(bytes[i + 1 + len] & 0xFF) : GemColor.BLUE);
            i = end;
        }

        long now = System.currentTimeMillis();
        for (String key : names) {
            inFlight.remove(key);
            GemColor color = hits.get(key);
            if (color != null) markUser(key, now, color);
            else markNonUser(key, now);
        }
    }

    private static void markUser(String key, long now, GemColor color) {
        if (color == GemColor.BLUE) colors.remove(key);
        else colors.put(key, color);
        if (confirmed.put(key, now) == null) ChatModifications.badgeExistingMessages(key);
        probation.remove(key);
        nonUsers.remove(key);
        negativeUntil.remove(key);
    }

    private static void markNonUser(String key, long now) {
        confirmed.remove(key);
        colors.remove(key);

        Probation p = probation.get(key);
        if (p != null) {
            if (p.isFinished()) {
                probation.remove(key);
                nonUsers.add(key);
            }
            return;
        }

        if (tab.contains(key)) nonUsers.add(key);
        else negativeUntil.put(key, now + NEGATIVE_TTL_MS);
    }

    private static void onReady(long now) {
        ready = true;
        backoff = MIN_BACKOFF;

        for (String key : requeueOnReady) {
            if (tab.contains(key)) probation.put(key, new Probation(now));
            else queued.add(key);
        }
        requeueOnReady.clear();

        for (String key : partyWatch) if (needsQuery(key, now)) queued.add(key);
        for (String key : guildWatch) if (needsQuery(key, now)) queued.add(key);
        for (String key : coopWatch) if (needsQuery(key, now)) queued.add(key);
    }

    private static void connect(Minecraft client) {
        connecting = true;
        HTTP.newWebSocketBuilder()
                .header("X-SSU-Client", "1")
                .buildAsync(WS_URI, new Listener(client))
                .whenComplete((ws, err) -> client.execute(() -> {
                    connecting = false;
                    if (err != null || ws.isInputClosed()) {
                        retryLater();
                        return;
                    }
                    if (ticksOutOfWorld >= LEAVE_GRACE_TICKS) {
                        ws.sendClose(WebSocket.NORMAL_CLOSURE, "");
                        return;
                    }
                    socket = ws;
                    sendChain = CompletableFuture.completedFuture(ws);
                }));
    }

    private static void authenticate(Minecraft client, WebSocket ws, String serverId) {
        CompletableFuture.runAsync(() -> {
            try {
                var user = client.getUser();
                client.services().sessionService().joinServer(user.getProfileId(), user.getAccessToken(), serverId);

                JsonObject auth = new JsonObject();
                auth.addProperty("type", "auth");
                auth.addProperty("name", user.getName());
                auth.addProperty("protocol", PROTOCOL);
                ws.sendText(auth.toString(), true);
            } catch (Exception e) {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "");
            }
        });
    }

    private static void onDisconnected(WebSocket ws) {
        if (ws != socket) return;
        socket = null;
        ready = false;
        VoiceChat.onSocketClosed();

        requeueOnReady.addAll(confirmed.keySet());
        requeueOnReady.addAll(inFlight);
        confirmed.clear();
        colors.clear();
        inFlight.clear();
        pendingQueries.clear();

        retryLater();
    }

    private static void retryLater() {
        nextAttempt = System.currentTimeMillis() + backoff + ThreadLocalRandom.current().nextLong(backoff);
        backoff = Math.min(backoff * 2, MAX_BACKOFF);
    }

    private static final class Probation {
        private final long firstSeen;
        private int step = 0;

        private Probation(long firstSeen) {
            this.firstSeen = firstSeen;
        }

        private boolean isDue(long now) {
            return step < PROBATION_OFFSETS.length && now - firstSeen >= PROBATION_OFFSETS[step];
        }

        private void advance(long now) {
            long elapsed = now - firstSeen;
            while (step < PROBATION_OFFSETS.length && elapsed >= PROBATION_OFFSETS[step]) step++;
        }

        private boolean isFinished() {
            return step >= PROBATION_OFFSETS.length;
        }
    }

    private static final class Listener implements WebSocket.Listener {
        private final Minecraft client;
        private final StringBuilder text = new StringBuilder();
        private final ByteArrayOutputStream binary = new ByteArrayOutputStream();

        private Listener(Minecraft client) {
            this.client = client;
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            text.append(data);
            if (last) {
                String msg = text.toString();
                text.setLength(0);
                client.execute(() -> handleText(client, ws, msg));
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
            byte[] chunk = new byte[data.remaining()];
            data.get(chunk);
            binary.writeBytes(chunk);
            if (last) {
                byte[] msg = binary.toByteArray();
                binary.reset();
                client.execute(() -> handleBinary(ws, msg));
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            client.execute(() -> onDisconnected(ws));
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            client.execute(() -> onDisconnected(ws));
        }
    }
}