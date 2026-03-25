package com.skyblockutils.features;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.utils.SSU;
import com.skyblockutils.utils.SideBarUtils;
import com.skyblockutils.utils.WaypointRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NpcFinder {
    public record Npc(String name, String location, BlockPos coordinates) {
    }

    public static final Map<String, Npc> allSkyblockNpcs = new HashMap<>();

    static {
// Hub
        allSkyblockNpcs.put("Adventurer|Hub", new Npc("Adventurer", "Hub", new BlockPos(-50, 70, -67)));
        allSkyblockNpcs.put("Alda|Hub", new Npc("Alda", "Hub", new BlockPos(71, 81, -60)));
        allSkyblockNpcs.put("Karis|Hub", new Npc("Karis", "Hub", new BlockPos(65, 82, -60)));
        allSkyblockNpcs.put("Hub Selector|Hub", new Npc("Hub Selector", "Hub", new BlockPos(-6, 70, -23)));
        allSkyblockNpcs.put("Alchemist|Hub", new Npc("Alchemist", "Hub", new BlockPos(80, 73, -91)));
        allSkyblockNpcs.put("Anita|Hub", new Npc("Anita", "Hub", new BlockPos(53, 74, -129)));
        allSkyblockNpcs.put("Amelia|Hub", new Npc("Amelia", "Hub", new BlockPos(-16, 75, -69)));
        allSkyblockNpcs.put("Builder|Hub", new Npc("Builder", "Hub", new BlockPos(-9, 72, -62)));
        allSkyblockNpcs.put("Wool Weaver|Hub", new Npc("Wool Weaver", "Hub", new BlockPos(-18, 72, -58)));
        allSkyblockNpcs.put("Fishing Merchant|Hub", new Npc("Fishing Merchant", "Hub", new BlockPos(112, 72, -45)));
        allSkyblockNpcs.put("Jacob|Hub", new Npc("Jacob", "Hub", new BlockPos(46, 74, -129)));
        allSkyblockNpcs.put("Lumber Merchant|Hub", new Npc("Lumber Merchant", "Hub", new BlockPos(-126, 74, -43)));
        allSkyblockNpcs.put("Mine Merchant|Hub", new Npc("Mine Merchant", "Hub", new BlockPos(14, 64, -115)));
        allSkyblockNpcs.put("Weaponsmith|Hub", new Npc("Weaponsmith", "Hub", new BlockPos(-51, 70, -86)));
        allSkyblockNpcs.put("Rosetta|Hub", new Npc("Rosetta", "Hub", new BlockPos(-54, 70, -89)));
        allSkyblockNpcs.put("Librarian|Hub", new Npc("Librarian", "Hub", new BlockPos(-69, 71, -80)));
        allSkyblockNpcs.put("Guy|Hub", new Npc("Guy", "Hub", new BlockPos(51, 79, 20)));
        allSkyblockNpcs.put("Dusk|Hub", new Npc("Dusk", "Hub", new BlockPos(20, 64, -136)));
        allSkyblockNpcs.put("Carpenter|Hub", new Npc("Carpenter", "Hub", new BlockPos(-138, 75, -42)));
        allSkyblockNpcs.put("Plumber Joe|Hub", new Npc("Plumber Joe", "Hub", new BlockPos(123, 75, -39)));
        allSkyblockNpcs.put("Banker|Hub", new Npc("Banker", "Hub", new BlockPos(-30, 73, -39)));
        allSkyblockNpcs.put("Scoop|Hub", new Npc("Scoop", "Hub", new BlockPos(4, 181, 56)));
        allSkyblockNpcs.put("Curator|Hub", new Npc("Curator", "Hub", new BlockPos(27, 69, 33)));
        allSkyblockNpcs.put("Madame Eleanor Q. Goldsworth III|Hub", new Npc("Madame Eleanor Q. Goldsworth III", "Hub", new BlockPos(33, 74, 11)));
        allSkyblockNpcs.put("Seraphine|Hub", new Npc("Seraphine", "Hub", new BlockPos(-2, 80, 10)));
        allSkyblockNpcs.put("Elizabeth|Hub", new Npc("Elizabeth", "Hub", new BlockPos(-7, 80, 19)));
        allSkyblockNpcs.put("Auction Master|Hub", new Npc("Auction Master", "Hub", new BlockPos(-40, 74, -13)));
        allSkyblockNpcs.put("Bazaar|Hub", new Npc("Bazaar", "Hub", new BlockPos(-34, 74, -23)));
        allSkyblockNpcs.put("Bazaar Agent|Hub", new Npc("Bazaar Agent", "Hub", new BlockPos(-36, 74, -32)));
        allSkyblockNpcs.put("Bea|Hub", new Npc("Bea", "Hub", new BlockPos(11, 73, -59)));
        allSkyblockNpcs.put("Kat|Hub", new Npc("Kat", "Hub", new BlockPos(10, 73, -54)));
        allSkyblockNpcs.put("George|Hub", new Npc("George", "Hub", new BlockPos(21, 75, -60)));
        allSkyblockNpcs.put("Zog|Hub", new Npc("Zog", "Hub", new BlockPos(10, 70, -72)));
        allSkyblockNpcs.put("Blacksmith|Hub", new Npc("Blacksmith", "Hub", new BlockPos(10, 64, -127)));
        allSkyblockNpcs.put("Marco|Hub", new Npc("Marco", "Hub", new BlockPos(90, 77, 3)));
        allSkyblockNpcs.put("Bartender|Hub", new Npc("Bartender", "Hub", new BlockPos(-84, 75, -136)));
        allSkyblockNpcs.put("Maddox the Slayer|Hub", new Npc("Maddox the Slayer", "Hub", new BlockPos(-84, 69, -130)));
        allSkyblockNpcs.put("Taylor|Hub", new Npc("Taylor", "Hub", new BlockPos(37, 75, -40)));
        allSkyblockNpcs.put("Seymour|Hub", new Npc("Seymour", "Hub", new BlockPos(28, 67, -41)));
        allSkyblockNpcs.put("Arthur|Hub", new Npc("Arthur", "Hub", new BlockPos(53, 73, -112)));
        allSkyblockNpcs.put("Farmer Rigby|Hub", new Npc("Farmer Rigby", "Hub", new BlockPos(61, 73, -148)));
        allSkyblockNpcs.put("Pat|Hub", new Npc("Pat", "Hub", new BlockPos(-88, 74, -95)));
        allSkyblockNpcs.put("Lumber Jack|Hub", new Npc("Lumber Jack", "Hub", new BlockPos(-124, 74, -30)));
        allSkyblockNpcs.put("Fisherman Gerald|Hub", new Npc("Fisherman Gerald", "Hub", new BlockPos(118, 71, -33)));
        allSkyblockNpcs.put("Tia the Fairy|Hub", new Npc("Tia the Fairy", "Hub", new BlockPos(119, 66, 147)));
        allSkyblockNpcs.put("Lucius|Hub", new Npc("Lucius", "Hub", new BlockPos(125, 74, 164)));
        allSkyblockNpcs.put("Shifty|Hub", new Npc("Shifty", "Hub", new BlockPos(114, 74, 175)));
        allSkyblockNpcs.put("Sirius|Hub", new Npc("Sirius", "Hub", new BlockPos(91, 76, 176)));
        allSkyblockNpcs.put("Wizard|Hub", new Npc("Wizard", "Hub", new BlockPos(48, 120, 99)));
        allSkyblockNpcs.put("Elise|Hub", new Npc("Elise", "Hub", new BlockPos(44, 78, 100)));
        allSkyblockNpcs.put("Nicole|Hub", new Npc("Nicole", "Hub", new BlockPos(41, 94, 96)));
        allSkyblockNpcs.put("Erihann|Hub", new Npc("Erihann", "Hub", new BlockPos(42, 97, 93)));
        allSkyblockNpcs.put("Lonely Philosopher|Hub", new Npc("Lonely Philosopher", "Hub", new BlockPos(-251, 131, 41)));
        allSkyblockNpcs.put("Security Sloth|Hub", new Npc("Security Sloth", "Hub", new BlockPos(10, 72, -16)));
        allSkyblockNpcs.put("Salesman|Hub", new Npc("Salesman", "Hub", new BlockPos(-10, 72, -16)));
        allSkyblockNpcs.put("Jacobus|Hub", new Npc("Jacobus", "Hub", new BlockPos(-50, 71, -61)));
        allSkyblockNpcs.put("Ozanne|Hub", new Npc("Ozanne", "Hub", new BlockPos(-55, 71, -66)));
        allSkyblockNpcs.put("Maxwell|Hub", new Npc("Maxwell", "Hub", new BlockPos(-67, 71, -67)));
        allSkyblockNpcs.put("Maths Enjoyer|Hub", new Npc("Maths Enjoyer", "Hub", new BlockPos(-73, 71, -63)));
        allSkyblockNpcs.put("Mad Redstone Engineer|Hub", new Npc("Mad Redstone Engineer", "Hub", new BlockPos(-9, 72, -56)));
        allSkyblockNpcs.put("Christopher|Hub", new Npc("Christopher", "Hub", new BlockPos(-15, 76, -76)));
        allSkyblockNpcs.put("Fann|Hub", new Npc("Fann", "Hub", new BlockPos(9, 74, -63)));
        allSkyblockNpcs.put("Fisherwoman Enid|Hub", new Npc("Fisherwoman Enid", "Hub", new BlockPos(41, 71, -23)));
        allSkyblockNpcs.put("Gwynnie|Hub", new Npc("Gwynnie", "Hub", new BlockPos(116, 72, -26)));
        allSkyblockNpcs.put("Gavin|Hub", new Npc("Gavin", "Hub", new BlockPos(147, 71, -60)));
        allSkyblockNpcs.put("Captain Baha|Hub", new Npc("Captain Baha", "Hub", new BlockPos(162, 70, -66)));
        allSkyblockNpcs.put("Angler Angus|Hub", new Npc("Angler Angus", "Hub", new BlockPos(125, 71, -68)));
        allSkyblockNpcs.put("Farm Merchant|Hub", new Npc("Farm Merchant", "Hub", new BlockPos(63, 73, -114)));
        allSkyblockNpcs.put("Smithmonger|Hub", new Npc("Smithmonger", "Hub", new BlockPos(15, 64, -136)));
        allSkyblockNpcs.put("Jax|Hub", new Npc("Jax", "Hub", new BlockPos(-41, 70, -93)));
        allSkyblockNpcs.put("The Handler|Hub", new Npc("The Handler", "Hub", new BlockPos(40, 73, 1)));
        allSkyblockNpcs.put("Vincent|Hub", new Npc("Vincent", "Hub", new BlockPos(79, 75, 53)));
        allSkyblockNpcs.put("Biblio|Hub", new Npc("Biblio", "Hub", new BlockPos(8, 80, 10)));
        allSkyblockNpcs.put("Baker|Hub", new Npc("Baker", "Hub", new BlockPos(8, 71, -95))); //TODO
        allSkyblockNpcs.put("Bingo|Hub", new Npc("Bingo", "Hub", new BlockPos(2, 70, -92))); //TODO
        allSkyblockNpcs.put("Alixer|Hub", new Npc("Alixer", "Hub", new BlockPos(0, 70, -93))); //TODO
        allSkyblockNpcs.put("Oringo|Hub", new Npc("Oringo", "Hub", new BlockPos(-3, 70, -43))); //TODO
        allSkyblockNpcs.put("Fear Mongerer|Hub", new Npc("Fear Mongerer", "Hub", new BlockPos(-2, 70, -43))); //TODO
        allSkyblockNpcs.put("Tyashoi Alchemist|Hub", new Npc("Tyashoi Alchemist", "Hub", new BlockPos(41, 68, -55))); //TODO
        allSkyblockNpcs.put("Hoppity|Hub", new Npc("Hoppity", "Hub", new BlockPos(-9, 72, -56))); //TODO

// The Barn
        allSkyblockNpcs.put("Farmhand|The Barn", new Npc("Farmhand", "The Barn", new BlockPos(144, 74, -241)));
        allSkyblockNpcs.put("Windmill Operator|The Barn", new Npc("Windmill Operator", "The Barn", new BlockPos(98, 90, -283)));

// Mushroom Desert
        allSkyblockNpcs.put("Beth|Mushroom Desert", new Npc("Beth", "Mushroom Desert", new BlockPos(163, 78, -359)));
        allSkyblockNpcs.put("Mason|Mushroom Desert", new Npc("Mason", "Mushroom Desert", new BlockPos(177, 78, -356)));
        allSkyblockNpcs.put("Friendly Hiker|Mushroom Desert", new Npc("Friendly Hiker", "Mushroom Desert", new BlockPos(181, 77, -381)));
        allSkyblockNpcs.put("Hungry Hiker|Mushroom Desert", new Npc("Hungry Hiker", "Mushroom Desert", new BlockPos(269, 49, -481)));
        allSkyblockNpcs.put("Treasure Hunter|Mushroom Desert", new Npc("Treasure Hunter", "Mushroom Desert", new BlockPos(200, 93, -438)));
        allSkyblockNpcs.put("Shepherd|Mushroom Desert", new Npc("Shepherd", "Mushroom Desert", new BlockPos(388, 86, -373)));
        allSkyblockNpcs.put("Jake|Mushroom Desert", new Npc("Jake", "Mushroom Desert", new BlockPos(261, 185, -566)));
        allSkyblockNpcs.put("Hunter Ava|Mushroom Desert", new Npc("Hunter Ava", "Mushroom Desert", new BlockPos(319, 103, -471)));
        allSkyblockNpcs.put("Tammy|Mushroom Desert", new Npc("Tammy", "Mushroom Desert", new BlockPos(284, 105, -545)));
        allSkyblockNpcs.put("Tony|Mushroom Desert", new Npc("Tony", "Mushroom Desert", new BlockPos(278, 105, -545)));
        allSkyblockNpcs.put("Trevor|Mushroom Desert", new Npc("Trevor", "Mushroom Desert", new BlockPos(281, 105, -543)));
        allSkyblockNpcs.put("Talbot|Mushroom Desert", new Npc("Talbot", "Mushroom Desert", new BlockPos(285, 105, -549)));
        allSkyblockNpcs.put("Farmer Jon|Mushroom Desert", new Npc("Farmer Jon", "Mushroom Desert", new BlockPos(167, 92, -599)));
        allSkyblockNpcs.put("Moby|Mushroom Desert", new Npc("Moby", "Mushroom Desert", new BlockPos(205, 44, -500)));

// Gold Mine
        allSkyblockNpcs.put("Gold Forger|Gold Mine", new Npc("Gold Forger", "Gold Mine", new BlockPos(-28, 75, -295)));
        allSkyblockNpcs.put("Iron Forger|Gold Mine", new Npc("Iron Forger", "Gold Mine", new BlockPos(-2, 76, -308)));
        allSkyblockNpcs.put("Rusty|Gold Mine", new Npc("Rusty", "Gold Mine", new BlockPos(-21, 79, -326)));
        allSkyblockNpcs.put("Lazy Miner|Gold Mine", new Npc("Lazy Miner", "Gold Mine", new BlockPos(-12, 79, -338)));
        allSkyblockNpcs.put("Blacksmith|Gold Mine", new Npc("Blacksmith", "Gold Mine", new BlockPos(-40, 78, -300)));

// Deep Caverns
        allSkyblockNpcs.put("Walter|Deep Caverns", new Npc("Walter", "Deep Caverns", new BlockPos(18, 157, -37)));
        allSkyblockNpcs.put("Lapis Miner|Deep Caverns", new Npc("Lapis Miner", "Deep Caverns", new BlockPos(-11, 121, 35)));
        allSkyblockNpcs.put("Redstone Miner|Deep Caverns", new Npc("Redstone Miner", "Deep Caverns", new BlockPos(24, 105, 16)));
        allSkyblockNpcs.put("Rhys|Deep Caverns", new Npc("Rhys", "Deep Caverns", new BlockPos(31, 13, 14)));
        allSkyblockNpcs.put("Lift Operator|Deep Caverns", new Npc("Lift Operator", "Deep Caverns", new BlockPos(45, 151, 15)));

// Dwarven Mines
        allSkyblockNpcs.put("Rhys|Dwarven Mines", new Npc("Rhys", "Dwarven Mines", new BlockPos(-38, 201, -119)));
        allSkyblockNpcs.put("Bulvar|Dwarven Mines", new Npc("Bulvar", "Dwarven Mines", new BlockPos(-16, 202, -99)));
        allSkyblockNpcs.put("Bubu|Dwarven Mines", new Npc("Bubu", "Dwarven Mines", new BlockPos(-11, 202, -104)));
        allSkyblockNpcs.put("Blacksmith|Dwarven Mines", new Npc("Blacksmith", "Dwarven Mines", new BlockPos(-7, 202, -155)));
        allSkyblockNpcs.put("Old Man Garry|Dwarven Mines", new Npc("Old Man Garry", "Dwarven Mines", new BlockPos(5, 201, -110)));
        allSkyblockNpcs.put("Banker Broadjaw|Dwarven Mines", new Npc("Banker Broadjaw", "Dwarven Mines", new BlockPos(13, 202, -149)));
        allSkyblockNpcs.put("Bednom|Dwarven Mines", new Npc("Bednom", "Dwarven Mines", new BlockPos(-31, 215, -90)));
        allSkyblockNpcs.put("Bomin|Dwarven Mines", new Npc("Bomin", "Dwarven Mines", new BlockPos(26, 204, -145)));
        allSkyblockNpcs.put("Tarwen|Dwarven Mines", new Npc("Tarwen", "Dwarven Mines", new BlockPos(33, 203, -140)));
        allSkyblockNpcs.put("Hornum|Dwarven Mines", new Npc("Hornum", "Dwarven Mines", new BlockPos(35, 203, -149)));
        allSkyblockNpcs.put("Gimley|Dwarven Mines", new Npc("Gimley", "Dwarven Mines", new BlockPos(29, 203, -152)));
        allSkyblockNpcs.put("Sargwyn|Dwarven Mines", new Npc("Sargwyn", "Dwarven Mines", new BlockPos(37, 203, -151)));
        allSkyblockNpcs.put("Brynmor|Dwarven Mines", new Npc("Brynmor", "Dwarven Mines", new BlockPos(31, 203, -155)));
        allSkyblockNpcs.put("Kings|Dwarven Mines", new Npc("Kings", "Dwarven Mines", new BlockPos(129, 197, 196)));
        allSkyblockNpcs.put("Tornora|Dwarven Mines", new Npc("Tornora", "Dwarven Mines", new BlockPos(136, 197, 167)));
        allSkyblockNpcs.put("Queen Mismyla|Dwarven Mines", new Npc("Queen Mismyla", "Dwarven Mines", new BlockPos(126, 196, 195)));
        allSkyblockNpcs.put("Royal Resident|Dwarven Mines", new Npc("Royal Resident", "Dwarven Mines", new BlockPos(62, 205, 200)));
        allSkyblockNpcs.put("Fetchur|Dwarven Mines", new Npc("Fetchur", "Dwarven Mines", new BlockPos(84, 225, -119)));
        allSkyblockNpcs.put("Gwendolyn|Dwarven Mines", new Npc("Gwendolyn", "Dwarven Mines", new BlockPos(88, 199, -99)));
        allSkyblockNpcs.put("Geo|Dwarven Mines", new Npc("Geo", "Dwarven Mines", new BlockPos(87, 200, -116)));
        allSkyblockNpcs.put("Lumina|Dwarven Mines", new Npc("Lumina", "Dwarven Mines", new BlockPos(77, 200, -111)));
        allSkyblockNpcs.put("Fragilis|Dwarven Mines", new Npc("Fragilis", "Dwarven Mines", new BlockPos(88, 200, -109)));
        allSkyblockNpcs.put("Station Master|Dwarven Mines", new Npc("Station Master", "Dwarven Mines", new BlockPos(38, 202, -86)));
        allSkyblockNpcs.put("Ticket Master|Dwarven Mines", new Npc("Ticket Master", "Dwarven Mines", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Dirt Guy|Dwarven Mines", new Npc("Dirt Guy", "Dwarven Mines", new BlockPos(43, 109, 174)));
        allSkyblockNpcs.put("Dalbrek|Dwarven Mines", new Npc("Dalbrek", "Dwarven Mines", new BlockPos(191, 217, 153)));
        allSkyblockNpcs.put("Puzzler|Dwarven Mines", new Npc("Puzzler", "Dwarven Mines", new BlockPos(181, 197, 135)));
        allSkyblockNpcs.put("Tal Ker|Dwarven Mines", new Npc("Tal Ker", "Dwarven Mines", new BlockPos(193, 197, 205)));
        allSkyblockNpcs.put("Bylma|Dwarven Mines", new Npc("Bylma", "Dwarven Mines", new BlockPos(-10, 129, 59)));
        allSkyblockNpcs.put("Emissary Carlton|Dwarven Mines", new Npc("Emissary Carlton", "Dwarven Mines", new BlockPos(-73, 154, -11)));
        allSkyblockNpcs.put("Emissary Ceanna|Dwarven Mines", new Npc("Emissary Ceanna", "Dwarven Mines", new BlockPos(42, 135, 22)));
        allSkyblockNpcs.put("Emissary Wilson|Dwarven Mines", new Npc("Emissary Wilson", "Dwarven Mines", new BlockPos(171, 151, 31)));
        allSkyblockNpcs.put("Emissary Lilith|Dwarven Mines", new Npc("Emissary Lilith", "Dwarven Mines", new BlockPos(58, 199, -9)));
        allSkyblockNpcs.put("Emissary Fraiser|Dwarven Mines", new Npc("Emissary Fraiser", "Dwarven Mines", new BlockPos(-133, 175, -51)));
        allSkyblockNpcs.put("Emissary Eliza|Dwarven Mines", new Npc("Emissary Eliza", "Dwarven Mines", new BlockPos(-38, 201, -132)));
        allSkyblockNpcs.put("Emissary Braum|Dwarven Mines", new Npc("Emissary Braum", "Dwarven Mines", new BlockPos(89, 199, -93)));
        allSkyblockNpcs.put("Malmar|Dwarven Mines", new Npc("Malmar", "Dwarven Mines", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Silnar|Dwarven Mines", new Npc("Silnar", "Dwarven Mines", new BlockPos(53, 142, 19)));
        allSkyblockNpcs.put("Fred|Dwarven Mines", new Npc("Fred", "Dwarven Mines", new BlockPos(-3, 150, -69)));
        allSkyblockNpcs.put("Jotraeline Greatforge|Dwarven Mines", new Npc("Jotraeline Greatforge", "Dwarven Mines", new BlockPos(-7, 146, -19)));
        allSkyblockNpcs.put("Dulin|Dwarven Mines", new Npc("Dulin", "Dwarven Mines", new BlockPos(79, 187, 128)));
        allSkyblockNpcs.put("Cold Enjoyer|Dwarven Mines", new Npc("Cold Enjoyer", "Dwarven Mines", new BlockPos(-16, 138, 217)));
        allSkyblockNpcs.put("Sor'Hen|Dwarven Mines", new Npc("Sor'Hen", "Dwarven Mines", new BlockPos(0, 121, 225)));
        allSkyblockNpcs.put("Scardius|Dwarven Mines", new Npc("Scardius", "Dwarven Mines", new BlockPos(-17, 122, 232)));
        allSkyblockNpcs.put("Plinius|Dwarven Mines", new Npc("Plinius", "Dwarven Mines", new BlockPos(10, 122, 236)));
        allSkyblockNpcs.put("Emissary Lissandra|Dwarven Mines", new Npc("Emissary Lissandra", "Dwarven Mines", new BlockPos(2, 122, 237)));
        allSkyblockNpcs.put("Dr. Stone|Dwarven Mines", new Npc("Dr. Stone", "Dwarven Mines", new BlockPos(28, 121, 238)));
        allSkyblockNpcs.put("Lift Operator|Dwarven Mines", new Npc("Lift Operator", "Dwarven Mines", new BlockPos(-80, 201, -124)));
        allSkyblockNpcs.put("Matigold|Dwarven Mines", new Npc("Marigold", "Dwarven Mines", new BlockPos(181, 151, 60)));

// Crystal Hollows
        allSkyblockNpcs.put("King Yolkar|Crystal Hollows", new Npc("King Yolkar", "Crystal Hollows", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Professor Robot|Crystal Hollows", new Npc("Professor Robot", "Crystal Hollows", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Kalhuiki Door Guardian|Crystal Hollows", new Npc("Kalhuiki Door Guardian", "Crystal Hollows", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Keepers|Crystal Hollows", new Npc("Keepers", "Crystal Hollows", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Sludge|Crystal Hollows", new Npc("Sludge", "Crystal Hollows", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Xalx|Crystal Hollows", new Npc("Xalx", "Crystal Hollows", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Three Bears|Crystal Hollows", new Npc("Three Bears", "Crystal Hollows", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Odawa|Crystal Hollows", new Npc("Odawa", "Crystal Hollows", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Geonathan Greatforge|Crystal Hollows", new Npc("Geonathan Greatforge", "Crystal Hollows", new BlockPos(530, 107, 556)));
        allSkyblockNpcs.put("Emissary Sisko|Crystal Hollows", new Npc("Emissary Sisko", "Crystal Hollows", new BlockPos(495, 107, 556)));
        allSkyblockNpcs.put("Gemma|Crystal Hollows", new Npc("Gemma", "Crystal Hollows", new BlockPos(475, 107, 513)));
        allSkyblockNpcs.put("Chunk|Crystal Hollows", new Npc("Chunk", "Crystal Hollows", new BlockPos(20000, 20000, 20000)));

// Spider's Den
        allSkyblockNpcs.put("Haymitch|Spider's Den", new Npc("Haymitch", "Spider's Den", new BlockPos(-203, 83, -237)));
        allSkyblockNpcs.put("Rick|Spider's Den", new Npc("Rick", "Spider's Den", new BlockPos(-265, 72, -324)));
        allSkyblockNpcs.put("Grandma Wolf|Spider's Den", new Npc("Grandma Wolf", "Spider's Den", new BlockPos(-282, 123, -191)));
        allSkyblockNpcs.put("Shaggy|Spider's Den", new Npc("Shaggy", "Spider's Den", new BlockPos(-279, 122, -183)));
        allSkyblockNpcs.put("Bramass Beastslayer|Spider's Den", new Npc("Bramass Beastslayer", "Spider's Den", new BlockPos(-272, 114, -197)));
        allSkyblockNpcs.put("Archaeologist|Spider's Den", new Npc("Archaeologist", "Spider's Den", new BlockPos(-361, 112, -291)));
        allSkyblockNpcs.put("Spider Tamer|Spider's Den", new Npc("Spider Tamer", "Spider's Den", new BlockPos(-299, 62, -195)));

// The End
        allSkyblockNpcs.put("Pearl Dealer|The End", new Npc("Pearl Dealer", "The End", new BlockPos(-505, 102, -285)));
        allSkyblockNpcs.put("Gregory|The End", new Npc("Gregory", "The End", new BlockPos(-607, 22, -284)));
        allSkyblockNpcs.put("Guber|The End", new Npc("Guber", "The End", new BlockPos(-495, 122, -242)));
        allSkyblockNpcs.put("Lone Adventurer|The End", new Npc("Lone Adventurer", "The End", new BlockPos(-589, 23, -271)));
        allSkyblockNpcs.put("Tyzzo|The End", new Npc("Tyzzo", "The End", new BlockPos(-597, 6, -272)));

// Crimson Isle
        allSkyblockNpcs.put("Elle|Crimson Isle", new Npc("Elle", "Crimson Isle", new BlockPos(-365, 81, -477)));
        allSkyblockNpcs.put("Desperate Engineer|Crimson Isle", new Npc("Desperate Engineer", "Crimson Isle", new BlockPos(-290, 128, -982)));
        allSkyblockNpcs.put("Crag|Crimson Isle", new Npc("Crag", "Crimson Isle", new BlockPos(-371, 115, -1043)));
        allSkyblockNpcs.put("Vesuvius|Crimson Isle", new Npc("Vesuvius", "Crimson Isle", new BlockPos(-381, 116, -1031)));
        allSkyblockNpcs.put("Vulcan|Crimson Isle", new Npc("Vulcan", "Crimson Isle", new BlockPos(-363, 116, -1032)));
        allSkyblockNpcs.put("Odger|Crimson Isle", new Npc("Odger", "Crimson Isle", new BlockPos(-374, 208, -811)));
        allSkyblockNpcs.put("Mage Emissary|Crimson Isle", new Npc("Mage Emissary", "Crimson Isle", new BlockPos(-132, 90, -723)));
        allSkyblockNpcs.put("Barbarian Emissary|Crimson Isle", new Npc("Barbarian Emissary", "Crimson Isle", new BlockPos(-581, 99, -711)));
        allSkyblockNpcs.put("Blacksmith|Crimson Isle", new Npc("Blacksmith", "Crimson Isle", new BlockPos(-83, 93, -735)));
        allSkyblockNpcs.put("Udel|Crimson Isle", new Npc("Udel", "Crimson Isle", new BlockPos(-79, 109, -788)));

// The Park
        allSkyblockNpcs.put("Charlie|The Park", new Npc("Charlie", "The Park", new BlockPos(-285, 82, -16)));
        allSkyblockNpcs.put("Vanessa|The Park", new Npc("Vanessa", "The Park", new BlockPos(-311, 83, -70)));
        allSkyblockNpcs.put("Gustave|The Park", new Npc("Gustave", "The Park", new BlockPos(-385, 89, 55)));
        allSkyblockNpcs.put("Melancholic Viking|The Park", new Npc("Melancholic Viking", "The Park", new BlockPos(-359, 91, 76)));
        allSkyblockNpcs.put("Ryan|The Park", new Npc("Ryan", "The Park", new BlockPos(-330, 103, -103)));
        allSkyblockNpcs.put("Melody|The Park", new Npc("Melody", "The Park", new BlockPos(-398, 110, 34)));
        allSkyblockNpcs.put("Master Tactician Funk|The Park", new Npc("Master Tactician Funk", "The Park", new BlockPos(-462, 110, -15)));
        allSkyblockNpcs.put("Juliette|The Park", new Npc("Juliette", "The Park", new BlockPos(-476, 134, -116)));
        allSkyblockNpcs.put("Romero|The Park", new Npc("Romero", "The Park", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Old Shaman Nyko|The Park", new Npc("Old Shaman Nyko", "The Park", new BlockPos(-379, 60, 36)));

// Dungeon Hub
        allSkyblockNpcs.put("Malik|Dungeon Hub", new Npc("Malik", "Dungeon Hub", new BlockPos(-80, 56, -119)));
        allSkyblockNpcs.put("Mort|Dungeon Hub", new Npc("Mort", "Dungeon Hub", new BlockPos(-88, 55, -128)));
        allSkyblockNpcs.put("Ophelia|Dungeon Hub", new Npc("Ophelia", "Dungeon Hub", new BlockPos(-85, 55, -139)));
        allSkyblockNpcs.put("Hub Selector|Dungeon Hub", new Npc("Hub Selector", "Dungeon Hub", new BlockPos(-37, 118, 10)));
        allSkyblockNpcs.put("Guildford|Dungeon Hub", new Npc("Guildford", "Dungeon Hub", new BlockPos(-18, 86, 4)));
        allSkyblockNpcs.put("Tomioka|Dungeon Hub", new Npc("Tomioka", "Dungeon Hub", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Duncan|Dungeon Hub", new Npc("Duncan", "Dungeon Hub", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Winona|Dungeon Hub", new Npc("Winona", "Dungeon Hub", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Trinity|Dungeon Hub", new Npc("Trinity", "Dungeon Hub", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Wizard|Dungeon Hub", new Npc("Wizard", "Dungeon Hub", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Zodd|Dungeon Hub", new Npc("Zodd", "Dungeon Hub", new BlockPos(20000, 20000, 20000)));

// Jerry's Workshop
        allSkyblockNpcs.put("St. Jerry|Jerry's Workshop", new Npc("St. Jerry", "Jerry's Workshop", new BlockPos(-22, 76, 92)));
        allSkyblockNpcs.put("Sherry|Jerry's Workshop", new Npc("Sherry", "Jerry's Workshop", new BlockPos(97, 69, 9)));
        allSkyblockNpcs.put("Gulliver|Jerry's Workshop", new Npc("Gulliver", "Jerry's Workshop", new BlockPos(69, 105, 33)));
        allSkyblockNpcs.put("Frosty|Jerry's Workshop", new Npc("Frosty", "Jerry's Workshop", new BlockPos(-17, 69, 2)));
        allSkyblockNpcs.put("Gary|Jerry's Workshop", new Npc("Gary", "Jerry's Workshop", new BlockPos(53, 103, 56)));
        allSkyblockNpcs.put("Terry|Jerry's Workshop", new Npc("Terry", "Jerry's Workshop", new BlockPos(-9, 27, 8)));
        allSkyblockNpcs.put("Banker Barry|Jerry's Workshop", new Npc("Banker Barry", "Jerry's Workshop", new BlockPos(20, 77, 44)));

// Rift
        allSkyblockNpcs.put("Alatar|Rift", new Npc("Alatar", "Rift", new BlockPos(-46, 116, 70)));
        allSkyblockNpcs.put("Wizard|Rift", new Npc("Wizard", "Rift", new BlockPos(-48, 122, 77)));
        allSkyblockNpcs.put("Wizardman|Rift", new Npc("Wizardman", "Rift", new BlockPos(-45, 90, 70)));
        allSkyblockNpcs.put("Elise|Rift", new Npc("Elise", "Rift", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Enigma|Rift", new Npc("Enigma", "Rift", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Motes Grubber|Rift", new Npc("Motes Grubber", "Rift", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Argofay Bughunter|Rift", new Npc("Argofay Bughunter", "Rift", new BlockPos(-85, 108, 97)));
        allSkyblockNpcs.put("Argofay Bugshopper|Rift", new Npc("Argofay Bugshopper", "Rift", new BlockPos(-95, 110, 95)));
        allSkyblockNpcs.put("Argofay Serialbather|Rift", new Npc("Argofay Serialbather", "Rift", new BlockPos(-139, 119, 115)));
        allSkyblockNpcs.put("Argofay Sonfather|Rift", new Npc("Argofay Sonfather", "Rift", new BlockPos(-96, 100, 156)));
        allSkyblockNpcs.put("Argofay Threebrother|Rift", new Npc("Argofay Threebrother", "Rift", new BlockPos(-93, 76, 108)));
        allSkyblockNpcs.put("Argofay Trafficker|Rift", new Npc("Argofay Trafficker", "Rift", new BlockPos(-92, 90, 106)));
        allSkyblockNpcs.put("Argofay Trailblazer|Rift", new Npc("Argofay Trailblazer", "Rift", new BlockPos(-97, 66, 147)));
        allSkyblockNpcs.put("Argofay Treemerger|Rift", new Npc("Argofay Treemerger", "Rift", new BlockPos(-144, 93, 139)));
        allSkyblockNpcs.put("Argofay Tencounter|Rift", new Npc("Argofay Tencounter", "Rift", new BlockPos(-93, 100, 95)));
        allSkyblockNpcs.put("Hound|Rift", new Npc("Hound", "Rift", new BlockPos(-69, 66, 147)));
        allSkyblockNpcs.put("Inverted Sirius|Rift", new Npc("Inverted Sirius", "Rift", new BlockPos(-96, 75, 190)));
        allSkyblockNpcs.put("Jacquelle|Rift", new Npc("Jacquelle", "Rift", new BlockPos(-122, 120, 137)));
        allSkyblockNpcs.put("Kiermet|Rift", new Npc("Kiermet", "Rift", new BlockPos(-135, 67, 158)));
        allSkyblockNpcs.put("Ribbit|Rift", new Npc("Ribbit", "Rift", new BlockPos(-137, 67, 159)));
        allSkyblockNpcs.put("Tel Kar|Rift", new Npc("Tel Kar", "Rift", new BlockPos(-113, 69, 62)));
        allSkyblockNpcs.put("Alabaster|Rift", new Npc("Alabaster", "Rift", new BlockPos(-130, 74, 166)));
        allSkyblockNpcs.put("Arora|Rift", new Npc("Arora", "Rift", new BlockPos(-129, 73, 169)));
        allSkyblockNpcs.put("Ashera|Rift", new Npc("Ashera", "Rift", new BlockPos(-130, 74, 169)));
        allSkyblockNpcs.put("Chester|Rift", new Npc("Chester", "Rift", new BlockPos(-130, 74, 173)));
        allSkyblockNpcs.put("Dackinoru|Rift", new Npc("Dackinoru", "Rift", new BlockPos(-118, 74, 172)));
        allSkyblockNpcs.put("Fafnir|Rift", new Npc("Fafnir", "Rift", new BlockPos(-127, 74, 172)));
        allSkyblockNpcs.put("Garlacius|Rift", new Npc("Garlacius", "Rift", new BlockPos(-128, 80, 170)));
        allSkyblockNpcs.put("Lazarus|Rift", new Npc("Lazarus", "Rift", new BlockPos(-127, 80, 166)));
        allSkyblockNpcs.put("Seskel|Rift", new Npc("Seskel", "Rift", new BlockPos(-129, 73, 164)));
        allSkyblockNpcs.put("Shifted|Rift", new Npc("Shifted", "Rift", new BlockPos(-120, 73, 174)));
        allSkyblockNpcs.put("Tybalt|Rift", new Npc("Tybalt", "Rift", new BlockPos(-129, 79, 169)));
        allSkyblockNpcs.put("Vreike|Rift", new Npc("Vreike", "Rift", new BlockPos(-124, 74, 172)));
        allSkyblockNpcs.put("Porhtal|Rift", new Npc("Porhtal", "Rift", new BlockPos(-158, 70, 162)));
        allSkyblockNpcs.put("Unbound Explorer|Rift", new Npc("Unbound Explorer", "Rift", new BlockPos(-29, 72, 316)));
        allSkyblockNpcs.put("Dr. Edwin|Rift", new Npc("Dr. Edwin", "Rift", new BlockPos(-80, 73, 10)));
        allSkyblockNpcs.put("Dr. Phear|Rift", new Npc("Dr. Phear", "Rift", new BlockPos(-146, 36, 27)));
        allSkyblockNpcs.put("Mushroom Guy|Rift", new Npc("Mushroom Guy", "Rift", new BlockPos(-178, 75, 12)));
        allSkyblockNpcs.put("Kay|Rift", new Npc("Kay", "Rift", new BlockPos(-191, 68, 59)));
        allSkyblockNpcs.put("Reed|Rift", new Npc("Reed", "Rift", new BlockPos(-212, 72, 60)));
        allSkyblockNpcs.put("Roy|Rift", new Npc("Roy", "Rift", new BlockPos(-205, 75, 49)));
        allSkyblockNpcs.put("Alchemist|Rift", new Npc("Alchemist", "Rift", new BlockPos(-50, 70, -66)));
        allSkyblockNpcs.put("Chef|Rift", new Npc("Chef", "Rift", new BlockPos(-103, 72, -104)));
        allSkyblockNpcs.put("Cosmo|Rift", new Npc("Cosmo", "Rift", new BlockPos(-67, 70, -92)));
        allSkyblockNpcs.put("Finplex|Rift", new Npc("Finplex", "Rift", new BlockPos(-57, 68, -82)));
        allSkyblockNpcs.put("Grandma|Rift", new Npc("Grandma", "Rift", new BlockPos(-71, 65, -60)));
        allSkyblockNpcs.put("Gunther|Rift", new Npc("Gunther", "Rift", new BlockPos(-69, 71, -62)));
        allSkyblockNpcs.put("Joey McPizza|Rift", new Npc("Joey McPizza", "Rift", new BlockPos(-106, 72, -103)));
        allSkyblockNpcs.put("Plumber Joe|Rift", new Npc("Plumber Joe", "Rift", new BlockPos(-62, 70, -78)));
        allSkyblockNpcs.put("Skylark|Rift", new Npc("Skylark", "Rift", new BlockPos(-72, 71, -115)));
        allSkyblockNpcs.put("Sorcerer Okron|Rift", new Npc("Sorcerer Okron", "Rift", new BlockPos(-71, 81, -111)));
        allSkyblockNpcs.put("Unhinged Kloon|Rift", new Npc("Unhinged Kloon", "Rift", new BlockPos(-56, 79, -13)));
        allSkyblockNpcs.put("Yoshua|Rift", new Npc("Yoshua", "Rift", new BlockPos(-69, 71, -118)));
        allSkyblockNpcs.put("Dr. Emmett|Rift", new Npc("Dr. Emmett", "Rift", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Disinfestor|Rift", new Npc("Disinfestor", "Rift", new BlockPos(-38, 71, -103)));
        allSkyblockNpcs.put("Kat|Rift", new Npc("Kat", "Rift", new BlockPos(-62, 70, -78)));
        allSkyblockNpcs.put("Shania|Rift", new Npc("Shania", "Rift", new BlockPos(-48, 72, -161)));
        allSkyblockNpcs.put("Cowboy Nick|Rift", new Npc("Cowboy Nick", "Rift", new BlockPos(-13, 70, -7)));
        allSkyblockNpcs.put("Cryptosis|Rift", new Npc("Cryptosis", "Rift", new BlockPos(28, 71, -77)));
        allSkyblockNpcs.put("Marcia|Rift", new Npc("Marcia", "Rift", new BlockPos(3, 71, -11)));
        allSkyblockNpcs.put("Nene|Rift", new Npc("Nene", "Rift", new BlockPos(24, 88, -91)));
        allSkyblockNpcs.put("Roger|Rift", new Npc("Roger", "Rift", new BlockPos(-3, 70, -44)));
        allSkyblockNpcs.put("Detransfigured Seraphine|Rift", new Npc("Detransfigured Seraphine", "Rift", new BlockPos(-17, 71, -101)));
        allSkyblockNpcs.put("Creed|Rift", new Npc("Creed", "Rift", new BlockPos(-48, 122, 77)));
        allSkyblockNpcs.put("Dust|Rift", new Npc("Dust", "Rift", new BlockPos(-48, 122, 77)));
        allSkyblockNpcs.put("Frankie|Rift", new Npc("Frankie", "Rift", new BlockPos(-48, 122, 77)));
        allSkyblockNpcs.put("Harriette|Rift", new Npc("Harriette", "Rift", new BlockPos(-48, 122, 77)));
        allSkyblockNpcs.put("Soma|Rift", new Npc("Soma", "Rift", new BlockPos(-48, 122, 77)));
        allSkyblockNpcs.put("Stain|Rift", new Npc("Stain", "Rift", new BlockPos(-48, 122, 77)));
        allSkyblockNpcs.put("Violetta|Rift", new Npc("Violetta", "Rift", new BlockPos(-48, 122, 77)));
        allSkyblockNpcs.put("Barry|Rift", new Npc("Barry", "Rift", new BlockPos(-46, 54, -143)));
        allSkyblockNpcs.put("Joliet|Rift", new Npc("Joliet", "Rift", new BlockPos(-32, 64, -43)));
        allSkyblockNpcs.put("Seymour|Rift", new Npc("Seymour", "Rift", new BlockPos(-27, 71, -45)));
        allSkyblockNpcs.put("Taylor|Rift", new Npc("Taylor", "Rift", new BlockPos(-32, 71, -39)));
        allSkyblockNpcs.put("Ramero|Rift", new Npc("Ramero", "Rift", new BlockPos(-49, 68, -39)));
        allSkyblockNpcs.put("Detective Amog|Rift", new Npc("Detective Amog", "Rift", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Jerry|Rift", new Npc("Jerry", "Rift", new BlockPos(-86, 20, 4)));
        allSkyblockNpcs.put("Lonely Ävaeìkx|Rift", new Npc("Lonely Ävaeìkx", "Rift", new BlockPos(20000, 20000, 20000)));
        allSkyblockNpcs.put("Damia|Rift", new Npc("Damia", "Rift", new BlockPos(31, 72, -95)));
        allSkyblockNpcs.put("Blacksmith|Rift", new Npc("Blacksmith", "Rift", new BlockPos(-2, 68, -142)));
        allSkyblockNpcs.put("Master Tactician Fink|Rift", new Npc("Master Tactician Fink", "Rift", new BlockPos(-191, 72, -57)));
        allSkyblockNpcs.put("Mole|Rift", new Npc("Mole", "Rift", new BlockPos(-160, 97, -76)));
        allSkyblockNpcs.put("Deer|Rift", new Npc("Deer", "Rift", new BlockPos(139, 70, -12)));
        allSkyblockNpcs.put("Phaser|Rift", new Npc("Phaser", "Rift", new BlockPos(60, 70, -89)));
        allSkyblockNpcs.put("Maddox the Slayer|Rift", new Npc("Maddox the Slayer", "Rift", new BlockPos(205, 77, 45)));
        allSkyblockNpcs.put("Fairylosopher|Rift", new Npc("Fairylosopher", "Rift", new BlockPos(20000, 20000, 20000)));
    }

    private static final Map<String, Npc> pendingCallbacks = new HashMap<>();

    public static void handleCallback(String id) {
        Npc npc = pendingCallbacks.remove(id);
        if (npc == null) return;

        String currentLocation = ModFunctions.mapLocationToGeneralArea(SideBarUtils.getSideBarInfo("location"));
        if (!npc.location().equals(currentLocation)) {
            ModFunctions.displayMessageWithHeader("§cYou need to be in §eThe " + npc.location() + " §cto display this waypoint.");
            return;
        }

        addToBeMarked(npc);
        ModFunctions.displayMessageWithHeader("§aWaypoint displayed.");
    }

    private static final List<Npc> toBeMarked = new ArrayList<>();

    public static void addToBeMarked(Npc npc) {
        toBeMarked.add(npc);
    }

    public static void clearToBeMarked() {
        toBeMarked.clear();
    }

    public static void onWorldRender(WorldRenderContext context) {
        if (MinecraftClient.getInstance().player == null) return;
        String currentLocation = ModFunctions.mapLocationToGeneralArea(SideBarUtils.getSideBarInfo("location"));

        toBeMarked.removeIf(npc ->
                MinecraftClient.getInstance().player.getBlockPos().isWithinDistance(Vec3d.ofCenter(npc.coordinates()), 5.0)
        );

        for (Npc npc : toBeMarked) {
            if (npc.location().equals(currentLocation))
                WaypointRenderer.render(context, new WaypointRenderer.Waypoint(npc.coordinates, npc.name, 0x4CFF00, WaypointRenderer.WaypointStyle.BADGE), -1);
        }
    }

    public static void handleCommand(String npcName) {
        List<Npc> matches = allSkyblockNpcs.values().stream()
                .filter(npc -> npc.name().equalsIgnoreCase(npcName))
                .toList();

        if (matches.isEmpty()) {
            ModFunctions.displayMessageWithHeader("§cNo NPC found with name: " + npcName);
            return;
        }

        MutableText message = Text.empty()
                .append(SSU.NAME)
                .append(Text.literal(npcName + " can be found in these locations:\n"));

        for (int i = 0; i < matches.size(); i++) {
            Npc match = matches.get(i);
            String id = java.util.UUID.randomUUID().toString().replace("-", "");
            pendingCallbacks.put(id, match);

            Text waypointButton = Text.literal("[Display Waypoint]")
                    .setStyle(Style.EMPTY
                            .withColor(Formatting.GREEN)
                            .withClickEvent(new ClickEvent.RunCommand("/snpc " + id)));

            if (i > 0) message.append(Text.literal("\n"));

            message.append(Text.literal("    - ").formatted(Formatting.GRAY))
                    .append(Text.literal(match.location()).formatted(Formatting.DARK_GREEN))
                    .append(match.coordinates.getX() == 20000
                            ? Text.literal(": Location varies").formatted(Formatting.DARK_GRAY, Formatting.ITALIC)
                            : Text.literal(": " + match.coordinates().getX() + " " +
                            match.coordinates().getY() + " " +
                            match.coordinates().getZ() + " ").formatted(Formatting.GRAY))
                    .append(match.coordinates.getX() == 20000
                            ? Text.empty()
                            : waypointButton);
        }

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(message);
    }
}