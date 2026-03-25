package com.skyblockutils.features;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.PlayerLookup;
import net.minecraft.client.MinecraftClient;

import java.util.Map;
import java.util.UUID;


public class GlowingPlayers {
    public static class GlowingPlayer {
        public String username;
        public UUID uuid;
        public int color;

        public GlowingPlayer(String username, UUID uuid, int color) {
            this.username = username;
            this.uuid = uuid;
            this.color = color;
        }

        public UUID getUuid() {
            return this.uuid;
        }

        public String getUsername() {
            return this.username;
        }

        public int getColor() {
            return this.color;
        }
    }

    public static final Map<String, Integer> MINECRAFT_COLORS = Map.ofEntries(
            Map.entry("BLACK", 0x000000),
            Map.entry("DARK_BLUE", 0x0000AA),
            Map.entry("DARK_GREEN", 0x00AA00),
            Map.entry("DARK_AQUA", 0x00AAAA),
            Map.entry("DARK_RED", 0xAA0000),
            Map.entry("DARK_PURPLE", 0xAA00AA),
            Map.entry("GOLD", 0xFFAA00),
            Map.entry("GRAY", 0xAAAAAA),
            Map.entry("DARK_GRAY", 0x555555),
            Map.entry("BLUE", 0x5555FF),
            Map.entry("GREEN", 0x55FF55),
            Map.entry("AQUA", 0x55FFFF),
            Map.entry("RED", 0xFF5555),
            Map.entry("LIGHT_PURPLE", 0xFF55FF),
            Map.entry("YELLOW", 0xFFFF55),
            Map.entry("WHITE", 0xFFFFFF)
    );

    public static void add(String username, int color) {
        if (isPlayerGlowing(username)) {
            ModFunctions.displayMessageWithHeader("§c" + username + " is already glowing!");
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        PlayerLookup.getUuidOffline(username).thenAccept(uuid ->
                client.execute(() -> {
                    if (uuid != null) {
                        ModConfig.INSTANCE.addGlowingPlayer(new GlowingPlayer(username, uuid, color));
                        ModFunctions.displayMessageWithHeader("§a" + username + " is now glowing!");
                        ModConfig.save();
                    } else {
                        ModFunctions.displayMessageWithHeader("§cPlayer " + username + " not found :c");
                    }
                })
        );
    }

    public static void remove(String username) {
        if (!isPlayerGlowing(username)) {
            ModFunctions.displayMessageWithHeader("§c" + username + " already wasn't glowing");
            return;
        }

        ModConfig.INSTANCE.removeGlowingPlayer(username);
        ModFunctions.displayMessageWithHeader("§a" + username + " is no longer glowing");
        ModConfig.save();
    }

    public static void clearAll() {
        ModConfig.INSTANCE.getGlowingPlayers().clear();
        ModConfig.save();
    }

    private static boolean isPlayerGlowing(String username) {
        return ModConfig.INSTANCE.getGlowingPlayers().stream().anyMatch(player -> player.username.equalsIgnoreCase(username));
    }
}