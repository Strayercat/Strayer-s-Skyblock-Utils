package com.skyblockutils;

import com.skyblockutils.config.ClothConfigHandler;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.*;
import com.skyblockutils.features.chat.ChatCommands;
import com.skyblockutils.features.chat.ChatFilter;
import com.skyblockutils.features.chat.FancyEmotes;
import com.skyblockutils.features.events.spookyfest.SpookyMessageHandler;
import com.skyblockutils.features.foraging.TreeGiftNotifications;
import com.skyblockutils.features.mining.PowderChestNotifications;
import com.skyblockutils.features.dungeons.AutoRejoin;
import com.skyblockutils.features.glowingPlayers.GlowingPlayersGui;
import com.skyblockutils.features.hud.CustomSidebar;
import com.skyblockutils.features.hud.ScreenshotManager;
import com.skyblockutils.features.mining.CorlTimer;
import com.skyblockutils.features.dungeons.DowntimeTracker;
import com.skyblockutils.features.hud.SsuHud;
import com.skyblockutils.features.dungeons.DungeonPartyCommands;
import com.skyblockutils.features.mining.GlaciteTunnelsWaypoints;
import com.skyblockutils.features.party.PartyCommands;
import com.skyblockutils.features.party.PartyInfo;
import com.skyblockutils.features.party.PartyInviteNotifications;
import com.skyblockutils.features.party.PartyListParser;
import com.skyblockutils.utils.GuiBlocker;
import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.utils.SideBarUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.Identifier;

public class StrayersSkyblockUtilsClient implements ClientModInitializer {
    public static boolean isInSkyblock = false;
    private static boolean soundListenerRegistered = false;

    @Override
    public void onInitializeClient() {
        ModKeyBindings.init();
        ModConfig.load();
        GuiBlocker.init();
        DailyReminders.init();

        ClientCommandRegistrationCallback.EVENT.register(ModCommands::register);

        ClientPlayConnectionEvents.JOIN.register((handler, _, client) -> {
            if (!handler.getConnection().getRemoteAddress().toString().contains("hypixel.net")) return;
            AutoFish.registerListener(client);
            ModFunctions.connectionEventDataReset("Join");
        });

        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> ModFunctions.connectionEventDataReset("Leave"));

        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, Identifier.fromNamespaceAndPath("strayers-skyblock-utils", "ssu_hud"), (context, _) -> SsuHud.onHudRender(context, SideBarUtils.location));

        HudElementRegistry.attachElementBefore(VanillaHudElements.TITLE_AND_SUBTITLE, Identifier.fromNamespaceAndPath("strayers-skyblock-utils", "ssu_custom_scoreboard"), (context, _) -> {
            if (isInSkyblock && ModConfig.INSTANCE.customSidebar) CustomSidebar.displayCustomSidebar(context);
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, Identifier.fromNamespaceAndPath("strayers-skyblock-utils", "ssu_screenshot_manager"), (context, _) -> ScreenshotManager.buildScreenshotHud(context));

        LevelRenderEvents.END_MAIN.register(context -> {
            GlaciteTunnelsWaypoints.onWorldRender(context);
            NpcFinder.onWorldRender(context);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!soundListenerRegistered) {
                client.getSoundManager().addListener(new PowderChestNotifications());
                soundListenerRegistered = true;
            }

            OnScreenNotification.tick();
            ClothConfigHandler.handleConfigScreen(client);
            GlowingPlayersGui.handleConfigScreen(client);
            ModFunctions.handleNonSkyblockExclusiveKeybinds(client);
            ScreenshotManager.tick();
            PowderChestNotifications.tick();

            if (client.level == null) return;

            if (client.getConnection() instanceof ClientPacketListener listener) {
                ModFunctions.calculatePing(client, listener);
            }

            Boolean skyblockCheck = ModFunctions.isInSkyblock(client);
            if (skyblockCheck == null) return;
            isInSkyblock = skyblockCheck;
            if (!isInSkyblock) return;

            AutoFish.autoFish(client);
            CorlTimer.corlTimerTick(client);
            PuffTracker.tick(client);
            ModFunctions.handleSkyblockExclusiveKeybinds(client);
            PartyListParser.handleOnJoinCommand();
            SideBarUtils.updateLocation();
            DailyReminders.tick(client);
        });

        ClientReceiveMessageEvents.GAME.register((message, _) -> {
            if (!isInSkyblock) return;
            String cleanMessage = message.getString().replaceAll("§.", "").trim();
            DowntimeTracker.trackDowntime(cleanMessage);
            DungeonPartyCommands.handleDungeonPartyCommands(cleanMessage);
            AutoRejoin.autoRejoin(cleanMessage);
            ChatCommands.handleCommands(cleanMessage);
            PartyCommands.handlePartyCommands(cleanMessage);
            PartyInfo.handlePartyMessages(cleanMessage);
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, _) -> {
            if (!isInSkyblock) return true;
            String cleanMessage = message.getString().replaceAll("§.", "").trim();

            PowderChestNotifications.handleMessage(message);

            boolean partyMsgFilter = PartyInviteNotifications.handleNotifications(message);
            boolean partyListMessages = PartyListParser.handleMessage(cleanMessage);
            boolean powderChestMessage = PowderChestNotifications.parseChestReward(message);
            boolean spookyFestMessage = SpookyMessageHandler.handleMessage(message);
            boolean treeGiftMessage = TreeGiftNotifications.handleMessage(message);

            boolean chatFilter = !ChatFilter.filterMessages(cleanMessage);
            return chatFilter && partyMsgFilter && partyListMessages && powderChestMessage && spookyFestMessage && treeGiftMessage;
        });

        ClientSendMessageEvents.MODIFY_CHAT.register(FancyEmotes::fancyEmotes);

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> PowderChestNotifications.handleChestclick(hitResult));
    }
}