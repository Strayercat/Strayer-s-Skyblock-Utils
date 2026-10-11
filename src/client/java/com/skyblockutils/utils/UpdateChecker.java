package com.skyblockutils.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.skyblockutils.utils.Scheduler.scheduler;

public class UpdateChecker {
    private static final String CURRENT_VERSION = "5.0.0";
    private static final URI UPDATE_URI = URI.create("https://raw.githubusercontent.com/Strayercat/Strayer-s-Skyblock-Utils/main/update.json");
    private static final String MOD_URL = "https://modrinth.com/mod/strayers-skyblock-utils/versions";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static boolean userNotified = false;
    private static boolean initialized = false;

    public static void init(Minecraft client) {
        if (initialized) return;
        initialized = true;
        userNotified = false;
        scheduleCheck(client);
    }

    private static void scheduleCheck(Minecraft client) {
        scheduler.schedule(() -> {
            String latestVersion = fetchLatestVersion();
            client.execute(() -> onVersionFetched(client, latestVersion));
        }, 1, TimeUnit.MINUTES);
    }

    private static String fetchLatestVersion() {
        try {
            String gameVersion = SharedConstants.getCurrentVersion().id();

            HttpRequest req = HttpRequest.newBuilder(UPDATE_URI)
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "SkyblockUtils/" + CURRENT_VERSION)
                    .GET()
                    .build();

            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return null;

            JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();

            if (!json.has(gameVersion)) return null;

            return json.getAsJsonObject(gameVersion).get("latest").getAsString();

        } catch (Exception e) {
            return null;
        }
    }

    private static void onVersionFetched(Minecraft client, String latestVersion) {
        if (latestVersion == null || !isNewer(latestVersion)) return;

        if (client.level != null) {
            userNotified = true;
            sendUpdateMessage(latestVersion);
            return;
        }

        if (userNotified) return;

        scheduleCheck(client);
    }

    private static boolean isNewer(String latest) {
        String[] a = latest.split("\\.");
        String[] b = UpdateChecker.CURRENT_VERSION.split("\\.");
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int av = i < a.length ? Integer.parseInt(a[i]) : 0;
            int bv = i < b.length ? Integer.parseInt(b[i]) : 0;
            if (av > bv) return true;
            if (av < bv) return false;
        }
        return false;
    }

    private static void sendUpdateMessage(String latestVersion) {
        Component message = Component.literal("")
                .append(Component.literal("Update available: ")
                        .withStyle(s -> s.withColor(ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TEXT) & 0xFFFFFF)))
                .append(Component.literal(CURRENT_VERSION + " → " + latestVersion + " ")
                        .withStyle(s -> s.withColor(ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN) & 0xFFFFFF)))
                .append(
                        Component.literal("[Download]")
                                .setStyle(Style.EMPTY
                                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(MOD_URL)))
                                        .withUnderlined(true)
                                        .withColor(ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TITLE_END) & 0xFFFFFF)
                                )
                );

        ModFunctions.sendSystemMessage(message, true);
    }
}