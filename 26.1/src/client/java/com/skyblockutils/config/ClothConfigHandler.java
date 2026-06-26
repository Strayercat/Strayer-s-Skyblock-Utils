package com.skyblockutils.config;

import com.skyblockutils.features.chat.ChatFilterDefinitions;
import com.skyblockutils.features.glowingPlayers.GlowingPlayersGui;
import com.skyblockutils.utils.CustomEntry;
import com.skyblockutils.utils.ModStyle;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ClothConfigHandler {
    public static boolean configScreenRequested = false;

    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTransparentBackground(false)
                .setDefaultBackgroundTexture(Identifier.fromNamespaceAndPath("skyblockutils", "textures/gui/background.png"))
                .setTitle(Component.literal("Strayer's Skyblock Utils (SSU) Config"))
                .setSavingRunnable(ModConfig::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        buildGeneralCategory(builder, eb);
        buildHudCategory(builder, eb);
        buildChatFiltersCategory(builder, eb);

        return builder.build();
    }

    public static void handleConfigScreen(Minecraft client) {
        if (configScreenRequested) client.setScreen(createConfigScreen(client.screen));
        configScreenRequested = false;
    }

    // General Settings
    public static void buildGeneralCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General Settings"));

        general.addEntry(eb.startEnumSelector(Component.literal("Mod Color Style"), ModStyle.ColorStyle.class, ModConfig.INSTANCE.colorStyle)
                .setDefaultValue(ModStyle.ColorStyle.ORIGINAL)
                .setEnumNameProvider(value -> {
                    ModStyle.ColorStyle style = (ModStyle.ColorStyle) value;
                    int rgb = style.getColor(ModStyle.ColorType.MAIN);
                    String name = style.name().charAt(0) + style.name().substring(1).toLowerCase();
                    return Component.literal(name).withColor(rgb);
                })
                .setSaveConsumer(v -> ModConfig.INSTANCE.colorStyle = v).requireRestart().build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Party Invite Notifications"), ModConfig.INSTANCE.partyInviteNotifications)
                .setDefaultValue(true).setTooltip(Component.literal("Sends party invites as a notification instead of a chat message"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.partyInviteNotifications = v).build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Fancy Emotes"), ModConfig.INSTANCE.fancyEmotes)
                .setDefaultValue(true).setTooltip(Component.literal("Transforms <3 into ♥ and such"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.fancyEmotes = v).build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Auto Hoppity Eggs"), ModConfig.INSTANCE.autoHoppityEggs)
                .setDefaultValue(false).setTooltip(Component.literal("Whether to instantly collect hoppity eggs or not"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.autoHoppityEggs = v).build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Coordinates Send Location"), ModConfig.INSTANCE.coordinatesSendLocation)
                .setDefaultValue(true).setTooltip(Component.literal("Whether printing your coordinates (via the keybind) should also send the Skyblock location"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.coordinatesSendLocation = v).build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Chat Commands"), ModConfig.INSTANCE.chatCommands)
                .setDefaultValue(true).setTooltip(Component.literal("Whether or not to enable !gay !furry !sus and such commands"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.chatCommands = v).build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Custom Sidebar"), ModConfig.INSTANCE.customSidebar)
                .setDefaultValue(false).setTooltip(Component.literal("Whether or not to replace the vanilla scoreboard (sidebar) with a custom one"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.customSidebar = v).build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Sidebar Coordinates"), ModConfig.INSTANCE.sidebarCoords)
                .setDefaultValue(false).setTooltip(Component.literal("Whether or not to show coordinates in the custom sidebar"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.sidebarCoords = v).build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Party Glow"), ModConfig.INSTANCE.partyGlow)
                .setDefaultValue(true).setTooltip(Component.literal("Whether or not party members should automatically glow"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.partyGlow = v).build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Ragebait button"), true)
                .setDefaultValue(true).setTooltip(Component.literal("No matter how much you toggle it, whenever you come back it'll always be true"))
                .build());

        general.addEntry(new CustomEntry()
                .addButton(Component.literal("Glowing Players GUI"), 120, CustomEntry.Alignment.LEFT, () ->
                                GlowingPlayersGui.configScreenRequested = true
                        , Component.literal("Open the glowing players config GUI")));

        general.addEntry(buildDungeonsSubcategory(eb).build());
        general.addEntry(buildMiningSubcategory(eb).build());
    }

    private static SubCategoryBuilder buildDungeonsSubcategory(ConfigEntryBuilder eb) {
        SubCategoryBuilder dungeons = eb.startSubCategory(Component.literal("Dungeons"))
                .setTooltip(Component.literal("Dungeon-related features"));

        dungeons.add(eb.startBooleanToggle(Component.literal("Floor Commands"), ModConfig.INSTANCE.dungeonPartyCommands)
                .setDefaultValue(true).setTooltip(Component.literal("Allow party members to type '!f2' style commands to start a dungeon run"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.dungeonPartyCommands = v).build());

        dungeons.add(eb.startBooleanToggle(Component.literal("Downtime Tracker"), ModConfig.INSTANCE.downtimeTracker)
                .setDefaultValue(true).setTooltip(Component.literal("Detect '!dt' messages and remind you when a dungeon run ends"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.downtimeTracker = v).build());

        dungeons.add(eb.startBooleanToggle(Component.literal("Auto-Rejoin Reminders"), ModConfig.INSTANCE.autoRejoinReminders)
                .setDefaultValue(false).setTooltip(Component.literal("Reminds you that auto-rejoin is still enabled every time a run ends"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.autoRejoinReminders = v).build());

        return dungeons;
    }

    private static SubCategoryBuilder buildMiningSubcategory(ConfigEntryBuilder eb) {
        SubCategoryBuilder mining = eb.startSubCategory(Component.literal("Mining"))
                .setTooltip(Component.literal("Mining-related features"));

        mining.add(eb.startBooleanToggle(Component.literal("Display Glacite Tunnels Waypoints"), ModConfig.INSTANCE.displayGlaciteWaypoints)
                .setDefaultValue(true).setTooltip(Component.literal("Display umber and tungsten vein waypoints while in the glacite tunnels"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.displayGlaciteWaypoints = v).build());

        mining.add(eb.startEnumSelector(Component.literal("Waypoint Type"), ModConfig.GlaciteWaypoints.class, ModConfig.INSTANCE.glaciteWaypoints)
                .setDefaultValue(ModConfig.GlaciteWaypoints.BOTH).setTooltip(Component.literal("Which waypoints to display: Umber, Tungsten, or Both"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.glaciteWaypoints = v).build());

        return mining;
    }

    // HUD Settings
    public static void buildHudCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory hud = builder.getOrCreateCategory(Component.literal("HUD Settings"));

        hud.addEntry(eb.startBooleanToggle(Component.literal("HUD Enabled"), ModConfig.INSTANCE.hudEnabled)
                .setDefaultValue(true).setTooltip(Component.literal("Master toggle for the SSU HUD"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudEnabled = v).build());

        hud.addEntry(eb.startIntSlider(Component.literal("HUD Scale (%)"), ModConfig.INSTANCE.hudScale, 50, 100)
                .setDefaultValue(100).setTooltip(Component.literal("Scale of the HUD elements"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudScale = v).build());

        SubCategoryBuilder stats = eb.startSubCategory(Component.literal("Stats"))
                .setTooltip(Component.literal("Server and client stat display"));

        stats.add(eb.startBooleanToggle(Component.literal("Show Ping"), ModConfig.INSTANCE.hudPing)
                .setDefaultValue(true).setTooltip(Component.literal("Show your current ping"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudPing = v).build());

        stats.add(eb.startBooleanToggle(Component.literal("Show TPS"), ModConfig.INSTANCE.hudTps)
                .setDefaultValue(true).setTooltip(Component.literal("Show the server's current TPS"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudTps = v).build());

        stats.add(eb.startBooleanToggle(Component.literal("Show FPS"), ModConfig.INSTANCE.hudFps)
                .setDefaultValue(true).setTooltip(Component.literal("Show your current FPS"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudFps = v).build());

        stats.add(eb.startBooleanToggle(Component.literal("Show Coordinates"), ModConfig.INSTANCE.hudCoords)
                .setDefaultValue(true).setTooltip(Component.literal("Show your current coordinates"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudCoords = v).build());

        hud.addEntry(stats.build());

        SubCategoryBuilder time = eb.startSubCategory(Component.literal("Time"))
                .setTooltip(Component.literal("Real-time clock display"));

        time.add(eb.startBooleanToggle(Component.literal("Show Time"), ModConfig.INSTANCE.hudTime)
                .setDefaultValue(true).setTooltip(Component.literal("Show your real local time in the HUD"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudTime = v).build());

        time.add(eb.startSelector(Component.literal("Time Format"), new String[]{"24H", "12H"}, ModConfig.INSTANCE.hudTime12hFormat ? "12H" : "24H")
                .setDefaultValue("24H").setTooltip(Component.literal("Whether to display time in 12-hour or 24-hour format"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudTime12hFormat = v.equals("12H")).build());

        hud.addEntry(time.build());

        SubCategoryBuilder info = eb.startSubCategory(Component.literal("Info"))
                .setTooltip(Component.literal("Contextual info overlays"));

        info.add(eb.startBooleanToggle(Component.literal("Party Info"), ModConfig.INSTANCE.hudPartyInfo)
                .setDefaultValue(true).setTooltip(Component.literal("Show current party leader and members in the HUD"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudPartyInfo = v).build());

        info.add(eb.startBooleanToggle(Component.literal("Party Info In Dungeons"), ModConfig.INSTANCE.hudPartyInfoInDungeons)
                .setDefaultValue(false).setTooltip(Component.literal("Also show party info while inside a dungeon"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudPartyInfoInDungeons = v).build());

        info.add(eb.startBooleanToggle(Component.literal("Scoreboard In HUD"), ModConfig.INSTANCE.sideBarInHud)
                .setDefaultValue(false).setTooltip(Component.literal("Show scoreboard content in the HUD instead of the sidebar"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.sideBarInHud = v).build());

        info.add(eb.startBooleanToggle(Component.literal("Island Fun Facts"), ModConfig.INSTANCE.hudIslandFunFact)
                .setDefaultValue(true).setTooltip(Component.literal("Display a fun fact when you're on your island"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.hudIslandFunFact = v).build());

        hud.addEntry(info.build());
    }

    // Chat Filters
    public static void buildChatFiltersCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.literal("Chat Filters"));

        category.addEntry(eb.startBooleanToggle(Component.literal("Chat Filters Enabled"), ModConfig.INSTANCE.chatFiltersEnabled)
                .setDefaultValue(true).setTooltip(Component.literal("Master toggle for all chat filters"))
                .setSaveConsumer(v -> ModConfig.INSTANCE.chatFiltersEnabled = v).build());

        for (ChatFilterDefinitions.FilterCategory filterCategory : ChatFilterDefinitions.getRootCategories()) {
            category.addEntry(buildFilterCategory(filterCategory, eb).build());
        }
    }

    private static SubCategoryBuilder buildFilterCategory(ChatFilterDefinitions.FilterCategory category, ConfigEntryBuilder eb) {
        SubCategoryBuilder sub = eb.startSubCategory(Component.literal(category.name))
                .setTooltip(Component.literal(category.tooltip));

        for (ChatFilterDefinitions.FilterEntry entry : category.entries) {
            sub.add(eb.startBooleanToggle(Component.literal(entry.displayName), ModConfig.INSTANCE.getChatFilter(entry.configKey))
                    .setDefaultValue(entry.defaultValue).setTooltip(Component.literal(entry.tooltip))
                    .setSaveConsumer(v -> ModConfig.INSTANCE.setChatFilter(entry.configKey, v)).build());
        }

        for (ChatFilterDefinitions.FilterCategory child : category.subCategories) {
            sub.add(buildFilterCategory(child, eb).build());
        }

        return sub;
    }
}