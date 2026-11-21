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
import com.skyblockutils.utils.SideBarUtils;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
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

    private static final long MATCH_WINDOW_MS = 60;
    private static final long RESOLVE_DELAY_MS = 1_600;
    private static final long DEATH_EXPIRY_MS = 2_000;
    private static final long PASSIVE_COINS_BEFORE_MS = 300;
    private static final long PASSIVE_COINS_AFTER_MS = 1_500;
    private static final long PASSIVE_COIN_AMOUNT = 5;
    private static final Pattern PURSE = Pattern.compile("(?:Purse|Piggy): ([\\d,]+)");
    private static final long TAG_MS = 5_000;
    private static final long ACTION_WINDOW_MS = 1_000;
    private static final long COIN_WINDOW_MS = 300;
    private static final long LATE_TAG_MS = 400;
    private static final long NAME_CACHE_MS = 10_000;
    private static final int NAME_SCAN_INTERVAL = 5;
    private static final double NAME_SCAN_RANGE = 48.0;
    private static final float KILL_DING_PITCH = 1.4921f;
    private static final float PITCH_TOLERANCE = 0.01f;
    private static final double PROJECTILE_HIT_MARGIN = 0.6;
    private static final double PROJECTILE_SCAN_RANGE = 128.0;
    private static final double INDICATOR_SCAN_RANGE = 32.0;
    private static final double INDICATOR_HORIZONTAL = 2.5;
    private static final double INDICATOR_VERTICAL = 3.5;
    private static final Pattern DAMAGE_INDICATOR = Pattern.compile("^[^\\p{L}\\p{N}]*\\d[\\d,.]*[kKmMbB]?[^\\p{L}\\p{N}]*$");
    private static final double NAMETAG_RADIUS = 1.0;
    private static final double NAMETAG_HEIGHT = 2.5;
    private static final Pattern NAMETAG_LEVEL = Pattern.compile("^.*?\\[Lv[\\d,]+]\\s*");
    private static final Pattern NAMETAG_SYMBOLS = Pattern.compile("[^\\p{L}\\p{N} '\\-]");
    private static final Pattern NAMETAG_HEALTH = Pattern.compile("\\s+[\\d,.]+[kKmMbB]?(?:/[\\d,.]+[kKmMbB]?)?❤.*$");

    private enum Tier { NONE, MEDIUM, STRONG }

    private record PendingDeath(int id, String name, int standId, long at, double distanceSq) {}

    private record Split(String parent, String child, int count) {}

    private static final List<Split> SPLITS = List.of(new Split("Splitter Spider", "Silverfish", 2));
    private static final long SPLIT_WINDOW_MS = 150;
    private static final Map<String, Long> UNSEEN_DEATHS = Map.of("Silverfish", 2_000L);
    private static final Map<String, Long> lastCredited = new HashMap<>();
    private static String splitChild = null;
    private static int splitRemaining = 0;
    private static long splitAt = 0;

    private record Nametag(String name, int standId) {}

    private record CachedName(Nametag tag, long at) {}

    private static final List<PendingDeath> deaths = new ArrayList<>();
    private static final List<Long> dings = new ArrayList<>();
    private static final List<Long> coinPickups = new ArrayList<>();
    private static final Map<Integer, CachedName> names = new HashMap<>();
    private static final List<Long> passiveCoins = new ArrayList<>();
    private static long lastPurse = -1;
    private static final Map<Integer, Long> strongTags = new HashMap<>();
    private static final Map<Integer, Long> mediumTags = new HashMap<>();
    private static long lastActionAt = 0;
    private static long lastHurtAt = 0;
    private static int lastHurtTime = 0;
    private static final Map<Integer, Vec3> ownProjectiles = new HashMap<>();
    private static final Set<Integer> scannedStands = new HashSet<>();
    private static final List<BestiaryEntity> allMobs = new ArrayList<>();
    private static final LinkedHashMap<BestiaryEntity, Long> recent = new LinkedHashMap<>();
    private static final Map<String, ItemStack> skinIcons = new HashMap<>();
    private static List<BestiaryEntity> previewMobs;

    private static String profileId;

    public static String profileId() {
        return profileId;
    }
    private static boolean loaded = false;
    private static boolean loadQueued = false;
    private static boolean dirty = false;
    private static long lastRequestAt = 0;
    private static long lastSaveAt = 0;
    private static int attempts = 0;
    private static int ticks = 0;

    public static final HudElement ELEMENT = new HudElement("bestiary_hud", "Bestiary HUD") {

        private final List<Option> options = List.of(
                new Option("maxed", "Maxed mobs", () -> ModConfig.INSTANCE.bestiaryHudShowMaxed),
                new Option("dungeons", "Show in dungeons", () -> !ModConfig.INSTANCE.bestiaryHudHideInCatacombs)
        );

        @Override
        public List<Option> options() {
            return options;
        }
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
            return !show("dungeons") && ModFunctions.isInDungeons(Minecraft.getInstance());
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
        strongTags.values().removeIf(at -> now - at > TAG_MS);
        mediumTags.values().removeIf(at -> now - at > TAG_MS);
        trackHurt(client, now);
        trackPurse(now);
        if (ticks % NAME_SCAN_INTERVAL == 0) cacheNames(client, now);
        trackProjectiles(client, now);
        trackDamageIndicators(client, now);
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
        List<BestiaryEntity> list = new ArrayList<>(recent.keySet());
        if (!ELEMENT.show("maxed")) list.removeIf(BestiaryEntity::isMaxed);
        if (!list.isEmpty()) {
            Collections.reverse(list);
            return list;
        }
        if (!preview) return List.of();
        List<BestiaryEntity> mobs = new ArrayList<>(preview());
        if (!ELEMENT.show("maxed")) mobs.removeIf(BestiaryEntity::isMaxed);
        return mobs;
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
            int diff = entity.currentKills - kills;
            LogHud.addLog("§e[Bestiary] corrected " + entity.name + " " + entity.currentKills + " -> " + kills + " (" + (diff > 0 ? "counted " + diff + " too many" : "missed " + (-diff)) + ")");
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
        coinPickups.clear();
        lastCredited.clear();
        names.clear();
        passiveCoins.clear();
        lastPurse = -1;
        strongTags.clear();
        mediumTags.clear();
        ownProjectiles.clear();
        scannedStands.clear();
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
            if (player == Minecraft.getInstance().player) strongTags.put(entity.getId(), System.currentTimeMillis());
            return InteractionResult.PASS;
        });
    }

    public static void onPlayerAction() {
        lastActionAt = System.currentTimeMillis();
    }

    public static void onDamage(ClientboundDamageEventPacket packet) {
        Minecraft client = Minecraft.getInstance();
        if (!ModConfig.INSTANCE.bestiaryHud || !loaded || client.player == null) return;
        long now = System.currentTimeMillis();
        int me = client.player.getId();
        if (packet.entityId() == me) {
            int attacker = packet.sourceCauseId() >= 0 ? packet.sourceCauseId() : packet.sourceDirectId();
            if (attacker >= 0 && attacker != me) strongTags.put(attacker, now);
            return;
        }
        if (packet.sourceCauseId() == me || packet.sourceDirectId() == me || ownProjectiles.containsKey(packet.sourceDirectId())) {
            strongTags.put(packet.entityId(), now);
        }
    }

    public static void onEntityDeath(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (!ModConfig.INSTANCE.bestiaryHud || !loaded || client.player == null) return;
        if (!(entity instanceof LivingEntity) || entity instanceof ArmorStand || entity == client.player) return;
        for (PendingDeath death : deaths) {
            if (death.id() == entity.getId()) return;
        }
        long now = System.currentTimeMillis();
        Nametag tag = nametag(entity);
        if (tag == null) {
            CachedName cached = names.get(entity.getId());
            if (cached != null && now - cached.at() <= NAME_CACHE_MS) tag = cached.tag();
        }
        if (tag == null && (entity.isPassenger() || entity.isVehicle())) {
            List<Entity> linked = new ArrayList<>(entity.getPassengers());
            if (entity.getVehicle() != null) linked.add(entity.getVehicle());
            for (Entity other : linked) {
                tag = nametag(other);
                if (tag == null) {
                    CachedName cached = names.get(other.getId());
                    if (cached != null && now - cached.at() <= NAME_CACHE_MS) tag = cached.tag();
                }
                if (tag != null) break;
            }
            if (tag == null) {
                LogHud.addLog("§8[Bestiary] mount or rider without a name, " + entity.getType().getDescription().getString());
                return;
            }
        }
        if (tag == null) {
            String type = entity.getType().getDescription().getString();
            String mode = HypixelLocation.mode();
            if (find(type, e -> e.island.equals(mode)) == null) {
                LogHud.addLog("§8[Bestiary] death without a name, " + type);
                return;
            }
            tag = new Nametag(type, -1);
        }
        if (tag.standId() >= 0) {
            for (PendingDeath death : deaths) {
                if (death.standId() == tag.standId()) return;
            }
        }
        deaths.add(new PendingDeath(entity.getId(), tag.name(), tag.standId(), now, entity.distanceToSqr(client.player)));
    }

    private static void cacheNames(Minecraft client, long now) {
        names.values().removeIf(cached -> now - cached.at() > NAME_CACHE_MS);
        if (client.level == null || client.player == null) return;
        AABB around = client.player.getBoundingBox().inflate(NAME_SCAN_RANGE);
        for (LivingEntity entity : client.level.getEntitiesOfClass(LivingEntity.class, around, e -> !(e instanceof ArmorStand) && e != client.player && e.isAlive())) {
            Nametag tag = nametag(entity);
            if (tag != null) names.put(entity.getId(), new CachedName(tag, now));
        }
    }

    private static Tier tier(PendingDeath death) {
        long limit = death.at() + LATE_TAG_MS;
        Long strong = strongTags.get(death.id());
        if (strong != null && strong <= limit && death.at() - strong <= TAG_MS) return Tier.STRONG;
        Long medium = mediumTags.get(death.id());
        if (medium != null && medium <= limit && death.at() - medium <= TAG_MS) return Tier.MEDIUM;
        return Tier.NONE;
    }

    public static void onXpSound(float pitch) {
        if (!ModConfig.INSTANCE.bestiaryHud || !loaded) return;
        if (Math.abs(pitch - KILL_DING_PITCH) > PITCH_TOLERANCE) return;
        dings.add(System.currentTimeMillis());
    }

    public static void onItemPickup(int collectorId, Entity item) {
        Minecraft client = Minecraft.getInstance();
        if (!ModConfig.INSTANCE.bestiaryHud || !loaded || client.player == null || collectorId != client.player.getId()) return;
        if (item instanceof ItemEntity itemEntity && itemEntity.getItem().is(Items.PLAYER_HEAD)) coinPickups.add(System.currentTimeMillis());
    }

    private static void trackPurse(long now) {
        long purse = -1;
        for (String line : SideBarUtils.getSidebarLines()) {
            Matcher m = PURSE.matcher(clean(line).replaceAll("[^\\x20-\\x7E]", ""));
            if (!m.find()) continue;
            purse = Long.parseLong(m.group(1).replace(",", ""));
            break;
        }
        if (purse < 0) return;
        if (lastPurse >= 0 && purse - lastPurse == PASSIVE_COIN_AMOUNT) passiveCoins.add(now);
        lastPurse = purse;
    }

    private static void trackHurt(Minecraft client, long now) {
        if (client.player == null) return;
        int hurtTime = client.player.hurtTime;
        if (hurtTime > lastHurtTime) lastHurtAt = now;
        lastHurtTime = hurtTime;
    }

    private static void trackProjectiles(Minecraft client, long now) {
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
            Vec3 last = entry.getValue();
            AABB area = AABB.ofSize(last, 4, 4, 4);
            for (LivingEntity hit : client.level.getEntitiesOfClass(LivingEntity.class, area, e -> !(e instanceof ArmorStand) && e != client.player)) {
                if (hit.getBoundingBox().inflate(PROJECTILE_HIT_MARGIN).contains(last)) strongTags.put(hit.getId(), now);
            }
        }
    }

    private static void trackDamageIndicators(Minecraft client, long now) {
        if (client.level == null || client.player == null) {
            scannedStands.clear();
            return;
        }

        Set<Integer> present = new HashSet<>();
        AABB around = client.player.getBoundingBox().inflate(INDICATOR_SCAN_RANGE);
        for (ArmorStand stand : client.level.getEntitiesOfClass(ArmorStand.class, around, ArmorStand::hasCustomName)) {
            present.add(stand.getId());
            if (scannedStands.contains(stand.getId())) continue;
            Component customName = stand.getCustomName();
            if (customName == null) continue;
            String text = clean(customName.getString());
            if (text.isEmpty()) continue;
            scannedStands.add(stand.getId());
            if (text.contains("❤") || !DAMAGE_INDICATOR.matcher(text).matches()) continue;

            LivingEntity target = null;
            double best = Double.MAX_VALUE;
            AABB area = stand.getBoundingBox().inflate(INDICATOR_HORIZONTAL, INDICATOR_VERTICAL, INDICATOR_HORIZONTAL);
            for (LivingEntity entity : client.level.getEntitiesOfClass(LivingEntity.class, area, e -> !(e instanceof ArmorStand) && e != client.player)) {
                double dx = entity.getX() - stand.getX();
                double dz = entity.getZ() - stand.getZ();
                double distance = dx * dx + dz * dz;
                if (distance < best) {
                    best = distance;
                    target = entity;
                }
            }
            if (target != null && now - Math.max(lastActionAt, lastHurtAt) <= ACTION_WINDOW_MS) mediumTags.put(target.getId(), now);
        }
        scannedStands.retainAll(present);
    }

    private static void resolveKills(long now) {
        coinPickups.removeIf(at -> now - at > RESOLVE_DELAY_MS + COIN_WINDOW_MS);
        passiveCoins.removeIf(at -> now - at > RESOLVE_DELAY_MS + PASSIVE_COINS_BEFORE_MS);
        Iterator<Long> it = dings.iterator();
        while (it.hasNext()) {
            long ding = it.next();
            if (now - ding < RESOLVE_DELAY_MS) break;
            it.remove();

            if (consumeCoinPickup(ding)) {
                consumePassiveCoins(ding);
                LogHud.addLog("§7[Bestiary] kill ding ignored, coin pickup");
                continue;
            }
            if (consumePassiveCoins(ding)) {
                LogHud.addLog("§7[Bestiary] kill ding ignored, passive +" + PASSIVE_COIN_AMOUNT + " coins");
                continue;
            }

            PendingDeath best = null;
            Tier bestTier = Tier.NONE;
            int candidates = 0;
            for (PendingDeath death : deaths) {
                long gap = Math.abs(death.at() - ding);
                if (gap > MATCH_WINDOW_MS) continue;
                candidates++;
                Tier tier = tier(death);
                if (best == null || tier.compareTo(bestTier) > 0
                        || (tier == bestTier && gap < Math.abs(best.at() - ding))) {
                    best = death;
                    bestTier = tier;
                }
            }
            boolean splitPending = splitChild != null && splitRemaining > 0 && Math.abs(ding - splitAt) <= SPLIT_WINDOW_MS;
            if (splitPending && (best == null || bestTier == Tier.NONE)) {
                splitRemaining--;
                LogHud.addLog("§7[Bestiary] §f" + splitChild + " §7split" + (best != null ? " over " + best.name() : ""));
                handleMobKill(splitChild);
                continue;
            }
            if (best == null) {
                String unseen = unseenFamily(ding);
                if (unseen != null) {
                    LogHud.addLog("§7[Bestiary] §f" + unseen + " §7unseen");
                    handleMobKill(unseen);
                } else {
                    LogHud.addLog("§7[Bestiary] kill ding with no matching death");
                }
                continue;
            }
            String pick = "";
            if (bestTier == Tier.NONE && candidates > 1 && !sameName(ding)) {
                best = closest(ding);
                pick = " closest";
            }
            deaths.remove(best);
            LogHud.addLog("§7[Bestiary] §f" + best.name() + " §7" + bestTier.name().toLowerCase(Locale.ROOT) + " " + Math.abs(best.at() - ding) + "ms" + (candidates > 1 ? " of " + candidates : "") + pick);
            handleMobKill(best.name());
            for (Split split : SPLITS) {
                if (!split.parent().equals(best.name())) continue;
                boolean stacking = split.child().equals(splitChild) && splitRemaining > 0 && Math.abs(ding - splitAt) <= SPLIT_WINDOW_MS;
                splitChild = split.child();
                splitRemaining = (stacking ? splitRemaining : 0) + split.count();
                splitAt = ding;
            }
        }
        deaths.removeIf(death -> now - death.at() > DEATH_EXPIRY_MS);
    }

    private static String unseenFamily(long ding) {
        String family = null;
        long latest = Long.MIN_VALUE;
        for (Map.Entry<String, Long> entry : UNSEEN_DEATHS.entrySet()) {
            Long at = lastCredited.get(entry.getKey());
            if (at == null || ding - at > entry.getValue() || ding < at - MATCH_WINDOW_MS) continue;
            if (at > latest) {
                latest = at;
                family = entry.getKey();
            }
        }
        return family;
    }

    private static PendingDeath closest(long ding) {
        PendingDeath best = null;
        for (PendingDeath death : deaths) {
            if (Math.abs(death.at() - ding) > MATCH_WINDOW_MS) continue;
            if (best == null || death.distanceSq() < best.distanceSq()) best = death;
        }
        return best;
    }

    private static boolean sameName(long ding) {
        String name = null;
        for (PendingDeath death : deaths) {
            if (Math.abs(death.at() - ding) > MATCH_WINDOW_MS) continue;
            if (name == null) name = death.name();
            else if (!name.equals(death.name())) return false;
        }
        return name != null;
    }

    private static boolean consumePassiveCoins(long ding) {
        Iterator<Long> it = passiveCoins.iterator();
        while (it.hasNext()) {
            long at = it.next();
            if (at < ding - PASSIVE_COINS_BEFORE_MS || at > ding + PASSIVE_COINS_AFTER_MS) continue;
            it.remove();
            return true;
        }
        return false;
    }

    private static boolean consumeCoinPickup(long ding) {
        Iterator<Long> it = coinPickups.iterator();
        while (it.hasNext()) {
            if (Math.abs(it.next() - ding) > COIN_WINDOW_MS) continue;
            it.remove();
            return true;
        }
        return false;
    }

    private static Nametag nametag(Entity entity) {
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
        return name.isEmpty() ? null : new Nametag(name, closest.getId());
    }

    public static void handleMobKill(String mobName) {
        if (UNSEEN_DEATHS.containsKey(mobName)) lastCredited.put(mobName, System.currentTimeMillis() - RESOLVE_DELAY_MS);
        addMobKill(mobName);
    }

    public static void addMobKill(String mobName) {
        String mode = HypixelLocation.mode();
        BestiaryEntity entity = find(mobName, e -> e.island.equals(mode));
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