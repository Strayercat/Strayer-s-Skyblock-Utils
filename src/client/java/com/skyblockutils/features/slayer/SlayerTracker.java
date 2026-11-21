package com.skyblockutils.features.slayer;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.hud.BestiaryHud;
import com.skyblockutils.features.hud.layout.HudContext;
import com.skyblockutils.features.hud.layout.HudElement;
import com.skyblockutils.features.hud.layout.HudLayoutEntry;
import com.skyblockutils.utils.ModStyle;
import com.skyblockutils.utils.SideBarUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlayerTracker {
    private static final String BOSS_UP = "Slay the boss!";
    private static final String BOSS_SLAIN = "Boss slain!";
    private static final long KILL_DEBOUNCE_MS = 5_000;
    private static final Pattern XP_NEEDED = Pattern.compile("(Wolf|Zombie|Spider|Enderman|Blaze|Vampire) Slayer LVL (\\d+) - (?:Next LVL in ([\\d,]+) XP!|LVL MAXED OUT!)");
    private static final Pattern LVL_UP = Pattern.compile("LVL UP! ➜ (Wolf|Zombie|Spider|Enderman|Blaze|Vampire) Slayer LVL (\\d+)");
    private static final String[] TIERS = {"I", "II", "III", "IV", "V"};
    private static final int[] STANDARD_XP = {5, 25, 100, 500, 1500};
    private static final int[] WIDE_MILESTONES = {10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000};

    private enum SlayerType {
        ZOMBIE("Zombie", null, STANDARD_XP, new int[]{5, 15, 200, 1000, 5000, 20000, 100000, 400000, 1000000}),
        SPIDER("Spider", "http://textures.minecraft.net/texture/cd541541daaff50896cd258bdbdd4cf80c3ba816735726078bfe393927e57f1", STANDARD_XP, new int[]{5, 25, 200, 1000, 5000, 20000, 100000, 400000, 1000000}),
        WOLF("Wolf", "http://textures.minecraft.net/texture/69d1d3113ec43ac2961dd59f28175fb4718873c6c448dfca8722317d67", STANDARD_XP, WIDE_MILESTONES),
        ENDERMAN("Enderman", "http://textures.minecraft.net/texture/96c0b36d53fff69a49c7d6f3932f2b0fe948e032226d5e8045ec58408a36e951", STANDARD_XP, WIDE_MILESTONES),
        BLAZE("Blaze", "http://textures.minecraft.net/texture/b78ef2e4cf2c41a2d14bfde9caff10219f5b1bf5b35a49eb51c6467882cb5f0", STANDARD_XP, WIDE_MILESTONES),
        VAMPIRE("Vampire", "http://textures.minecraft.net/texture/8d44756e0b4ece8d746296a3d5e297e1415f4ba17647ffe228385383d161a9", new int[]{10, 25, 60, 120, 150}, new int[]{20, 75, 240, 840, 2400});

        final String label;
        final String skin;
        final int[] xpPerTier;
        final int[] milestones;
        private ItemStack icon;

        SlayerType(String label, String skin, int[] xpPerTier, int[] milestones) {
            this.label = label;
            this.skin = skin;
            this.xpPerTier = xpPerTier;
            this.milestones = milestones;
        }

        ItemStack icon() {
            if (icon == null) icon = skin == null ? new ItemStack(Items.ZOMBIE_HEAD) : skull(skin);
            return icon;
        }

        static SlayerType fromLabel(String label) {
            for (SlayerType type : values()) {
                if (type.label.equals(label)) return type;
            }
            return null;
        }

        static SlayerType fromBoss(String boss) {
            String lower = boss.toLowerCase(Locale.ROOT);
            if (lower.contains("revenant") || lower.contains("atoned")) return ZOMBIE;
            if (lower.contains("tarantula") || lower.contains("conjoined")) return SPIDER;
            if (lower.contains("sven")) return WOLF;
            if (lower.contains("voidgloom")) return ENDERMAN;
            if (lower.contains("inferno") || lower.contains("demonlord")) return BLAZE;
            if (lower.contains("riftstalker") || lower.contains("bloodfiend")) return VAMPIRE;
            return null;
        }
    }

    public static class SlayerXp {
        public int level = -1;
        public int xpRemaining = -1;
        public boolean maxed = false;
        public Map<String, Integer> xpPerBoss = new HashMap<>();

        boolean known() {
            return level >= 0 && (maxed || xpRemaining >= 0);
        }
    }

    private static final int PADDING = 6;
    private static final int CORNER_RADIUS = 5;
    private static final int MIN_WIDTH = 110;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;
    private static final int TITLE_GAP = 4;
    private static final int LINE_GAP = 1;

    private static final class Stats {
        int kills;
        int timedKills;
        long totalMs;
        long lastMs = -1;

        long averageMs() {
            return timedKills == 0 ? -1 : totalMs / timedKills;
        }
    }

    private static final Map<String, Stats> stats = new HashMap<>();
    private static final ItemStack DEFAULT_ICON = new ItemStack(Items.PLAYER_HEAD);
    private static final String SAMPLE_SLAYER = "Voidgloom Seraph IV";
    private static final Stats SAMPLE_STATS = new Stats();

    static {
        SAMPLE_STATS.kills = 12;
        SAMPLE_STATS.timedKills = 12;
        SAMPLE_STATS.totalMs = 325_200;
        SAMPLE_STATS.lastMs = 24_300;
    }

    private static String currentSlayer = null;
    private static String lastSlayer = null;
    private static String lastStatus = "";
    private static long spawnAt = 0;
    private static long lastKillAt = 0;

    public static void tick() {
        List<String> lines = SideBarUtils.getSidebarLines();
        if (lines.isEmpty() || !SideBarUtils.isReady()) return;

        String slayer = null;
        String status = "";
        for (int i = 0; i < lines.size(); i++) {
            if (!clean(lines.get(i)).equals("Slayer Quest")) continue;
            if (i + 1 < lines.size()) slayer = clean(lines.get(i + 1));
            if (i + 2 < lines.size()) status = clean(lines.get(i + 2));
            break;
        }

        if (slayer == null || slayer.isEmpty()) {
            currentSlayer = null;
            lastStatus = "";
            spawnAt = 0;
            return;
        }

        if (!slayer.equals(currentSlayer)) {
            currentSlayer = slayer;
            spawnAt = 0;
        }
        lastSlayer = slayer;

        long now = System.currentTimeMillis();
        boolean bossUp = status.equals(BOSS_UP);
        boolean slain = status.equals(BOSS_SLAIN);

        if (bossUp && !lastStatus.isEmpty() && !lastStatus.equals(BOSS_UP)) spawnAt = now;
        if (slain && !lastStatus.equals(BOSS_SLAIN)) recordKill(slayer, now);
        if (!bossUp && !slain) spawnAt = 0;
        lastStatus = status;
    }

    public static void handleMessage(String message) {
        for (String raw : message.split("\n")) {
            String line = raw.trim();
            if (line.contains("NICE! SLAYER BOSS SLAIN!")) {
                if (currentSlayer != null) recordKill(currentSlayer, System.currentTimeMillis());
                continue;
            }
            if (line.contains("SLAYER QUEST FAILED!")) {
                spawnAt = 0;
                continue;
            }

            Matcher needed = XP_NEEDED.matcher(line);
            if (needed.find()) {
                SlayerType type = SlayerType.fromLabel(needed.group(1));
                if (type != null) updateXp(type, Integer.parseInt(needed.group(2)), needed.group(3));
                continue;
            }

            Matcher levelUp = LVL_UP.matcher(line);
            if (levelUp.find()) {
                SlayerType type = SlayerType.fromLabel(levelUp.group(1));
                if (type != null) levelUp(type, Integer.parseInt(levelUp.group(2)));
            }
        }
    }

    private static String xpKey(SlayerType type) {
        String profile = BestiaryHud.profileId();
        return (profile == null ? "" : profile + ":") + type.label;
    }

    private static SlayerXp xp(SlayerType type) {
        if (ModConfig.INSTANCE.slayerXp == null) ModConfig.INSTANCE.slayerXp = new HashMap<>();
        SlayerXp xp = ModConfig.INSTANCE.slayerXp.computeIfAbsent(xpKey(type), _ -> new SlayerXp());
        if (xp.xpPerBoss == null) xp.xpPerBoss = new HashMap<>();
        return xp;
    }

    private static SlayerXp peekXp(SlayerType type) {
        if (ModConfig.INSTANCE.slayerXp == null) return null;
        return ModConfig.INSTANCE.slayerXp.get(xpKey(type));
    }

    private static String tier(String slayer) {
        if (slayer == null) return null;
        int space = slayer.lastIndexOf(' ');
        String tier = space < 0 ? "" : slayer.substring(space + 1);
        for (String t : TIERS) {
            if (t.equals(tier)) return t;
        }
        return null;
    }

    private static int tierIndex(String tier) {
        for (int i = 0; i < TIERS.length; i++) {
            if (TIERS[i].equals(tier)) return i;
        }
        return -1;
    }

    private static void updateXp(SlayerType type, int level, String remainingText) {
        SlayerXp xp = xp(type);
        if (remainingText == null) {
            xp.level = level;
            xp.xpRemaining = 0;
            xp.maxed = true;
            ModConfig.save();
            return;
        }

        int remaining = Integer.parseInt(remainingText.replace(",", ""));
        String slayer = currentSlayer != null ? currentSlayer : lastSlayer;
        String tier = slayer != null && SlayerType.fromBoss(slayer) == type ? tier(slayer) : null;
        if (tier != null && !xp.maxed && xp.level == level && xp.xpRemaining > remaining) {
            xp.xpPerBoss.put(tier, xp.xpRemaining - remaining);
        }

        xp.level = level;
        xp.xpRemaining = remaining;
        xp.maxed = false;
        ModConfig.save();
    }

    private static void levelUp(SlayerType type, int level) {
        SlayerXp xp = xp(type);
        if (xp.level >= level) return;
        xp.level = level;
        xp.maxed = level >= type.milestones.length;
        xp.xpRemaining = xp.maxed ? 0 : type.milestones[level] - (level > 0 ? type.milestones[level - 1] : 0);
        ModConfig.save();
    }

    private static void recordKill(String slayer, long now) {
        if (now - lastKillAt < KILL_DEBOUNCE_MS) return;
        lastKillAt = now;

        Stats s = stats.computeIfAbsent(slayer, _ -> new Stats());
        s.kills++;
        if (spawnAt != 0) {
            long duration = now - spawnAt;
            s.lastMs = duration;
            s.totalMs += duration;
            s.timedKills++;
        }
        spawnAt = 0;
    }

    public static void reset() {
        stats.clear();
        lastSlayer = currentSlayer;
    }

    private static String clean(String line) {
        return line.replaceAll("§.", "").replaceAll("[^\\p{L}\\p{N} '!:()/,.\\-]", "").trim();
    }

    private static String shownSlayer(boolean preview) {
        String slayer = currentSlayer != null ? currentSlayer : lastSlayer;
        if (slayer == null && preview) return SAMPLE_SLAYER;
        return slayer;
    }

    private static Stats shownStats(String slayer, boolean preview) {
        Stats s = stats.get(slayer);
        if (s != null) return s;
        if (preview && currentSlayer == null && lastSlayer == null) return SAMPLE_STATS;
        return new Stats();
    }

    private static ItemStack icon(String slayer) {
        SlayerType type = SlayerType.fromBoss(slayer);
        return type == null ? DEFAULT_ICON : type.icon();
    }

    private static ItemStack skull(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        String value = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", value)));
        UUID id = UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8));
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(new GameProfile(id, "SSUSlayer", properties)));
        return stack;
    }

    private static String number(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static String formatDuration(long ms) {
        if (ms < 0) return "-";
        if (ms < 60_000) return String.format(Locale.ROOT, "%.1fs", ms / 1000.0);
        return String.format(Locale.ROOT, "%d:%04.1f", ms / 60_000, (ms % 60_000) / 1000.0);
    }

    private static Component title(String slayer) {
        return Component.literal(slayer).withColor(ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN));
    }

    private static List<Component> lines(String slayer, boolean preview) {
        Stats s = shownStats(slayer, preview);
        List<Component> lines = new ArrayList<>();
        lines.add(line("Kills: ", String.valueOf(s.kills)));
        lines.add(line("Last Kill: ", formatDuration(s.lastMs)));
        lines.add(line("Average: ", formatDuration(s.averageMs())));
        addXpLines(lines, slayer, preview);
        if (!preview && spawnAt != 0 && slayer.equals(currentSlayer)) {
            lines.add(line("Current: ", formatDuration(System.currentTimeMillis() - spawnAt)));
        }
        return lines;
    }

    private static void addXpLines(List<Component> lines, String slayer, boolean preview) {
        if (preview && currentSlayer == null && lastSlayer == null) {
            lines.add(line("XP: ", "540,622/1,000,000"));
            lines.add(line("Level up in ", "919", " bosses"));
            return;
        }

        SlayerType type = SlayerType.fromBoss(slayer);
        SlayerXp xp = type == null ? null : peekXp(type);
        if (xp == null || !xp.known()) return;

        if (xp.maxed || xp.level >= type.milestones.length) {
            lines.add(line("XP: ", "Maxed"));
            return;
        }

        int next = type.milestones[xp.level];
        lines.add(line("XP: ", number(Math.max(0, next - xp.xpRemaining)) + "/" + number(next)));

        String tier = tier(slayer);
        Integer perBoss = tier == null || xp.xpPerBoss == null ? null : xp.xpPerBoss.get(tier);
        if (perBoss == null && tier != null) {
            int index = tierIndex(tier);
            if (index >= 0 && index < type.xpPerTier.length) perBoss = type.xpPerTier[index];
        }
        if (perBoss != null && perBoss > 0 && xp.xpRemaining > 0) {
            lines.add(line("Level up in ", number((xp.xpRemaining + perBoss - 1) / perBoss), xp.xpRemaining <= perBoss ? " boss" : " bosses"));
        }
    }

    private static Component line(String label, String value, String suffix) {
        return line(label, value).copy().append(Component.literal(suffix).withColor(ModStyle.COLOR_SUBTITLE));
    }

    private static Component line(String label, String value) {
        int text = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TEXT);
        return Component.empty()
                .append(Component.literal(label).withColor(ModStyle.COLOR_SUBTITLE))
                .append(Component.literal(value).withColor(text));
    }

    public static final HudElement ELEMENT = new HudElement("slayer_tracker", "Slayer Tracker") {
        @Override
        public HudLayoutEntry defaultLayout() {
            return new HudLayoutEntry(HudLayoutEntry.Anchor.START, HudLayoutEntry.Anchor.START, 4, 40, 1f);
        }

        @Override
        public int minWidth() {
            return 80;
        }

        @Override
        public int maxWidth() {
            return 300;
        }

        @Override
        public int autoWidth(boolean preview) {
            String slayer = shownSlayer(preview);
            if (slayer == null) return MIN_WIDTH;
            Minecraft mc = Minecraft.getInstance();
            int width = Math.max(MIN_WIDTH, PADDING * 2 + ICON_SIZE + ICON_GAP + mc.font.width(title(slayer)));
            for (Component line : lines(slayer, preview)) width = Math.max(width, PADDING * 2 + mc.font.width(line));
            return width;
        }

        @Override
        public int height(int width, boolean preview) {
            String slayer = shownSlayer(preview);
            int rows = slayer == null ? 0 : lines(slayer, preview).size();
            int lineHeight = Minecraft.getInstance().font.lineHeight + LINE_GAP;
            return PADDING * 2 + ICON_SIZE + TITLE_GAP + rows * lineHeight - LINE_GAP;
        }

        @Override
        public boolean isEnabled() {
            return ModConfig.INSTANCE.slayerTrackerHud;
        }

        @Override
        public boolean isActive() {
            return !StrayersSkyblockUtilsClient.isInSkyblock || shownSlayer(false) == null;
        }

        @Override
        public void render(HudContext ctx) {
            String slayer = shownSlayer(ctx.preview);
            if (slayer == null) return;

            int accent = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
            ctx.panel(0, 0, ctx.width, ctx.height, CORNER_RADIUS, ModStyle.COLOR_BACKGROUND, accent);

            ctx.graphics.fakeItem(icon(slayer), PADDING, PADDING);
            ctx.text(title(slayer), PADDING + ICON_SIZE + ICON_GAP, PADDING + (ICON_SIZE - ctx.font.lineHeight) / 2 + 1, 0xFFFFFFFF, true);

            int y = PADDING + ICON_SIZE + TITLE_GAP;
            for (Component line : lines(slayer, ctx.preview)) {
                ctx.text(line, PADDING, y, 0xFFFFFFFF, true);
                y += ctx.font.lineHeight + LINE_GAP;
            }
        }
    };
}
