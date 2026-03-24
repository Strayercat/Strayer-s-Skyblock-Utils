package com.skyblockutils.features.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ChatFilterDefinitions {

    public static class FilterCategory {
        public final String name;
        public final String tooltip;
        public final String requiredLocation;
        public final List<FilterEntry> entries = new ArrayList<>();
        public final List<FilterCategory> subCategories = new ArrayList<>();

        public FilterCategory(String name, String tooltip) {
            this(name, tooltip, null);
        }

        public FilterCategory(String name, String tooltip, String requiredLocation) {
            this.name = name;
            this.tooltip = tooltip;
            this.requiredLocation = requiredLocation;
        }

        public void addEntry(FilterEntry entry) {
            entries.add(entry);
        }

        public void addSubCategory(FilterCategory category) {
            subCategories.add(category);
        }
    }

    public static class FilterEntry {
        public final String configKey;
        public final String displayName;
        public final String tooltip;
        public final String regex;
        public final boolean defaultValue;
        public final Pattern pattern;
        public final String requiredLocation;

        public FilterEntry(String configKey, String displayName, String tooltip, String regex, boolean defaultValue, String requiredLocation) {
            this.configKey = configKey;
            this.displayName = displayName;
            this.tooltip = tooltip;
            this.regex = regex;
            this.defaultValue = defaultValue;
            this.pattern = Pattern.compile(regex);
            this.requiredLocation = requiredLocation;
        }

        public FilterEntry(String configKey, String displayName, String tooltip, String regex) {
            this(configKey, displayName, tooltip, regex, false, null);
        }
    }

    private static final List<FilterCategory> ROOT_CATEGORIES = new ArrayList<>();

    static {
        ROOT_CATEGORIES.add(buildGeneral());
        ROOT_CATEGORIES.add(buildDungeons());
        ROOT_CATEGORIES.add(buildSlayers());
        ROOT_CATEGORIES.add(buildFishing());
        ROOT_CATEGORIES.add(buildHunting());
        ROOT_CATEGORIES.add(buildMining());
        ROOT_CATEGORIES.add(buildEvents());
    }

    // General
    private static FilterCategory buildGeneral() {
        FilterCategory general = new FilterCategory("General", "General game messages");

        general.addEntry(new FilterEntry("emptyMessages", "Empty Messages", "Filter empty messages", "^\\s*$"));
        general.addEntry(new FilterEntry("welcomeToSkyblock", "Welcome to SkyBlock", "Filter 'Welcome to Hypixel SkyBlock!' messages", "^Welcome to Hypixel SkyBlock!$"));
        general.addEntry(new FilterEntry("lastUpdate", "Last Update", "Filter 'Latest update: SkyBlock...' messages", "^Latest update: SkyBlock .+ CLICK$"));
        general.addEntry(new FilterEntry("sendingToServer", "Sending to Server", "Filter 'Sending to server...' messages", "^Sending to server .+\\.{3}$"));
        general.addEntry(new FilterEntry("warpingMessage", "Warping Message", "Filter 'Warping...' messages", "^(Warping\\.{3}|Warping you to your SkyBlock island\\.{3})$"));
        general.addEntry(new FilterEntry("playingOnProfile", "Playing on Profile", "Filter 'You are playing on profile:' messages", "^You are playing on profile: .+$"));
        general.addEntry(new FilterEntry("profileId", "Profile ID", "Filter 'Profile ID:' messages", "^Profile ID: .+$"));
        general.addEntry(new FilterEntry("unclaimedEventRewards", "Unclaimed Event Rewards", "Filter unclaimed event rewards messages", "^(You have \\d+ unclaimed event rewards?!|>>> CLICK HERE to claim! <<<|Event rewards are deleted after \\d+ SkyBlock years!)$"));
        general.addEntry(new FilterEntry("deathMessages", "Death Messages", "Filter death messages", "^☠ .+$"));
        general.addEntry(new FilterEntry("coinsLostOnDeath", "Coins Lost on Death", "Filter 'You died and lost X coins!' messages", "^You died and lost [\\d.,]+ coins!$"));
        general.addEntry(new FilterEntry("lootShare", "Loot Share", "Filter 'LOOT SHARE You received loot for assisting' messages", "^LOOT SHARE You received loot for assisting .+!$"));
        general.addEntry(new FilterEntry("killCombo", "Kill Combo", "Filter '+X Kill Combo' messages", "^\\+\\d+ Kill Combo.*$"));
        general.addEntry(new FilterEntry("killComboExpired", "Kill Combo Expired", "Filter 'Your Kill Combo has expired!' messages", "^Your Kill Combo has expired! You reached a \\d+ Kill Combo!$"));

        general.addSubCategory(buildGeneralBank());
        general.addSubCategory(buildGeneralPets());
        general.addSubCategory(buildGeneralShops());
        general.addSubCategory(buildGeneralIsland());

        return general;
    }

    private static FilterCategory buildGeneralBank() {
        FilterCategory bank = new FilterCategory("Bank", "Bank related messages");
        bank.addEntry(new FilterEntry("depositingCoins", "Depositing Coins", "Filter 'Depositing coins...' messages", "^Depositing coins\\.{3}$"));
        bank.addEntry(new FilterEntry("withdrawingCoins", "Withdrawing Coins", "Filter 'Withdrawing coins...' messages", "^Withdrawing coins\\.{3}$"));
        bank.addEntry(new FilterEntry("depositedCoins", "Deposited Coins", "Filter 'Deposited X coins! There's now Y coins in the account!' messages", "^Deposited .+ coins! There's now .+ coins in the account!$"));
        bank.addEntry(new FilterEntry("withdrewCoins", "Withdrew Coins", "Filter 'Withdrew X coins! There's now Y coins left in the account!' messages", "^Withdrew .+ coins! There's now .+ coins left in the account!$"));
        return bank;
    }

    private static FilterCategory buildGeneralPets() {
        FilterCategory pets = new FilterCategory("Pets", "Pet related messages");
        pets.addEntry(new FilterEntry("petLevelUp", "Pet Level Up", "Filter 'Your [pet] leveled up to level X!' messages", "^Your .+ leveled up to level \\d+!$"));
        pets.addEntry(new FilterEntry("petSummoned", "Pet Summoned", "Filter 'You summoned your [pet]!' messages", "^You summoned your .+!$"));
        pets.addEntry(new FilterEntry("petDespawned", "Pet Despawned", "Filter 'You despawned your [pet]!' messages", "^You despawned your .+!$"));
        return pets;
    }

    private static FilterCategory buildGeneralShops() {
        FilterCategory shops = new FilterCategory("Shops", "Auction House / Bazaar / NPC sell messages");

        FilterCategory auctionHouse = new FilterCategory("Auction House", "Auction House related messages");
        auctionHouse.addEntry(new FilterEntry("puttingCoinsInEscrow", "Putting Coins In Escrow", "Filter 'Putting coins in escrow...' messages", "^Putting coins in escrow...$"));
        auctionHouse.addEntry(new FilterEntry("puttingItemInEscrow", "Putting Item In Escrow", "Filter 'Putting item in escrow...' messages", "^Putting item in escrow...$"));
        auctionHouse.addEntry(new FilterEntry("processingPurchase", "Processing Purchase", "Filter 'Processing purchase...' messages", "^Processing purchase...$"));
        auctionHouse.addEntry(new FilterEntry("settingUpAuction", "Setting Up Auction", "Filter 'Setting up the auction...' messages", "^Setting up the auction...$"));
        auctionHouse.addEntry(new FilterEntry("claimingBinAuction", "Claiming BIN Auction", "Filter 'Claiming BIN auction...' messages", "^Claiming BIN auction...$"));
        auctionHouse.addEntry(new FilterEntry("ahEscrowRefunded", "Escrow Refunded", "Filter 'Escrow refunded [x] coins for BIN Auction Buy!'", "^Escrow refunded .+ coins for BIN Auction Buy!$"));
        auctionHouse.addEntry(new FilterEntry("auctionStarted", "Auction Started", "Filter 'Auction started for [item]!' messages", "^(BIN )?Auction started for .*!$"));
        auctionHouse.addEntry(new FilterEntry("auctionPurchased", "Item Purchased", "Filter 'You purchased [item] for [x] coins!' messages", "^You purchased .+ for .+ coins!$"));
        auctionHouse.addEntry(new FilterEntry("auctionClaimed", "Auction Claimed", "Filter 'You claimed [item] from [player]'s auction!'", "^You claimed .+ from .+'s auction!$"));
        shops.addSubCategory(auctionHouse);

        FilterCategory bazaar = new FilterCategory("Bazaar", "Bazaar related messages");
        bazaar.addEntry(new FilterEntry("bazaarBought", "Bought", "Filter '[Bazaar] Bought x[x] [item] for [amount] coins!' messages", "^\\[Bazaar] Bought .+x .+ for .+ coins!$"));
        bazaar.addEntry(new FilterEntry("bazaarSold", "Sold", "Filter '[Bazaar] Sold [x]x [item] for [amount] coins!' messages", "^\\[Bazaar] Sold .+x .+ for .+ coins!$"));
        bazaar.addEntry(new FilterEntry("bazaarNoBuyer", "No Buyer", "Filter '[Bazaar] Couldn't find any buyers for [item]!' messages", "^\\[Bazaar] Couldn't find any buyers for .+!$"));
        bazaar.addEntry(new FilterEntry("executingInstantSell", "Executing Instant Sell", "Filter '[Bazaar] Executing instant sell...' messages", "^\\[Bazaar] Executing instant sell\\.{3}$"));
        bazaar.addEntry(new FilterEntry("bazaarEscrowRefunded", "Escrow Refunded", "Filter 'Escrow refunded [amount]x [item]!' messages", "^Escrow refunded .+x .+!$"));
        shops.addSubCategory(bazaar);

        FilterCategory npcShop = new FilterCategory("NPC Shop", "NPC sell related messages");
        npcShop.addEntry(new FilterEntry("itemSold", "Item Sold", "Filter 'You sold [item] [x]x for [amount] Coins!' messages", "^You sold .+ x.+ for .+ Coins!$"));
        npcShop.addEntry(new FilterEntry("itemCantBeSold", "Can't Be Sold", "Filter 'That item cannot be sold!' messages", "^That item cannot be sold!$"));
        shops.addSubCategory(npcShop);

        return shops;
    }

    private static FilterCategory buildGeneralIsland() {
        FilterCategory island = new FilterCategory("Island", "Private island messages");

        FilterCategory parkour = new FilterCategory("Parkour", "Island parkour messages");
        parkour.addEntry(new FilterEntry("placedParkourStartOrEnd", "Placed Start / End", "Filter 'You placed a Parkour Start/End.' messages", "^You placed a Parkour Start/End.$"));
        parkour.addEntry(new FilterEntry("removedParkourStartOrEnd", "Removed Start / End", "Filter 'You removed a Parkour Start/End.' messages", "^You removed a Parkour Start/End.$"));
        parkour.addEntry(new FilterEntry("startedParkour", "Started Parkour", "Filter 'Started parkour [name]!' messages", "^Started parkour .+!$"));
        parkour.addEntry(new FilterEntry("finishedParkour", "Finished Parkour", "Filter 'Finished parkour [name] in [time]!' messages", "^Finished parkour .+ in .+!$"));
        parkour.addEntry(new FilterEntry("canceledParkour", "Canceled Parkour", "Filter 'Cancelled parkour!' messages", "^Cancelled parkour!.*$"));
        parkour.addEntry(new FilterEntry("resetParkourTime", "Reset Time", "Filter 'Reset time for parkour [name]!' messages", "^Reset time for parkour .+!$"));
        parkour.addEntry(new FilterEntry("reachedCheckpoint", "Reached Checkpoint", "Filter 'Reached checkpoint #[x] for parkour [name]!' messages", "^Reached checkpoint #\\d for parkour .+!$"));
        parkour.addEntry(new FilterEntry("wrongChackpoint", "Wrong Checkpoint", "Filter 'Wrong checkpoint for parkour [name]!' messages", "^Wrong checkpoint for parkour .+!$"));
        island.addSubCategory(parkour);

        FilterCategory teleportPads = new FilterCategory("Teleport Pads", "Teleport pad messages");
        teleportPads.addEntry(new FilterEntry("placedTeleportPad", "Placed Pad", "Filter 'You placed down a [name]!' messages", "^You placed down a .+! RIGHT CLICK the Teleport Pad to change its settings!$"));
        teleportPads.addEntry(new FilterEntry("pickedUpTeleportPad", "Picked Up Pad", "Filter 'You have picked up the Teleport Pad!' messages", "^You have picked up the Teleport Pad!$"));
        teleportPads.addEntry(new FilterEntry("teleportPad", "Warped", "Filter 'Warped from X Teleport Pad to Y Teleport Pad!' messages", "^Warped from the .+ Teleport Pad to the .+ Teleport Pad!$"));
        teleportPads.addEntry(new FilterEntry("setTeleportDirection", "Set Direction", "Filter 'Set teleport direction to: [direction]' messages", "^Set teleport direction to: .+$"));
        teleportPads.addEntry(new FilterEntry("setTeleportDestination", "Set Destination", "Filter 'Set Teleport Pad destination to the [destination]!'", "^Set Teleport Pad destination to the .+!$"));
        teleportPads.addEntry(new FilterEntry("teleportPadNameChanged", "Name Changed", "Filter 'Teleport pad name changed!' messages", "^Teleport pad name changed!$"));
        teleportPads.addEntry(new FilterEntry("setTeleportPadIcon", "Set Icon", "Filter 'Set Teleport Pad icon to [icon]!' messages", "^Set Teleport Pad icon to .+!$"));
        teleportPads.addEntry(new FilterEntry("teleportPadNoDestination", "No Destination Set", "Filter 'This Teleport Pad does not have a destination set!'", "^This Teleport Pad does not have a destination set!$"));
        teleportPads.addEntry(new FilterEntry("invalidName", "Invalid Name", "Filter 'Invalid name' messages", "^Invalid name$"));
        island.addSubCategory(teleportPads);

        FilterCategory eggs = new FilterCategory("Island Eggs", "Island egg messages");
        eggs.addEntry(new FilterEntry("foundEgg", "Found Egg", "Filter 'You found an egg!' messages", "^You found an egg!$"));
        eggs.addEntry(new FilterEntry("eggsRemain", "Eggs Remain", "Filter '[x] eggs remain...' messages", "^\\d+ eggs remain\\.\\.\\.$"));
        eggs.addEntry(new FilterEntry("alreadyFoundEgg", "Already Found", "Filter 'You already found this egg!' messages", "^You already found this egg!$"));
        eggs.addEntry(new FilterEntry("foundAllEggs", "Found All Eggs", "Filter 'Wow, you found them all!' messages", "^Wow, you found them all!$"));
        island.addSubCategory(eggs);

        return island;
    }

    // Dungeons
    private static FilterCategory buildDungeons() {
        FilterCategory dungeons = new FilterCategory("Dungeons", "Dungeon related message filters (only active in The Catacombs)", "The Catacombs");

        dungeons.addEntry(new FilterEntry("dungeonNoEffects", "No Potion Effects", "Filter 'You are not allowed to use Potion Effects while in Dungeon' messages", "^You are not allowed to use Potion Effects while in Dungeon.*$", false, null));
        dungeons.addEntry(new FilterEntry("dungeonQueue", "Queuing Message", "Filter 'Queuing... (Attempt X/Y)' messages", "^Queuing\\.{3} \\(Attempt \\d+/\\d+\\)$", false, null));

        dungeons.addSubCategory(buildDungeonsClassSpecific());
        dungeons.addSubCategory(buildDungeonsAbilities());
        dungeons.addSubCategory(buildDungeonsLoot());
        dungeons.addSubCategory(buildDungeonsBuffs());
        dungeons.addSubCategory(buildDungeonsDoors());
        dungeons.addSubCategory(buildDungeonsNpcs());
        dungeons.addSubCategory(buildDungeonsEssence());
        dungeons.addSubCategory(buildDungeonsRevives());
        dungeons.addSubCategory(buildDungeonsReady());
        dungeons.addSubCategory(buildDungeonsRestrictions());
        dungeons.addSubCategory(buildDungeonsLevers());
        dungeons.addSubCategory(buildDungeonsMisc());

        return dungeons;
    }

    private static FilterCategory buildDungeonsClassSpecific() {
        FilterCategory c = new FilterCategory("Class Specific", "Class-related messages");
        c.addEntry(new FilterEntry("dungeonStatsDoubled", "Stats Doubled", "Filter 'Your stats are doubled because you are the only player using this class'", "^Your .+ stats are doubled because you are the only player using this class!$"));
        c.addEntry(new FilterEntry("dungeonClassStats", "Class Stats", "Filter class stat messages (e.g., [Berserk], [Archer], etc.)", "^\\[(Berserk|Archer|Tank|Mage|Healer)] .*$"));
        c.addEntry(new FilterEntry("dungeonClassMilestones", "Class Milestones", "Filter class milestone damage messages", "^(Berserk|Archer|Tank|Mage|Healer) Milestone .:.*$"));
        c.addEntry(new FilterEntry("dungeonBonePlating", "Bone Plating", "Filter 'Your bone plating reduced the damage' messages", "^Your bone plating reduced the damage you took by [\\d,]+(?:\\.\\d+)?!$"));
        c.addEntry(new FilterEntry("dungeonClassSelection", "Class Selection", "Filter class selection messages", "^(?:.+ (?:have )?selected the .+ (?:Dungeon )?Class!|You already have this class selected!)$"));
        return c;
    }

    private static FilterCategory buildDungeonsAbilities() {
        FilterCategory c = new FilterCategory("Abilities", "Ability related messages");
        c.addEntry(new FilterEntry("dungeonAbilityReady", "Ability Ready", "Filter 'ability is ready to use' messages", "^(?:.+ is ready to use! Press DROP to activate it!|.+ is now available!)$"));
        c.addEntry(new FilterEntry("dungeonAbilityUsed", "Ability Used", "Filter 'Used [ability]' messages", "^Used .+!$"));
        c.addEntry(new FilterEntry("dungeonAbilityCooldown", "Ability Cooldown", "Filter 'Your ability is currently on cooldown' messages", "^Your (Ultimate|Regular)(?: Ability)? is currently on cooldown for \\d+ more seconds\\.$"));
        return c;
    }

    private static FilterCategory buildDungeonsLoot() {
        FilterCategory c = new FilterCategory("Loot", "Loot related messages");
        c.addEntry(new FilterEntry("dungeonLoot", "Loot Drops", "Filter '[player] has obtained [item]' messages", "^.+ has obtained .+!$"));
        c.addEntry(new FilterEntry("dungeonRareDrop", "Rare Drop", "Filter rare drop messages in dungeons", "^RARE DROP! .*$"));
        c.addEntry(new FilterEntry("dungeonJournalFound", "Journal Found", "Filter 'You found a journal named' messages", "^You found a journal named .*!$"));
        c.addEntry(new FilterEntry("dungeonFairyKilled", "Fairy Killed", "Filter fairy dialogue and Revive Stone messages", "^.+ the Fairy: You killed me! Take this Revive Stone so that my death is not in vain!$"));
        return c;
    }

    private static FilterCategory buildDungeonsBuffs() {
        FilterCategory c = new FilterCategory("Buffs", "Buff related messages");
        c.addEntry(new FilterEntry("dungeonBuffPickUp", "Buff Pick Up", "Filter 'A Blessing of [type] was picked up' messages", "^A Blessing of .* was picked up!$"));
        c.addEntry(new FilterEntry("dungeonBuff", "Buff Announcement", "Filter 'DUNGEON BUFF!' messages", "^DUNGEON BUFF! .*$"));
        c.addEntry(new FilterEntry("dungeonBuffPerks", "Buff Perks", "Filter buff perk descriptions (e.g., 'Granted you', 'Also granted you')", "^(Granted you|Also granted you) .*$"));
        return c;
    }

    private static FilterCategory buildDungeonsDoors() {
        FilterCategory c = new FilterCategory("Doors", "Door related messages");
        c.addEntry(new FilterEntry("dungeonDoorRightClick", "Right Click Door", "Filter 'RIGHT CLICK on door to open it' messages", "^RIGHT CLICK on (the|a) (BLOOD|WITHER) (DOOR|door) to open it\\. This key can only be used to open 1 door!$"));
        c.addEntry(new FilterEntry("dungeonDoorNoKey", "No Key", "Filter 'You do not have the key for this door' messages", "^You do not have the key for this door!$"));
        c.addEntry(new FilterEntry("dungeonDoorShiver", "Shiver", "Filter 'A shiver runs down your spine...' messages", "^A shiver runs down your spine\\.{3}$"));
        c.addEntry(new FilterEntry("dungeonDoorWitherOpened", "Wither Door Opened", "Filter '[player] opened a WITHER door' messages", "^.* opened a WITHER door!$"));
        c.addEntry(new FilterEntry("dungeonDoorBloodOpened", "Blood Door Opened", "Filter 'The BLOOD DOOR has been opened' messages", "^The BLOOD DOOR has been opened!$"));
        c.addEntry(new FilterEntry("dungeonBloodKeyPickedUp", "Blood Key Picked Up", "Filter 'A Blood Key was picked up!' messages", "^A Blood Key was picked up!$"));
        return c;
    }

    private static FilterCategory buildDungeonsNpcs() {
        FilterCategory c = new FilterCategory("NPCs", "NPC and boss dialogue");
        c.addEntry(new FilterEntry("dungeonMort", "Mort", "Filter '[NPC] Mort:' dialogue", "^\\[NPC] Mort: .*$"));
        c.addEntry(new FilterEntry("dungeonOruo", "Oruo", "Filter '[STATUE] Oruo the Omniscient:' dialogue", "^\\[STATUE] Oruo the Omniscient: .*$"));
        c.addEntry(new FilterEntry("dungeonSkull", "Wither Skull", "Filter '[SKULL] Wither Skull:' messages", "^\\[SKULL] Wither Skull: .*$"));
        c.addEntry(new FilterEntry("dungeonThreeWeirdos", "Three Weirdos", "Filter the 3 weirdos' messages", "^\\[NPC] (Ardis|Baxter|Benson|Carver|Elmo|Eveleth|Hope|Hugo|Lino|Luverne|Madelia|Marshall|Melrose|Montgomery|Morris|Ramsey|Rose|Victoria|Virginia|Willmar|Winona): .*$"));

        FilterCategory bosses = new FilterCategory("Bosses", "Boss dialogue");
        bosses.addEntry(new FilterEntry("dungeonBossWatcher", "The Watcher", "Filter '[BOSS] The Watcher:' dialogue", "^\\[BOSS] The Watcher: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossBonzo", "Bonzo", "Filter '[BOSS] Bonzo:' dialogue", "^\\[BOSS] Bonzo: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossScarf", "Scarf", "Filter '[BOSS] Scarf:' dialogue", "^\\[BOSS] Scarf: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossProfessor", "The Professor", "Filter '[BOSS] The Professor:' dialogue", "^\\[BOSS] The Professor: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossThorn", "Thorn", "Filter '[BOSS] Thorn:' dialogue", "^\\[BOSS] Thorn: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossThornCrowd", "Thorn Crowd", "Filter 'Crowd:' dialogue in Thorn's arena", "^CROWD: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossLivid", "Livid", "Filter '[BOSS] Livid:' dialogue", "^\\[BOSS] .*Livid: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossLividEndermen", "Livid Endermen", "Filter Livid Enderman messages", "[BOSS] .* Enderman: .*"));
        bosses.addEntry(new FilterEntry("dungeonBossSadan", "Sadan", "Filter '[BOSS] Sadan:' dialogue", "^\\[BOSS] Sadan: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossMaxor", "Maxor", "Filter '[BOSS] Maxor:' dialogue", "^\\[BOSS] Maxor: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossStorm", "Storm", "Filter '[BOSS] Storm:' dialogue", "^\\[BOSS] Storm: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossGoldor", "Goldor", "Filter '[BOSS] Goldor:' dialogue", "^\\[BOSS] Goldor: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossNecron", "Necron", "Filter '[BOSS] Necron:' dialogue", "^\\[BOSS] Necron: .*$"));
        c.addSubCategory(bosses);

        return c;
    }

    private static FilterCategory buildDungeonsEssence() {
        FilterCategory c = new FilterCategory("Essence", "Essence related messages");
        c.addEntry(new FilterEntry("dungeonWitherEssence", "Wither Essence", "Filter '[player] found a Wither Essence' messages", "^.* found a Wither Essence! Everyone gains an extra essence!$"));
        c.addEntry(new FilterEntry("dungeonChestEssence", "Chest Essence", "Filter 'ESSENCE! [player] found x[x] [type] Essence' messages", "^ESSENCE! .* found x10 .* Essence!$"));
        c.addEntry(new FilterEntry("dungeonUnlockEssence", "Unlock Essence", "Filter '[player] unlocked [type] essence x[x]' messages", "^.* unlocked .* Essence x\\d!$"));
        return c;
    }

    private static FilterCategory buildDungeonsRevives() {
        FilterCategory c = new FilterCategory("Revives", "Revive related messages");
        c.addEntry(new FilterEntry("playerWasRevived", "Player Revived", "Filter '[player] was revived!' messages", "^.* was revived!$"));
        c.addEntry(new FilterEntry("playerRevivingPlayer", "Player Reviving Player", "Filter '[player] is reviving [player]!' messages", ".* is reviving .*!"));
        c.addEntry(new FilterEntry("revivedByWatcher", "Revived By Watcher", "Filter '[player] was revived by The Watcher!' messages", ".* was revived by The Watcher!"));
        c.addEntry(new FilterEntry("dungeonReviveSelf", "Revive Stone", "Filter 'Your Revive Stone revived you and broke!' messages", "^Your Revive Stone revived you and broke!$"));
        c.addEntry(new FilterEntry("dungeonNoPlayerRevive", "No Revivable Player", "Filter 'There are no players available to revive' messages", "^There are no players available to revive right now!$"));
        return c;
    }

    private static FilterCategory buildDungeonsReady() {
        FilterCategory c = new FilterCategory("Ready Notifications", "Ready up messages");
        c.addEntry(new FilterEntry("dungeonReadyUp", "Ready Up", "Filter '[player] is now/no longer ready' messages", "^.+ is (now|no longer) ready!$"));
        c.addEntry(new FilterEntry("dungeonAutoReadyToggle", "Auto Ready Toggle", "Filter 'You will now/no longer auto ready up' messages", "^You will (now|no longer) auto ready up when joining The Catacombs!$"));
        c.addEntry(new FilterEntry("dungeonAutoReadyNotice", "Auto Ready Notice", "Filter '/togglereadyup' instruction messages", "^Use '/togglereadyup' when the instance has started or the toggle in the queue menu to change this option whenever\\.$"));
        return c;
    }

    private static FilterCategory buildDungeonsRestrictions() {
        FilterCategory c = new FilterCategory("Restrictions", "Room restriction messages");
        c.addEntry(new FilterEntry("dungeonCantDig", "Can't Dig", "Filter 'A mystical force prevents you digging' messages", "^A mystical force prevents you digging (there|in this room|that block)!$"));
        c.addEntry(new FilterEntry("dungeonCantUseAbility", "Can't Use Ability", "Filter 'You cannot use abilities in this room' messages", "^You cannot use abilities in this room!$"));
        c.addEntry(new FilterEntry("dungeonCantDo", "Can't Do", "Filter 'You cannot do that in this room' messages", "^You cannot do that in this room!$"));
        c.addEntry(new FilterEntry("dungeonNotEnoughCharges", "Not Enough Charges", "Filter dungeon breaker out of charges messages", "^You don't have enough charges to break this block right now!$"));
        return c;
    }

    private static FilterCategory buildDungeonsLevers() {
        FilterCategory c = new FilterCategory("Levers", "Lever related messages");
        c.addEntry(new FilterEntry("dungeonLeverTrigger", "Lever Trigger", "Filter 'You hear the sound of something opening...' messages", "^You hear the sound of something opening\\.{3}$"));
        c.addEntry(new FilterEntry("dungeonLeverUsed", "Lever Used", "Filter 'This lever has already been used' messages", "^This lever has already been used\\.$"));
        return c;
    }

    private static FilterCategory buildDungeonsMisc() {
        FilterCategory c = new FilterCategory("Miscellaneous", "Miscellaneous dungeon messages");
        c.addEntry(new FilterEntry("dungeonInstanceClose", "Instance Close Warning", "Filter 'Warning! This instance will close in x seconds' messages", "^Warning! (?:The|This) instance will close in (\\d+)\\s*(?:s|seconds).*$"));
        c.addEntry(new FilterEntry("dungeonStarTimer", "Start Timer", "Filter 'Starting in X seconds' messages", "^Starting in \\d+ seconds\\.$"));
        c.addEntry(new FilterEntry("dungeonDamage", "Damage Taken", "Filter every message indicating you took damage", ".* (hit|hitting|struck) you for .* damage.*"));
        c.addEntry(new FilterEntry("dungeonCantLeap", "Can't Leap Yet", "Filter 'The dungeon hasn't started yet!' messages when leaping", "^The dungeon hasn't started yet!$"));
        c.addEntry(new FilterEntry("dungeonLeaped", "Leap", "Filter 'You have teleported to [player]!' messages", "^You have teleported to .*!$"));
        c.addEntry(new FilterEntry("dungeonInventoryFull", "Inventory Full", "Filter 'not enough space in your inventory' messages", "^You don't have enough space in your inventory to pick up this item!$"));
        c.addEntry(new FilterEntry("dungeonClaimOrb", "Claim Orb", "Filter 'You claimed Dungeon Orb!' messages", "^You claimed Dungeon Orb!$"));
        c.addEntry(new FilterEntry("dungeonPickedUpOrb", "Orb Pickup", "Filter orb pickup messages", "^(.* picked up your .* Orb!|You picked up a .* Orb .*)$"));
        c.addEntry(new FilterEntry("healerWish", "Healer Wish", "Filter healer Wish heal messages", "^.*'s Wish healed you for .+ health and granted you an absorption shield with .+ health!$"));
        c.addEntry(new FilterEntry("dungeonMuteSilenced", "Mute Silenced", "Filter 'Mute silenced you!' messages", "^Mute silenced you!$"));
        c.addEntry(new FilterEntry("dungeonUseJerry", "Jerry Deployed", "Filter 'Jerry deployed!' messages", "^Jerry deployed!$"));
        c.addEntry(new FilterEntry("dungeonUseDecoy", "Decoy Deployed", "Filter 'Decoy deployed!' messages", "^Decoy deployed!$"));
        c.addEntry(new FilterEntry("dungeonUseTrap", "Trap Placed", "Filter 'You placed a trap!' messages", "^You placed a trap!$"));
        c.addEntry(new FilterEntry("dungeonPuzzleSolved", "Puzzle Solved", "Filter 'PUZZLE SOLVED!' messages", "^PUZZLE SOLVED! .+$"));
        c.addEntry(new FilterEntry("dungeonPuzzleFailed", "Puzzle Failed", "Filter 'PUZZLE FAIL!' messages", "^PUZZLE FAIL! .+$"));
        c.addEntry(new FilterEntry("dungeonFrozenAdventurer", "Frozen Adventurer", "Filter 'The Frozen Adventurer used Ice Spray on you!' messages", "^The Frozen Adventurer used Ice Spray on you!$"));
        return c;
    }

    // Slayers
    private static FilterCategory buildSlayers() {
        FilterCategory c = new FilterCategory("Slayers", "Slayer related messages");
        c.addEntry(new FilterEntry("slayerQuestStarted", "Quest Started", "Filter quest started messages", "^SLAYER QUEST STARTED!$"));
        c.addEntry(new FilterEntry("slayerQuestComplete", "Quest Complete", "Filter quest complete messages", "^SLAYER QUEST COMPLETE!$"));
        c.addEntry(new FilterEntry("slayerQuestCancelled", "Quest Cancelled", "Filter quest cancelled messages", "^Your Slayer Quest has been cancelled!$"));
        c.addEntry(new FilterEntry("slayerLevelUpdate", "Level Update", "Filter Slayer 'Next LVL in' messages", "^.+ Slayer LVL \\d+ - Next LVL in [\\d.,]+ XP!$"));
        c.addEntry(new FilterEntry("slayCombatXP", "Slay Combat XP", "Filter combat XP requirements for slayers", "^» Slay [\\d.,]+ Combat XP worth of .*\\.$"));
        c.addEntry(new FilterEntry("slayerMiniBossSpawned", "Mini-Boss Spawned", "Filter mini-boss spawn messages", "^SLAYER MINI-BOSS .+ has spawned!$"));
        c.addEntry(new FilterEntry("autoSlayerCoinsTaken", "Auto-Slayer Coins", "Filter coins taken for auto-slayer", "^Took [\\w,.]+ coins from your bank for auto-slayer\\.{3}$"));
        c.addEntry(new FilterEntry("rngMeterUpdate", "RNG Meter Update", "Filter RNG meter XP messages", "^RNG Meter - [\\d.,]+ Stored XP$"));
        c.addEntry(new FilterEntry("cannotDamageEnemy", "Cannot Damage Enemy", "Filter messages when enemy can't be damaged", "^You need to kill .* to damage this!$"));
        c.addEntry(new FilterEntry("visitMaddox", "Visit Maddox", "Filter instructions to visit Maddox", "^Visit Maddox in the Hub's tavern to complete Slayer quests!$"));
        return c;
    }

    // Fishing
    private static FilterCategory buildFishing() {
        FilterCategory fishing = new FilterCategory("Fishing", "Fishing related messages");

        fishing.addSubCategory(buildFishingSeaCreatures());

        FilterCategory rarity = new FilterCategory("Catch Rarity", "Catch rarity messages");
        rarity.addEntry(new FilterEntry("fishingGoodCatch", "Good Catch", "Filter 'GOOD CATCH!' messages", "^. GOOD CATCH! .*$"));
        rarity.addEntry(new FilterEntry("fishingGreatCatch", "Great Catch", "Filter 'GREAT CATCH!' messages", "^. GREAT CATCH! .*$"));
        rarity.addEntry(new FilterEntry("fishingOutstandingCatch", "Outstanding Catch", "Filter 'OUTSTANDING CATCH!' messages", "^. OUTSTANDING CATCH! .*$"));
        fishing.addSubCategory(rarity);

        return fishing;
    }

    private static FilterCategory buildFishingSeaCreatures() {
        FilterCategory seaCreatures = new FilterCategory("Sea Creatures", "Sea creature spawn messages");

        FilterCategory normal = new FilterCategory("Normal", "Normal fishing creatures");
        normal.addEntry(new FilterEntry("fishingSeaArcher", "Sea Archer", "Filter 'You reeled in a Sea Archer' messages", "^You reeled in a Sea Archer\\.$"));
        normal.addEntry(new FilterEntry("fishingSquid", "Squid", "Filter 'A Squid appeared' messages", "^A Squid appeared\\.$"));
        normal.addEntry(new FilterEntry("fishingSeaWitchDisrupted", "Sea Witch", "Filter Sea Witch messages", "^It looks like you've disrupted the Sea Witch's brewing session\\. Watch out, she's furious!$"));
        normal.addEntry(new FilterEntry("fishingRiderOfTheDeep", "Rider of the Deep", "Filter 'The Rider of the Deep has emerged' messages", "^The Rider of the Deep has emerged\\.$"));
        normal.addEntry(new FilterEntry("fishingSeaGuardian", "Sea Guardian", "Filter 'You stumbled upon a Sea Guardian' messages", "^You stumbled upon a Sea Guardian\\.$"));
        normal.addEntry(new FilterEntry("fishingDeepSeaProtector", "Deep Sea Protector", "Filter Deep Sea Protector messages", "^You have awoken the Deep Sea Protector, prepare for a battle!$"));
        normal.addEntry(new FilterEntry("fishingSeaWalker", "Sea Walker", "Filter 'You caught a Sea Walker' messages", "^You caught a Sea Walker\\.$"));
        normal.addEntry(new FilterEntry("fishingCatfish", "Catfish", "Filter 'Huh? A Catfish!' messages", "^Huh\\? A Catfish!$"));
        normal.addEntry(new FilterEntry("fishingNightSquid", "Night Squid", "Filter Night Squid messages", "^Pitch Darkness reveals a Night Squid\\.$"));
        normal.addEntry(new FilterEntry("fishingCarrotKing", "Carrot King", "Filter Carrot King messages", "^Is this even a fish\\? It's the Carrot King!$"));
        normal.addEntry(new FilterEntry("fishingAgarimoo", "Agarimoo", "Filter Agarimoo messages", "^Your Chumcap Bucket trembles, it's an Agarimoo$"));
        normal.addEntry(new FilterEntry("fishingSeaLeech", "Sea Leech", "Filter 'Gross! A Sea Leech!' messages", "^Gross! A Sea Leech!$"));
        normal.addEntry(new FilterEntry("fishingGuardianDefender", "Guardian Defender", "Filter Guardian Defender messages", "^You've discovered a Guardian Defender of the sea$"));
        normal.addEntry(new FilterEntry("fishingWaterHydra", "Water Hydra", "Filter Water Hydra messages", "^The Water Hydra has come to test your Strength\\.$"));
        seaCreatures.addSubCategory(normal);

        FilterCategory oasis = new FilterCategory("Oasis", "Oasis fishing creatures");
        oasis.addEntry(new FilterEntry("fishingOasisRabbit", "Oasis Rabbit", "Filter Oasis Rabbit messages", "^An Oasis Rabbit appears from the water$"));
        oasis.addEntry(new FilterEntry("fishingOasisSheep", "Oasis Sheep", "Filter Oasis Sheep messages", "^An Oasis Sheep appears from the water$"));
        seaCreatures.addSubCategory(oasis);

        FilterCategory crystal = new FilterCategory("Crystal Hollows", "Crystal Hollows fishing creatures");
        crystal.addEntry(new FilterEntry("fishingWaterWorm", "Water Worm", "Filter Water Worm messages", "^A Water Worm surfaces!$"));
        crystal.addEntry(new FilterEntry("fishingPoisonedWaterWorm", "Poisoned Water Worm", "Filter Poisoned Water Worm messages", "^A Poisoned Water Worm surfaces!$"));
        crystal.addEntry(new FilterEntry("fishingAbyssalMiner", "Abyssal Miner", "Filter Abyssal Miner messages", "^An Abyssal Miner breaks out of the water!$"));
        seaCreatures.addSubCategory(crystal);

        FilterCategory spooky = new FilterCategory("Spooky", "Spooky fishing creatures");
        spooky.addEntry(new FilterEntry("fishingScarecrow", "Scarecrow", "Filter Scarecrow messages", "^Phew! It's only a scarecrow\\.$"));
        spooky.addEntry(new FilterEntry("fishingNightmare", "Nightmare", "Filter Nightmare messages", "^You hear trotting from beneath the waves, you caught a Nightmare$"));
        spooky.addEntry(new FilterEntry("fishingWerewolf", "Werewolf", "Filter Werewolf messages", "^It must be a full moon, it's a Werewolf!$"));
        spooky.addEntry(new FilterEntry("fishingPhantomFisher", "Phantom Fisher", "Filter Phantom Fisher messages", "^The spirit of a long lost Phantom Fisher has come to haunt you\\.$"));
        spooky.addEntry(new FilterEntry("fishingManifestationOfDeath", "Manifestation of Death", "Filter Manifestation of Death messages", "^This can't be! The manifestation of death himself!$"));
        seaCreatures.addSubCategory(spooky);

        FilterCategory winter = new FilterCategory("Winter", "Winter fishing creatures");
        winter.addEntry(new FilterEntry("fishingFrozenSteve", "Frozen Steve", "Filter Frozen Steve messages", "^Frozen Steve fell into the pond long ago, never to resurface\\.\\.\\. until now!$"));
        winter.addEntry(new FilterEntry("fishingSnowman", "Snowman", "Filter Snowman messages", "^Its a Snowman! It looks harmless\\.$"));
        winter.addEntry(new FilterEntry("fishingGrinch", "Grinch", "Filter Grinch messages", "^The Grinch stole Jerry's Gifts\\.\\.\\.get them back!$"));
        winter.addEntry(new FilterEntry("fishingNutcracker", "Nutcracker", "Filter Nutcracker messages", "^You found a forgotten Nutcracker laying beneath the ice\\.$"));
        winter.addEntry(new FilterEntry("fishingUnknownCreature", "Unknown Creature", "Filter Unknown Creature messages", "^What is this creature!\\?$"));
        winter.addEntry(new FilterEntry("fishingReindrake", "Reindrake", "Filter Reindrake messages", "^A Reindrake forms from the depths$"));
        seaCreatures.addSubCategory(winter);

        FilterCategory festival = new FilterCategory("Fishing Festival", "Fishing Festival sharks");
        festival.addEntry(new FilterEntry("fishingNurseShark", "Nurse Shark", "Filter Nurse Shark messages", "^A tiny fin emerges from the water, you've caught a Nurse Shark\\.\\.\\.$"));
        festival.addEntry(new FilterEntry("fishingBlueShark", "Blue Shark", "Filter Blue Shark messages", "^You spot a fin as blue as the water it came from, it's a Blue Shark\\.$"));
        festival.addEntry(new FilterEntry("fishingTigerShark", "Tiger Shark", "Filter Tiger Shark messages", "^A striped beast bounds from the depths, the wild Tiger Shark!$"));
        festival.addEntry(new FilterEntry("fishingGreatWhiteShark", "Great White Shark", "Filter Great White Shark messages", "^Hide no longer, a Great White Shark has tracked your scent and thirsts for your blood!$"));
        seaCreatures.addSubCategory(festival);

        FilterCategory bayou = new FilterCategory("Backwater Bayou", "Backwater Bayou fishing creatures");
        bayou.addEntry(new FilterEntry("fishingTrashGobbler", "Trash Gobbler", "Filter Trash Gobbler messages", "^The Trash Gobbler is hungry for you!$"));
        bayou.addEntry(new FilterEntry("fishingDumpsterDiver", "Dumpster Diver", "Filter Dumpster Diver messages", "^A Dumpster Diver has emerged from the swamp!$"));
        bayou.addEntry(new FilterEntry("fishingBanshee", "Banshee", "Filter Banshee messages", "^The desolate wail of a Banshee breaks the silence\\.$"));
        bayou.addEntry(new FilterEntry("fishingBayouSludge", "Bayou Sludge", "Filter Bayou Sludge messages", "^A swampy mass of slime emerges, the Bayou Sludge!$"));
        bayou.addEntry(new FilterEntry("fishingAlligator", "Alligator", "Filter Alligator messages", "^A long snout breaks the surface of the water\\. It's an Alligator!$"));
        bayou.addEntry(new FilterEntry("fishingTitanoboa", "Titanoboa", "Filter Titanoboa messages", "^A massive Titanoboa surfaces\\. It's body stretches as far as the eye can see\\.$"));
        seaCreatures.addSubCategory(bayou);

        FilterCategory galatea = new FilterCategory("Galatea", "Galatea fishing creatures");
        galatea.addEntry(new FilterEntry("fishingBogged", "Bogged", "Filter Bogged messages", "^You've hooked a Bogged!$"));
        galatea.addEntry(new FilterEntry("fishingWetwing", "Wetwing", "Filter Wetwing messages", "^Look! A Wetwing emerges!$"));
        galatea.addEntry(new FilterEntry("fishingTadgang", "Tadgang", "Filter Tadgang messages", "^A gang of Liltads! \\(Tadgang\\)$"));
        galatea.addEntry(new FilterEntry("fishingEnt", "Ent", "Filter Ent messages", "^You've hooked an Ent, as ancient as the forest itself\\.$"));
        galatea.addEntry(new FilterEntry("fishingLochEmperor", "Loch Emperor", "Filter Loch Emperor messages", "^The Loch Emperor arises from the depths\\.$"));
        seaCreatures.addSubCategory(galatea);

        return seaCreatures;
    }

    // Hunting
    private static FilterCategory buildHunting() {
        FilterCategory c = new FilterCategory("Hunting", "Hunting related messages");
        c.addEntry(new FilterEntry("huntingCharm", "Charm", "Filter 'CHARM You charmed a [mob] and captured its Shard.' messages", "^CHARM You charmed .* and captured its Shard\\.$"));
        c.addEntry(new FilterEntry("huntingSyphon", "Syphon", "Filter 'You used Syphon on [X] Shards!' messages", "^You used Syphon on \\d+ Shards!$"));
        c.addEntry(new FilterEntry("huntingAttributeLevel", "Attribute Level", "Filter attribute level up messages", "^+\\d+ .* Attribute \\(Level \\d+\\) \\- \\d+ more to upgrade!$"));
        c.addEntry(new FilterEntry("huntingNoShardToSyphon", "No Shard To Syphon", "Filter 'You don't have any Shards to Syphon!' messages", "^You don't have any Shards to Syphon!$"));
        return c;
    }

    // Mining
    private static FilterCategory buildMining() {
        FilterCategory c = new FilterCategory("Mining", "Mining related messages");
        c.addEntry(new FilterEntry("breakingPowerWarning", "Breaking Power Warning", "Filter 'You need a tool with a Breaking Power of [x] to mine [block]!' messages", "^You need a tool with a Breaking Power of \\d+ to mine .*! Speak to Fragilis by the entrance to the Crystal Hollows to learn more!$"));
        return c;
    }

    // Events
    private static FilterCategory buildEvents() {
        FilterCategory events = new FilterCategory("Event Specific", "Event specific messages");

        FilterCategory hoppity = new FilterCategory("Hoppity's Hunt", "Hoppity's Hunt messages");
        hoppity.addEntry(new FilterEntry("hoppityHuntBegun", "Hunt Begun", "Filter 'Hoppity's Hunt has begun!' messages", "^Hoppity's Hunt has begun! Help Hoppity find his Chocolate Rabbit Eggs across SkyBlock each day during the Spring!$"));
        hoppity.addEntry(new FilterEntry("eggAppeared", "Egg Appeared", "Filter 'HOPPITY'S HUNT A [egg type] Egg has appeared!' messages", "HOPPITY'S HUNT A .+ Egg has appeared!$"));
        hoppity.addEntry(new FilterEntry("foundEgg", "Egg Found", "Filter 'HOPPITY'S HUNT You found a [egg type] Egg' messages", "^HOPPITY'S HUNT You found a .+ Egg.*!$"));
        hoppity.addEntry(new FilterEntry("foundRabbit", "Rabbit Found", "Filter 'HOPPITY'S HUNT You found [rabbit]' messages", "HOPPITY'S HUNT You found .+ (.+)!$"));
        hoppity.addEntry(new FilterEntry("newRabbit", "New Rabbit", "Filter 'NEW RABBIT!' messages", "^NEW RABBIT! \\+.+ Chocolate and \\+.+x Chocolate per second!$"));
        hoppity.addEntry(new FilterEntry("duplicateRabbit", "Duplicate Rabbit", "Filter 'DUPLICATE RABBIT!' messages", "DUPLICATE RABBIT! \\+.+ Chocolate$"));
        events.addSubCategory(hoppity);

        return events;
    }

    // API
    public static List<FilterCategory> getRootCategories() {
        return ROOT_CATEGORIES;
    }

    public static List<FilterEntry> getAllEntries() {
        List<FilterEntry> allEntries = new ArrayList<>();
        collectEntries(ROOT_CATEGORIES, allEntries, null);
        return allEntries;
    }

    private static void collectEntries(List<FilterCategory> categories, List<FilterEntry> collector, String parentLocation) {
        for (FilterCategory category : categories) {
            String effectiveLocation = category.requiredLocation != null ? category.requiredLocation : parentLocation;

            for (FilterEntry entry : category.entries) {
                if (effectiveLocation != null && entry.requiredLocation == null) {
                    collector.add(new FilterEntry(entry.configKey, entry.displayName, entry.tooltip, entry.regex, entry.defaultValue, effectiveLocation));
                } else {
                    collector.add(entry);
                }
            }

            collectEntries(category.subCategories, collector, effectiveLocation);
        }
    }
}