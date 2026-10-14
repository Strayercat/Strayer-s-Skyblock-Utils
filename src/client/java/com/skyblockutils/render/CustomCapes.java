package com.skyblockutils.render;

import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.Map;
import java.util.UUID;

public final class CustomCapes {
    private static final Map<UUID, ClientAsset.Texture> CAPES = Map.of(
            UUID.fromString("5cc567f8-4516-4f30-b0a6-ac181193a718"), cape("puppygirllily")
    );

    private CustomCapes() {
    }

    private static ClientAsset.Texture cape(String name) {
        return new ClientAsset.ResourceTexture(Identifier.fromNamespaceAndPath("skyblockutils", "entity/capes/" + name));
    }

    public static PlayerSkin apply(UUID uuid, PlayerSkin skin) {
        ClientAsset.Texture cape = CAPES.get(uuid);
        if (cape == null || skin == null) return skin;
        return new PlayerSkin(skin.body(), cape, skin.elytra(), skin.model(), skin.secure());
    }
}
