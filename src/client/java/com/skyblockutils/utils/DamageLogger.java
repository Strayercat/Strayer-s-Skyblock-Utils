package com.skyblockutils.utils;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.features.hud.LogHud;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.entity.Entity;

import java.util.Locale;

public class DamageLogger {
    public static boolean enabled = false;

    public static void toggle() {
        enabled = !enabled;
        ModFunctions.sendSystemMessage(enabled ? "§aDamage logging enabled" : "§cDamage logging disabled", false);
    }

    public static void onDamage(ClientboundDamageEventPacket packet) {
        if (!enabled) return;
        LogHud.addLog("§7[DMG] §f" + describe(packet.entityId())
                + " §7type=§e" + packet.sourceType().getRegisteredName()
                + " §7cause=§f" + describe(packet.sourceCauseId())
                + " §7direct=§f" + describe(packet.sourceDirectId()));
    }

    public static void onXpSound(float pitch, float volume) {
        if (!enabled) return;
        LogHud.addLog(String.format(Locale.ROOT, "§7[XP] §fpitch=§e%.4f §7volume=§e%.2f", pitch, volume));
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
