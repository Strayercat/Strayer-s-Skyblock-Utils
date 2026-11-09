package com.skyblockutils.utils;

import com.skyblockutils.ModFunctions;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.entity.Entity;

public class DamageLogger {
    public static boolean enabled = false;

    public static void toggle() {
        enabled = !enabled;
        ModFunctions.sendSystemMessage(enabled ? "§aDamage logging enabled" : "§cDamage logging disabled", false);
    }

    public static void onDamage(ClientboundDamageEventPacket packet) {
        if (!enabled) return;
        ModFunctions.sendSystemMessage("§7[DMG] §f" + describe(packet.entityId())
                + " §7type=§e" + packet.sourceType().getRegisteredName()
                + " §7cause=§f" + describe(packet.sourceCauseId())
                + " §7direct=§f" + describe(packet.sourceDirectId()), false);
    }

    public static void onDeath(Entity entity) {
        if (!enabled) return;
        ModFunctions.sendSystemMessage("§7[DEATH] §f" + describe(entity.getId()), false);
    }

    private static String describe(int id) {
        if (id < 0) return "none";
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.getId() == id) return "§aYOU§f#" + id;
        Entity entity = client.level == null ? null : client.level.getEntity(id);
        if (entity == null) return "?#" + id;
        return entity.getName().getString() + "#" + id;
    }
}
