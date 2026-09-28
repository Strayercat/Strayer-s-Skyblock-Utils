package com.skyblockutils.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SSUIndicator {
    private static final String API = "https://cawcah.duckdns.org/ssu";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();

    public static final Identifier BADGE = Identifier.fromNamespaceAndPath("skyblockutils", "ssu_gem");
    public static final Identifier BADGE_BIG = Identifier.fromNamespaceAndPath("skyblockutils", "ssu_gem_big");
    public static final String BADGE_GLYPH = "\uE7A5";

    private static final Pattern SB_TAB_NAME = Pattern.compile("\\[\\d+] (?:\\[[^]]+] )?(\\w{3,16})");
    private static final Pattern VALID_NAME = Pattern.compile("^\\w{3,16}$");

    private static final Set<String> users = ConcurrentHashMap.newKeySet();
    private static final Set<String> checked = ConcurrentHashMap.newKeySet();
    private static final Set<String> pending = ConcurrentHashMap.newKeySet();

    private static final int HEARTBEAT_TICKS = 20 * 60 * 4;
    private static final int CHECK_TICKS = 20 * 10;
    private static final int RECHECK_TICKS = 20 * 60 * 5;
    private static int ticks = 0;
    private static volatile boolean busy = false;

    public static void tick(Minecraft client) {
        if (client.player == null || client.getConnection() == null) return;
        ticks++;

        if (ticks % HEARTBEAT_TICKS == 1) heartbeat(client);
        if (ticks % RECHECK_TICKS == 0) checked.clear();
        if (ticks % CHECK_TICKS == 0 && !busy) check(client);
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
        return name != null && users.contains(name.toLowerCase(Locale.ROOT));
    }

    public static boolean isUser(PlayerInfo info) {
        return !users.isEmpty() && isUser(extractName(info));
    }

    public static void requestCheck(String name) {
        if (name == null || !VALID_NAME.matcher(name).matches()) return;
        String key = name.toLowerCase(Locale.ROOT);
        if (!checked.contains(key)) pending.add(key);
    }

    public static void drawBadge(GuiGraphicsExtractor graphics, int headX, int headY, int headSize) {
        int scale = Math.max(1, headSize / 8);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BADGE, headX + headSize - 4 * scale, headY - scale, 5 * scale, 4 * scale);
    }

    public static Component withNametagBadge(Component name) {
        if (name.getString().startsWith(BADGE_GLYPH)) return name;
        MutableComponent out = Component.literal(BADGE_GLYPH + " ").withColor(0xFFFFFF);
        return out.append(name);
    }

    private static void heartbeat(Minecraft client) {
        CompletableFuture.runAsync(() -> {
            try {
                String serverId = GSON.fromJson(get(), JsonObject.class).get("serverId").getAsString();
                var user = client.getUser();
                client.services().sessionService().joinServer(user.getProfileId(), user.getAccessToken(), serverId);

                JsonObject body = new JsonObject();
                body.addProperty("name", user.getName());
                body.addProperty("serverId", serverId);
                post("/heartbeat", body.toString());
            } catch (Exception ignored) {
            }
        });
    }

    private static void check(Minecraft client) {
        Set<String> names = new HashSet<>();

        for (String name : pending) {
            pending.remove(name);
            if (checked.add(name)) names.add(name);
            if (names.size() >= 100) break;
        }

        for (PlayerInfo info : Objects.requireNonNull(client.getConnection()).getOnlinePlayers()) {
            if (names.size() >= 100) break;
            String name = extractName(info);
            if (name == null) continue;
            name = name.toLowerCase(Locale.ROOT);
            if (checked.add(name)) names.add(name);
        }
        if (names.isEmpty()) return;

        busy = true;
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject body = new JsonObject();
                body.add("names", GSON.toJsonTree(names));
                String[] found = GSON.fromJson(post("/check", body.toString()), String[].class);
                for (String n : found) users.add(n.toLowerCase(Locale.ROOT));
            } catch (Exception ignored) {
                checked.removeAll(names);
            } finally {
                busy = false;
            }
        });
    }

    private static String get() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(API + "/challenge"))
                .header("X-SSU-Client", "1")
                .GET()
                .build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }

    private static String post(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(API + path))
                .header("X-SSU-Client", "1")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }
}