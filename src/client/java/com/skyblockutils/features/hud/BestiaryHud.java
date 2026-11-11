package com.skyblockutils.features.hud;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.skyblockutils.ModFunctions;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.hud.layout.HudContext;
import com.skyblockutils.features.hud.layout.HudElement;
import com.skyblockutils.features.hud.layout.HudLayoutEntry;
import com.skyblockutils.hypixel.HypixelLocation;
import com.skyblockutils.utils.ClipboardUtils;
import com.skyblockutils.utils.ModStyle;
import com.skyblockutils.utils.SSUIndicator;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BestiaryHud {
    public static class BestiaryEntity {
        BestiaryEntity(String island, String islandName, String name, int currentKills, int maxKills, int[] milestones, String skin, String item) {
            this.island = island;
            this.islandName = islandName;
            this.name = name;
            this.currentKills = currentKills;
            this.maxKills = maxKills;
            this.milestones = milestones;
            this.skin = skin;
            this.item = item;
        }

        public final String island;
        public final String islandName;
        public final String name;
        public int currentKills;
        public int maxKills;
        public final int[] milestones;
        private final String skin;
        private final String item;
        private ItemStack icon;

        public boolean isMaxed() {
            return maxKills > 0 && currentKills >= maxKills;
        }

        public int nextMilestone() {
            for (int milestone : milestones) if (milestone > currentKills) return Math.min(milestone, maxKills);
            return maxKills;
        }

        public ItemStack icon() {
            if (icon == null) icon = createIcon(skin, item);
            return icon;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("SSU Bestiary");

    private static final long RETRY_MS = 60_000;
    private static final int MAX_ATTEMPTS = 3;
    private static final long AUTOSAVE_MS = 5 * 60_000;
    private static final int PAGE_READ_INTERVAL = 10;
    private static final int PLAYER_SLOTS = 36;

    private static final int HUD_ENTRIES = 5;
    private static final long HUD_TIMEOUT_MS = 2 * 60_000;
    private static final int PADDING = 6;
    private static final int CORNER_RADIUS = 5;
    private static final int MIN_WIDTH = 80;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;
    private static final int ROW_GAP = 1;
    private static final int TITLE_GAP = 4;

    private static final Pattern PROFILE_ID = Pattern.compile("^Profile ID: ([0-9a-fA-F-]{32,36})$");
    private static final Pattern KILLS = Pattern.compile("^Kills: ([\\d,.]+[kKmM]?)$");
    private static final Pattern FRACTION = Pattern.compile("([\\d,.]+[kKmM]?)/([\\d,.]+[kKmM]?)");
    private static final Pattern PAGE_PREFIX = Pattern.compile("^\\(\\d+/\\d+\\)\\s*");
    private static final Pattern TIER_SUFFIX = Pattern.compile("\\s+[IVXLCDM]+$");

    private static final long MATCH_WINDOW_MS = 600;
    private static final long MELEE_TAG_MS = 5_000;
    private static final long PROJECTILE_TAG_MS = 3_000;
    private static final long ABILITY_TAG_MS = 1_500;
    private static final double ABILITY_RADIUS = 12.0;
    private static final double PROJECTILE_RADIUS = 2.5;
    private static final double PROJECTILE_SCAN_RANGE = 128.0;
    private static final double NAMETAG_RADIUS = 1.0;
    private static final double NAMETAG_HEIGHT = 2.5;
    private static final Pattern NAMETAG_LEVEL = Pattern.compile("^.*?\\[Lv[\\d,]+]\\s*");
    private static final Pattern NAMETAG_SYMBOLS = Pattern.compile("[^\\p{L}\\p{N} '\\-]");
    private static final Pattern NAMETAG_HEALTH = Pattern.compile("\\s+[\\d,.]+[kKmMbB]?(?:/[\\d,.]+[kKmMbB]?)?❤.*$");

    private enum Evidence { WEAK, MEDIUM, STRONG }

    private record PendingDeath(String name, long at, Evidence evidence) {}

    private record Ability(Vec3 pos, long at) {}

    private static final List<PendingDeath> deaths = new ArrayList<>();
    private static final List<Long> dings = new ArrayList<>();
    private static final Map<Integer, Long> meleeTags = new HashMap<>();
    private static final Map<Integer, Long> projectileTags = new HashMap<>();
    private static final Map<Integer, Vec3> ownProjectiles = new HashMap<>();
    private static final Deque<Ability> abilities = new ArrayDeque<>();
    private static final List<BestiaryEntity> allMobs = new ArrayList<>();
    private static final LinkedHashMap<BestiaryEntity, Long> recent = new LinkedHashMap<>();
    private static final Map<String, ItemStack> skinIcons = new HashMap<>();
    private static List<BestiaryEntity> previewMobs;

    private static String profileId;
    private static boolean loaded = false;
    private static boolean loadQueued = false;
    private static boolean dirty = false;
    private static long lastRequestAt = 0;
    private static long lastSaveAt = 0;
    private static int attempts = 0;
    private static int ticks = 0;

    public static final HudElement ELEMENT = new HudElement("bestiary_hud", "Bestiary HUD") {
        @Override
        public HudLayoutEntry defaultLayout() {
            return new HudLayoutEntry(HudLayoutEntry.Anchor.START, HudLayoutEntry.Anchor.CENTER, 4, 0, 1f);
        }

        @Override
        public int maxWidth() {
            return 350;
        }

        @Override
        public int autoWidth(boolean preview) {
            Minecraft mc = Minecraft.getInstance();
            int width = Math.max(MIN_WIDTH, mc.font.width(title()) + PADDING * 2);
            for (BestiaryEntity entity : shown(preview)) {
                width = Math.max(width, PADDING * 2 + ICON_SIZE + ICON_GAP + mc.font.width(line(entity)));
            }
            return width;
        }

        @Override
        public int height(int width, boolean preview) {
            int rows = shown(preview).size();
            return PADDING * 2 + Minecraft.getInstance().font.lineHeight + TITLE_GAP + rows * ICON_SIZE + Math.max(0, rows - 1) * ROW_GAP;
        }

        @Override
        public boolean isEnabled() {
            return ModConfig.INSTANCE.bestiaryHud;
        }

        @Override
        public boolean isActive() {
            if (!StrayersSkyblockUtilsClient.isInSkyblock || shown(false).isEmpty()) return true;
            return ModConfig.INSTANCE.bestiaryHudHideInCatacombs && ModFunctions.isInDungeons(Minecraft.getInstance());
        }

        @Override
        public void render(HudContext ctx) {
            int accent = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
            ctx.panel(0, 0, ctx.width, ctx.height, CORNER_RADIUS, ModStyle.COLOR_BACKGROUND, accent);

            Component title = title();
            int y = PADDING;
            ctx.text(title, ctx.width / 2 - ctx.font.width(title) / 2, y, 0xFFFFFFFF, true);
            y += ctx.font.lineHeight + TITLE_GAP;

            int textOffset = (ICON_SIZE - ctx.font.lineHeight) / 2 + 1;
            for (BestiaryEntity entity : shown(ctx.preview)) {
                ctx.graphics.fakeItem(entity.icon(), PADDING, y);
                ctx.text(line(entity), PADDING + ICON_SIZE + ICON_GAP, y + textOffset, 0xFFFFFFFF, true);
                y += ICON_SIZE + ROW_GAP;
            }
        }
    };

    public static void handleMessage(String message) {
        Matcher m = PROFILE_ID.matcher(message);
        if (!m.matches()) return;
        String id = m.group(1).replace("-", "").toLowerCase(Locale.ROOT);
        if (id.equals(profileId)) return;
        save();
        profileId = id;
        allMobs.clear();
        recent.clear();
        loaded = false;
        dirty = false;
        attempts = 0;
        loadQueued = true;
    }

    public static void tick(Minecraft client) {
        if (!ModConfig.INSTANCE.bestiaryHud || profileId == null) return;
        long now = System.currentTimeMillis();

        if (!loaded && !loadQueued && attempts < MAX_ATTEMPTS && now - lastRequestAt >= RETRY_MS) loadQueued = true;
        if (loadQueued && requestLoad()) {
            loadQueued = false;
            lastRequestAt = now;
            attempts++;
        }

        recent.values().removeIf(at -> now - at > HUD_TIMEOUT_MS);
        trackProjectiles(client, now);
        resolveKills(now);
        if (dirty && now - lastSaveAt >= AUTOSAVE_MS) save();
        if (++ticks % PAGE_READ_INTERVAL == 0) readOpenBestiary(client);
    }

    private static boolean requestLoad() {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "profile_load");
        msg.addProperty("profile", profileId);
        return SSUIndicator.sendText(msg.toString());
    }

    public static void save() {
        if (!loaded || !dirty || profileId == null) return;
        JsonArray entries = new JsonArray();
        for (BestiaryEntity entity : allMobs) {
            JsonObject entry = new JsonObject();
            entry.addProperty("i", entity.island);
            entry.addProperty("n", entity.name);
            entry.addProperty("k", entity.currentKills);
            entries.add(entry);
        }
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "profile_save");
        msg.addProperty("profile", profileId);
        msg.add("bestiary", entries);
        if (SSUIndicator.sendText(msg.toString())) {
            dirty = false;
            lastSaveAt = System.currentTimeMillis();
        }
    }

    public static void handleServerMessage(String type, JsonObject msg) {
        if (!msg.has("profile") || !msg.get("profile").getAsString().equals(profileId)) return;

        if (type.equals("profile_error")) {
            LOGGER.warn("Bestiary load failed: {}", msg.has("error") ? msg.get("error").getAsString() : "unknown");
            return;
        }
        if (!type.equals("profile_bestiary") || !msg.has("mobs")) return;

        allMobs.clear();
        recent.clear();
        for (JsonElement element : msg.getAsJsonArray("mobs")) {
            BestiaryEntity entity = parseEntity(element.getAsJsonObject());
            if (entity != null) allMobs.add(entity);
        }
        loaded = true;
        dirty = false;
        lastSaveAt = System.currentTimeMillis();
    }

    private static BestiaryEntity parseEntity(JsonObject mob) {
        try {
            int[] milestones = new int[0];
            if (mob.has("t")) {
                JsonArray array = mob.getAsJsonArray("t");
                milestones = new int[array.size()];
                for (int i = 0; i < milestones.length; i++) milestones[i] = array.get(i).getAsInt();
            }
            return new BestiaryEntity(
                    mob.get("i").getAsString(),
                    mob.get("l").getAsString(),
                    mob.get("n").getAsString(),
                    mob.get("k").getAsInt(),
                    mob.get("m").getAsInt(),
                    milestones,
                    mob.has("s") ? mob.get("s").getAsString() : null,
                    mob.has("it") ? mob.get("it").getAsString() : null
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static ItemStack createIcon(String skin, String item) {
        if (skin != null) return skinIcons.computeIfAbsent(skin, BestiaryHud::skullIcon).copy();
        if (item != null) {
            Identifier id = Identifier.tryParse(item.contains(":") ? item : "minecraft:" + item.toLowerCase(Locale.ROOT));
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) return new ItemStack(BuiltInRegistries.ITEM.getValue(id));
        }
        return new ItemStack(Items.PLAYER_HEAD);
    }

    private static ItemStack skullIcon(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        String value = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", value)));
        UUID id = UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8));
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(new GameProfile(id, "SSUBestiary", properties)));
        return stack;
    }

    private static List<BestiaryEntity> shown(boolean preview) {
        if (!recent.isEmpty()) {
            List<BestiaryEntity> list = new ArrayList<>(recent.keySet());
            Collections.reverse(list);
            return list;
        }
        return preview ? preview() : List.of();
    }

    private static List<BestiaryEntity> preview() {
        if (previewMobs == null) {
            previewMobs = List.of(
                    new BestiaryEntity("", "", "Zealot", 19596, 10000, new int[]{10000}, null, "ender_pearl"),
                    new BestiaryEntity("", "", "Crypt Ghoul", 3412, 25000, new int[]{3000, 5000, 7500, 25000}, null, "rotten_flesh"),
                    new BestiaryEntity("", "", "Enderman", 16, 50, new int[]{5, 10, 15, 25, 50}, null, "ender_eye")
            );
        }
        return previewMobs;
    }

    private static Component title() {
        int accent = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
        return Component.literal("Bestiary").withColor(accent);
    }

    private static Component line(BestiaryEntity entity) {
        int text = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TEXT);
        int accent = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
        Component name = Component.literal(entity.name + ": ").withColor(ModStyle.COLOR_TITLE);
        if (entity.isMaxed()) return Component.empty().append(name).append(Component.literal("MAX").withColor(accent));
        String value = String.format("%,d/%,d", entity.currentKills, entity.nextMilestone());
        return Component.empty()
                .append(name)
                .append(Component.literal(value).withColor(text))
                .append(Component.literal(" | ").withColor(ModStyle.COLOR_SUBTITLE))
                .append(Component.literal(String.format("%,d", entity.maxKills)).withColor(text));
    }

    private static void readOpenBestiary(Minecraft client) {
        if (!loaded || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) return;
        String title = PAGE_PREFIX.matcher(clean(screen.getTitle().getString())).replaceFirst("");
        if (!title.startsWith("Bestiary")) return;
        int arrow = title.indexOf('➜');
        String islandName = arrow == -1 ? "" : title.substring(arrow + 1).trim();
        if (islandName.equals("Your Island")) islandName = "Private Island";

        AbstractContainerMenu menu = screen.getMenu();
        int size = menu.slots.size() - PLAYER_SLOTS;
        int rows = size / 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) continue;
            ItemStack stack = menu.getSlot(i).getItem();
            if (!stack.isEmpty()) correctEntry(islandName, clean(stack.getHoverName().getString()), lore(stack));
        }
    }

    private static void correctEntry(String islandName, String name, List<String> lore) {
        Integer kills = null, max = null;
        boolean overall = false;
        for (String line : lore) {
            if (line.contains("unlocked this Family yet")) {
                kills = 0;
                break;
            }
            Matcher k = KILLS.matcher(line);
            if (k.matches()) kills = parseNumber(k.group(1));
            else if (line.startsWith("Overall Progress")) overall = true;
            else if (overall && max == null) {
                Matcher f = FRACTION.matcher(line);
                if (f.find()) max = parseNumber(f.group(2));
            }
        }
        if (kills == null) return;

        String mob = TIER_SUFFIX.matcher(name).replaceFirst("");
        BestiaryEntity entity = find(mob, e -> e.islandName.equals(islandName));
        if (entity == null) entity = find(name, e -> e.islandName.equals(islandName));
        if (entity == null) return;
        if (entity.currentKills != kills) {
            entity.currentKills = kills;
            dirty = true;
        }
        if (max != null) entity.maxKills = max;
    }

    private static BestiaryEntity find(String name, Predicate<BestiaryEntity> preferred) {
        BestiaryEntity fallback = null;
        int matches = 0;
        for (BestiaryEntity entity : allMobs) {
            if (!entity.name.equals(name)) continue;
            if (preferred.test(entity)) return entity;
            fallback = entity;
            matches++;
        }
        return matches == 1 ? fallback : null;
    }

    public static void reset() {
        profileId = null;
        allMobs.clear();
        recent.clear();
        loaded = false;
        loadQueued = false;
        dirty = false;
        attempts = 0;
        deaths.clear();
        dings.clear();
        meleeTags.clear();
        projectileTags.clear();
        ownProjectiles.clear();
        abilities.clear();
    }

    public static void devCopy() {
        if (!loaded) {
            ModFunctions.sendSystemMessage(profileId == null ? "§cNo SkyBlock profile detected yet" : "§cBestiary data hasn't loaded yet", false);
            return;
        }
        StringBuilder out = new StringBuilder();
        for (BestiaryEntity entity : allMobs) {
            out.append(entity.islandName).append(" / ").append(entity.name).append(": ")
                    .append(entity.currentKills).append('/').append(entity.maxKills).append('\n');
        }
        ClipboardUtils.copyTextToClipboard(out.toString().trim());
        ModFunctions.sendSystemMessage("§aCopied " + allMobs.size() + " bestiary entries to clipboard", false);
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, _, _, entity, _) -> {
            if (player == Minecraft.getInstance().player) meleeTags.put(entity.getId(), System.currentTimeMillis());
            return InteractionResult.PASS;
        });
    }

    public static void onPlayerAction() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !loaded) return;
        abilities.addLast(new Ability(client.player.position(), System.currentTimeMillis()));
        while (abilities.size() > 20) abilities.pollFirst();
    }

    public static void onEntityDeath(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (!ModConfig.INSTANCE.bestiaryHud || !loaded || client.player == null) return;
        if (!(entity instanceof LivingEntity) || entity instanceof ArmorStand || entity == client.player) return;
        String name = nametagName(entity);
        if (name == null) return;
        long now = System.currentTimeMillis();
        deaths.add(new PendingDeath(name, now, evidence(entity, now)));
    }

    public static void onXpSound() {
        if (!ModConfig.INSTANCE.bestiaryHud || !loaded) return;
        dings.add(System.currentTimeMillis());
    }

    private static Evidence evidence(Entity entity, long now) {
        Long melee = meleeTags.get(entity.getId());
        if (melee != null && now - melee <= MELEE_TAG_MS) return Evidence.STRONG;
        Long projectile = projectileTags.get(entity.getId());
        if (projectile != null && now - projectile <= PROJECTILE_TAG_MS) return Evidence.STRONG;
        for (Ability ability : abilities) {
            if (now - ability.at() <= ABILITY_TAG_MS && entity.position().distanceToSqr(ability.pos()) <= ABILITY_RADIUS * ABILITY_RADIUS) {
                return Evidence.MEDIUM;
            }
        }
        return Evidence.WEAK;
    }

    private static void trackProjectiles(Minecraft client, long now) {
        meleeTags.values().removeIf(at -> now - at > MELEE_TAG_MS);
        projectileTags.values().removeIf(at -> now - at > PROJECTILE_TAG_MS);
        abilities.removeIf(ability -> now - ability.at() > ABILITY_TAG_MS);
        if (client.level == null || client.player == null) {
            ownProjectiles.clear();
            return;
        }

        Set<Integer> alive = new HashSet<>();
        AABB around = client.player.getBoundingBox().inflate(PROJECTILE_SCAN_RANGE);
        for (Projectile projectile : client.level.getEntitiesOfClass(Projectile.class, around, p -> p.getOwner() == client.player)) {
            alive.add(projectile.getId());
            ownProjectiles.put(projectile.getId(), projectile.position());
        }

        Iterator<Map.Entry<Integer, Vec3>> it = ownProjectiles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Vec3> entry = it.next();
            if (alive.contains(entry.getKey())) continue;
            it.remove();
            AABB area = AABB.ofSize(entry.getValue(), PROJECTILE_RADIUS * 2, PROJECTILE_RADIUS * 2, PROJECTILE_RADIUS * 2);
            for (LivingEntity hit : client.level.getEntitiesOfClass(LivingEntity.class, area, e -> !(e instanceof ArmorStand) && e != client.player)) {
                projectileTags.put(hit.getId(), now);
            }
        }
    }

    private static void resolveKills(long now) {
        if (deaths.isEmpty() && dings.isEmpty()) return;
        long start = Long.MAX_VALUE;
        for (PendingDeath death : deaths) start = Math.min(start, death.at());
        for (long ding : dings) start = Math.min(start, ding);
        if (now - start < MATCH_WINDOW_MS) return;

        long end = start + MATCH_WINDOW_MS;
        List<PendingDeath> group = new ArrayList<>();
        deaths.removeIf(death -> {
            if (death.at() > end) return false;
            group.add(death);
            return true;
        });
        int confirmations = 0;
        Iterator<Long> it = dings.iterator();
        while (it.hasNext()) {
            if (it.next() > end) continue;
            it.remove();
            confirmations++;
        }

        group.sort(Comparator.comparing(PendingDeath::evidence).reversed().thenComparingLong(PendingDeath::at));
        for (int i = 0; i < Math.min(confirmations, group.size()); i++) handleMobKill(group.get(i).name());
    }

    private static String nametagName(Entity entity) {
        AABB box = entity.getBoundingBox().inflate(NAMETAG_RADIUS, 0, NAMETAG_RADIUS).expandTowards(0, NAMETAG_HEIGHT, 0);
        ArmorStand closest = null;
        String raw = null;
        for (ArmorStand stand : entity.level().getEntitiesOfClass(ArmorStand.class, box)) {
            Component customName = stand.getCustomName();
            if (customName == null) continue;
            String text = customName.getString();
            if (!text.contains("❤")) continue;
            if (closest == null || stand.distanceToSqr(entity) < closest.distanceToSqr(entity)) {
                closest = stand;
                raw = text;
            }
        }
        if (raw == null) return null;
        String name = NAMETAG_LEVEL.matcher(clean(raw)).replaceFirst("");
        name = NAMETAG_HEALTH.matcher(name).replaceFirst("");
        name = NAMETAG_SYMBOLS.matcher(name).replaceAll("").replaceAll("\\s+", " ").trim();
        return name.isEmpty() ? null : name;
    }

    public static void handleMobKill(String mobName) {
        addMobKill(mobName);
    }

    public static void addMobKill(String mobName) {
        String mode = HypixelLocation.mode();
        BestiaryEntity entity = find(mobName, e -> e.island.equals(mode));
        ModFunctions.sendSystemMessage(entity == null
                ? "§7You killed §f" + mobName + " §c(not in bestiary)"
                : "§7You killed §f" + entity.name + " §7(" + entity.islandName + ")", false);
        if (entity == null) return;
        entity.currentKills++;
        dirty = true;

        recent.remove(entity);
        recent.put(entity, System.currentTimeMillis());
        while (recent.size() > HUD_ENTRIES) recent.remove(recent.keySet().iterator().next());
    }

    private static List<String> lore(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return List.of();
        List<String> lines = new ArrayList<>();
        for (Component line : lore.lines()) lines.add(clean(line.getString()));
        return lines;
    }

    private static String clean(String text) {
        return text.replaceAll("§.", "").trim();
    }

    private static int parseNumber(String text) {
        String s = text.replace(",", "");
        double mult = 1;
        char last = Character.toLowerCase(s.charAt(s.length() - 1));
        if (last == 'k') mult = 1_000;
        else if (last == 'm') mult = 1_000_000;
        if (mult != 1) s = s.substring(0, s.length() - 1);
        return (int) Math.round(Double.parseDouble(s) * mult);
    }
}
