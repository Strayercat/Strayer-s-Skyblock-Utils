package com.skyblockutils.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockutils.ModFunctions;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static com.skyblockutils.utils.Scheduler.scheduler;

public class UpdateChecker {
    private static final String CURRENT_VERSION = "4.4.0";
    private static final String UPDATE_URL = "https://raw.githubusercontent.com/Strayercat/Strayer-s-Skyblock-Utils/main/update.json";
    private static final String MOD_URL = "https://modrinth.com/mod/strayers-skyblock-utils/versions";
    private static boolean userNotified = false;
    private static boolean initialized = false;

    public static void init(Minecraft client) {
        if (initialized) return;
        initialized = true;

        System.out.println("Updater Initialized with version " + SharedConstants.getCurrentVersion().id());
        userNotified = false;
        scheduler.schedule(() -> checkForUpdate(client), 1, TimeUnit.MINUTES);
    }

    private static String fetchLatestVersion() {
        try {
            String gameVersion = SharedConstants.getCurrentVersion().id();

            HttpURLConnection conn = (HttpURLConnection) new URL(UPDATE_URL).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "SkyblockUtils/" + CURRENT_VERSION);

            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();

            if (!json.has(gameVersion)) return null;

            return json.getAsJsonObject(gameVersion).get("latest").getAsString();

        } catch (Exception e) {
            return null;
        }
    }

    private static void checkForUpdate(Minecraft client) {
        System.out.println("Checking for update");

        String latestVersion = fetchLatestVersion();

        if (latestVersion == null || !isNewer(latestVersion)) return;

        if (client.level != null) {
            userNotified = true;
            sendUpdateMessage(client, latestVersion);
            return;
        }

        if (userNotified) return;

        scheduler.schedule(() -> checkForUpdate(client), 1, TimeUnit.MINUTES);
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

    private static void sendUpdateMessage(Minecraft client, String latestVersion) {
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

        client.execute(() -> ModFunctions.displayComponentMessageWithHeader(message));
    }
}