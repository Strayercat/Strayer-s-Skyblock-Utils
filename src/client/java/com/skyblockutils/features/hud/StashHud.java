package com.skyblockutils.features.hud;

import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.hud.layout.HudContext;
import com.skyblockutils.features.hud.layout.HudElement;
import com.skyblockutils.features.hud.layout.HudLayoutEntry;
import com.skyblockutils.utils.ModStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StashHud {
    private static final Pattern AMOUNT = Pattern.compile("You have ([\\d,]+) (items?|materials?) stashed away!");
    private static final Pattern PICKUP = Pattern.compile("^From stash: .+$");
    private static final Pattern STASH_LINE = Pattern.compile("^\\s*(?:You have [\\d,]+ (?:items?|materials?) stashed away!|\\(This totals [\\d,]+ types? of (?:items?|materials?) stashed!\\)|>>> CLICK HERE to pick (?:them|it) up! <<<)\\s*$");

    private static final int PADDING = 6;
    private static final int CORNER_RADIUS = 5;
    private static final int MIN_WIDTH = 80;
    private static final int LINE_GAP = 2;
    private static final int TITLE_GAP = 3;

    private static int materials = 0;
    private static int items = 0;

    private StashHud() {
    }

    public static final HudElement ELEMENT = new HudElement("stash_hud", "Stash HUD") {
        @Override
        public HudLayoutEntry defaultLayout() {
            return new HudLayoutEntry(HudLayoutEntry.Anchor.END, HudLayoutEntry.Anchor.CENTER, 4, 0, 1f);
        }

        @Override
        public int maxWidth() {
            return 300;
        }

        @Override
        public int autoWidth(boolean preview) {
            Minecraft mc = Minecraft.getInstance();
            int width = MIN_WIDTH;
            for (Component line : lines(preview)) width = Math.max(width, mc.font.width(line) + PADDING * 2);
            return width;
        }

        @Override
        public int height(int width, boolean preview) {
            int lineHeight = Minecraft.getInstance().font.lineHeight;
            int entries = lines(preview).size() - 1;
            return PADDING * 2 + lineHeight * (entries + 1) + TITLE_GAP + LINE_GAP * Math.max(0, entries - 1);
        }

        @Override
        public boolean isEnabled() {
            return ModConfig.INSTANCE.stashHud;
        }

        @Override
        public boolean isActive() {
            return !StrayersSkyblockUtilsClient.isInSkyblock || (materials == 0 && items == 0);
        }

        @Override
        public void render(HudContext ctx) {
            int accent = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
            ctx.panel(0, 0, ctx.width, ctx.height, CORNER_RADIUS, ModStyle.COLOR_BACKGROUND, accent);

            List<Component> lines = lines(ctx.preview);
            int y = PADDING;
            Component title = lines.getFirst();
            ctx.text(title, ctx.width / 2 - ctx.font.width(title) / 2, y, 0xFFFFFFFF, true);
            y += ctx.font.lineHeight + TITLE_GAP;
            for (int i = 1; i < lines.size(); i++) {
                ctx.text(lines.get(i), PADDING, y, 0xFFFFFFFF, true);
                y += ctx.font.lineHeight + LINE_GAP;
            }
        }
    };

    public static boolean handleMessage(String message) {
        if (PICKUP.matcher(message).matches()) {
            items = Math.max(0, items - 1);
            return true;
        }

        boolean stashMessage = false;
        for (String line : message.split("\n")) {
            if (!STASH_LINE.matcher(line).matches()) {
                if (!line.isBlank()) return true;
                continue;
            }
            stashMessage = true;
            Matcher amount = AMOUNT.matcher(line);
            if (!amount.find()) continue;
            int value = parse(amount.group(1));
            if (amount.group(2).startsWith("material")) materials = value;
            else items = value;
        }
        return !stashMessage || !ModConfig.INSTANCE.stashHud;
    }

    public static void reset() {
        materials = 0;
        items = 0;
    }

    private static int parse(String number) {
        try {
            return Integer.parseInt(number.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static List<Component> lines(boolean preview) {
        boolean sample = preview && materials == 0 && items == 0;
        int accent = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
        int shownMaterials = sample ? 1024 : materials;
        int shownItems = sample ? 12 : items;
        List<Component> lines = new ArrayList<>(3);
        lines.add(Component.literal("Stashes").withColor(accent));
        if (shownMaterials > 0) lines.add(entry("Material", shownMaterials));
        if (shownItems > 0) lines.add(entry("Item", shownItems));
        return lines;
    }

    private static Component entry(String name, int amount) {
        int text = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TEXT);
        return Component.empty()
                .append(Component.literal(name + ": ").withColor(ModStyle.COLOR_TITLE))
                .append(Component.literal(String.format("%,d", amount)).withColor(text));
    }
}
