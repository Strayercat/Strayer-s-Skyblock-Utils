package com.skyblockutils.config;

import com.skyblockutils.features.chat.ChatFilterDefinitions;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ClothConfigHandler {
    public static boolean configScreenRequested = false;

    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTransparentBackground(false)
                .setDefaultBackgroundTexture(Identifier.of("skyblockutils", "textures/config/background.png"))
                .setTitle(Text.literal("Strayer's Skyblock Utils (SSU) Config"))
                .setSavingRunnable(ModConfig::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        buildGeneralCategory(builder, eb);
        buildHudCategory(builder, eb);
        buildChatFiltersCategory(builder, eb);

        return builder.build();
    }

    public static void handleConfigScreen(MinecraftClient client) {
        if (configScreenRequested) client.setScreen(createConfigScreen(client.currentScreen));
        configScreenRequested = false;
    }

    // General Settings
    private static void buildGeneralCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General Settings"));

        general.addEntry(eb.startBooleanToggle(Text.literal("Party Invite Notifications"), ModConfig.INSTANCE.partyInviteNotifications)
                .setDefaultValue(true).setTooltip(Text.literal("Sends party invites as a notification instead of a chat message"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.partyInviteNotifications = v).build());

        general.addEntry(eb.startBooleanToggle(Text.literal("Fancy Emotes"), ModConfig.INSTANCE.fancyEmotes)
                .setDefaultValue(true).setTooltip(Text.literal("Transforms <3 into ♥ and such"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.fancyEmotes = v).build());

        general.addEntry(eb.startBooleanToggle(Text.literal("Auto Hoppity Eggs"), ModConfig.INSTANCE.autoHoppityEggs)
                .setDefaultValue(false).setTooltip(Text.literal("Whether to instantly collect hoppity eggs or not"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.autoHoppityEggs = v).build());

        general.addEntry(eb.startBooleanToggle(Text.literal("Coordinates Send Location"), ModConfig.INSTANCE.coordinatesSendLocation)
                .setDefaultValue(true).setTooltip(Text.literal("Whether printing your coordinates (via the keybind) should also send the Skyblock location"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.coordinatesSendLocation = v).build());

        general.addEntry(eb.startBooleanToggle(Text.literal("Chat Commands"), ModConfig.INSTANCE.chatCommands)
                .setDefaultValue(true).setTooltip(Text.literal("Whether or not to enable !gay !furry !sus and such commands"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.chatCommands = v).build());

        general.addEntry(eb.startBooleanToggle(Text.literal("Party Glow"), ModConfig.INSTANCE.partyGlow)
                .setDefaultValue(true).setTooltip(Text.literal("Whether or not party members should automatically glow"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.partyGlow = v).build());

        general.addEntry(eb.startBooleanToggle(Text.literal("Ragebait button"), true)
                .setDefaultValue(true).setTooltip(Text.literal("No matter how much you toggle it, whenever you come back it'll always be true"))
                .build());

        general.addEntry(buildDungeonsSubcategory(eb).build());
        general.addEntry(buildMiningSubcategory(eb).build());
    }

    private static SubCategoryBuilder buildDungeonsSubcategory(ConfigEntryBuilder eb) {
        SubCategoryBuilder dungeons = eb.startSubCategory(Text.literal("Dungeons"))
                .setTooltip(Text.literal("Dungeon-related features"));

        dungeons.add(eb.startBooleanToggle(Text.literal("Floor Commands"), ModConfig.INSTANCE.dungeonPartyCommands)
                .setDefaultValue(true).setTooltip(Text.literal("Allow party members to type '!f2' style commands to start a dungeon run"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.dungeonPartyCommands = v).build());

        dungeons.add(eb.startBooleanToggle(Text.literal("Downtime Tracker"), ModConfig.INSTANCE.downtimeTracker)
                .setDefaultValue(true).setTooltip(Text.literal("Detect '!dt' messages and remind you when a dungeon run ends"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.downtimeTracker = v).build());

        dungeons.add(eb.startBooleanToggle(Text.literal("Auto-Rejoin Reminders"), ModConfig.INSTANCE.autoRejoinReminders)
                .setDefaultValue(false).setTooltip(Text.literal("Reminds you that auto-rejoin is still enabled every time a run ends"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.autoRejoinReminders = v).build());

        return dungeons;
    }

    private static SubCategoryBuilder buildMiningSubcategory(ConfigEntryBuilder eb) {
        SubCategoryBuilder mining = eb.startSubCategory(Text.literal("Mining"))
                .setTooltip(Text.literal("Mining-related features"));

        mining.add(eb.startBooleanToggle(Text.literal("Display Glacite Tunnels Waypoints"), ModConfig.INSTANCE.displayGlaciteWaypoints)
                .setDefaultValue(true).setTooltip(Text.literal("Display umber and tungsten vein waypoints while in the glacite tunnels"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.displayGlaciteWaypoints = v).build());

        mining.add(eb.startEnumSelector(Text.literal("Waypoint Type"), ModConfig.GlaciteWaypoints.class, ModConfig.INSTANCE.glaciteWaypoints)
                .setDefaultValue(ModConfig.GlaciteWaypoints.BOTH).setTooltip(Text.literal("Which waypoints to display: Umber, Tungsten, or Both"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.glaciteWaypoints = v).build());

        return mining;
    }

    // HUD Settings
    private static void buildHudCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory hud = builder.getOrCreateCategory(Text.literal("HUD Settings"));

        // Core
        hud.addEntry(eb.startBooleanToggle(Text.literal("HUD Enabled"), ModConfig.INSTANCE.hudEnabled)
                .setDefaultValue(true).setTooltip(Text.literal("Master toggle for the SSU HUD"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudEnabled = v).build());

        hud.addEntry(eb.startIntSlider(Text.literal("HUD Scale (%)"), ModConfig.INSTANCE.hudScale, 50, 100)
                .setDefaultValue(100).setTooltip(Text.literal("Scale of the HUD elements"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudScale = v).build());

        // Stats subcategory
        SubCategoryBuilder stats = eb.startSubCategory(Text.literal("Stats"))
                .setTooltip(Text.literal("Server and client stat display"));

        stats.add(eb.startBooleanToggle(Text.literal("Show Ping"), ModConfig.INSTANCE.hudPing)
                .setDefaultValue(true).setTooltip(Text.literal("Show your current ping"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudPing = v).build());

        stats.add(eb.startBooleanToggle(Text.literal("Show TPS"), ModConfig.INSTANCE.hudTps)
                .setDefaultValue(true).setTooltip(Text.literal("Show the server's current TPS"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudTps = v).build());

        stats.add(eb.startBooleanToggle(Text.literal("Show FPS"), ModConfig.INSTANCE.hudFps)
                .setDefaultValue(true).setTooltip(Text.literal("Show your current FPS"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudFps = v).build());

        stats.add(eb.startBooleanToggle(Text.literal("Show Coordinates"), ModConfig.INSTANCE.hudCoords)
                .setDefaultValue(true).setTooltip(Text.literal("Show your current coordinates"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudCoords = v).build());

        hud.addEntry(stats.build());

        // Time subcategory
        SubCategoryBuilder time = eb.startSubCategory(Text.literal("Time"))
                .setTooltip(Text.literal("Real-time clock display"));

        time.add(eb.startBooleanToggle(Text.literal("Show Time"), ModConfig.INSTANCE.hudTime)
                .setDefaultValue(true).setTooltip(Text.literal("Show your real local time in the HUD"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudTime = v).build());

        time.add(eb.startSelector(Text.literal("Time Format"), new String[]{"24H", "12H"}, ModConfig.INSTANCE.hudTime12hFormat ? "12H" : "24H")
                .setDefaultValue("24H").setTooltip(Text.literal("Whether to display time in 12-hour or 24-hour format"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudTime12hFormat = v.equals("12H")).build());

        hud.addEntry(time.build());

        // Info subcategory
        SubCategoryBuilder info = eb.startSubCategory(Text.literal("Info"))
                .setTooltip(Text.literal("Contextual info overlays"));

        info.add(eb.startBooleanToggle(Text.literal("Party Info"), ModConfig.INSTANCE.hudPartyInfo)
                .setDefaultValue(true).setTooltip(Text.literal("Show current party leader and members in the HUD"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudPartyInfo = v).build());

        info.add(eb.startBooleanToggle(Text.literal("Party Info In Dungeons"), ModConfig.INSTANCE.hudPartyInfoInDungeons)
                .setDefaultValue(false).setTooltip(Text.literal("Also show party info while inside a dungeon"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudPartyInfoInDungeons = v).build());

        info.add(eb.startBooleanToggle(Text.literal("Scoreboard In HUD"), ModConfig.INSTANCE.sideBarInHud)
                .setDefaultValue(false).setTooltip(Text.literal("Show scoreboard content in the HUD instead of the sidebar"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.sideBarInHud = v).build());

        info.add(eb.startBooleanToggle(Text.literal("Island Fun Facts"), ModConfig.INSTANCE.hudIslandFunFact)
                .setDefaultValue(true).setTooltip(Text.literal("Display a fun fact when you're on your island"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudIslandFunFact = v).build());

        hud.addEntry(info.build());
    }

    // Chat Filters
    private static void buildChatFiltersCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Text.literal("Chat Filters"));

        category.addEntry(eb.startBooleanToggle(Text.literal("Chat Filters Enabled"), ModConfig.INSTANCE.chatFiltersEnabled)
                .setDefaultValue(true).setTooltip(Text.literal("Master toggle for all chat filters"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.chatFiltersEnabled = v).build());

        for (ChatFilterDefinitions.FilterCategory filterCategory : ChatFilterDefinitions.getRootCategories()) {
            category.addEntry(buildFilterCategory(filterCategory, eb).build());
        }
    }

    private static SubCategoryBuilder buildFilterCategory(ChatFilterDefinitions.FilterCategory category, ConfigEntryBuilder eb) {
        SubCategoryBuilder sub = eb.startSubCategory(Text.literal(category.name))
                .setTooltip(Text.literal(category.tooltip));

        for (ChatFilterDefinitions.FilterEntry entry : category.entries) {
            sub.add(eb.startBooleanToggle(Text.literal(entry.displayName), ModConfig.INSTANCE.getChatFilter(entry.configKey))
                    .setDefaultValue(entry.defaultValue).setTooltip(Text.literal(entry.tooltip))
                    .setSaveConsumer(v -> ModConfig.INSTANCE.setChatFilter(entry.configKey, v)).build());
        }

        for (ChatFilterDefinitions.FilterCategory child : category.subCategories) {
            sub.add(buildFilterCategory(child, eb).build());
        }

        return sub;
    }
}