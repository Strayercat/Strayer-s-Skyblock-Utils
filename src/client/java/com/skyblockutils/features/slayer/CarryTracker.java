package com.skyblockutils.features.slayer;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.hud.SsuHud;
import com.skyblockutils.features.hud.layout.HudContext;
import com.skyblockutils.features.hud.layout.HudElement;
import com.skyblockutils.features.hud.layout.HudLayoutEntry;
import com.skyblockutils.features.hud.layout.HudManager;
import com.skyblockutils.utils.ModStyle;
import com.skyblockutils.utils.OnScreenNotification;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CarryTracker {
    private static final Pattern PRICE_PATTERN = Pattern.compile("^([0-9][0-9.,]*)([kmb]?)$");
    private static final Pattern THOUSANDS_PATTERN = Pattern.compile("^\\d{1,3}(,\\d{3})+(\\.\\d+)?$");

    private static final int MISSING_TICKS_TO_COMPLETE = 20;
    private static final double MAX_TRACK_DISTANCE_SQ = 40 * 40;
    private static final double BOSS_SEARCH_RADIUS_SQ = 50 * 50;
    private static final long DEATH_WINDOW_MS = 3_000;

    private static final int HUD_MIN_WIDTH = 160;
    private static final int HUD_PADDING = 6;
    private static final int HUD_CORNER_RADIUS = 5;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_GAP = 2;
    private static final int CARRY_GAP = 5;
    private static final int BAR_TRACK_COLOR = 0xFF1A1A1A;

    private static final class Carry {
        final String target;
        final Pattern bossPattern;
        final Pattern namePattern;
        final int amount;
        final BigDecimal price;
        int completed = 0;
        boolean bossSeen = false;
        int missingTicks = 0;
        Vec3 lastBossPos = null;
        long missingSince = 0;
        long lastDeathAt = 0;
        float displayedProgress = 0f;

        Carry(String target, int amount, BigDecimal price) {
            this.target = target;
            this.bossPattern = Pattern.compile("(?:^|[^A-Za-z0-9_])" + Pattern.quote(target) + "'s SLAYER BOSS", Pattern.CASE_INSENSITIVE);
            this.namePattern = Pattern.compile("(?:^|[^A-Za-z0-9_])" + Pattern.quote(target) + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE);
            this.amount = amount;
            this.price = price;
        }

        Carry(String target, int amount, int completed) {
            this(target, amount, BigDecimal.ZERO);
            this.completed = completed;
            this.displayedProgress = completed / (float) amount;
        }

        void resetBossState() {
            bossSeen = false;
            missingTicks = 0;
            lastBossPos = null;
            missingSince = 0;
        }

        BigDecimal total() {
            return price.multiply(BigDecimal.valueOf(amount));
        }

        BigDecimal owed() {
            return price.multiply(BigDecimal.valueOf(completed));
        }
    }

    private static final Map<String, Carry> carries = new LinkedHashMap<>();
    private static final List<Carry> SAMPLE = List.of(new Carry("Player", 5, 2), new Carry("Teammate", 3, 1));
    private static ClientLevel lastLevel = null;

    private static String key(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    public static boolean isHudShowing() {
        return !carries.isEmpty() && StrayersSkyblockUtilsClient.isInSkyblock;
    }

    public static List<String> activeTargets() {
        return carries.values().stream().map(c -> c.target).toList();
    }

    public static void start(String username, int carryAmount, String priceInput) {
        BigDecimal parsed = parsePrice(priceInput);
        if (parsed == null) {
            ModFunctions.sendSystemMessage("§cInvalid price: " + priceInput + " (examples: 26k, 1.2m, 1,2m, 1,200,000)", false);
            return;
        }

        Carry previous = carries.get(key(username));
        if (previous != null) {
            ModFunctions.sendSystemMessage("§eReplaced carry for " + previous.target + " (" + previous.completed + "/" + previous.amount + ")", false);
        }

        Carry carry = new Carry(username, carryAmount, parsed);
        carries.put(key(username), carry);
        if (carries.size() == 1) lastLevel = Minecraft.getInstance().level;

        ModFunctions.sendSystemMessage("§aStarted carrying " + username + ": " + carryAmount + "x at "
                + formatCoins(parsed) + " each (" + formatCoins(carry.total()) + " total)", false);
    }

    public static void stop(String username) {
        if (carries.isEmpty()) {
            ModFunctions.sendSystemMessage("§cYou aren't carrying anyone.", false);
            return;
        }

        Carry carry;
        if (username == null) {
            if (carries.size() > 1) {
                ModFunctions.sendSystemMessage("§cYou're carrying multiple players, use /ssu carry stop <username> ("
                        + String.join(", ", activeTargets()) + ")", false);
                return;
            }
            carry = carries.values().iterator().next();
        } else {
            carry = carries.get(key(username));
            if (carry == null) {
                ModFunctions.sendSystemMessage("§cYou aren't carrying " + username + ".", false);
                return;
            }
        }

        carries.remove(key(carry.target));
        ModFunctions.sendSystemMessage("§cStopped carrying " + carry.target + " (" + carry.completed + "/" + carry.amount + ", "
                + formatCoins(carry.owed()) + " owed)", false);
    }

    public static void reset() {
        carries.clear();
        lastLevel = null;
    }

    public static void handleMessage(String message) {
        if (carries.isEmpty() || !message.startsWith("☠")) return;
        long now = System.currentTimeMillis();
        for (Carry carry : carries.values()) {
            if (carry.namePattern.matcher(message).find()) carry.lastDeathAt = now;
        }
    }

    public static void tick(Minecraft client) {
        if (carries.isEmpty() || client.level == null || client.player == null) return;

        if (client.level != lastLevel) {
            lastLevel = client.level;
            carries.values().forEach(Carry::resetBossState);
            return;
        }

        List<Carry> finished = new ArrayList<>();
        for (Carry carry : carries.values()) {
            Entity boss = findBossTag(client.level, carry.bossPattern, carry.bossSeen ? carry.lastBossPos : null);
            if (boss != null) {
                carry.bossSeen = true;
                carry.missingTicks = 0;
                carry.missingSince = 0;
                carry.lastBossPos = boss.position();
                continue;
            }

            if (!carry.bossSeen) continue;

            if (carry.lastBossPos != null && client.player.position().distanceToSqr(carry.lastBossPos) > MAX_TRACK_DISTANCE_SQ) {
                carry.resetBossState();
                continue;
            }

            long now = System.currentTimeMillis();
            if (carry.missingTicks++ == 0) carry.missingSince = now;
            if (carry.missingTicks < MISSING_TICKS_TO_COMPLETE) continue;

            boolean died = carry.lastDeathAt != 0 && carry.lastDeathAt >= carry.missingSince - DEATH_WINDOW_MS;
            carry.resetBossState();
            if (died) {
                carry.lastDeathAt = 0;
                ModFunctions.sendSystemMessage("§c" + carry.target + " died, carry not counted (" + carry.completed + "/" + carry.amount + ")", false);
                continue;
            }
            if (completeCarry(client, carry)) finished.add(carry);
        }

        for (Carry carry : finished) carries.remove(key(carry.target));
    }

    private static Entity findBossTag(ClientLevel level, Pattern bossPattern, Vec3 near) {
        for (Entity entity : level.entitiesForRendering()) {
            if (near != null && entity.position().distanceToSqr(near) > BOSS_SEARCH_RADIUS_SQ) continue;
            Component name = entity.getCustomName();
            if (name == null) continue;
            String clean = name.getString().replaceAll("§.", "");
            if (bossPattern.matcher(clean).find()) return entity;
        }
        return null;
    }

    private static boolean completeCarry(Minecraft client, Carry carry) {
        carry.completed++;

        if (carry.completed < carry.amount) {
            ModFunctions.sendSystemMessage("§aCarry completed for " + carry.target + " (" + carry.completed + "/" + carry.amount + ")", false);
            return false;
        }

        String owed = formatCoins(carry.total());
        ModFunctions.showTitle(client, Component.literal("COLLECT PAYMENT")
                .withColor(ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN)), 60, true);
        OnScreenNotification.builder()
                .title("Carries Completed")
                .subtitle("Finished " + carry.amount + "/" + carry.amount + " carries for " + carry.target + "\nCollect " + owed + " coins")
                .tickTime(200)
                .withSound(false)
                .send();
        ModFunctions.sendSystemMessage("§aAll " + carry.amount + " carries done for " + carry.target + "! Collect §6" + owed + " coins", false);
        return true;
    }

    public static BigDecimal parsePrice(String input) {
        if (input == null) return null;
        String s = input.trim().toLowerCase().replace(" ", "").replace("_", "");
        if (s.endsWith("coins")) s = s.substring(0, s.length() - 5);

        Matcher m = PRICE_PATTERN.matcher(s);
        if (!m.matches()) return null;

        String number = m.group(1);
        String suffix = m.group(2);

        if (THOUSANDS_PATTERN.matcher(number).matches()) {
            number = number.replace(",", "");
        } else {
            number = number.replace(",", ".");
        }

        if (number.indexOf('.') != number.lastIndexOf('.') || number.endsWith(".")) return null;

        BigDecimal value;
        try {
            value = new BigDecimal(number);
        } catch (NumberFormatException e) {
            return null;
        }

        BigDecimal multiplier = switch (suffix) {
            case "k" -> BigDecimal.valueOf(1_000L);
            case "m" -> BigDecimal.valueOf(1_000_000L);
            case "b" -> BigDecimal.valueOf(1_000_000_000L);
            default -> BigDecimal.ONE;
        };

        value = value.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
        return value.signum() > 0 ? value : null;
    }

    public static String formatCoins(BigDecimal coins) {
        String[] suffixes = {"", "k", "M", "B", "T"};
        BigDecimal value = coins;
        int index = 0;
        BigDecimal thousand = BigDecimal.valueOf(1000);
        while (value.compareTo(thousand) >= 0 && index < suffixes.length - 1) {
            value = value.divide(thousand, 10, RoundingMode.HALF_UP);
            index++;
        }
        BigDecimal rounded = value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.toPlainString() + suffixes[index];
    }

    public static final HudElement ELEMENT = new HudElement("carry_tracker", "Slayer Carry Tracker") {
        @Override
        public HudLayoutEntry defaultLayout() {
            return new HudLayoutEntry(HudLayoutEntry.Anchor.CENTER, HudLayoutEntry.Anchor.START, 0, 0, 1f);
        }

        @Override
        public int minWidth() {
            return 80;
        }

        @Override
        public int maxWidth() {
            return 400;
        }

        @Override
        public int autoWidth(boolean preview) {
            Minecraft mc = Minecraft.getInstance();
            int width = HUD_MIN_WIDTH;
            for (Carry carry : shown(preview)) width = Math.max(width, mc.font.width(header(carry)) + HUD_PADDING * 2);
            return width;
        }

        @Override
        public int height(int width, boolean preview) {
            Minecraft mc = Minecraft.getInstance();
            List<Carry> list = shown(preview);
            int height = HUD_PADDING * 2 + CARRY_GAP * Math.max(0, list.size() - 1);
            for (Carry carry : list) height += carryHeight(mc, carry, width);
            return height;
        }

        @Override
        public boolean isActive() {
            if (!isHudShowing()) return true;
            if (!SsuHud.ELEMENT.isEnabled() || SsuHud.ELEMENT.isActive()) return false;
            return HudManager.bounds(SsuHud.ELEMENT, false).intersects(HudManager.bounds(this, false));
        }

        @Override
        public void render(HudContext ctx) {
            renderHud(ctx);
        }
    };

    private static List<Carry> shown(boolean preview) {
        if (preview && carries.isEmpty()) return SAMPLE;
        return new ArrayList<>(carries.values());
    }

    private static int carryHeight(Minecraft mc, Carry carry, int width) {
        int lines = Math.max(1, mc.font.split(header(carry), Math.max(1, width - HUD_PADDING * 2)).size());
        return lines * (mc.font.lineHeight + 1) + BAR_GAP + BAR_HEIGHT;
    }

    private static Component header(Carry carry) {
        int text = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TEXT);
        return Component.empty()
                .append(Component.literal("Carrying ").withColor(ModStyle.COLOR_TITLE))
                .append(Component.literal(carry.target).withColor(text))
                .append(Component.literal(" - ").withColor(ModStyle.COLOR_SUBTITLE))
                .append(Component.literal(carry.completed + "/" + carry.amount).withColor(ModStyle.COLOR_TITLE));
    }

    private static void renderHud(HudContext ctx) {
        ModStyle.ColorStyle style = ModConfig.INSTANCE.colorStyle;
        int accent = ModStyle.getColor(style, ModStyle.ColorType.MAIN);
        int gradStart = 0xFF000000 | ModStyle.getColor(style, ModStyle.ColorType.TITLE_START);
        int gradEnd = 0xFF000000 | ModStyle.getColor(style, ModStyle.ColorType.TITLE_END);

        ctx.panel(0, 0, ctx.width, ctx.height, HUD_CORNER_RADIUS, ModStyle.COLOR_BACKGROUND, accent);

        int lineStep = ctx.font.lineHeight + 1;
        int barX1 = HUD_PADDING;
        int barX2 = ctx.width - HUD_PADDING;
        int barWidth = barX2 - barX1;
        int y = HUD_PADDING;

        for (Carry carry : shown(ctx.preview)) {
            for (FormattedCharSequence line : ctx.wrap(header(carry), ctx.width - HUD_PADDING * 2)) {
                ctx.text(line, ctx.width / 2 - ctx.font.width(line) / 2, y, 0xFFFFFFFF, true);
                y += lineStep;
            }

            y += BAR_GAP;
            float target = carry.amount <= 0 ? 0f : Math.min(1f, carry.completed / (float) carry.amount);
            carry.displayedProgress += (target - carry.displayedProgress) * 0.15f;
            if (Math.abs(target - carry.displayedProgress) < 0.001f) carry.displayedProgress = target;

            ctx.fill(barX1, y, barX2, y + BAR_HEIGHT, BAR_TRACK_COLOR);
            int filled = Math.round(barWidth * carry.displayedProgress);
            for (int i = 0; i < filled; i++) {
                float t = barWidth <= 1 ? 0f : i / (float) (barWidth - 1);
                ctx.fill(barX1 + i, y, barX1 + i + 1, y + BAR_HEIGHT, lerpColor(gradStart, gradEnd, t));
            }
            y += BAR_HEIGHT + CARRY_GAP;
        }
    }

    private static int lerpColor(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ra = Math.round(aa + (ba - aa) * t);
        int rr = Math.round(ar + (br - ar) * t);
        int rg = Math.round(ag + (bg - ag) * t);
        int rb = Math.round(ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }
}