package com.skyblockutils.features.inventory;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.skyblockutils.features.chat.emoji.EmojiRegistry;
import com.skyblockutils.mixin.client.ModelManagerAccessor;
import com.skyblockutils.utils.SSU;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ButtonIcons {
    public record Icon(ItemStack item, Identifier sprite, String glyph) {
        public static final Icon NONE = new Icon(ItemStack.EMPTY, null, null);

        public boolean isEmpty() {
            return sprite == null && glyph == null && item.isEmpty();
        }
    }

    private static final Map<String, Icon> CACHE = new HashMap<>();
    private static Map<String, EmojiRegistry.Emoji> emojis;
    private static ItemStack ownHead;
    private static Map<Identifier, ItemModel> lastModels;
    private static int generation;

    private ButtonIcons() {
    }

    public static Icon icon(String spec) {
        if (spec == null || spec.isBlank()) return Icon.NONE;
        checkReload();
        String key = spec.trim();
        if (key.equalsIgnoreCase("self")) return new Icon(ownHead(), null, null);
        Icon cached = CACHE.get(key);
        if (cached != null) return cached;
        Icon icon = parse(key);
        String lower = key.toLowerCase(Locale.ROOT);
        boolean pending = (lower.startsWith("skyblock:") && SkyblockItems.state() != SkyblockItems.State.READY)
                || (lower.startsWith("stack:") && registries() == null);
        if (!pending) CACHE.put(key, icon);
        return icon;
    }

    private static Icon parse(String spec) {
        int colon = spec.indexOf(':');
        String type = colon < 0 ? "item" : spec.substring(0, colon).toLowerCase(Locale.ROOT);
        String value = colon < 0 ? spec : spec.substring(colon + 1);
        try {
            return switch (type) {
                case "sprite" -> new Icon(ItemStack.EMPTY, value.contains(":") ? Identifier.parse(value) : SSU.id(value), null);
                case "head" -> new Icon(head(ResolvableProfile.createUnresolved(value)), null, null);
                case "texture" -> new Icon(textureHead(value), null, null);
                case "skyblock" -> {
                    SkyblockItems.SbItem item = SkyblockItems.get(value);
                    yield new Icon(item == null ? new ItemStack(Items.PLAYER_HEAD) : SkyblockItems.stack(item), null, null);
                }
                case "emoji" -> {
                    EmojiRegistry.Emoji emoji = emoji(value);
                    yield emoji == null ? barrier() : new Icon(ItemStack.EMPTY, null, emoji.text());
                }
                case "stack" -> {
                    ItemStack stack = decodeStack(value);
                    yield stack == null ? barrier() : new Icon(stack, null, null);
                }
                case "item" -> new Icon(item(value), null, null);
                default -> new Icon(item(spec), null, null);
            };
        } catch (RuntimeException e) {
            return barrier();
        }
    }

    private static Icon barrier() {
        return new Icon(new ItemStack(Items.BARRIER), null, null);
    }

    public static EmojiRegistry.Emoji emoji(String name) {
        if (emojis == null) {
            emojis = new HashMap<>();
            for (EmojiRegistry.Emoji emoji : EmojiRegistry.normal()) for (String n : emoji.names()) emojis.putIfAbsent(n, emoji);
            for (EmojiRegistry.Emoji emoji : EmojiRegistry.custom()) for (String n : emoji.names()) emojis.put(n, emoji);
        }
        return emojis.get(name.toLowerCase(Locale.ROOT).replace(":", ""));
    }

    private static ItemStack item(String id) {
        Identifier identifier = Identifier.tryParse(id.toLowerCase(Locale.ROOT));
        if (identifier == null || !BuiltInRegistries.ITEM.containsKey(identifier)) return new ItemStack(Items.BARRIER);
        Item item = BuiltInRegistries.ITEM.getValue(identifier);
        return item == Items.AIR ? new ItemStack(Items.BARRIER) : new ItemStack(item);
    }

    private static ItemStack head(ResolvableProfile profile) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.PROFILE, profile);
        return stack;
    }

    public static ItemStack textureHead(String value) {
        PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", value)));
        UUID id = UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
        return head(ResolvableProfile.createResolved(new GameProfile(id, "SSUButton", properties)));
    }

    public static ItemStack ownHead() {
        if (ownHead != null) return ownHead;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) return new ItemStack(Items.PLAYER_HEAD);
        PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
        if (info == null) return new ItemStack(Items.PLAYER_HEAD);
        ownHead = head(ResolvableProfile.createResolved(info.getProfile()));
        return ownHead;
    }

    public static void resetHead() {
        ownHead = null;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static String specOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        boolean custom = stack.has(DataComponents.CUSTOM_DATA) || stack.has(DataComponents.ITEM_MODEL) || stack.has(DataComponents.PROFILE);
        if (custom) {
            String encoded = encodeStack(stack);
            if (encoded != null) return "stack:" + encoded;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return "item:" + (id.getNamespace().equals("minecraft") ? id.getPath() : id.toString());
    }

    private static HolderLookup.Provider registries() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) return client.level.registryAccess();
        return client.getConnection() == null ? null : client.getConnection().registryAccess();
    }

    private static String encodeStack(ItemStack stack) {
        HolderLookup.Provider provider = registries();
        if (provider == null) return null;
        ItemStack copy = stack.copyWithCount(1);
        copy.remove(DataComponents.LORE);
        return ItemStack.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, provider), copy)
                .result()
                .map(json -> Base64.getEncoder().encodeToString(json.toString().getBytes(StandardCharsets.UTF_8)))
                .orElse(null);
    }

    private static ItemStack decodeStack(String value) {
        HolderLookup.Provider provider = registries();
        if (provider == null) return null;
        JsonElement json = JsonParser.parseString(new String(Base64.getDecoder().decode(value.trim()), StandardCharsets.UTF_8));
        return ItemStack.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, provider), json).result().orElse(null);
    }

    static int generation() {
        return generation;
    }

    static boolean hasItemModel(Identifier id) {
        Map<Identifier, ItemModel> models = ((ModelManagerAccessor) Minecraft.getInstance().getModelManager()).getBakedItemStackModels();
        return models != null && models.containsKey(id);
    }

    static void checkReload() {
        Map<Identifier, ItemModel> models = ((ModelManagerAccessor) Minecraft.getInstance().getModelManager()).getBakedItemStackModels();
        if (models == lastModels) return;
        lastModels = models;
        generation++;
        CACHE.clear();
        SkyblockItems.clearStacks();
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, Icon icon, int x, int y, int size) {
        if (icon.sprite() != null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon.sprite(), x, y, size, size);
            return;
        }
        if (icon.glyph() != null) {
            float scale = size / 9f;
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(scale, scale);
            int width = font.width(icon.glyph());
            graphics.text(font, Component.literal(icon.glyph()), Math.round((9 - width) / 2f), 1, 0xFFFFFFFF, false);
            graphics.pose().popMatrix();
            return;
        }
        if (icon.item().isEmpty()) return;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(size / 16f, size / 16f);
        graphics.fakeItem(icon.item(), 0, 0);
        graphics.pose().popMatrix();
    }
}
