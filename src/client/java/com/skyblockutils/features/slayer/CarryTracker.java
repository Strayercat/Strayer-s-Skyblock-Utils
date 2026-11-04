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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CarryTracker {
    private static final Pattern PRICE_PATTERN = Pattern.compile("^([0-9][0-9.,]*)([kmb]?)$");
    private static final Pattern THOUSANDS_PATTERN = Pattern.compile("^\\d{1,3}(,\\d{3})+(\\.\\d+)?$");

    private static final int MISSING_TICKS_TO_COMPLETE = 5;
    private static final double MAX_TRACK_DISTANCE_SQ = 40 * 40;

    private static final int HUD_MIN_WIDTH = 160;
    private static final int HUD_PADDING = 6;
    private static final int HUD_CORNER_RADIUS = 5;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_TRACK_COLOR = 0xFF1A1A1A;

    private static boolean active = false;
    private static String target = "";
    private static Pattern bossPattern = null;
    private static int amount = 0;
    private static int completed = 0;
    private static BigDecimal price = BigDecimal.ZERO;

    private static boolean bossSeen = false;
    private static int missingTicks = 0;
    private static Vec3 lastBossPos = null;
    private static ClientLevel lastLevel = null;

    private static float displayedProgress = 0f;

    public static boolean isHudShowing() {
        return active && StrayersSkyblockUtilsClient.isInSkyblock;
    }

    public static void start(String username, int carryAmount, String priceInput) {
        BigDecimal parsed = parsePrice(priceInput);
        if (parsed == null) {
            ModFunctions.sendSystemMessage("§cInvalid price: " + priceInput + " (examples: 26k, 1.2m, 1,2m, 1,200,000)", false);
            return;
        }

        if (active) {
            ModFunctions.sendSystemMessage("§eReplaced carry for " + target + " (" + completed + "/" + amount + ")", false);
        }

        active = true;
        target = username;
        bossPattern = Pattern.compile("(?:^|[^A-Za-z0-9_])" + Pattern.quote(username) + "'s SLAYER BOSS", Pattern.CASE_INSENSITIVE);
        amount = carryAmount;
        completed = 0;
        price = parsed;
        displayedProgress = 0f;
        resetBossState();
        lastLevel = Minecraft.getInstance().level;

        ModFunctions.sendSystemMessage("§aStarted carrying " + username + ": " + carryAmount + "x at "
                + formatCoins(parsed) + " each (" + formatCoins(total()) + " total)", false);
    }

    public static void stop() {
        if (!active) {
            ModFunctions.sendSystemMessage("§cYou aren't carrying anyone.", false);
            return;
        }
        ModFunctions.sendSystemMessage("§cStopped carrying " + target + " (" + completed + "/" + amount + ", "
                + formatCoins(price.multiply(BigDecimal.valueOf(completed))) + " owed)", false);
        reset();
    }

    public static void reset() {
        active = false;
        target = "";
        bossPattern = null;
        amount = 0;
        completed = 0;
        price = BigDecimal.ZERO;
        displayedProgress = 0f;
        resetBossState();
        lastLevel = null;
    }

    private static void resetBossState() {
        bossSeen = false;
        missingTicks = 0;
        lastBossPos = null;
    }

    public static void tick(Minecraft client) {
        if (!active || client.level == null || client.player == null) return;

        if (client.level != lastLevel) {
            lastLevel = client.level;
            resetBossState();
            return;
        }

        Entity boss = findBossTag(client.level);
        if (boss != null) {
            bossSeen = true;
            missingTicks = 0;
            lastBossPos = boss.position();
            return;
        }

        if (!bossSeen) return;

        if (lastBossPos != null && client.player.position().distanceToSqr(lastBossPos) > MAX_TRACK_DISTANCE_SQ) {
            resetBossState();
            return;
        }

        if (++missingTicks >= MISSING_TICKS_TO_COMPLETE) {
            resetBossState();
            completeCarry(client);
        }
    }

    private static Entity findBossTag(ClientLevel level) {
        for (Entity entity : level.entitiesForRendering()) {
            Component name = entity.getCustomName();
            if (name == null) continue;
            String clean = name.getString().replaceAll("§.", "");
            if (bossPattern.matcher(clean).find()) return entity;
        }
        return null;
    }

    private static void completeCarry(Minecraft client) {
        completed++;

        if (completed < amount) {
            ModFunctions.sendSystemMessage("§aCarry completed for " + target + " (" + completed + "/" + amount + ")", false);
            return;
        }

        String owed = formatCoins(total());
        ModFunctions.showTitle(client, Component.literal("COLLECT PAYMENT")
                .withColor(ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN)), 60, true);
        OnScreenNotification.builder()
                .title("Carries Completed")
                .subtitle("Finished " + amount + "/" + amount + " carries for " + target + "\nCollect " + owed + " coins")
                .tickTime(200)
                .withSound(false)
                .send();
        ModFunctions.sendSystemMessage("§aAll " + amount + " carries done for " + target + "! Collect §6" + owed + " coins", false);
        reset();
    }

    private static BigDecimal total() {
        return price.multiply(BigDecimal.valueOf(amount));
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
            return Math.max(HUD_MIN_WIDTH, Minecraft.getInstance().font.width(header(preview)) + HUD_PADDING * 2);
        }

        @Override
        public int height(int width, boolean preview) {
            Minecraft mc = Minecraft.getInstance();
            int lines = Math.max(1, mc.font.split(header(preview), Math.max(1, width - HUD_PADDING * 2)).size());
            return HUD_PADDING + lines * (mc.font.lineHeight + 1) + 2 + BAR_HEIGHT + HUD_PADDING;
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

    private static Component header(boolean preview) {
        boolean sample = preview && !active;
        String name = sample ? "Player" : target;
        int done = sample ? 2 : completed;
        int total = sample ? 5 : amount;
        int text = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TEXT);
        return Component.empty()
                .append(Component.literal("Carrying ").withColor(ModStyle.COLOR_TITLE))
                .append(Component.literal(name).withColor(text))
                .append(Component.literal(" - ").withColor(ModStyle.COLOR_SUBTITLE))
                .append(Component.literal(done + "/" + total).withColor(ModStyle.COLOR_TITLE));
    }

    private static void renderHud(HudContext ctx) {
        boolean sample = ctx.preview && !active;
        ModStyle.ColorStyle style = ModConfig.INSTANCE.colorStyle;
        int accent = ModStyle.getColor(style, ModStyle.ColorType.MAIN);
        int gradStart = 0xFF000000 | ModStyle.getColor(style, ModStyle.ColorType.TITLE_START);
        int gradEnd = 0xFF000000 | ModStyle.getColor(style, ModStyle.ColorType.TITLE_END);

        ctx.panel(0, 0, ctx.width, ctx.height, HUD_CORNER_RADIUS, ModStyle.COLOR_BACKGROUND, accent);

        int lineStep = ctx.font.lineHeight + 1;
        int y = HUD_PADDING;
        for (FormattedCharSequence line : ctx.wrap(header(ctx.preview), ctx.width - HUD_PADDING * 2)) {
            ctx.text(line, ctx.width / 2 - ctx.font.width(line) / 2, y, 0xFFFFFFFF, true);
            y += lineStep;
        }

        int barX1 = HUD_PADDING;
        int barX2 = ctx.width - HUD_PADDING;
        int barY1 = ctx.height - HUD_PADDING - BAR_HEIGHT;
        int barY2 = barY1 + BAR_HEIGHT;
        int barWidth = barX2 - barX1;

        float progress;
        if (sample) {
            progress = 0.4f;
        } else {
            float targetProgress = amount <= 0 ? 0f : Math.min(1f, completed / (float) amount);
            displayedProgress += (targetProgress - displayedProgress) * 0.15f;
            if (Math.abs(targetProgress - displayedProgress) < 0.001f) displayedProgress = targetProgress;
            progress = displayedProgress;
        }

        ctx.fill(barX1, barY1, barX2, barY2, BAR_TRACK_COLOR);

        int filled = Math.round(barWidth * progress);
        for (int i = 0; i < filled; i++) {
            float t = barWidth <= 1 ? 0f : i / (float) (barWidth - 1);
            ctx.fill(barX1 + i, barY1, barX1 + i + 1, barY2, lerpColor(gradStart, gradEnd, t));
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
