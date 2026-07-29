package com.skyblockutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.skyblockutils.features.DailyReminders;
import com.skyblockutils.features.glowingPlayers.GlowingPlayers.GlowingPlayer;
import com.skyblockutils.features.chat.ChatFilterDefinitions;
import com.skyblockutils.utils.ModStyle;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("SkyblockUtils.json");
    public static final ModConfig INSTANCE = new ModConfig();

    // General
    public ModStyle.ColorStyle colorStyle = ModStyle.ColorStyle.ORIGINAL;
    public ModStyle.NotificationStyle notificationStyle = ModStyle.NotificationStyle.ROUNDED;
    public boolean chatFiltersEnabled = true;
    public boolean partyInviteNotifications = true;
    public boolean boopPartyInvites = true;
    public boolean fancyEmotes = true;
    public boolean autoHoppityEggs = false;
    public boolean coordinatesSendLocation = true;
    public boolean statCommands = true;
    public boolean chatCommands = true;
    public boolean customSidebar = true;
    public boolean sidebarCoords = false;
    public boolean cat = true;
    public boolean partyGlow = true;
    public boolean screenshotHud = true;

    // Dungeons
    public boolean downtimeTracker = true;
    public boolean dungeonPartyCommands = true;
    public boolean autoRejoinReminders = false;
    public boolean f7VoidLava = true;

    // HUD
    public boolean hudEnabled = true;
    public int hudScale = 100;
    public boolean hudTime = true;
    public boolean hudTime12hFormat = false;
    public boolean hudPing = true;
    public boolean hudTps = true;
    public boolean hudFps = true;
    public boolean hudCoords = true;
    public boolean hudDailies = true;
    public boolean sideBarInHud = false;
    public boolean hudPartyInfo = true;
    public boolean hudPartyInfoInDungeons = false;
    public boolean hudIslandFunFact = true;

    // Mining
    public boolean displayGlaciteWaypoints = true;
    public GlaciteWaypoints glaciteWaypoints = GlaciteWaypoints.BOTH;
    public boolean powderChestNotification = true;
    public int powderChestNotificationTime = 60;

    //Foraging
    public boolean treeGiftNotification = true;
    public int treeGiftNotificationTime = 60;
    public boolean phantomTitle = true;

    // Chat filters & glowing players
    private final Map<String, Boolean> chatFilters = new HashMap<>();
    private List<GlowingPlayer> glowingPlayers = new ArrayList<>();

    // Events
    public boolean spookyChestTitle = true;
    public boolean spookyLootNotification = true;
    public boolean spookyTrickJumpscare = false;

    // Enums
    public enum GlaciteWaypoints {
        @SerializedName("UMBER") UMBER("Umber"),
        @SerializedName("TUNGSTEN") TUNGSTEN("Tungsten"),
        @SerializedName("BOTH") BOTH("Umber & Tungsten");

        private final String name;

        GlaciteWaypoints(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // Constructor
    private ModConfig() {
        for (ChatFilterDefinitions.FilterEntry entry : ChatFilterDefinitions.getAllEntries()) {
            chatFilters.put(entry.configKey, entry.defaultValue);
        }
    }

    // Glowing players API
    public List<GlowingPlayer> getGlowingPlayers() {
        return glowingPlayers;
    }

    public void addGlowingPlayer(GlowingPlayer player) {
        if (glowingPlayers == null) glowingPlayers = new ArrayList<>();
        glowingPlayers.add(player);
    }

    public void removeGlowingPlayer(String username) {
        glowingPlayers.removeIf(p -> p.username.equalsIgnoreCase(username));
    }

    // Chat filter API
    public boolean getChatFilter(String key) {
        return chatFilters.getOrDefault(key, false);
    }

    public void setChatFilter(String key, boolean value) {
        chatFilters.put(key, value);
    }

    // Daily reminders
    public boolean dailyReminders = true;
    public Date lastReset = new Date(0);
    public List<DailyReminders.ReminderType> disabledTypes = new ArrayList<>();
    public List<DailyReminders.ReminderType> completedTypes = new ArrayList<>();

    public static void load() {
        if (!CONFIG_PATH.toFile().exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
            ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
            if (loaded == null) return;

            INSTANCE.colorStyle = loaded.colorStyle != null ? loaded.colorStyle : ModStyle.ColorStyle.ORIGINAL;
            INSTANCE.chatFiltersEnabled = loaded.chatFiltersEnabled;
            INSTANCE.partyInviteNotifications = loaded.partyInviteNotifications;
            INSTANCE.boopPartyInvites = loaded.boopPartyInvites;
            INSTANCE.fancyEmotes = loaded.fancyEmotes;
            INSTANCE.autoHoppityEggs = loaded.autoHoppityEggs;
            INSTANCE.coordinatesSendLocation = loaded.coordinatesSendLocation;
            INSTANCE.statCommands = loaded.statCommands;
            INSTANCE.chatCommands = loaded.chatCommands;
            INSTANCE.customSidebar = loaded.customSidebar;
            INSTANCE.sidebarCoords = loaded.sidebarCoords;
            INSTANCE.cat = loaded.cat;
            INSTANCE.partyGlow = loaded.partyGlow;
            INSTANCE.screenshotHud = loaded.screenshotHud;
            INSTANCE.downtimeTracker = loaded.downtimeTracker;
            INSTANCE.dungeonPartyCommands = loaded.dungeonPartyCommands;
            INSTANCE.autoRejoinReminders = loaded.autoRejoinReminders;
            INSTANCE.f7VoidLava = loaded.f7VoidLava;
            INSTANCE.hudEnabled = loaded.hudEnabled;
            INSTANCE.hudScale = loaded.hudScale;
            INSTANCE.hudTime = loaded.hudTime;
            INSTANCE.hudTime12hFormat = loaded.hudTime12hFormat;
            INSTANCE.hudPing = loaded.hudPing;
            INSTANCE.hudTps = loaded.hudTps;
            INSTANCE.hudFps = loaded.hudFps;
            INSTANCE.hudCoords = loaded.hudCoords;
            INSTANCE.hudDailies = loaded.hudDailies;
            INSTANCE.sideBarInHud = loaded.sideBarInHud;
            INSTANCE.hudPartyInfo = loaded.hudPartyInfo;
            INSTANCE.hudPartyInfoInDungeons = loaded.hudPartyInfoInDungeons;
            INSTANCE.hudIslandFunFact = loaded.hudIslandFunFact;
            INSTANCE.displayGlaciteWaypoints = loaded.displayGlaciteWaypoints;
            INSTANCE.glaciteWaypoints = loaded.glaciteWaypoints != null ? loaded.glaciteWaypoints : GlaciteWaypoints.BOTH;
            INSTANCE.powderChestNotification = loaded.powderChestNotification;
            INSTANCE.powderChestNotificationTime = loaded.powderChestNotificationTime;
            INSTANCE.treeGiftNotification = loaded.treeGiftNotification;
            INSTANCE.treeGiftNotificationTime = loaded.treeGiftNotificationTime;
            INSTANCE.phantomTitle = loaded.phantomTitle;
            INSTANCE.glowingPlayers = loaded.glowingPlayers != null ? new ArrayList<>(loaded.glowingPlayers) : new ArrayList<>();
            INSTANCE.dailyReminders = loaded.dailyReminders;
            INSTANCE.lastReset = loaded.lastReset != null ? loaded.lastReset : new Date(0);
            INSTANCE.disabledTypes = loaded.disabledTypes != null ? loaded.disabledTypes : new ArrayList<>();
            INSTANCE.completedTypes = loaded.completedTypes != null ? loaded.completedTypes : new ArrayList<>();

            loaded.chatFilters.forEach(INSTANCE::setChatFilter);
        } catch (IOException ignored) {
        }

        save();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException ignored) {
        }
    }
}