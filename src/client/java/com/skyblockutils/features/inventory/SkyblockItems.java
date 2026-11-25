package com.skyblockutils.features.inventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockutils.config.ConfigPaths;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;

import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class SkyblockItems {
    public enum State {IDLE, LOADING, READY, FAILED}

    public record SbItem(String id, String name, String tier, String material, int durability, String color,
                         String skin, String model, boolean glowing) {
        public boolean isHead() {
            return skin != null && !skin.isBlank();
        }

        public String spec() {
            return "skyblock:" + id;
        }

        public Component displayName() {
            return Component.literal(name).withStyle(tierColor(tier));
        }
    }

    private record Cache(long fetched, List<SbItem> items) {
    }

    private static final String URL = "https://api.hypixel.net/v2/resources/skyblock/items";
    private static final Path PATH = ConfigPaths.CACHE.resolve("items.json");
    private static final long MAX_AGE = Duration.ofDays(1).toMillis();
    private static final Gson GSON = new GsonBuilder().create();
    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    private static final String[] DYES = {"ink_sac", "red_dye", "green_dye", "cocoa_beans", "lapis_lazuli", "purple_dye",
            "cyan_dye", "light_gray_dye", "gray_dye", "pink_dye", "lime_dye", "yellow_dye", "light_blue_dye", "magenta_dye",
            "orange_dye", "bone_meal"};
    private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
    private static final Map<String, String> RENAMES = new HashMap<>();
    private static final Map<String, String[]> VARIANTS = new HashMap<>();

    static {
        String[][] renames = {
                {"GOLD_SWORD", "golden_sword"}, {"GOLD_AXE", "golden_axe"}, {"GOLD_PICKAXE", "golden_pickaxe"},
                {"GOLD_SPADE", "golden_shovel"}, {"GOLD_HOE", "golden_hoe"}, {"GOLD_HELMET", "golden_helmet"},
                {"GOLD_CHESTPLATE", "golden_chestplate"}, {"GOLD_LEGGINGS", "golden_leggings"}, {"GOLD_BOOTS", "golden_boots"},
                {"GOLD_BARDING", "golden_horse_armor"}, {"IRON_BARDING", "iron_horse_armor"}, {"DIAMOND_BARDING", "diamond_horse_armor"},
                {"WOOD_SWORD", "wooden_sword"}, {"WOOD_AXE", "wooden_axe"}, {"WOOD_PICKAXE", "wooden_pickaxe"},
                {"WOOD_SPADE", "wooden_shovel"}, {"WOOD_HOE", "wooden_hoe"}, {"IRON_SPADE", "iron_shovel"},
                {"STONE_SPADE", "stone_shovel"}, {"DIAMOND_SPADE", "diamond_shovel"}, {"WATCH", "clock"},
                {"SULPHUR", "gunpowder"}, {"SNOW_BALL", "snowball"}, {"EXP_BOTTLE", "experience_bottle"},
                {"FIREWORK", "firework_rocket"}, {"FIREWORK_CHARGE", "firework_star"}, {"LEASH", "lead"},
                {"SEEDS", "wheat_seeds"}, {"NETHER_STALK", "nether_wart"}, {"MELON", "melon_slice"},
                {"MELON_BLOCK", "melon"}, {"SPECKLED_MELON", "glistering_melon_slice"}, {"CARROT_ITEM", "carrot"},
                {"POTATO_ITEM", "potato"}, {"RAW_BEEF", "beef"}, {"RAW_CHICKEN", "chicken"}, {"PORK", "porkchop"},
                {"GRILLED_PORK", "cooked_porkchop"}, {"MUSHROOM_SOUP", "mushroom_stew"}, {"BOOK_AND_QUILL", "writable_book"},
                {"EMPTY_MAP", "map"}, {"MAP", "filled_map"}, {"WORKBENCH", "crafting_table"}, {"ENDER_STONE", "end_stone"},
                {"ENDER_PORTAL_FRAME", "end_portal_frame"}, {"EYE_OF_ENDER", "ender_eye"}, {"NETHER_BRICK_ITEM", "nether_brick"},
                {"NETHER_BRICK", "nether_bricks"}, {"QUARTZ_ORE", "nether_quartz_ore"}, {"CLAY_BRICK", "brick"},
                {"BRICK", "bricks"}, {"IRON_FENCE", "iron_bars"}, {"THIN_GLASS", "glass_pane"}, {"FENCE", "oak_fence"},
                {"FENCE_GATE", "oak_fence_gate"}, {"TRAP_DOOR", "oak_trapdoor"}, {"WOOD_DOOR", "oak_door"},
                {"WOODEN_DOOR", "oak_door"}, {"REDSTONE_TORCH_ON", "redstone_torch"}, {"DIODE", "repeater"},
                {"REDSTONE_COMPARATOR", "comparator"}, {"PISTON_BASE", "piston"}, {"PISTON_STICKY_BASE", "sticky_piston"},
                {"WEB", "cobweb"}, {"LONG_GRASS", "short_grass"}, {"YELLOW_FLOWER", "dandelion"},
                {"HUGE_MUSHROOM_1", "brown_mushroom_block"}, {"HUGE_MUSHROOM_2", "red_mushroom_block"}, {"MYCEL", "mycelium"},
                {"WATER_LILY", "lily_pad"}, {"ENCHANTMENT_TABLE", "enchanting_table"}, {"BREWING_STAND_ITEM", "brewing_stand"},
                {"CAULDRON_ITEM", "cauldron"}, {"FLOWER_POT_ITEM", "flower_pot"}, {"SIGN", "oak_sign"}, {"BOAT", "oak_boat"},
                {"BED", "red_bed"}, {"REDSTONE_LAMP_OFF", "redstone_lamp"}, {"STEP", "stone_slab"}, {"WOOD_STEP", "oak_slab"},
                {"MOB_SPAWNER", "spawner"}, {"MONSTER_EGG", "pig_spawn_egg"}, {"HARD_CLAY", "terracotta"},
                {"SMOOTH_STAIRS", "stone_brick_stairs"}, {"WOOD_STAIRS", "oak_stairs"}, {"WOOD_BUTTON", "oak_button"},
                {"WOOD_PLATE", "oak_pressure_plate"}, {"STONE_PLATE", "stone_pressure_plate"}, {"IRON_PLATE", "heavy_weighted_pressure_plate"},
                {"GOLD_PLATE", "light_weighted_pressure_plate"}, {"GOLD_RECORD", "music_disc_13"}, {"GREEN_RECORD", "music_disc_cat"},
                {"RECORD_3", "music_disc_blocks"}, {"RECORD_4", "music_disc_chirp"}, {"RECORD_5", "music_disc_far"},
                {"RECORD_6", "music_disc_mall"}, {"RECORD_7", "music_disc_mellohi"}, {"RECORD_8", "music_disc_stal"},
                {"RECORD_9", "music_disc_strad"}, {"RECORD_10", "music_disc_ward"}, {"RECORD_11", "music_disc_11"},
                {"RECORD_12", "music_disc_wait"}, {"CARROT_STICK", "carrot_on_a_stick"}, {"COMMAND", "command_block"},
                {"SKULL", "skeleton_skull"}, {"STORAGE_MINECART", "chest_minecart"}, {"POWERED_MINECART", "furnace_minecart"},
                {"EXPLOSIVE_MINECART", "tnt_minecart"}, {"HOPPER_MINECART", "hopper_minecart"}, {"RAILS", "rail"},
                {"SUGAR_CANE_BLOCK", "sugar_cane"}, {"CAKE_BLOCK", "cake"}, {"NETHER_WARTS", "nether_wart"},
                {"STAINED_GLASS_PANE", "white_stained_glass_pane"}, {"IRON_DOOR_BLOCK", "iron_door"}, {"SNOW", "snow"},
                {"BURNING_FURNACE", "furnace"}, {"GLOWING_REDSTONE_ORE", "redstone_ore"}, {"SOIL", "farmland"},
                {"DOUBLE_STEP", "smooth_stone"}, {"ENDER_CHEST", "ender_chest"}, {"BANNER", "white_banner"}
        };
        for (String[] entry : renames) RENAMES.put(entry[0], entry[1]);

        String[] colored = new String[16];
        for (String type : new String[]{"WOOL", "STAINED_GLASS", "STAINED_GLASS_PANE", "STAINED_CLAY", "CARPET", "BANNER"}) {
            String suffix = switch (type) {
                case "WOOL" -> "_wool";
                case "STAINED_GLASS" -> "_stained_glass";
                case "STAINED_GLASS_PANE" -> "_stained_glass_pane";
                case "STAINED_CLAY" -> "_terracotta";
                case "CARPET" -> "_carpet";
                default -> "_banner";
            };
            String[] variants = colored.clone();
            for (int i = 0; i < 16; i++) variants[i] = COLORS[type.equals("BANNER") ? 15 - i : i] + suffix;
            VARIANTS.put(type, variants);
        }
        VARIANTS.put("INK_SACK", DYES);
        VARIANTS.put("LOG", new String[]{"oak_log", "spruce_log", "birch_log", "jungle_log"});
        VARIANTS.put("LOG_2", new String[]{"acacia_log", "dark_oak_log"});
        VARIANTS.put("LEAVES", new String[]{"oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves"});
        VARIANTS.put("LEAVES_2", new String[]{"acacia_leaves", "dark_oak_leaves"});
        String[] planks = new String[6];
        String[] saplings = new String[6];
        for (int i = 0; i < 6; i++) {
            planks[i] = WOODS[i] + "_planks";
            saplings[i] = WOODS[i] + "_sapling";
        }
        VARIANTS.put("WOOD", planks);
        VARIANTS.put("SAPLING", saplings);
        VARIANTS.put("SAND", new String[]{"sand", "red_sand"});
        VARIANTS.put("DIRT", new String[]{"dirt", "coarse_dirt", "podzol"});
        VARIANTS.put("STONE", new String[]{"stone", "granite", "polished_granite", "diorite", "polished_diorite", "andesite", "polished_andesite"});
        VARIANTS.put("COAL", new String[]{"coal", "charcoal"});
        VARIANTS.put("GOLDEN_APPLE", new String[]{"golden_apple", "enchanted_golden_apple"});
        VARIANTS.put("RAW_FISH", new String[]{"cod", "salmon", "tropical_fish", "pufferfish"});
        VARIANTS.put("COOKED_FISH", new String[]{"cooked_cod", "cooked_salmon"});
        VARIANTS.put("PRISMARINE", new String[]{"prismarine", "prismarine_bricks", "dark_prismarine"});
        VARIANTS.put("SPONGE", new String[]{"sponge", "wet_sponge"});
        VARIANTS.put("SANDSTONE", new String[]{"sandstone", "chiseled_sandstone", "cut_sandstone"});
        VARIANTS.put("RED_SANDSTONE", new String[]{"red_sandstone", "chiseled_red_sandstone", "cut_red_sandstone"});
        VARIANTS.put("SMOOTH_BRICK", new String[]{"stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks"});
        VARIANTS.put("QUARTZ_BLOCK", new String[]{"quartz_block", "chiseled_quartz_block", "quartz_pillar"});
        VARIANTS.put("RED_ROSE", new String[]{"poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy"});
        VARIANTS.put("DOUBLE_PLANT", new String[]{"sunflower", "lilac", "tall_grass", "large_fern", "rose_bush", "peony"});
        VARIANTS.put("SKULL_ITEM", new String[]{"skeleton_skull", "wither_skeleton_skull", "zombie_head", "player_head", "creeper_head", "dragon_head"});
        VARIANTS.put("ANVIL", new String[]{"anvil", "chipped_anvil", "damaged_anvil"});
        VARIANTS.put("COBBLE_WALL", new String[]{"cobblestone_wall", "mossy_cobblestone_wall"});
    }

    private static volatile List<SbItem> items = List.of();
    private static volatile Map<String, SbItem> byId = Map.of();
    private static volatile State state = State.IDLE;
    private static final Map<String, ItemStack> STACKS = new HashMap<>();

    private SkyblockItems() {
    }

    public static List<SbItem> items() {
        ensureLoaded();
        return items;
    }

    public static SbItem get(String id) {
        ensureLoaded();
        return byId.get(id.toUpperCase(Locale.ROOT));
    }

    static void clearStacks() {
        synchronized (STACKS) {
            STACKS.clear();
        }
    }

    public static State state() {
        return state;
    }

    public static void ensureLoaded() {
        if (state != State.IDLE) return;
        state = State.LOADING;
        CompletableFuture.runAsync(SkyblockItems::load);
    }

    public static void refresh() {
        if (state == State.LOADING) return;
        state = State.LOADING;
        CompletableFuture.runAsync(() -> {
            try {
                List<SbItem> fetched = fetch();
                writeCache(new Cache(System.currentTimeMillis(), fetched));
                finish(fetched);
            } catch (Exception e) {
                state = items.isEmpty() ? State.FAILED : State.READY;
            }
        });
    }

    public static ItemStack stack(SbItem item) {
        ButtonIcons.checkReload();
        synchronized (STACKS) {
            return STACKS.computeIfAbsent(item.id(), _ -> build(item));
        }
    }

    private static ItemStack build(SbItem item) {
        ItemStack stack;
        if (item.isHead()) {
            stack = ButtonIcons.textureHead(item.skin());
        } else {
            stack = new ItemStack(resolve(item));
            if (item.color() != null && !item.color().isBlank()) {
                try {
                    String[] rgb = item.color().split(",");
                    int color = Integer.parseInt(rgb[0].trim()) << 16 | Integer.parseInt(rgb[1].trim()) << 8 | Integer.parseInt(rgb[2].trim());
                    stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
                } catch (RuntimeException ignored) {
                }
            }
        }
        if (item.glowing()) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.set(DataComponents.CUSTOM_NAME, item.displayName());
        CompoundTag tag = new CompoundTag();
        tag.putString("id", item.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static Item resolve(SbItem item) {
        if (item.model() != null && item.model().startsWith("minecraft:")) {
            Item model = lookup(item.model());
            if (model != Items.AIR) return model;
        }
        String material = item.material() == null ? "" : item.material().toUpperCase(Locale.ROOT);
        String[] variants = VARIANTS.get(material);
        String id;
        if (variants != null) {
            id = variants[Math.clamp(item.durability(), 0, variants.length - 1)];
        } else {
            id = RENAMES.getOrDefault(material, material.toLowerCase(Locale.ROOT));
        }
        Item resolved = lookup("minecraft:" + id);
        return resolved == Items.AIR ? Items.PAPER : resolved;
    }

    private static Item lookup(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null || !BuiltInRegistries.ITEM.containsKey(identifier)) return Items.AIR;
        return BuiltInRegistries.ITEM.getValue(identifier);
    }

    private static void load() {
        Cache cache = readCache();
        if (cache != null) finish(cache.items());
        if (cache != null && System.currentTimeMillis() - cache.fetched() < MAX_AGE) return;
        try {
            List<SbItem> fetched = fetch();
            writeCache(new Cache(System.currentTimeMillis(), fetched));
            finish(fetched);
        } catch (Exception e) {
            if (cache == null) state = State.FAILED;
        }
    }

    private static void finish(List<SbItem> list) {
        Map<String, SbItem> map = new HashMap<>();
        for (SbItem item : list) map.put(item.id().toUpperCase(Locale.ROOT), item);
        items = List.copyOf(list);
        byId = map;
        state = State.READY;
        Minecraft.getInstance().execute(() -> {
            synchronized (STACKS) {
                STACKS.clear();
            }
            ButtonIcons.clearCache();
        });
    }

    private static List<SbItem> fetch() throws Exception {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(URL)).timeout(Duration.ofSeconds(30)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IllegalStateException("HTTP " + response.statusCode());

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            List<SbItem> list = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("items")) {
                JsonObject item = element.getAsJsonObject();
                String id = string(item, "id");
                if (id.isEmpty()) continue;
                list.add(new SbItem(
                        id,
                        string(item, "name").replaceAll("§.", ""),
                        string(item, "tier"),
                        string(item, "material"),
                        item.has("durability") ? item.get("durability").getAsInt() : 0,
                        string(item, "color"),
                        skin(item.get("skin")),
                        string(item, "item_model"),
                        item.has("glowing") && item.get("glowing").getAsBoolean()));
            }
            list.sort(Comparator.comparing(SbItem::name, String.CASE_INSENSITIVE_ORDER));
            return list;
        }
    }

    private static String skin(JsonElement skin) {
        if (skin == null || skin.isJsonNull()) return null;
        if (skin.isJsonPrimitive()) return skin.getAsString();
        if (skin.isJsonObject() && skin.getAsJsonObject().has("value")) return skin.getAsJsonObject().get("value").getAsString();
        return null;
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    private static ChatFormatting tierColor(String tier) {
        return switch (tier == null ? "" : tier) {
            case "UNCOMMON" -> ChatFormatting.GREEN;
            case "RARE" -> ChatFormatting.BLUE;
            case "EPIC" -> ChatFormatting.DARK_PURPLE;
            case "LEGENDARY" -> ChatFormatting.GOLD;
            case "MYTHIC" -> ChatFormatting.LIGHT_PURPLE;
            case "DIVINE" -> ChatFormatting.AQUA;
            case "SPECIAL", "VERY_SPECIAL" -> ChatFormatting.RED;
            case "ULTIMATE" -> ChatFormatting.DARK_RED;
            case "ADMIN" -> ChatFormatting.DARK_RED;
            default -> ChatFormatting.WHITE;
        };
    }

    private static Cache readCache() {
        if (!Files.exists(PATH)) return null;
        try (Reader reader = Files.newBufferedReader(PATH)) {
            Cache cache = GSON.fromJson(reader, Cache.class);
            return cache == null || cache.items() == null ? null : cache;
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeCache(Cache cache) {
        ConfigPaths.ensureDirectories();
        try (Writer writer = Files.newBufferedWriter(PATH)) {
            GSON.toJson(cache, writer);
        } catch (Exception ignored) {
        }
    }
}
