package com.skyblockutils.features.wiki;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.ModKeyBindings;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.ModStyle;
import com.skyblockutils.utils.TextUtils;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

public final class WikiLookup {
    private static final String SEARCH_URL = "https://hypixelskyblock.minecraft.wiki/index.php?search=";
    private static final String HOME_URL = "https://hypixelskyblock.minecraft.wiki/";
    private static final Pattern STACK_COUNT = Pattern.compile("\\s+x\\d+$");
    private static final Pattern PET_LEVEL = Pattern.compile("^\\[Lvl \\d+]\\s*");
    private static final Pattern DECORATIONS = Pattern.compile("[✪➊➋➌➍➎⚚✿]");
    private static final String BROWSER_MOD = "mcef-modern";

    private static boolean preloaded;

    private WikiLookup() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, _, _, lines) -> {
            if (!ModConfig.INSTANCE.wikiTooltipHint || !StrayersSkyblockUtilsClient.isInSkyblock) return;
            if (ModKeyBindings.WIKI_KEY.isUnbound() || !lookable(stack)) return;
            int main = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
            lines.add(Component.literal("[SSU] ").withColor(main)
                    .append(Component.literal("Press ").withStyle(ChatFormatting.GRAY))
                    .append(ModKeyBindings.WIKI_KEY.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" to open wiki").withStyle(ChatFormatting.GRAY)));
        });
    }

    public static void tick(Minecraft client) {
        if (!preloaded && ModConfig.INSTANCE.wikiInGame && ModConfig.INSTANCE.wikiPreload && FabricLoader.getInstance().isModLoaded(BROWSER_MOD)) {
            preloaded = true;
            WikiBrowserScreen.preload();
        }
        while (ModKeyBindings.WIKI_KEY.consumeClick()) {
            if (client.player != null && client.gui.screen() == null) open(client.player.getMainHandItem(), null);
        }
    }

    public static void open(ItemStack stack, Screen parent) {
        if (stack == null || stack.isEmpty() || isMenuButton(stack)) {
            openUrl(HOME_URL, parent);
            return;
        }

        String query = query(stack);
        openUrl(query.isEmpty() ? HOME_URL : SEARCH_URL + URLEncoder.encode(query, StandardCharsets.UTF_8), parent);
    }

    private static void openUrl(String url, Screen parent) {
        if (ModConfig.INSTANCE.wikiInGame && FabricLoader.getInstance().isModLoaded(BROWSER_MOD)) {
            WikiBrowserScreen.open(url, parent);
        } else {
            openExternal(url);
        }
    }

    public static void openExternal(String url) {
        try {
            Util.getPlatform().openUri(URI.create(url));
        } catch (Exception e) {
            ModFunctions.sendSystemMessage("§cCouldn't open the wiki: " + e.getMessage(), false);
        }
    }

    private static String skyblockId(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getStringOr("id", "");
    }

    private static boolean lookable(ItemStack stack) {
        return !skyblockId(stack).isEmpty() && !isMenuButton(stack);
    }

    private static boolean isMenuButton(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        for (Component line : lore.lines()) {
            if (TextUtils.clean(line).toLowerCase(Locale.ROOT).startsWith("click to view")) return true;
        }
        return false;
    }

    private static String originalName(ItemStack stack) {
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name == null) name = stack.get(DataComponents.ITEM_NAME);
        if (name == null) name = stack.getHoverName();
        return name.getString();
    }

    private static String query(ItemStack stack) {
        String name = TextUtils.strip(originalName(stack));
        name = DECORATIONS.matcher(name).replaceAll("");
        name = PET_LEVEL.matcher(name.trim()).replaceFirst("");
        name = STACK_COUNT.matcher(name).replaceFirst("");

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        String id = "";
        if (data != null) {
            CompoundTag tag = data.copyTag();
            id = tag.getStringOr("id", "");
            String modifier = tag.getStringOr("modifier", "").replace('_', ' ');
            if (!modifier.isEmpty() && name.toLowerCase(Locale.ROOT).startsWith(modifier.toLowerCase(Locale.ROOT) + " ")) {
                name = name.substring(modifier.length() + 1);
            }
        }

        name = name.replaceAll("\\s+", " ").trim();
        if (!name.isEmpty()) return name;
        return id.replace('_', ' ').toLowerCase(Locale.ROOT);
    }
}
