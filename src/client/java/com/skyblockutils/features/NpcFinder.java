package com.skyblockutils.features;

import com.google.gson.JsonArray;
import com.skyblockutils.ModFunctions;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.ModStyle;
import com.skyblockutils.utils.Resources;
import com.skyblockutils.utils.SSU;
import com.skyblockutils.utils.SkyblockAreas;
import com.skyblockutils.utils.WaypointRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NpcFinder {
    public record Npc(String name, String location, BlockPos coordinates) {
    }

    public static final List<Npc> NPCS = new ArrayList<>();

    static {
        Resources.json("data/npcs.json").entrySet().forEach(area -> area.getValue().getAsJsonObject().entrySet().forEach(npc -> {
            JsonArray pos = npc.getValue().getAsJsonArray();
            NPCS.add(new Npc(npc.getKey(), area.getKey(), new BlockPos(pos.get(0).getAsInt(), pos.get(1).getAsInt(), pos.get(2).getAsInt())));
        }));
    }

    private static final Map<String, Npc> pendingCallbacks = new HashMap<>();

    public static void handleCallback(String id) {
        Npc npc = pendingCallbacks.remove(id);
        if (npc == null) return;

        String currentLocation = SkyblockAreas.current();
        if (!npc.location().equals(currentLocation)) {
            ModFunctions.sendSystemMessage("§cYou need to be in §eThe " + npc.location() + " §cto display this waypoint.", false);
            return;
        }

        addToBeMarked(npc);
        ModFunctions.sendSystemMessage("§aWaypoint displayed.", false);
    }

    private static final List<Npc> toBeMarked = new ArrayList<>();

    public static void addToBeMarked(Npc npc) {
        toBeMarked.add(npc);
    }

    public static void clear() {
        toBeMarked.clear();
        pendingCallbacks.clear();
    }

    public static void onWorldRender(LevelRenderContext context) {
        if (Minecraft.getInstance().player == null) return;
        String currentLocation = SkyblockAreas.current();

        toBeMarked.removeIf(npc ->
                Minecraft.getInstance().player.blockPosition().closerThan(npc.coordinates(), 5.0)
        );

        for (Npc npc : toBeMarked) {
            if (npc.location().equals(currentLocation))
                WaypointRenderer.render(context, new WaypointRenderer.Waypoint(npc.coordinates, npc.name, ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.WAYPOINT), WaypointRenderer.WaypointStyle.BADGE), -1);
        }
    }

    public static void handleCommand(String npcName) {
        if (!StrayersSkyblockUtilsClient.isInSkyblock) {
            ModFunctions.sendSystemMessage("§cYou must be in skyblock to use this feature.", false);
            return;
        }

        List<Npc> matches = NPCS.stream()
                .filter(npc -> npc.name().equalsIgnoreCase(npcName))
                .toList();

        if (matches.isEmpty()) {
            ModFunctions.sendSystemMessage("§cNo NPC found with name: " + npcName, false);
            return;
        }

        MutableComponent message = Component.empty()
                .append(SSU.getFullName())
                .append(Component.literal(matches.getFirst().name + " can be found in these locations:\n"));

        for (int i = 0; i < matches.size(); i++) {
            Npc match = matches.get(i);
            String id = java.util.UUID.randomUUID().toString().replace("-", "");
            pendingCallbacks.put(id, match);

            Component waypointButton = Component.literal("[Display Waypoint]")
                    .setStyle(Style.EMPTY
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent.RunCommand("/snpc " + id)));

            if (i > 0) message.append(Component.literal("\n"));

            message.append(Component.literal("    - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(match.location()).setStyle(Style.EMPTY.withColor(ModStyle.main())))
                    .append(match.coordinates.getX() == 20000
                            ? Component.literal(": Location varies").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                            : Component.literal(": " + match.coordinates().getX() + " " +
                            match.coordinates().getY() + " " +
                            match.coordinates().getZ() + " ").withStyle(ChatFormatting.GRAY))
                    .append(match.coordinates.getX() == 20000
                            ? Component.empty()
                            : waypointButton);
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        client.player.sendSystemMessage(message);
    }
}