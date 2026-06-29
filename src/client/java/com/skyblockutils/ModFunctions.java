package com.skyblockutils;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.*;
import com.skyblockutils.features.hud.SsuHud;
import com.skyblockutils.features.mining.CorlTimer;
import com.skyblockutils.features.dungeons.AutoRejoin;
import com.skyblockutils.features.dungeons.DowntimeTracker;
import com.skyblockutils.features.party.PartyListParser;
import com.skyblockutils.mixin.client.BossHealthOverlayAccessor;
import com.skyblockutils.mixin.client.PingDebugMonitorAccessor;
import com.skyblockutils.mixin.client.PingDebugMonitorInvoker;
import com.skyblockutils.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.world.scores.DisplaySlot;

import java.util.Arrays;
import java.util.List;

public class ModFunctions {
    public static boolean playerWelcomedToIsland = false;
    public static long lastPingMeasure = 0;
    public static int ping = 0;
    public static float tps = 0;

    public static int COLOR_MAIN = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);

    public static void connectionEventDataReset(String type) {
        Minecraft client = Minecraft.getInstance();

        if (type.equals("Join")) {
            GuiBlocker.shouldHideScreen = false;
            UpdateChecker.init(client);
        } else {
            StrayersSkyblockUtilsClient.isInSkyblock = false;
            AutoRejoin.resetAutoRejoin();
        }

        playerWelcomedToIsland = false;
        SsuHud.funFactHandled = false;
        CorlTimer.corlTimerEnabled = false;
        PuffTracker.puffTrackerEnabled = false;
        PartyListParser.onJoinCommandHandled = false;

        DowntimeTracker.resetDowntimeTracker();
        CorlTimer.resetCorlTimer();
        AutoFish.resetAutoFish();
        NpcFinder.clear();
        SideBarUtils.resetLocation();
    }

    public static void calculatePing(Minecraft client, ClientPacketListener listener) {
        ((PingDebugMonitorInvoker) ((PingDebugMonitorAccessor) listener).getPingDebugMonitor()).ssu$tick();

        if (lastPingMeasure + 1000 > System.currentTimeMillis()) return;
        lastPingMeasure = System.currentTimeMillis();

        LocalSampleLogger pingLog = client.getDebugOverlay().getPingLogger();
        if (pingLog.size() > 0) {
            ping = (int) pingLog.get(pingLog.size() - 1);
        }
    }

    public static void handleSkyblockExclusiveKeybinds(Minecraft client) {
        while (ModKeyBindings.CORLEONE_TIMER_KEY.consumeClick()) CorlTimer.toggleCorlTimer();
        while (ModKeyBindings.AUTOFISH_KEY.consumeClick()) AutoFish.toggleAutoFish(client);
        while (ModKeyBindings.PUFF_TIMER_KEY.consumeClick()) PuffTracker.togglePuffTimer();
        SsuHud.setVisible(ModKeyBindings.HUD_KEY.isDown());
    }

    public static void handleNonSkyblockExclusiveKeybinds(Minecraft client) {
        while (ModKeyBindings.PRINT_COORDINATES_KEY.consumeClick())
            sendCoordinates(client, ModConfig.INSTANCE.coordinatesSendLocation ? "withLocation" : "");
        boolean zoomPressed = ModKeyBindings.ZOOM_KEY.isDown();
        if (zoomPressed && !ZoomState.isZooming) Zoom.enter(client);
        else if (!zoomPressed && ZoomState.isZooming) Zoom.exit(client);
    }

    public static void sendCoordinates(Minecraft client, String argumentsString) {
        if (client.player == null || client.getConnection() == null) return;

        List<String> arguments = Arrays.asList(argumentsString.split("-"));

        String coordinates = "x:" + (int) client.player.getX()
                + " y:" + (int) client.player.getY()
                + " z:" + (int) client.player.getZ();

        String coordinatesMessage = (arguments.contains("withLocation")
                ? SideBarUtils.location.isEmpty() ? "" : SideBarUtils.location + " | "
                : "") + coordinates;

        client.getConnection().sendChat(coordinatesMessage);
    }

    public static Component getFormattedCoordinates() {
        Minecraft client = Minecraft.getInstance();
        MutableComponent coordinatesText = Component.empty();

        if (client.player == null) return coordinatesText;

        coordinatesText.append(Component.literal("X: ").withColor(COLOR_MAIN)).append(String.valueOf((int) client.player.getX()))
                .append(Component.literal(" Y: ").withColor(COLOR_MAIN)).append(String.valueOf((int) client.player.getY()))
                .append(Component.literal(" Z: ").withColor(COLOR_MAIN)).append(String.valueOf((int) client.player.getZ()));

        return coordinatesText;
    }

    public static void showTitle(Minecraft client, Component title, int displayTime) {
        if (client.player != null) {
            client.gui.hud.setTimes(10, displayTime, 10);
            client.gui.hud.setTitle(title);
        }
    }

    public static void displayTextMessageWithHeader(String message) {
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(
                Component.empty().append(SSU.getName()).append(Component.literal(message))
        );
    }

    public static void displayComponentMessageWithHeader(Component message) {
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(
                Component.empty().append(SSU.getName()).append(message)
        );
    }

    public static Boolean isInSkyblock(Minecraft client) {
        if (client.level == null) return null;
        var scoreboard = client.level.getScoreboard();
        var sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return null;
        return sidebar.getDisplayName().getString().contains("SKYBLOCK");
    }

    public static Boolean isInDungeons(Minecraft client) {
        String location = SideBarUtils.location;
        boolean isInCatacombs = location != null && location.contains("The Catacombs");
        boolean hasF3Boss = ((BossHealthOverlayAccessor) client.gui.hud.getBossOverlay())
                .getEvents().values().stream().findFirst()
                .map(bossBar -> bossBar.getName().getString().replaceAll("§.", "").contains("The Professor"))
                .orElse(false);

        return isInCatacombs || hasF3Boss;
    }

    public static String mapLocationToGeneralArea(String location) {
        if (location == null) return "Unknown";

        return switch (location) {
            // Hub and all sub-locations
            case "⏣ Hub", "⏣ Canvas Room", "⏣ Carnival", "⏣ Combat Settlement",
                 "⏣ Archery Range", "⏣ Library", "⏣ Thaumaturgist", "⏣ Trade Center",
                 "⏣ Colosseum", "⏣ Election Room", "⏣ Farm", "⏣ Farmhouse",
                 "⏣ Fishing Outpost", "⏣ Fisherman's Hut", "⏣ Forest", "⏣ Foraging Camp",
                 "⏣ Graveyard", "⏣ Hub Crypts", "⏣ Tavern", "⏣ Mining District",
                 "⏣ Blacksmith", "⏣ Coal Mine", "⏣ Mountain", "⏣ Wizard Tower",
                 "⏣ Regalia Room", "⏣ Ruins", "⏣ Unincorporated", "⏣ Village",
                 "⏣ Abiphones & Co.", "⏣ Auction House", "⏣ Bank", "⏣ Bazaar Alley",
                 "⏣ Builder's House", "⏣ Community Center", "⏣ Fashion Shop",
                 "⏣ Flower House", "⏣ Hexatorum", "⏣ Museum", "⏣ Pet Care",
                 "⏣ Rabbit House", "⏣ Sewer", "⏣ Shen's Auction", "⏣ Taylor's Shop",
                 "⏣ Wilderness", "⏣ Artist's Abode", "⏣ Dark Auction" -> "Hub";

            // Private Island
            case "⏣ Private Island" -> "Private Island";

            // The Garden
            case "⏣ The Garden" -> "The Garden";

            // The Park and sub-locations
            case "⏣ Birch Park", "⏣ Spruce Woods", "⏣ Viking Longhouse",
                 "⏣ Dark Thicket", "⏣ Howling Cave", "⏣ Trials of Fire",
                 "⏣ Savanna Woodland", "⏣ Soul Cave", "⏣ Melody's Plateau",
                 "⏣ Jungle Island", "⏣ Spirit Cave" -> "The Park";

            // Galatea / Moonglade Marsh
            case "⏣ Moonglade Marsh", "⏣ Ancient Ruins", "⏣ Bubbleboost Column",
                 "⏣ Dive-Ember Pass", "⏣ Driptoad Delve", "⏣ Driptoad Pass",
                 "⏣ Dragon's Lair", "⏣ Drowned Reliquary", "⏣ Evergreen Plateau",
                 "⏣ Forest Temple", "⏣ Fusion House", "⏣ Kelpwoven Tunnels",
                 "⏣ Moonglade's Edge", "⏣ Murkwater Depths", "⏣ Murkwater Loch",
                 "⏣ Murkwater Outpost", "⏣ Murkwater Shallows", "⏣ North Reaches",
                 "⏣ North Wetlands", "⏣ Red House", "⏣ Reefguard Pass",
                 "⏣ Side-Ember Way", "⏣ Stride-Ember Fissure", "⏣ South Reaches",
                 "⏣ South Wetlands", "⏣ SwampCut Inc.", "⏣ Tangleburg's Path",
                 "⏣ Tangleburg", "⏣ Tangleburg Library", "⏣ Tangleburg Bank",
                 "⏣ Tomb Floodway", "⏣ Tranquil Pass", "⏣ Tranquility Sanctum",
                 "⏣ Verdant Summit", "⏣ West Reaches", "⏣ Westbound Wetlands",
                 "⏣ Wyrmgrove Tomb" -> "Galatea";

            // The Barn
            case "⏣ The Barn", "⏣ Windmill" -> "The Barn";

            // Mushroom Desert
            case "⏣ Mushroom Desert", "⏣ Desert Settlement", "⏣ Oasis",
                 "⏣ Shepherd's Keep", "⏣ Trapper's Den", "⏣ Jake's House",
                 "⏣ Mushroom Gorge", "⏣ Overgrown Mushroom Cave",
                 "⏣ Glowing Mushroom Cave" -> "Mushroom Desert";

            // Spider's Den
            case "⏣ Spider's Den", "⏣ Arachne's Burrow", "⏣ Arachne's Sanctuary",
                 "⏣ Archaeologist's Camp", "⏣ Grandma's House", "⏣ Gravel Mines",
                 "⏣ Spider Mound" -> "Spider's Den";

            // The End
            case "⏣ The End", "⏣ Dragon's Nest", "⏣ Void Sepulture",
                 "⏣ Void Slate", "⏣ Zealot Bruiser Hideout" -> "The End";

            // Crimson Isle
            case "⏣ Crimson Isle", "⏣ Aura's Lab", "⏣ Barbarian Outpost",
                 "⏣ Belly of the Beast", "⏣ Blazing Volcano", "⏣ Burning Desert",
                 "⏣ Courtyard", "⏣ Crimson Fields", "⏣ Dojo", "⏣ Dragontail",
                 "⏣ Dragontail Auction House", "⏣ Dragontail Bank", "⏣ Dragontail Bazaar",
                 "⏣ Dragontail Blacksmith", "⏣ Chief's Hut", "⏣ Dragontail Minion Shop",
                 "⏣ Dragontail Townsquare", "⏣ Forgotten Skull", "⏣ Mage Outpost",
                 "⏣ Magma Chamber", "⏣ Matriarch's Lair", "⏣ Mystic Marsh",
                 "⏣ Odger's Hut", "⏣ Plhlegblast Pool", "⏣ Ruins of Ashfang",
                 "⏣ Scarleton", "⏣ Scarleton Auction House", "⏣ Cathedral",
                 "⏣ Igrupan's Chicken Coop", "⏣ Igrupan's House",
                 "⏣ Mage Council", "⏣ Scarleton Bank", "⏣ Scarleton Bazaar",
                 "⏣ Scarleton Blacksmith", "⏣ Scarleton Minion Shop", "⏣ Scarleton Plaza",
                 "⏣ Throne Room", "⏣ Smoldering Tomb", "⏣ Stronghold",
                 "⏣ The Bastion", "⏣ The Dukedom", "⏣ The Wasteland" -> "Crimson Isle";

            // Gold Mine
            case "⏣ Gold Mine" -> "Gold Mine";

            // Deep Caverns
            case "⏣ Deep Caverns", "⏣ Gunpowder Mines", "⏣ Lapis Quarry",
                 "⏣ Pigmen's Den", "⏣ Slimehill", "⏣ Diamond Reserve",
                 "⏣ Obsidian Sanctuary" -> "Deep Caverns";

            // Dwarven Mines
            case "⏣ Dwarven Mines", "⏣ Abandoned Quarry", "⏣ Cliffside Veins",
                 "⏣ Divan's Gateway", "⏣ Dwarven Base Camp", "⏣ Dwarven Village",
                 "⏣ Dwarven Tavern", "⏣ Far Reserve", "⏣ Fossil Research Center",
                 "⏣ Gates to the Mines", "⏣ Goblin Burrows", "⏣ Glacite Tunnels",
                 "⏣ Great Glacite Lake", "⏣ Great Ice Wall", "⏣ Rampart's Quarry",
                 "⏣ Ironman's Guild", "⏣ Royal Mines", "⏣ Royal Palace",
                 "⏣ Aristocrat Passage", "⏣ Barracks of Heroes", "⏣ Grand Library",
                 "⏣ Hanging Court", "⏣ Palace Bridge", "⏣ Royal Quarters",
                 "⏣ The Forge", "⏣ Forge Basin", "⏣ The Lift", "⏣ The Mist",
                 "⏣ Upper Mines", "⏣ Lava Springs" -> "Dwarven Mines";

            // Crystal Hollows
            case "⏣ Crystal Hollows", "⏣ Crystal Nucleus", "⏣ Fairy Grotto",
                 "⏣ Goblin Holdout", "⏣ Goblin Queen's Den", "⏣ Jungle",
                 "⏣ Jungle Temple", "⏣ Magma Fields", "⏣ Khazad-dûm",
                 "⏣ Mithril Deposits", "⏣ Mines of Divan", "⏣ Precursor Remnants",
                 "⏣ Lost Precursor City" -> "Crystal Hollows";

            // Jerry's Workshop
            case "⏣ Jerry's Workshop", "⏣ Einary's Emporium", "⏣ Gary's Shack",
                 "⏣ Glacial Cave", "⏣ Hot Springs", "⏣ Jerry Pond", "⏣ Mount Jerry",
                 "⏣ Reflective Pond", "⏣ Sherry's Showroom", "⏣ Sunken Jerry Pond",
                 "⏣ Terry's Shack" -> "Jerry's Workshop";

            // Rift Dimension
            case "ф Black Lagoon", "ф Lagoon Cave", "ф Lagoon Hut", "ф Leeches Lair",
                 "ф Colosseum", "ф Around Colosseum", "ф Dreadfarm", "ф Great Beanstalk",
                 "ф Living Cave", "ф Living Stillness", "ф The Mountaintop", "ф Walk of Fame",
                 "ф Wizard Brawl", "ф Cerebral Citadel", "ф Continuum", "ф The Vents",
                 "ф Rose's End", "ф Trial Grounds", "ф Wizardman Bureau", "ф Otherside",
                 "ф Rift Gallery", "ф Rift Gallery Entrance", "ф Stillgore Château",
                 "ф Fairylosophy Tower", "ф Oubliette", "ф Time Chamber", "ф Village Plaza",
                 "ф Barrier Street", "ф Barry Center", "ф Barry HQ", "ф Déjà Vu Alley",
                 "ф Empty Bank", "ф Half-Eaten Cave", "ф Murder House", "ф Lonely Terrace",
                 "ф Photon Pathway", "ф Taylor's", "ф \"Your\" Island", "ф West Village",
                 "ф Cake House", "ф Dolphin Trainer", "ф Infested House", "ф Mirrorverse",
                 "ф Wyld Woods", "ф Broken Cage", "ф Enigma's Crib", "ф Pumpgrotto",
                 "ф Shifted Tavern", "ф The Bastion", "ф Wizard Tower",
                 "ф Book in a Book" -> "Rift";

            // Dungeon Hub
            case "⏣ Dungeon Hub" -> "Dungeon Hub";

            // Backwater Bayou
            case "⏣ Backwater Bayou" -> "Backwater Bayou";

            // Lotus Atoll
            case "⏣ Lotus Atoll", "⏣ Lotus Easter's Cave", "⏣ Tewtil Tunnel", "⏣ Lotus Highlands" -> "Lotus Atoll";

            default -> location;
        };
    }
}