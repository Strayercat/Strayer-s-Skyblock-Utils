package com.skyblockutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.skyblockutils.features.DailyReminders;
import com.skyblockutils.features.chat.ChatFilterDefinitions;
import com.skyblockutils.features.glowingPlayers.GlowingPlayers.GlowingPlayer;
import com.skyblockutils.features.hud.layout.HudLayoutEntry;
import com.skyblockutils.features.slayer.SlayerTracker;
import com.skyblockutils.utils.ModStyle;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("SkyblockUtils.json");
    public static ModConfig INSTANCE = new ModConfig();

    // General
    public ModStyle.ColorStyle colorStyle = ModStyle.ColorStyle.ORIGINAL;
    public ModStyle.NotificationStyle notificationStyle = ModStyle.NotificationStyle.ROUNDED;
    public boolean wikiInGame = true;
    public boolean wikiTooltipHint = true;
    public boolean wikiPreload = true;
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
    public boolean bestiaryHud = true;
    public boolean slayerTrackerHud = true;
    public boolean carryTrackerHud = true;
    public boolean cancelSwings = false;

    // Dungeons
    public boolean downtimeTracker = true;
    public boolean dungeonPartyCommands = true;
    public boolean autoRejoinReminders = false;
    public float autoRejoinTimer = 2.0F;
    public boolean f7VoidLava = true;

    // Textures
    public boolean obviousGemstones = true;
    public boolean customBreakingAnimation = true;

    // HUD
    public boolean hudEnabled = true;
    public boolean stashHud = false;
    public boolean bestiaryHudHideInCatacombs = true;
    public boolean bestiaryHudShowMaxed = true;
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
    public Map<String, HudLayoutEntry> hudLayouts = new HashMap<>();
    public Map<String, Map<String, HudLayoutEntry>> hudLocationLayouts = new HashMap<>();
    public Map<String, Map<String, Boolean>> hudVisibility = new HashMap<>();
    public Map<String, Boolean> hudGlobalOptions = new HashMap<>();
    public Map<String, SlayerTracker.SlayerXp> slayerXp = new HashMap<>();

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
    public boolean separateMessage = false;
    public boolean ssuStyleChat = false;
    public boolean ssuStyleChatInput = true;
    public boolean systemDuplicateCounter = true;
    public String systemDuplicateCounterText = "&8(&7x&r$&8)";
    public int systemDuplicateCounterColor = 0xFFFF55;
    private final Map<String, Boolean> chatFilters = new HashMap<>();
    private List<GlowingPlayer> glowingPlayers = new ArrayList<>();

    // Events
    public boolean spookyChestTitle = true;
    public boolean spookyLootNotification = true;
    public boolean spookyTrickJumpscare = false;

    // Voice Chat
    public boolean voiceChatEnabled = false;
    public boolean voiceInParty = true;
    public boolean voiceInCoop = true;
    public boolean voiceNoCoopInParty = false;
    public String voiceInputDevice = "";
    public String voiceOutputDevice = "";
    public int voiceInputVolume = 250;
    public int voiceOutputVolume = 100;
    public VoiceActivation voiceActivation = VoiceActivation.VOICE_ACTIVATION;
    public int voiceActivationLevel = -30;
    public int voiceSmartSensitivity = 30;
    public boolean voiceNoiseSuppression = true;
    public boolean voiceKeyboardSuppression = true;
    public transient boolean voiceMicTest = false;
    public boolean voiceHud = true;
    public boolean voiceMuted = false;
    public boolean voiceDeafened = false;

    // Daily reminders
    public boolean dailyReminders = true;
    public Date lastReset = new Date(0);
    public List<DailyReminders.ReminderType> disabledTypes = new ArrayList<>();
    public List<DailyReminders.ReminderType> completedTypes = new ArrayList<>();

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

    public enum VoiceActivation {
        @SerializedName("PUSH_TO_TALK") PUSH_TO_TALK("Push To Talk"),
        @SerializedName("VOICE_ACTIVATION") VOICE_ACTIVATION("Voice Activation"),
        @SerializedName("SMART") SMART("Smart Voice Detection");

        private final String name;

        VoiceActivation(String name) {
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

    public static void load() {
        if (!CONFIG_PATH.toFile().exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
            ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
            if (loaded != null) INSTANCE = loaded.sanitize();
        } catch (IOException | RuntimeException ignored) {
        }

        save();
    }

    private ModConfig sanitize() {
        ModConfig defaults = new ModConfig();
        for (Field field : ModConfig.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;
            try {
                if (field.get(this) == null) field.set(this, field.get(defaults));
            } catch (IllegalAccessException ignored) {
            }
        }

        hudLayouts = sanitizeLayouts(hudLayouts);
        Map<String, Map<String, HudLayoutEntry>> locationLayouts = new HashMap<>();
        hudLocationLayouts.forEach((location, layouts) -> {
            if (location == null || layouts == null) return;
            Map<String, HudLayoutEntry> copy = sanitizeLayouts(layouts);
            if (!copy.isEmpty()) locationLayouts.put(location, copy);
        });
        hudLocationLayouts = locationLayouts;

        Map<String, Map<String, Boolean>> visibility = new HashMap<>();
        hudVisibility.forEach((location, flags) -> {
            if (location == null || flags == null) return;
            Map<String, Boolean> copy = new HashMap<>();
            flags.forEach((id, visible) -> {
                if (id != null && visible != null) copy.put(id, visible);
            });
            if (!copy.isEmpty()) visibility.put(location, copy);
        });
        hudVisibility = visibility;

        defaults.chatFilters.forEach(chatFilters::putIfAbsent);

        voiceInputVolume = Math.clamp(voiceInputVolume, 0, 500);
        voiceOutputVolume = Math.clamp(voiceOutputVolume, 0, 200);
        voiceActivationLevel = Math.clamp(voiceActivationLevel, -90, 0);
        voiceSmartSensitivity = Math.clamp(voiceSmartSensitivity, 10, 95);
        return this;
    }

    private static Map<String, HudLayoutEntry> sanitizeLayouts(Map<String, HudLayoutEntry> layouts) {
        Map<String, HudLayoutEntry> copy = new HashMap<>();
        layouts.forEach((id, layout) -> {
            if (id != null && layout != null) copy.put(id, layout.sanitize());
        });
        return copy;
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException ignored) {
        }
    }
}