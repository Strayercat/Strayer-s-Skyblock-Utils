package com.skyblockutils.config;

import com.skyblockutils.features.chatFilters.ChatFilterDefinitions;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ClothConfigHandler {
    public static boolean configScreenRequested = false;

    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Text.literal("Strayer's Skyblock Utils (SSU) Config")).setSavingRunnable(ModConfig::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // General Settings Category
        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General Settings"));

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Party Invite Notifications"), ModConfig.INSTANCE.partyInviteNotifications).setDefaultValue(false).setTooltip(Text.literal("Sends party invites as a notification instead of a chat message")).setSaveConsumer(newValue -> ModConfig.INSTANCE.partyInviteNotifications = newValue).build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Fancy Emotes"), ModConfig.INSTANCE.fancyEmotes).setDefaultValue(false).setTooltip(Text.literal("Transforms <3 into ♥ and such")).setSaveConsumer(newValue -> ModConfig.INSTANCE.fancyEmotes = newValue).build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Auto Hoppity Eggs"), ModConfig.INSTANCE.autoHoppityEggs).setDefaultValue(false).setTooltip(Text.literal("Whether to instantly collect hoppity eggs or not")).setSaveConsumer(newValue -> ModConfig.INSTANCE.autoHoppityEggs = newValue).build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Coordinates Location"), ModConfig.INSTANCE.coordinatesSendLocation).setDefaultValue(true).setTooltip(Text.literal("Wheter printing your coordinates (via the keybind) should also send the Skyblock location or not")).setSaveConsumer(newValue -> ModConfig.INSTANCE.coordinatesSendLocation = newValue).build());

        // Dungeons subcategory
        SubCategoryBuilder dungeonsBuilder = entryBuilder.startSubCategory(Text.literal("Dungeons")).setTooltip(Text.literal("Dungeon-related features"));
        dungeonsBuilder.add(entryBuilder.startBooleanToggle(Text.literal("Downtime Tracker"), ModConfig.INSTANCE.downtimeTracker).setDefaultValue(false).setTooltip(Text.literal("Detect messages starting with '!dt' or 'dt' and remind you when a dungeon run ends")).setSaveConsumer(newValue -> ModConfig.INSTANCE.downtimeTracker = newValue).build());
        dungeonsBuilder.add(entryBuilder.startBooleanToggle(Text.literal("Floor Commands"), ModConfig.INSTANCE.dungeonPartyCommands).setDefaultValue(false).setTooltip(Text.literal("Allow '!f2' format messages for joining a dungeon")).setSaveConsumer(newValue -> ModConfig.INSTANCE.dungeonPartyCommands = newValue).build());
        dungeonsBuilder.add(entryBuilder.startBooleanToggle(Text.literal("Auto-Rejoin Reminders"), ModConfig.INSTANCE.autoRejoinReminders).setDefaultValue(false).setTooltip(Text.literal("Reminds you about auto-rejoin being enabled every time a run ends")).setSaveConsumer(newValue -> ModConfig.INSTANCE.autoRejoinReminders = newValue).build());
        general.addEntry(dungeonsBuilder.build());

        // Mining subcategory
        SubCategoryBuilder miningBuilder = entryBuilder.startSubCategory(Text.literal("Mining")).setTooltip(Text.literal("Mining-related features"));
        miningBuilder.add(entryBuilder.startBooleanToggle(Text.literal("Display Glacite Tunnels Waypoints"), ModConfig.INSTANCE.displayGlaciteWaypoints).setDefaultValue(true).setTooltip(Text.literal("Whether or not to display umber and tungsten vein waypoints while in the glacite tunnels")).setSaveConsumer(newValue -> ModConfig.INSTANCE.displayGlaciteWaypoints = newValue).build());
        miningBuilder.add(entryBuilder
                .startEnumSelector(
                        Text.literal("Mining Waypoints"),
                        ModConfig.GlaciteWaypoints.class,
                        ModConfig.INSTANCE.glaciteWaypoints
                )
                .setDefaultValue(ModConfig.GlaciteWaypoints.BOTH)
                .setTooltip(Text.literal("Cycles between which waypoints to display: Umber, Tungsten, or Both."))
                .setSaveConsumer(newValue -> ModConfig.INSTANCE.glaciteWaypoints = newValue)
                .build());
        general.addEntry(miningBuilder.build());

        // Chat Filters Category
        ConfigCategory cfCategory = builder.getOrCreateCategory(Text.literal("Chat Filters"));

        cfCategory.addEntry(entryBuilder.startBooleanToggle(Text.literal("Chat Filters Enabled"), ModConfig.INSTANCE.chatFiltersEnabled).setDefaultValue(false).setTooltip(Text.literal("Master toggle for all chat filters")).setSaveConsumer(newValue -> ModConfig.INSTANCE.chatFiltersEnabled = newValue).build());

        for (ChatFilterDefinitions.FilterCategory category : ChatFilterDefinitions.getRootCategories()) {
            SubCategoryBuilder categoryBuilder = buildCategory(category, entryBuilder);
            cfCategory.addEntry(categoryBuilder.build());
        }

        // SSU HUD category
        ConfigCategory hud = builder.getOrCreateCategory(Text.literal("HUD Settings"));
        hud.addEntry(entryBuilder.startBooleanToggle(Text.literal("HUD Enabled"), ModConfig.INSTANCE.hudEnabled).setDefaultValue(true).setTooltip(Text.literal("Whether to enable SSU HUD or not")).setSaveConsumer(newValue -> ModConfig.INSTANCE.hudEnabled = newValue).build());
        hud.addEntry(entryBuilder.startIntSlider(Text.literal("HUD Scale (%)"), ModConfig.INSTANCE.hudScale, 50, 100).setDefaultValue(100).setTooltip(Text.literal("The scale adjustment for the HUD")).setSaveConsumer(newValue -> ModConfig.INSTANCE.hudScale = newValue).build());
        hud.addEntry(entryBuilder.startBooleanToggle(Text.literal("HUD Ping"), ModConfig.INSTANCE.hudPing).setDefaultValue(true).setTooltip(Text.literal("Whether to show ping in the hud or not")).setSaveConsumer(newValue -> ModConfig.INSTANCE.hudPing = newValue).build());
        hud.addEntry(entryBuilder.startBooleanToggle(Text.literal("HUD Time"), ModConfig.INSTANCE.hudTime).setDefaultValue(true).setTooltip(Text.literal("Whether to show real time in the hud or not")).setSaveConsumer(newValue -> ModConfig.INSTANCE.hudTime = newValue).build());
        hud.addEntry(entryBuilder.startSelector(Text.literal("HUD Time Format"), new String[]{"24H", "12H"}, ModConfig.INSTANCE.hudTime12hFormat ? "12H" : "24H")
                .setDefaultValue("24H")
                .setTooltip(Text.literal("The time format in which the HUD time is shown"))
                .setSaveConsumer(newValue -> ModConfig.INSTANCE.hudTime12hFormat = newValue.equals("12H"))
                .build());
        hud.addEntry(entryBuilder.startBooleanToggle(Text.literal("HUD TPS"), ModConfig.INSTANCE.hudTps).setDefaultValue(true).setTooltip(Text.literal("Whether to show server TPS in the hud or not")).setSaveConsumer(newValue -> ModConfig.INSTANCE.hudTps = newValue).build());
        hud.addEntry(entryBuilder.startBooleanToggle(Text.literal("HUD FPS"), ModConfig.INSTANCE.hudFps).setDefaultValue(true).setTooltip(Text.literal("Whether to show current fps in the hud or not")).setSaveConsumer(newValue -> ModConfig.INSTANCE.hudFps = newValue).build());
        hud.addEntry(entryBuilder.startBooleanToggle(Text.literal("Island Fun Facts"), ModConfig.INSTANCE.hudIslandFunFact).setDefaultValue(true).setTooltip(Text.literal("Whether to display a fun fact when on your island or not")).setSaveConsumer(newValue -> ModConfig.INSTANCE.hudIslandFunFact = newValue).build());
        hud.addEntry(entryBuilder.startBooleanToggle(Text.literal("HUD Party Info"), ModConfig.INSTANCE.hudPartyInfo).setDefaultValue(true).setTooltip(Text.literal("Whether or not to show the current party leader and members in the HUD")).setSaveConsumer(newValue -> ModConfig.INSTANCE.hudPartyInfo = newValue).build());
        hud.addEntry(entryBuilder.startBooleanToggle(Text.literal("HUD Party Info In Dungeons"), ModConfig.INSTANCE.hudPartyInfoInDungeons).setDefaultValue(false).setTooltip(Text.literal("Whether or not to show the current party leader and members in the HUD when in dungeons")).setSaveConsumer(newValue -> ModConfig.INSTANCE.hudPartyInfoInDungeons = newValue).build());
        hud.addEntry(entryBuilder.startBooleanToggle(Text.literal("Scoreboard In HUD"), ModConfig.INSTANCE.sideBarInHud).setDefaultValue(false).setTooltip(Text.literal("Whether or not to show the scoreboard info in the HUD instead")).setSaveConsumer(newValue -> ModConfig.INSTANCE.sideBarInHud = newValue).build());
        return builder.build();
    }

    private static SubCategoryBuilder buildCategory(ChatFilterDefinitions.FilterCategory category, ConfigEntryBuilder entryBuilder) {
        SubCategoryBuilder builder = entryBuilder.startSubCategory(Text.literal(category.name)).setTooltip(Text.literal(category.tooltip));

        for (ChatFilterDefinitions.FilterEntry entry : category.entries) {
            builder.add(entryBuilder.startBooleanToggle(Text.literal(entry.displayName), ModConfig.INSTANCE.getChatFilter(entry.configKey)).setDefaultValue(entry.defaultValue).setTooltip(Text.literal(entry.tooltip)).setSaveConsumer(newValue -> ModConfig.INSTANCE.setChatFilter(entry.configKey, newValue)).build());
        }

        for (ChatFilterDefinitions.FilterCategory subCategory : category.subCategories) {
            builder.add(buildCategory(subCategory, entryBuilder).build());
        }

        return builder;
    }

    public static void handleConfigScreen(MinecraftClient client) {
        if (configScreenRequested) client.setScreen(ClothConfigHandler.createConfigScreen(client.currentScreen));
        configScreenRequested = false;
    }
}