package com.skyblockutils;

import com.skyblockutils.config.ClothConfigHandler;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.*;
import com.skyblockutils.features.chatFilters.ChatFilter;
import com.skyblockutils.features.dungeons.DowntimeTracker;
import com.skyblockutils.features.SsuHud;
import com.skyblockutils.features.dungeons.DungeonPartyCommands;
import com.skyblockutils.mixin.client.ClientPlayNetworkHandlerAccessor;
import com.skyblockutils.utils.GuiBlocker;
import com.skyblockutils.utils.OnScreenNotification;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PingMeasurer;
import net.minecraft.util.Identifier;

public class StrayersSkyblockUtilsClient implements ClientModInitializer {
    public static boolean isInSkyblock = false;
    public static String location = "";

    @Override
    public void onInitializeClient() {
        ModKeyBindings.init();
        ModConfig.load();
        GuiBlocker.init();

        ClientCommandRegistrationCallback.EVENT.register(ModCommands::register);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!handler.getConnection().getAddress().toString().contains("hypixel.net")) return;
            AutoFish.registerListener(client);
            ModFunctions.connectionEventDataReset("Join");
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ModFunctions.connectionEventDataReset("Leave"));

        HudElementRegistry.attachElementBefore(VanillaHudElements.PLAYER_LIST, Identifier.of("strayers-skyblock-utils", "ssu_hud"), (guiGraphics, deltaTracker) ->
                SsuHud.onHudRender(guiGraphics, ModFunctions.getCurrentLocation(MinecraftClient.getInstance()))
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            OnScreenNotification.tick();
            ClothConfigHandler.handleConfigScreen(client);

            if (client.getNetworkHandler() != null) {
                PingMeasurer pingMeasurer = ((ClientPlayNetworkHandlerAccessor) client.getNetworkHandler()).getPingMeasurer();
                pingMeasurer.ping();
            }

            if (client.world == null) return;

            Boolean skyblockCheck = ModFunctions.isInSkyblock(client);
            if (skyblockCheck == null) return;
            isInSkyblock = skyblockCheck;
            if (!isInSkyblock) return;

            location = ModFunctions.getCurrentLocation(client);

            AutoFish.autoFish(client);
            CorlTimer.corlTimerTick(client);
            ModFunctions.handlePlayerOnIsland(client, location);
            ModFunctions.handleKeybinds(client);
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String cleanMessage = message.getString().replaceAll("§.", "").trim();
            boolean partyMsgFilter = PartyInviteNotifications.handleNotifications(cleanMessage);
            boolean chatFilter = !ChatFilter.filterMessages(cleanMessage);
            return chatFilter && partyMsgFilter;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String cleanMessage = message.getString().replaceAll("§.", "").trim();
            DowntimeTracker.trackDowntime(cleanMessage);
            DungeonPartyCommands.handleDungeonPartyCommands(cleanMessage);
            DungeonPartyCommands.autoRejoin(cleanMessage);
        });

        ClientSendMessageEvents.MODIFY_CHAT.register(ChatModifications::modifiedChat);
    }
}