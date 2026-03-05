package com.skyblockutils.features.chatFilters;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ChatFilterDefinitions {

    public static class FilterCategory {
        public final String name;
        public final String tooltip;
        public final String requiredLocation; // null means no location requirement
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
        public final String requiredLocation; // null means no location requirement

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
        // General category
        FilterCategory general = new FilterCategory("General", "General game messages");
        general.addEntry(new FilterEntry("emptyMessages", "Empty Messages", "Filter empty messages", "^\\s*$"));
        general.addEntry(new FilterEntry("welcomeToSkyblock", "Welcome to SkyBlock", "Filter 'Welcome to Hypixel SkyBlock!' messages", "^Welcome to Hypixel SkyBlock!$"));
        general.addEntry(new FilterEntry("lastUpdate", "Last Update", "Filter 'Latest update: SkyBlock...' messages", "^Latest update: SkyBlock .+ CLICK$"));
        general.addEntry(new FilterEntry("sendingToServer", "Sending to Server", "Filter 'Sending to server...' messages", "^Sending to server .+\\.{3}$"));
        general.addEntry(new FilterEntry("warpingMessage", "Warping Message", "Filter 'Warping...' and 'Warping you to your SkyBlock island...' messages", "^(Warping\\.{3}|Warping you to your SkyBlock island\\.{3})$"));
        general.addEntry(new FilterEntry("unclaimedEventRewards", "Unclaimed Event Rewards", "Filter unclaimed event rewards messages", "^(You have \\d+ unclaimed event rewards?!|>>> CLICK HERE to claim! <<<|Event rewards are deleted after \\d+ SkyBlock years!)$"));
        general.addEntry(new FilterEntry("playingOnProfile", "Playing on Profile", "Filter 'You are playing on profile:' messages", "^You are playing on profile: .+$"));
        general.addEntry(new FilterEntry("profileId", "Profile ID", "Filter 'Profile ID:' messages", "^Profile ID: .+$"));
        general.addEntry(new FilterEntry("killCombo", "Kill Combo", "Filter '+X Kill Combo' messages", "^\\+\\d+ Kill Combo.*$"));
        general.addEntry(new FilterEntry("killComboExpired", "Kill Combo Expired", "Filter 'Your Kill Combo has expired! You reached a X Kill Combo!' messages", "^Your Kill Combo has expired! You reached a \\d+ Kill Combo!$"));
        general.addEntry(new FilterEntry("coinsLostOnDeath", "Coins Lost on Death", "Filter 'You died and lost X coins!' messages", "^You died and lost [\\d.,]+ coins!$"));
        general.addEntry(new FilterEntry("lootShare", "Loot Share", "Filter 'LOOT SHARE You received loot for assisting' messages", "^LOOT SHARE You received loot for assisting .+!$"));
        general.addEntry(new FilterEntry("teleportPad", "Teleport Pad", "Filter 'Warped from X Teleport Pad to Y Teleport Pad!' messages", "^Warped from the .+ Teleport Pad to the .+ Teleport Pad!$"));
        general.addEntry(new FilterEntry("dungeonNoEffects", "No Potion Effects Message", "Filters the 'You are not allowed to use Potion Effects while in Dungeon' message", "^You are not allowed to use Potion Effects while in Dungeon.*$"));
        general.addEntry(new FilterEntry("dungeonQueue", "Dungeon Queuing Message", "Filter 'Queuing... (Attempt X/Y)' messages", "^Queuing\\.{3} \\(Attempt \\d+/\\d+\\)$"));
        // Bank subcategory
        FilterCategory bank = new FilterCategory("Bank", "Bank related messages");
        bank.addEntry(new FilterEntry("depositingCoins", "Depositing Coins", "Filter 'Depositing coins...' messages", "^Depositing coins\\.{3}$"));
        bank.addEntry(new FilterEntry("withdrawingCoins", "Withdrawing Coins", "Filter 'Withdrawing coins...' messages", "^Withdrawing coins\\.{3}$"));
        bank.addEntry(new FilterEntry("depositedCoins", "Deposited Coins", "Filter 'Deposited X coins! There's now Y coins in the account!' messages", "^Deposited .+ coins! There's now .+ coins in the account!$"));
        bank.addEntry(new FilterEntry("withdrewCoins", "Withdrew Coins", "Filter 'Withdrew X coins! There's now Y coins left in the account!' messages", "^Withdrew .+ coins! There's now .+ coins left in the account!$"));
        general.addSubCategory(bank);
        // Pets subcategory
        FilterCategory pets = new FilterCategory("Pets", "Pet related messages");
        pets.addEntry(new FilterEntry("petLevelUp", "Pet Level Up", "Filter 'Your [pet] leveled up to level X!' messages", "^Your .+ leveled up to level \\d+!$"));
        pets.addEntry(new FilterEntry("petSummoned", "Pet Summoned", "Filter 'You summoned your [pet]!' messages", "^You summoned your .+!$"));
        pets.addEntry(new FilterEntry("petDespawned", "Pet Despawned", "Filter 'You despawned your [pet]!' messages", "^You despawned your .+!$"));
        general.addSubCategory(pets);
        // Shops subcategory
        FilterCategory shops = new FilterCategory("Shops", "Filter Auction House / Bazaar / NPC Sell Messages");
        // Auction house subcategory
        FilterCategory auctionHouse = new FilterCategory("Auction House", "Filter Auction House related messages");
        auctionHouse.addEntry(new FilterEntry("puttingCoinsInEscrow", "Putting Coins In Escrow", "Filter 'Putting coins in escrow...' messages", "^Putting coins in escrow...$"));
        auctionHouse.addEntry(new FilterEntry("puttingItemInEscrow", "Putting Item In Escrow", "Filter 'Putting item in escrow...' messages", "^Putting item in escrow...$"));
        auctionHouse.addEntry(new FilterEntry("processingPurchase", "Processing Purchase", "Filter 'Processing purchase...' messages", "^Processing purchase...$"));
        auctionHouse.addEntry(new FilterEntry("settingUpAuction", "Setting Up Auction", "Filter 'Setting up the auction...' messages", "^Setting up the auction...$"));
        auctionHouse.addEntry(new FilterEntry("claimingBinAuction", "Claiming BIN Auction", "Filter 'Claiming BIN auction...' messages", "^Claiming BIN auction...$"));
        auctionHouse.addEntry(new FilterEntry("ahEscrowRefunded", "Escrow Refunded", "Filter 'Escrow refunded [x] coins for BIN Auction Buy!' messages", "^Escrow refunded .+ coins for BIN Auction Buy!$"));
        auctionHouse.addEntry(new FilterEntry("auctionStarted", "Auction Started", "Filter 'Auction started for [item]!' messages", "^(BIN )?Auction started for .*!$"));
        auctionHouse.addEntry(new FilterEntry("auctionPurchased", "You Purchased Item", "Filter 'You purchased [item] for [x] coins!' messages", "^You purchased .+ for .+ coins!$"));
        auctionHouse.addEntry(new FilterEntry("auctionClaimed", "Auction Claimed", "Filter 'You claimed [item] from [player]'s auction!' messages", "^You claimed .+ from .+'s auction!$"));
        shops.addSubCategory(auctionHouse);
        // Bazaar subcategory
        FilterCategory bazaar = new FilterCategory("Bazaar", "Filter Bazaar related messages");
        bazaar.addEntry(new FilterEntry("bazaarBought", "Bazaar Bought", "Filter '[Bazaar] Bought x[x] [item] for [amount] coins!' messages", "^\\[Bazaar] Bought .+x .+ for .+ coins!$"));
        bazaar.addEntry(new FilterEntry("bazaarSold", "Bazaar Sold", "Filter '[Bazaar] Sold [x]x [item] for [amount] coins!' messages", "^\\[Bazaar] Sold .+x .+ for .+ coins!$"));
        bazaar.addEntry(new FilterEntry("bazaarNoBuyer", "No Buyer", "Filter '[Bazaar] Couldn't find any buyers for [item]!' messages", "^\\[Bazaar] Couldn't find any buyers for .+!$"));
        bazaar.addEntry(new FilterEntry("executingInstantSell", "Executing Instant Sell", "Filter '[Bazaar] Executing instant sell...' messages", "^\\[Bazaar] Executing instant sell\\.{3}$"));
        bazaar.addEntry(new FilterEntry("bazaarEscrowRefunded", "Escrow Refunded", "Filter 'Escrow refunded [amount]x [item]!' messages", "^Escrow refunded .+x .+!$"));
        shops.addSubCategory(bazaar);
        // NPC Shop subcategory
        FilterCategory npcShop = new FilterCategory("NPC Shop", "Filter NPC sell related messages");
        npcShop.addEntry(new FilterEntry("itemSold", "Item Sold", "Filter 'You sold [item] [x]x for [amount] Coins!' messages", "^You sold .+ x.+ for .+ Coins!$"));
        npcShop.addEntry(new FilterEntry("itemCantBeSold", "Item Can't Be Sold", "Filter 'That item cannot be sold!' messages", "^That item cannot be sold!$"));
        shops.addSubCategory(npcShop);
        general.addSubCategory(shops);
        // Event specific subcategory
        // Island subcategory
        FilterCategory island = new FilterCategory("Island", "Island specific messages");
        // Parkour subcategory
        FilterCategory islandParkour = new FilterCategory("Parkour", "Island parkour messgaes");
        islandParkour.addEntry(new FilterEntry("placedParkourStartOrEnd", "Placed Parkour Start / End", "Filter 'You placed a Parkour Start/End.' messgaes", "^You placed a Parkour Start/End.$"));
        islandParkour.addEntry(new FilterEntry("removedParkourStartOrEnd", "Removed Parkour Start / End", "Filter 'You removed a Parkour Start/End.' messgaes", "^You removed a Parkour Start/End.$"));
        islandParkour.addEntry(new FilterEntry("startedParkour", "Started Parkour", "Filter 'Started parkour [name]!' messages", "^Started parkour .+!$"));
        islandParkour.addEntry(new FilterEntry("finishedParkour", "Finished Parkour", "Filter 'Finished parkour [name] in [time]!' messages", "^Finished parkour .+ in .+!$"));
        islandParkour.addEntry(new FilterEntry("canceledParkour", "Canceled Parkour", "Filter 'Cancelled parkour!' messages", "^Cancelled parkour!.*$"));
        islandParkour.addEntry(new FilterEntry("resetParkourTime", "Reset Parkour Time", "Filter 'Reset time for parkour [name]!' messages", "^Reset time for parkour .+!$"));
        islandParkour.addEntry(new FilterEntry("reachedCheckpoint", "Reached Checkpoint", "Filter 'Reached checkpoint #[checkpoint] for parkour [name]!' messages", "^Reached checkpoint #\\d for parkour .+!$"));
        islandParkour.addEntry(new FilterEntry("wrongChackpoint", "Wrong Checkpoint", "Filter 'Wrong checkpoint for parkour [name]!' messages", "^Wrong checkpoint for parkour .+!$"));
        island.addSubCategory(islandParkour);
        // Teleport pad subcategory
        FilterCategory teleportPad = new FilterCategory("Teleport Pads", "Teleport pad messages");
        teleportPad.addEntry(new FilterEntry("placedTeleportPad", "Placed Teleport Pad", "Filter 'You placed down a [name]! RIGHT CLICK the Teleport Pad to change its settings!' messages", "^You placed down a .+! RIGHT CLICK the Teleport Pad to change its settings!$"));
        teleportPad.addEntry(new FilterEntry("pickedUpTeleportPad", "Picked Up Teleport Pad", "Filter 'You have picked up the Teleport Pad!' messages", "^You have picked up the Teleport Pad!$"));
        teleportPad.addEntry(new FilterEntry("setTeleportDirection", "Set Teleport Direction", "Filter 'Set teleport direction to: [direction]' messages", "^Set teleport direction to: .+$"));
        teleportPad.addEntry(new FilterEntry("setTeleportDestination", "Set Teleport Destination", "Filter 'Set Teleport Pad destination to the [destination]!' messages", "^Set Teleport Pad destination to the .+!$"));
        teleportPad.addEntry(new FilterEntry("teleportPadNameChanged", "Teleport Pad Name Changed", "Filter 'Teleport pad name changed!' messages", "^Teleport pad name changed!$"));
        teleportPad.addEntry(new FilterEntry("setTeleportPadIcon", "Set Teleport Pad Icon", "Filter 'Set Teleport Pad icon to [icon]!' messages", "^Set Teleport Pad icon to .+!$"));
        teleportPad.addEntry(new FilterEntry("teleportPadNoDestination", "No Destination Set", "Filter 'This Teleport Pad does not have a destination set!' messages", "^This Teleport Pad does not have a destination set!$"));
        teleportPad.addEntry(new FilterEntry("invalidName", "Invalid Name", "Filter 'Invalid name' messages", "^Invalid name$"));
        island.addSubCategory(teleportPad);
        // Island eggs subcategory
        FilterCategory islandEggs = new FilterCategory("Island Eggs", "Island egg messages");
        islandEggs.addEntry(new FilterEntry("foundEgg", "Found Egg", "Filter 'You found an egg!' messages", "^You found an egg!$"));
        islandEggs.addEntry(new FilterEntry("eggsRemain", "Eggs Remain", "Filter '[x] eggs remain...' messages", "^\\d+ eggs remain\\.\\.\\.$"));
        islandEggs.addEntry(new FilterEntry("alreadyFoundEgg", "Already Found Egg", "Filter 'You already found this egg!' messages", "^You already found this egg!$"));
        islandEggs.addEntry(new FilterEntry("foundAllEggs", "Found All Eggs", "Filter 'Wow, you found them all!' messages", "^Wow, you found them all!$"));
        island.addSubCategory(islandEggs);
        general.addSubCategory(island);
        general.addEntry(new FilterEntry("deathMessages", "Death Messages", "Filter death messages", "^☠ .+$"));
        ROOT_CATEGORIES.add(general);

        // Dungeons category - ONLY active in The Catacombs
        FilterCategory dungeons = new FilterCategory("Dungeons", "Dungeon related message filters (only active in The Catacombs)", "The Catacombs");
        // Class Specific subcategory
        FilterCategory classSpecific = new FilterCategory("Class Specific", "Filter class-related messages");
        classSpecific.addEntry(new FilterEntry("dungeonStatsDoubled", "Stats Doubled", "Filter 'Your stats are doubled because you are the only player using this class'", "^Your .+ stats are doubled because you are the only player using this class!$"));
        classSpecific.addEntry(new FilterEntry("dungeonClassStats", "Class Stats", "Filter class stat messages (e.g., [Berserk], [Archer], etc.)", "^\\[(Berserk|Archer|Tank|Mage|Healer)] .*$"));
        classSpecific.addEntry(new FilterEntry("dungeonClassMilestones", "Class Milestones", "Filter class milestone damage messages", "^(Berserk|Archer|Tank|Mage|Healer) Milestone .:.*$"));
        classSpecific.addEntry(new FilterEntry("dungeonBonePlating", "Bone Plating", "Filter 'Your bone plating reduced the damage' messages", "^Your bone plating reduced the damage you took by [\\d,]+(?:\\.\\d+)?!$"));
        classSpecific.addEntry(new FilterEntry("dungeonClassSelection", "Class Selection", "Filter class selection messages", "^(?:.+ (?:have )?selected the .+ (?:Dungeon )?Class!|You already have this class selected!)$"));
        dungeons.addSubCategory(classSpecific);
        // Abilities subcategory
        FilterCategory abilities = new FilterCategory("Abilities", "Filter ability related messages");
        abilities.addEntry(new FilterEntry("dungeonAbilityReady", "Ability Ready", "Filter 'ability is ready to use' and 'is now available' messages", "^(?:.+ is ready to use! Press DROP to activate it!|.+ is now available!)$"));
        abilities.addEntry(new FilterEntry("dungeonAbilityUsed", "Ability Used", "Filter 'Used [ability]' messages", "^Used .+!$"));
        abilities.addEntry(new FilterEntry("dungeonAbilityCooldown", "Ability Cooldown", "Filter 'Your ability is currently on cooldown' messages", "^Your (Ultimate|Regular)(?: Ability)? is currently on cooldown for \\d+ more seconds\\.$"));
        dungeons.addSubCategory(abilities);
        // Loot subcategory
        FilterCategory loot = new FilterCategory("Loot", "Filter loot related messages");
        loot.addEntry(new FilterEntry("dungeonLoot", "Loot Drops", "Filter '[player] has obtained [item]' messages", "^.+ has obtained .+!$"));
        loot.addEntry(new FilterEntry("dungeonJournalFound", "Journal Found", "Filter 'You found a journal named' messages", "^You found a journal named .*!$"));
        loot.addEntry(new FilterEntry("dungeonFairyKilled", "Fairy Killed", "Filter fairy dialogue and Revive Stone messages", "^.+ the Fairy: You killed me! Take this Revive Stone so that my death is not in vain!$"));
        loot.addEntry(new FilterEntry("dungeonRareDrop", "Rare Drop", "Filter rare drop messages in dungeons", "^RARE DROP! .*$"));
        dungeons.addSubCategory(loot);
        // Buffs subcategory
        FilterCategory buffs = new FilterCategory("Buffs", "Filter buff related messages");
        buffs.addEntry(new FilterEntry("dungeonBuffPickUp", "Buff Pick Up", "Filter 'A Blessing of [type] was picked up' messages", "^A Blessing of .* was picked up!$"));
        buffs.addEntry(new FilterEntry("dungeonBuff", "Buff Announcement", "Filter 'DUNGEON BUFF!' messages", "^DUNGEON BUFF! .*$"));
        buffs.addEntry(new FilterEntry("dungeonBuffPerks", "Buff Perks", "Filter buff perk descriptions (e.g., 'Granted you', 'Also granted you')", "^(Granted you|Also granted you) .*$"));
        dungeons.addSubCategory(buffs);
        // Doors subcategory
        FilterCategory doors = new FilterCategory("Doors", "Filter door related messages");
        doors.addEntry(new FilterEntry("dungeonDoorRightClick", "Right Click Door", "Filter 'RIGHT CLICK on door to open it' messages", "^RIGHT CLICK on (the|a) (BLOOD|WITHER) (DOOR|door) to open it\\. This key can only be used to open 1 door!$"));
        doors.addEntry(new FilterEntry("dungeonDoorNoKey", "No Key", "Filter 'You do not have the key for this door' messages", "^You do not have the key for this door!$"));
        doors.addEntry(new FilterEntry("dungeonDoorShiver", "Shiver", "Filter 'A shiver runs down your spine...' messages", "^A shiver runs down your spine\\.{3}$"));
        doors.addEntry(new FilterEntry("dungeonDoorWitherOpened", "Wither Door Opened", "Filter '[player] opened a WITHER door' messages", "^.* opened a WITHER door!$"));
        doors.addEntry(new FilterEntry("dungeonDoorBloodOpened", "Blood Door Opened", "Filter 'The BLOOD DOOR has been opened' messages", "^The BLOOD DOOR has been opened!$"));
        doors.addEntry(new FilterEntry("dungeonBloodKeyPickedUp", "Blood Key Picked Up", "Filter 'A Blood Key was picked up!' messages", "^A Blood Key was picked up!$"));
        dungeons.addSubCategory(doors);
        // NPCs subcategory
        FilterCategory npcs = new FilterCategory("NPCs", "Filter NPC dialogue");
        npcs.addEntry(new FilterEntry("dungeonMort", "Mort", "Filter '[NPC] Mort:' dialogue", "^\\[NPC] Mort: .*$"));
        npcs.addEntry(new FilterEntry("dungeonOruo", "Oruo", "Filter '[STATUE] Oruo the Omniscient:' dialogue", "^\\[STATUE] Oruo the Omniscient: .*$"));
        npcs.addEntry(new FilterEntry("dungeonSkull", "Skull", "Filter '[SKULL] Wither Skull:' messages", "^\\[SKULL] Wither Skull: .*$"));
        npcs.addEntry(new FilterEntry("dungeonThreeWeirdos", "Three Weirdos", "Filter the 3 weirdos' messages", "^\\[NPC] (Ardis|Baxter|Benson|Carver|Elmo|Eveleth|Hope|Hugo|Lino|Luverne|Madelia|Marshall|Melrose|Montgomery|Morris|Ramsey|Rose|Victoria|Virginia|Willmar|Winona): .*$"));
        // Bosses sub-subcategory
        FilterCategory bosses = new FilterCategory("Bosses", "Filter boss dialogue");
        bosses.addEntry(new FilterEntry("dungeonBossWatcher", "The Watcher", "Filter '[BOSS] The Watcher:' dialogue", "^\\[BOSS] The Watcher: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossBonzo", "Bonzo", "Filter '[BOSS] Bonzo:' dialogue", "^\\[BOSS] Bonzo: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossScarf", "Scarf", "Filter '[BOSS] Scarf:' dialogue", "^\\[BOSS] Scarf: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossProfessor", "The Professor", "Filter '[BOSS] The Professor:' dialogue", "^\\[BOSS] The Professor: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossThorn", "Thorn", "Filter '[BOSS] Thorn:' dialogue", "^\\[BOSS] Thorn: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossThornCrowd", "Thorn Crowd", "Filter 'Crowd:' dialogue in Thorn's arena", "^CROWD: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossLivid", "Livid", "Filter '[BOSS] Livid:' dialogue", "^\\[BOSS] .*Livid: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossLividEndermen", "Livid Endermen", "Filter '[BOSS] Crossed Enderman: Wrong one!' messages", "[BOSS] .* Enderman: .*"));
        bosses.addEntry(new FilterEntry("dungeonBossSadan", "Sadan", "Filter '[BOSS] Sadan:' dialogue", "^\\[BOSS] Sadan: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossMaxor", "Maxor", "Filter '[BOSS] Maxor:' dialogue", "^\\[BOSS] Maxor: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossStorm", "Storm", "Filter '[BOSS] Storm:' dialogue", "^\\[BOSS] Storm: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossGoldor", "Goldor", "Filter '[BOSS] Goldor:' dialogue", "^\\[BOSS] Goldor: .*$"));
        bosses.addEntry(new FilterEntry("dungeonBossNecron", "Necron", "Filter '[BOSS] Necron:' dialogue", "^\\[BOSS] Necron: .*$"));
        npcs.addSubCategory(bosses);
        dungeons.addSubCategory(npcs);
        // Essence subcategory
        FilterCategory essence = new FilterCategory("Essence", "Filter essence related messages");
        essence.addEntry(new FilterEntry("dungeonWitherEssence", "Wither Essence", "Filter '[player] found a Wither Essence' messages", "^.* found a Wither Essence! Everyone gains an extra essence!$"));
        essence.addEntry(new FilterEntry("dungeonChestEssence", "Chest Essence", "Filter 'ESSENCE! [player] found x[x] [type] Essence' messages", "^ESSENCE! .* found x10 .* Essence!$"));
        essence.addEntry(new FilterEntry("dungeonUnlockEssence", "Unlock Essence", "Filter '[player] unlocked [type] essence x[x]' messages", "^.* unlocked .* Essence x\\d!$"));
        dungeons.addSubCategory(essence);
        // Ready subcategory
        FilterCategory ready = new FilterCategory("Ready Notifications", "Filter ready up messages");
        ready.addEntry(new FilterEntry("dungeonReadyUp", "Ready Up", "Filter '[player] is now/no longer ready' messages", "^.+ is (now|no longer) ready!$"));
        ready.addEntry(new FilterEntry("dungeonAutoReadyToggle", "Auto Ready Toggle", "Filter 'You will now/no longer auto ready up' messages", "^You will (now|no longer) auto ready up when joining The Catacombs!$"));
        ready.addEntry(new FilterEntry("dungeonAutoReadyNotice", "Auto Ready Notice", "Filter '/togglereadyup' instruction messages", "^Use '/togglereadyup' when the instance has started or the toggle in the queue menu to change this option whenever\\.$"));
        dungeons.addSubCategory(ready);
        // Room restrictions subcategory
        FilterCategory restrictions = new FilterCategory("Restrictions", "Filter restriction messages");
        restrictions.addEntry(new FilterEntry("dungeonCantDig", "Can't Dig", "Filter 'A mystical force prevents you digging' messages", "^A mystical force prevents you digging (there|in this room|that block)!$"));
        restrictions.addEntry(new FilterEntry("dungeonCantUseAbility", "Can't Use Ability", "Filter 'You cannot use abilities in this room' messages", "^You cannot use abilities in this room!$"));
        restrictions.addEntry(new FilterEntry("dungeonCantDo", "Can't Do", "Filter 'You cannot do that in this room' messages", "^You cannot do that in this room!$"));
        restrictions.addEntry(new FilterEntry("dungeonNotEnoughCharges", "Not Enough Charges", "Filter dungeon breaker out of charges messages", "^You don't have enough charges to break this block right now!$"));
        dungeons.addSubCategory(restrictions);
        // Levers subcategory
        FilterCategory levers = new FilterCategory("Levers", "Filter lever related messages");
        levers.addEntry(new FilterEntry("dungeonLeverTrigger", "Lever Trigger", "Filter 'You hear the sound of something opening...' messages", "^You hear the sound of something opening\\.{3}$"));
        levers.addEntry(new FilterEntry("dungeonLeverUsed", "Lever Used", "Filter 'This lever has already been used' messages", "^This lever has already been used\\.$"));
        dungeons.addSubCategory(levers);
        // Revives subcategory
        FilterCategory revives = new FilterCategory("Revives", "Filter revive related messages");
        revives.addEntry(new FilterEntry("playerWasRevived", "Player Was Revived", "Filter '❣ [player] was revived!' messages", "^.* was revived!$"));
        revives.addEntry(new FilterEntry("playerRevivingPlayer", "Player Reviving Player", "Filter '❣ [player] is reviving [player]!' messages", ".* is reviving .*!"));
        revives.addEntry(new FilterEntry("revivedByWatcher", "Player Revived By Watcher", "Filter '❣ [player] was revived by The Watcher!' messages", ".* was revived by The Watcher!"));
        dungeons.addSubCategory(revives);
        // Misc subcategory
        FilterCategory misc = new FilterCategory("Miscellaneous", "Filter miscellaneous dungeon messages");
        misc.addEntry(new FilterEntry("dungeonInstanceClose", "Instance Close Warnings", "Filters the 'Warning! This instance will close in x seconds' messages", "^Warning! (?:The|This) instance will close in (\\d+)\\s*(?:s|seconds).*$"));
        misc.addEntry(new FilterEntry("dungeonStarTimer", "Start Timer", "Filter 'Starting in X seconds' messages", "^Starting in \\d+ seconds\\.$"));
        misc.addEntry(new FilterEntry("dungeonNoPlayerRevive", "No Revivable Player", "Filter 'There are no players available to revive right now!' messages", "^There are no players available to revive right now!$"));
        misc.addEntry(new FilterEntry("dungeonCantLeap", "Dungeon Not Started Leap", "Filter 'The dungeon hasn't started yet!' messages when leaping", "^The dungeon hasn't started yet!$"));
        misc.addEntry(new FilterEntry("dungeonLeaped", "Leap message", "Filter 'You have teleported to [player]!' messages", "^You have teleported to .*!$"));
        misc.addEntry(new FilterEntry("dungeonReviveSelf", "Revive Yourself", "Filter 'Your Revive Stone revived you and broke!' messages", "^Your Revive Stone revived you and broke!$"));
        misc.addEntry(new FilterEntry("dungeonUseJerry", "Jerry Deployed", "Filter 'Jerry deployed!' messages", "^Jerry deployed!$"));
        misc.addEntry(new FilterEntry("dungeonUseDecoy", "Decoy Deployed", "Filter 'Decoy deployed!' messages", "^Decoy deployed!$"));
        misc.addEntry(new FilterEntry("dungeonUseTrap", "Trap Placed", "Filter 'You placed a trap!' messages", "^You placed a trap!$"));
        misc.addEntry(new FilterEntry("dungeonPuzzleSolved", "Puzzle Solved", "Filter 'PUZZLE SOLVED!' messages", "^PUZZLE SOLVED! .+$"));
        misc.addEntry(new FilterEntry("dungeonPuzzleFailed", "Puzzle Failed", "Filter 'PUZZLE FAIL!' messages", "^PUZZLE FAIL! .+$"));
        misc.addEntry(new FilterEntry("dungeonFrozenAdventurer", "Frozen Adventurer Ice Spray", "Filter 'The Frozen Adventurer used Ice Spray on you!' messages", "^The Frozen Adventurer used Ice Spray on you!$"));
        misc.addEntry(new FilterEntry("dungeonDamage", "Damage Taken", "Filter every message indicating you took damage", ".* (hit|hitting|struck) you for .* damage.*"));
        misc.addEntry(new FilterEntry("dungeonInventoryFull", "Inventory Full", "Filter 'not enough space in your inventory' messages", "^You don't have enough space in your inventory to pick up this item!$"));
        misc.addEntry(new FilterEntry("dungeonClaimOrb", "Claim Orb", "Filter 'You claimed Dungeon Orb!' messages", "^You claimed Dungeon Orb!$"));
        misc.addEntry(new FilterEntry("dungeonMuteSilenced", "Mute Silenced You", "Filter 'Mute silenced you!' messages", "^Mute silenced you!$"));
        misc.addEntry(new FilterEntry("dungeonPickedUpOrb", "Orb Pickup", "Filter messages indicating you or someone else picks up a stat orb", "^(.* picked up your .* Orb!|You picked up a .* Orb .*)$"));
        misc.addEntry(new FilterEntry("healerWish", "Healer Wish", "Filter '[player]'s Wish healed you for [x] health and granted you an absorption shield with [y] health!' messages", "^.*'s Wish healed you for .+ health and granted you an absorption shield with .+ health!$"));
        dungeons.addSubCategory(misc);
        ROOT_CATEGORIES.add(dungeons);

        // MINING CATEGORY
        FilterCategory mining = new FilterCategory("Mining", "Mining related messages");
        mining.addEntry(new FilterEntry("breakingPowerWarning", "Breaking Power Warning", "Filter 'You need a tool with a Breaking Power of [x] to mine [block]!' messages", "^You need a tool with a Breaking Power of \\d+ to mine .*! Speak to Fragilis by the entrance to the Crystal Hollows to learn more!$"));
        ROOT_CATEGORIES.add(mining);

        // SLAYERS CATEGORY
        FilterCategory slayers = new FilterCategory("Slayers", "Slayer related messages");
        slayers.addEntry(new FilterEntry("slayerQuestStarted", "Slayer Quest Started", "Filter quest started messages", "^SLAYER QUEST STARTED!$"));
        slayers.addEntry(new FilterEntry("slayerQuestComplete", "Slayer Quest Complete", "Filter quest complete messages", "^SLAYER QUEST COMPLETE!$"));
        slayers.addEntry(new FilterEntry("slayerQuestCancelled", "Slayer Quest Cancelled", "Filter quest cancelled messages", "^Your Slayer Quest has been cancelled!$"));
        slayers.addEntry(new FilterEntry("slayCombatXP", "Slay Combat XP", "Filter combat XP requirements for slayers", "^» Slay [\\d.,]+ Combat XP worth of .*\\.$"));
        slayers.addEntry(new FilterEntry("slayerMiniBossSpawned", "Slayer Mini-Boss Spawned", "Filter mini-boss spawn messages", "^SLAYER MINI-BOSS .+ has spawned!$"));
        slayers.addEntry(new FilterEntry("autoSlayerCoinsTaken", "Auto-Slayer Coins Taken", "Filter coins taken for auto-slayer", "^Took [\\w,.]+ coins from your bank for auto-slayer\\.{3}$"));
        slayers.addEntry(new FilterEntry("slayerLevelUpdate", "Slayer Level Update", "Filter Slayer 'Next LVL in' messages", "^.+ Slayer LVL \\d+ - Next LVL in [\\d.,]+ XP!$"));
        slayers.addEntry(new FilterEntry("rngMeterUpdate", "RNG Meter Update", "Filter RNG meter XP messages", "^RNG Meter - [\\d.,]+ Stored XP$"));
        slayers.addEntry(new FilterEntry("cannotDamageEnemy", "Cannot Damage Enemy", "Filter messages when enemy can't be damaged", "^You need to kill .* to damage this!$"));
        slayers.addEntry(new FilterEntry("visitMaddox", "Visit Maddox", "Filter instructions to visit Maddox", "^Visit Maddox in the Hub's tavern to complete Slayer quests!$"));
        ROOT_CATEGORIES.add(slayers);

        // FISHING CATEGORY
        FilterCategory fishing = new FilterCategory("Fishing", "Fishing related messages");
        // Sea Creatures subcategory
        FilterCategory seaCreatures = new FilterCategory("Sea Creatures", "Filter sea creature spawn messages");
        // Normal fishing
        FilterCategory fishingNormal = new FilterCategory("Normal", "Normal fishing creatures");
        fishingNormal.addEntry(new FilterEntry("fishingSeaArcher", "Sea Archer", "Filter 'You reeled in a Sea Archer' messages", "^You reeled in a Sea Archer\\.$"));
        fishingNormal.addEntry(new FilterEntry("fishingSquid", "Squid", "Filter 'A Squid appeared' messages", "^A Squid appeared\\.$"));
        fishingNormal.addEntry(new FilterEntry("fishingSeaWitchDisrupted", "Sea Witch Disrupted", "Filter Sea Witch messages", "^It looks like you've disrupted the Sea Witch's brewing session\\. Watch out, she's furious!$"));
        fishingNormal.addEntry(new FilterEntry("fishingRiderOfTheDeep", "Rider of the Deep", "Filter 'The Rider of the Deep has emerged' messages", "^The Rider of the Deep has emerged\\.$"));
        fishingNormal.addEntry(new FilterEntry("fishingSeaGuardian", "Sea Guardian", "Filter 'You stumbled upon a Sea Guardian' messages", "^You stumbled upon a Sea Guardian\\.$"));
        fishingNormal.addEntry(new FilterEntry("fishingDeepSeaProtector", "Deep Sea Protector", "Filter Deep Sea Protector messages", "^You have awoken the Deep Sea Protector, prepare for a battle!$"));
        fishingNormal.addEntry(new FilterEntry("fishingSeaWalker", "Sea Walker", "Filter 'You caught a Sea Walker' messages", "^You caught a Sea Walker\\.$"));
        fishingNormal.addEntry(new FilterEntry("fishingCatfish", "Catfish", "Filter 'Huh? A Catfish!' messages", "^Huh\\? A Catfish!$"));
        fishingNormal.addEntry(new FilterEntry("fishingNightSquid", "Night Squid", "Filter Night Squid messages", "^Pitch Darkness reveals a Night Squid\\.$"));
        fishingNormal.addEntry(new FilterEntry("fishingCarrotKing", "Carrot King", "Filter Carrot King messages", "^Is this even a fish\\? It's the Carrot King!$"));
        fishingNormal.addEntry(new FilterEntry("fishingAgarimoo", "Agarimoo", "Filter Agarimoo messages", "^Your Chumcap Bucket trembles, it's an Agarimoo$"));
        fishingNormal.addEntry(new FilterEntry("fishingSeaLeech", "Sea Leech", "Filter 'Gross! A Sea Leech!' messages", "^Gross! A Sea Leech!$"));
        fishingNormal.addEntry(new FilterEntry("fishingGuardianDefender", "Guardian Defender", "Filter Guardian Defender messages", "^You've discovered a Guardian Defender of the sea$"));
        fishingNormal.addEntry(new FilterEntry("fishingWaterHydra", "Water Hydra", "Filter Water Hydra messages", "^The Water Hydra has come to test your Strength\\.$"));
        seaCreatures.addSubCategory(fishingNormal);
        // Oasis fishing
        FilterCategory fishingOasis = new FilterCategory("Oasis", "Oasis fishing creatures");
        fishingOasis.addEntry(new FilterEntry("fishingOasisRabbit", "Oasis Rabbit", "Filter Oasis Rabbit messages", "^An Oasis Rabbit appears from the water$"));
        fishingOasis.addEntry(new FilterEntry("fishingOasisSheep", "Oasis Sheep", "Filter Oasis Sheep messages", "^An Oasis Sheep appears from the water$"));
        seaCreatures.addSubCategory(fishingOasis);
        // Crystal Hollows fishing
        FilterCategory fishingCrystal = new FilterCategory("Crystal Hollows", "Crystal Hollows fishing creatures");
        fishingCrystal.addEntry(new FilterEntry("fishingWaterWorm", "Water Worm", "Filter Water Worm messages", "^A Water Worm surfaces!$"));
        fishingCrystal.addEntry(new FilterEntry("fishingPoisonedWaterWorm", "Poisoned Water Worm", "Filter Poisoned Water Worm messages", "^A Poisoned Water Worm surfaces!$"));
        fishingCrystal.addEntry(new FilterEntry("fishingAbyssalMiner", "Abyssal Miner", "Filter Abyssal Miner messages", "^An Abyssal Miner breaks out of the water!$"));
        seaCreatures.addSubCategory(fishingCrystal);
        // Spooky fishing
        FilterCategory fishingSpooky = new FilterCategory("Spooky", "Spooky fishing creatures");
        fishingSpooky.addEntry(new FilterEntry("fishingScarecrow", "Scarecrow", "Filter Scarecrow messages", "^Phew! It's only a scarecrow\\.$"));
        fishingSpooky.addEntry(new FilterEntry("fishingNightmare", "Nightmare", "Filter Nightmare messages", "^You hear trotting from beneath the waves, you caught a Nightmare$"));
        fishingSpooky.addEntry(new FilterEntry("fishingWerewolf", "Werewolf", "Filter Werewolf messages", "^It must be a full moon, it's a Werewolf!$"));
        fishingSpooky.addEntry(new FilterEntry("fishingPhantomFisher", "Phantom Fisher", "Filter Phantom Fisher messages", "^The spirit of a long lost Phantom Fisher has come to haunt you\\.$"));
        fishingSpooky.addEntry(new FilterEntry("fishingManifestationOfDeath", "Manifestation of Death", "Filter Manifestation of Death messages", "^This can't be! The manifestation of death himself!$"));
        seaCreatures.addSubCategory(fishingSpooky);
        // Winter fishing
        FilterCategory fishingWinter = new FilterCategory("Winter", "Winter fishing creatures");
        fishingWinter.addEntry(new FilterEntry("fishingFrozenSteve", "Frozen Steve", "Filter Frozen Steve messages", "^Frozen Steve fell into the pond long ago, never to resurface\\.\\.\\. until now!$"));
        fishingWinter.addEntry(new FilterEntry("fishingSnowman", "Snowman", "Filter Snowman messages", "^Its a Snowman! It looks harmless\\.$"));
        fishingWinter.addEntry(new FilterEntry("fishingGrinch", "Grinch", "Filter Grinch messages", "^The Grinch stole Jerry's Gifts\\.\\.\\.get them back!$"));
        fishingWinter.addEntry(new FilterEntry("fishingNutcracker", "Nutcracker", "Filter Nutcracker messages", "^You found a forgotten Nutcracker laying beneath the ice\\.$"));
        fishingWinter.addEntry(new FilterEntry("fishingUnknownCreature", "Unknown Creature", "Filter Unknown Creature messages", "^What is this creature!\\?$"));
        fishingWinter.addEntry(new FilterEntry("fishingReindrake", "Reindrake", "Filter Reindrake messages", "^A Reindrake forms from the depths$"));
        seaCreatures.addSubCategory(fishingWinter);
        // Fishing Festival
        FilterCategory fishingFestival = new FilterCategory("Fishing Festival", "Fishing Festival sharks");
        fishingFestival.addEntry(new FilterEntry("fishingNurseShark", "Nurse Shark", "Filter Nurse Shark messages", "^A tiny fin emerges from the water, you've caught a Nurse Shark\\.\\.\\.$"));
        fishingFestival.addEntry(new FilterEntry("fishingBlueShark", "Blue Shark", "Filter Blue Shark messages", "^You spot a fin as blue as the water it came from, it's a Blue Shark\\.$"));
        fishingFestival.addEntry(new FilterEntry("fishingTigerShark", "Tiger Shark", "Filter Tiger Shark messages", "^A striped beast bounds from the depths, the wild Tiger Shark!$"));
        fishingFestival.addEntry(new FilterEntry("fishingGreatWhiteShark", "Great White Shark", "Filter Great White Shark messages", "^Hide no longer, a Great White Shark has tracked your scent and thirsts for your blood!$"));
        seaCreatures.addSubCategory(fishingFestival);
        // Backwater Bayou
        FilterCategory fishingBayou = new FilterCategory("Backwater Bayou", "Backwater Bayou fishing creatures");
        fishingBayou.addEntry(new FilterEntry("fishingTrashGobbler", "Trash Gobbler", "Filter Trash Gobbler messages", "^The Trash Gobbler is hungry for you!$"));
        fishingBayou.addEntry(new FilterEntry("fishingDumpsterDiver", "Dumpster Diver", "Filter Dumpster Diver messages", "^A Dumpster Diver has emerged from the swamp!$"));
        fishingBayou.addEntry(new FilterEntry("fishingBanshee", "Banshee", "Filter Banshee messages", "^The desolate wail of a Banshee breaks the silence\\.$"));
        fishingBayou.addEntry(new FilterEntry("fishingBayouSludge", "Bayou Sludge", "Filter Bayou Sludge messages", "^A swampy mass of slime emerges, the Bayou Sludge!$"));
        fishingBayou.addEntry(new FilterEntry("fishingAlligator", "Alligator", "Filter Alligator messages", "^A long snout breaks the surface of the water\\. It's an Alligator!$"));
        fishingBayou.addEntry(new FilterEntry("fishingTitanoboa", "Titanoboa", "Filter Titanoboa messages", "^A massive Titanoboa surfaces\\. It's body stretches as far as the eye can see\\.$"));
        seaCreatures.addSubCategory(fishingBayou);
        // Galatea
        FilterCategory fishingGalatea = new FilterCategory("Galatea", "Galatea fishing creatures");
        fishingGalatea.addEntry(new FilterEntry("fishingBogged", "Bogged", "Filter Bogged messages", "^You've hooked a Bogged!$"));
        fishingGalatea.addEntry(new FilterEntry("fishingWetwing", "Wetwing", "Filter Wetwing messages", "^Look! A Wetwing emerges!$"));
        fishingGalatea.addEntry(new FilterEntry("fishingTadgang", "Tadgang", "Filter Tadgang messages", "^A gang of Liltads! \\(Tadgang\\)$"));
        fishingGalatea.addEntry(new FilterEntry("fishingEnt", "Ent", "Filter Ent messages", "^You've hooked an Ent, as ancient as the forest itself\\.$"));
        fishingGalatea.addEntry(new FilterEntry("fishingLochEmperor", "Loch Emperor", "Filter Loch Emperor messages", "^The Loch Emperor arises from the depths\\.$"));
        seaCreatures.addSubCategory(fishingGalatea);
        fishing.addSubCategory(seaCreatures);
        // Rarity catches
        FilterCategory fishingRarity = new FilterCategory("Catch Rarity", "Filter catch rarity messages");
        fishingRarity.addEntry(new FilterEntry("fishingGoodCatch", "Good Catch", "Filter 'GOOD CATCH!' messages", "^. GOOD CATCH! .*$"));
        fishingRarity.addEntry(new FilterEntry("fishingGreatCatch", "Great Catch", "Filter 'GREAT CATCH!' messages", "^. GREAT CATCH! .*$"));
        fishingRarity.addEntry(new FilterEntry("fishingOutstandingCatch", "Outstanding Catch", "Filter 'OUTSTANDING CATCH!' messages", "^. OUTSTANDING CATCH! .*$"));
        fishing.addSubCategory(fishingRarity);
        ROOT_CATEGORIES.add(fishing);

        // Hunting
        FilterCategory hunting = new FilterCategory("Hunting", "Hunting related messages");
        hunting.addEntry(new FilterEntry("huntingCharm", "Charm", "Filter 'CHARM You charmed a [mob] and captured its Shard.' messages", "^CHARM You charmed .* and captured its Shard\\.$"));
        hunting.addEntry(new FilterEntry("huntingSyphon", "Syphon", "Filter 'You used Syphon on [X] Shards!' messages", "^You used Syphon on \\d+ Shards!$"));
        hunting.addEntry(new FilterEntry("huntingAttributeLevel", "Attribute Level", "Filter '+[X] [Attribute] Attribute (Level [Y]) - [Z] more to upgrade!' messages", "^+\\d+ .* Attribute \\(Level \\d+\\) \\- \\d+ more to upgrade!$"));
        hunting.addEntry(new FilterEntry("huntingNoShardToSyphon", "No Shard To Syphon", "Filter 'You don't have any Shards to Syphon!' messages", "^You don't have any Shards to Syphon!$"));
        ROOT_CATEGORIES.add(hunting);

        FilterCategory eventSpecific = new FilterCategory("Event Specific", "Filter messages per event");
        // Hoppity's hunt subcategory
        FilterCategory hoppityHunt = new FilterCategory("Hoppity's Hunt", "Filter Hoppity's Hunt messages");
        hoppityHunt.addEntry(new FilterEntry("hoppityHuntBegun", "Hoppity's Hunt Has Begun", "Filter 'Hoppity's Hunt has begun! Help Hoppity find his Chocolate Rabbit Eggs across SkyBlock each day during the Spring!' messages", "^Hoppity's Hunt has begun! Help Hoppity find his Chocolate Rabbit Eggs across SkyBlock each day during the Spring!$"));
        hoppityHunt.addEntry(new FilterEntry("eggAppeared", "Egg Appeared", "Filter '^HOPPITY'S HUNT A [egg type] Egg has appeared!' messages", "HOPPITY'S HUNT A .+ Egg has appeared!$"));
        hoppityHunt.addEntry(new FilterEntry("foundEgg", "You Found A Egg", "Filter '^HOPPITY'S HUNT You found a [egg type] Egg [location]]!' messages", "^HOPPITY'S HUNT You found a .+ Egg.*!$"));
        hoppityHunt.addEntry(new FilterEntry("foundRabbit", "You Found Rabbit", "Filter '^HOPPITY'S HUNT You found [rabbit] ([rarity])!' messages", "HOPPITY'S HUNT You found .+ (.+)!$"));
        hoppityHunt.addEntry(new FilterEntry("newRabbit", "New Rabbit", "Filter 'NEW RABBIT! +[x] Chocolate and +[y]x Chocolate per second!' messages", "^NEW RABBIT! \\+.+ Chocolate and \\+.+x Chocolate per second!$"));
        hoppityHunt.addEntry(new FilterEntry("duplicateRabbit", "Duplicate Rabbit", "Filter '^DUPLICATE RABBIT! +[amount] Chocolate' messages", "DUPLICATE RABBIT! \\+.+ Chocolate$"));
        eventSpecific.addSubCategory(hoppityHunt);
        ROOT_CATEGORIES.add(eventSpecific);
    }

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