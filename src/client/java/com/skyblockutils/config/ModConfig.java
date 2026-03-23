package com.skyblockutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skyblockutils.features.GlowingPlayers.GlowingPlayer;
import com.skyblockutils.features.chatFilters.ChatFilterDefinitions;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("SkyblockUtils.json");

    // General
    public boolean chatFiltersEnabled;
    public boolean partyInviteNotifications;
    public boolean fancyEmotes;
    public boolean autoHoppityEggs;
    public boolean coordinatesSendLocation;
    // Dungeons
    public boolean downtimeTracker;
    public boolean dungeonPartyCommands;
    public boolean autoRejoinReminders;

    // HUD
    public boolean hudEnabled;
    public int hudScale;
    public boolean hudTime;
    public boolean hudTime12hFormat;
    public boolean hudPing;
    public boolean hudTps;
    public boolean hudFps;
    public boolean sideBarInHud;
    public boolean hudPartyInfo;
    public boolean hudPartyInfoInDungeons;
    public boolean hudIslandFunFact;

    private final Map<String, Boolean> chatFilters = new HashMap<>();
    private List<GlowingPlayer> glowingPlayers = new ArrayList<>();


    public List<GlowingPlayer> getGlowingPlayers() {
        return this.glowingPlayers;
    }

    public void addGlowingPlayer(GlowingPlayer player) {
        if (this.glowingPlayers == null) this.glowingPlayers = new ArrayList<>();
        this.glowingPlayers.add(player);
    }

    public void removeGlowingPlayer(String username) {
        this.glowingPlayers.removeIf(p -> p.username.equalsIgnoreCase(username));
    }

    private ModConfig() {
        for (ChatFilterDefinitions.FilterEntry entry : ChatFilterDefinitions.getAllEntries()) {
            chatFilters.put(entry.configKey, entry.defaultValue);
        }
    }

    public static final ModConfig INSTANCE = new ModConfig();

    public static void load() {
        if (!CONFIG_PATH.toFile().exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
            ConfigData loadedData = GSON.fromJson(reader, ConfigData.class);

            if (loadedData != null) {
                INSTANCE.chatFiltersEnabled = loadedData.chatFiltersEnabled;
                INSTANCE.downtimeTracker = loadedData.downtimeTracker;
                INSTANCE.dungeonPartyCommands = loadedData.dungeonPartyCommands;
                INSTANCE.autoRejoinReminders = loadedData.autoRejoinReminders;
                INSTANCE.partyInviteNotifications = loadedData.partyInviteNotifications;
                INSTANCE.fancyEmotes = loadedData.fancyEmotes;
                INSTANCE.autoHoppityEggs = loadedData.autoHoppityEggs;
                INSTANCE.hudEnabled = loadedData.hudEnabled;
                INSTANCE.hudScale = loadedData.hudScale;
                INSTANCE.hudTime = loadedData.hudTime;
                INSTANCE.hudTime12hFormat = loadedData.hudTime12hFormat;
                INSTANCE.hudPing = loadedData.hudPing;
                INSTANCE.hudTps = loadedData.hudTps;
                INSTANCE.hudFps = loadedData.hudFps;
                INSTANCE.sideBarInHud = loadedData.sideBarInHud;
                INSTANCE.hudPartyInfo = loadedData.hudPartyInfo;
                INSTANCE.hudPartyInfoInDungeons = loadedData.hudPartyInfoInDungeons;
                INSTANCE.coordinatesSendLocation = loadedData.coordinatesSendLocation;
                INSTANCE.hudIslandFunFact = loadedData.hudIslandFunFact;

                if (loadedData.chatFilters != null) {
                    for (Map.Entry<String, Boolean> entry : loadedData.chatFilters.entrySet()) {
                        INSTANCE.setChatFilter(entry.getKey(), entry.getValue());
                    }
                }

                if (loadedData.glowingPlayers != null) {
                    INSTANCE.glowingPlayers = new ArrayList<>(loadedData.glowingPlayers);
                } else {
                    INSTANCE.glowingPlayers = new ArrayList<>();
                }
            }

            save();

        } catch (IOException ignored) {
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            ConfigData data = new ConfigData();
            data.chatFiltersEnabled = INSTANCE.chatFiltersEnabled;
            data.downtimeTracker = INSTANCE.downtimeTracker;
            data.chatFilters = INSTANCE.getChatFilters();
            data.dungeonPartyCommands = INSTANCE.dungeonPartyCommands;
            data.autoRejoinReminders = INSTANCE.autoRejoinReminders;
            data.partyInviteNotifications = INSTANCE.partyInviteNotifications;
            data.fancyEmotes = INSTANCE.fancyEmotes;
            data.autoHoppityEggs = INSTANCE.autoHoppityEggs;
            data.hudEnabled = INSTANCE.hudEnabled;
            data.hudScale = INSTANCE.hudScale;
            data.hudTime = INSTANCE.hudTime;
            data.hudTime12hFormat = INSTANCE.hudTime12hFormat;
            data.hudPing = INSTANCE.hudPing;
            data.hudTps = INSTANCE.hudTps;
            data.hudFps = INSTANCE.hudFps;
            data.sideBarInHud = INSTANCE.sideBarInHud;
            data.hudPartyInfo = INSTANCE.hudPartyInfo;
            data.hudPartyInfoInDungeons = INSTANCE.hudPartyInfoInDungeons;
            data.coordinatesSendLocation = INSTANCE.coordinatesSendLocation;
            data.glowingPlayers = INSTANCE.glowingPlayers;
            data.hudIslandFunFact = INSTANCE.hudIslandFunFact;

            GSON.toJson(data, writer);
        } catch (IOException ignored) {
        }
    }

    public boolean getChatFilter(String key) {
        return chatFilters.getOrDefault(key, false);
    }

    public void setChatFilter(String key, boolean value) {
        chatFilters.put(key, value);
    }

    public Map<String, Boolean> getChatFilters() {
        return chatFilters;
    }

    private static class ConfigData {
        public boolean chatFiltersEnabled = true;
        public boolean downtimeTracker = true;
        public boolean dungeonPartyCommands = true;
        public boolean autoRejoinReminders = false;
        public boolean partyInviteNotifications = true;
        public boolean fancyEmotes = true;
        public boolean autoHoppityEggs = false;
        public boolean hudEnabled = true;
        public int hudScale = 100;
        public boolean hudTime = true;
        public boolean hudTime12hFormat = false;
        public boolean hudPing = true;
        public boolean hudTps = true;
        public boolean hudFps = true;
        public boolean sideBarInHud = false;
        public boolean hudPartyInfo = true;
        public boolean hudPartyInfoInDungeons = false;
        public boolean hudIslandFunFact = true;
        public boolean coordinatesSendLocation = true;

        public Map<String, Boolean> chatFilters;
        public List<GlowingPlayer> glowingPlayers;
    }
}