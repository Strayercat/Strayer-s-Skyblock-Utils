package com.skyblockutils.render;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EggDetector {
    private static final String EGG_SIGNATURE = "EGG_HUNT";
    private static final String EGG_TEXTURE_URL = "http://textures.minecraft.net/texture/98c5530c2c856e30b11fb22c711117b073fd8333ae06c9fd1c697c4af4a0ccc9";
    public static final ResolvableProfile EGG_PROFILE = createEggProfile();
    private static final Set<BlockPos> EGGS = ConcurrentHashMap.newKeySet();
    private static @Nullable ClientLevel lastLevel;

    private EggDetector() {
    }

    public static void tick(Minecraft client) {
        if (client.level != lastLevel) {
            EGGS.clear();
            lastLevel = client.level;
        }
    }

    public static void check(SkullBlockEntity skull) {
        BlockPos pos = skull.getBlockPos().immutable();
        if (isEgg(skull.getOwnerProfile())) EGGS.add(pos);
        else EGGS.remove(pos);
    }

    public static void remove(BlockEntity be) {
        EGGS.remove(be.getBlockPos());
    }

    public static void clear() {
        EGGS.clear();
        lastLevel = null;
    }

    private static ResolvableProfile createEggProfile() {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + EGG_TEXTURE_URL + "\"}}}";
        String value = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        Property textures = new Property("textures", value, EGG_SIGNATURE);
        PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", textures));
        UUID id = UUID.nameUUIDFromBytes(EGG_TEXTURE_URL.getBytes(StandardCharsets.UTF_8));
        return ResolvableProfile.createResolved(new GameProfile(id, "SSUEgg", properties));
    }

    public static boolean isPlayerHead(BlockState state) {
        return state.is(Blocks.PLAYER_HEAD) || state.is(Blocks.PLAYER_WALL_HEAD);
    }

    public static boolean isEgg(@Nullable ResolvableProfile profile) {
        if (profile == null) return false;
        for (Property p : profile.partialProfile().properties().get("textures")) {
            if (p == null) return false;
            if (EGG_SIGNATURE.equals(p.signature())) return true;
        }
        return false;
    }
}