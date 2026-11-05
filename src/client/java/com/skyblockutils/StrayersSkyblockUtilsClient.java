package com.skyblockutils;

import com.skyblockutils.config.ClothConfigHandler;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.*;
import com.skyblockutils.features.chat.ChatCommands;
import com.skyblockutils.features.chat.ChatFilter;
import com.skyblockutils.features.chat.ChatModifications;
import com.skyblockutils.features.chat.SeparatedChat;
import com.skyblockutils.features.chat.emoji.EmojiRegistry;
import com.skyblockutils.features.coop.CoopListParser;
import com.skyblockutils.features.events.spookyfest.SpookyMessageHandler;
import com.skyblockutils.features.foraging.TreeGiftNotifications;
import com.skyblockutils.utils.ChatBlockRouter;
import com.skyblockutils.features.guild.GuildListParser;
import com.skyblockutils.features.hud.SeparatedChatHud;
import com.skyblockutils.features.mining.PowderChestNotifications;
import com.skyblockutils.features.dungeons.AutoRejoin;
import com.skyblockutils.features.glowingPlayers.GlowingPlayersGui;
import com.skyblockutils.features.hud.CustomSidebar;
import com.skyblockutils.features.hud.ScreenshotManager;
import com.skyblockutils.features.mining.CorlTimer;
import com.skyblockutils.features.dungeons.DowntimeTracker;
import com.skyblockutils.features.hud.SsuHud;
import com.skyblockutils.features.hud.layout.HudEditScreen;
import com.skyblockutils.features.hud.layout.HudManager;
import com.skyblockutils.features.dungeons.DungeonPartyCommands;
import com.skyblockutils.features.mining.GlaciteTunnelsWaypoints;
import com.skyblockutils.features.party.PartyCommands;
import com.skyblockutils.features.party.PartyInfo;
import com.skyblockutils.features.party.PartyInviteNotifications;
import com.skyblockutils.features.party.PartyListParser;
import com.skyblockutils.features.slayer.CarryTracker;
import com.skyblockutils.features.textures.F7VoidLava;
import com.skyblockutils.features.voice.VoiceChat;
import com.skyblockutils.features.voice.VoiceHud;
import com.skyblockutils.render.EggDetector;
import com.skyblockutils.render.EggClickFilter;
import com.skyblockutils.render.SkullRefresher;
import com.skyblockutils.utils.*;
import com.skyblockutils.features.AboutScreen;
import com.skyblockutils.render.PawPrints;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

public class StrayersSkyblockUtilsClient implements ClientModInitializer {
    public static boolean isInSkyblock = false;
    public static boolean isOnHypixel = false;

    @Override
    public void onInitializeClient() {
        ModKeyBindings.init();
        ChatBlockRouter.setReleaseSink(message -> {
            if (passesChatFilters(message, false)) Minecraft.getInstance().gui.hud.getChat().addServerSystemMessage(message);
        });
        ChatBlockRouter.register(
                PartyListParser.BLOCK_HANDLER,
                GuildListParser.BLOCK_HANDLER,
                PartyInviteNotifications.BLOCK_HANDLER,
                PowderChestNotifications.BLOCK_HANDLER,
                TreeGiftNotifications.BLOCK_HANDLER
        );
        ModConfig.load();
        GuiBlocker.init();
        AboutScreen.registerInfoButton();
        DailyReminders.init();
        F7VoidLava.register();

        ClientCommandRegistrationCallback.EVENT.register(ModCommands::register);

        ClientPlayConnectionEvents.JOIN.register((handler, _, _) -> {
            isOnHypixel = handler.getConnection().getRemoteAddress().toString().toLowerCase(java.util.Locale.ROOT).contains("hypixel");
            if (!isOnHypixel) return;
            ModFunctions.connectionEventDataReset("Join");
        });

        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> {
            isOnHypixel = false;
            EggDetector.clear();
            CoopListParser.reset();
            ModFunctions.connectionEventDataReset("Leave");
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, Identifier.fromNamespaceAndPath("strayers-skyblock-utils", "ssu_hud"), (context, _) -> HudManager.render(SsuHud.ELEMENT, context));
        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, Identifier.fromNamespaceAndPath("strayers-skyblock-utils", "ssu_carry_hud"), (context, _) -> HudManager.render(CarryTracker.ELEMENT, context));
        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, Identifier.fromNamespaceAndPath("strayers-skyblock-utils", "ssu_system_messages"), (context, _) -> SeparatedChatHud.render(context));
        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, Identifier.fromNamespaceAndPath("strayers-skyblock-utils", "ssu_voice_chat"), (context, _) -> HudManager.render(VoiceHud.ELEMENT, context));
        HudElementRegistry.attachElementBefore(VanillaHudElements.TITLE_AND_SUBTITLE, Identifier.fromNamespaceAndPath("strayers-skyblock-utils", "ssu_custom_scoreboard"), (context, _) -> HudManager.render(CustomSidebar.ELEMENT, context));

        LevelRenderEvents.END_MAIN.register(context -> {
            GlaciteTunnelsWaypoints.onWorldRender(context);
            NpcFinder.onWorldRender(context);
            PawPrints.onWorldRender(context);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            OnScreenNotification.tick();
            ClothConfigHandler.handleConfigScreen(client);
            GlowingPlayersGui.handleConfigScreen(client);
            HudEditScreen.handleRequest(client);
            ModFunctions.handleNonSkyblockExclusiveKeybinds(client);
            ScreenshotManager.tick();
            ChatBlockRouter.tick();
            PartyListParser.tickCapture();
            GuildListParser.tickCapture();
            VoiceChat.tick(client);

            PawPrints.tick(client);
            if (client.level == null) return;

            if (client.getConnection() instanceof ClientPacketListener listener) {
                ModFunctions.calculatePing(client, listener);
            }

            Boolean skyblockCheck = ModFunctions.isInSkyblock(client);
            if (skyblockCheck != null) isInSkyblock = skyblockCheck;
            if (isOnHypixel || isInSkyblock) SSUIndicator.tick(client);
            if (skyblockCheck == null) return;
            if (!isInSkyblock) return;

            SkullRefresher.tick(client);
            AutoFish.autoFish(client);
            CorlTimer.corlTimerTick(client);
            PuffTracker.tick(client);
            CarryTracker.tick(client);
            ModFunctions.handleSkyblockExclusiveKeybinds(client);
            PartyListParser.handleOnJoinCommand();
            GuildListParser.tick();
            CoopListParser.tick(client);
            SideBarUtils.updateLocation();
            SeparatedChat.tickAllMessages();
            SeparatedChatHud.tick();
            DailyReminders.tick(client);
        });

        ClientReceiveMessageEvents.GAME.register((message, _) -> handleGameMessage(message));
        ClientReceiveMessageEvents.GAME_CANCELED.register((message, _) -> handleGameMessage(message));

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!overlay) {
                GuildListParser.observeMessage(message);
                if (!ChatBlockRouter.handle(message)) return false;
            }
            return passesChatFilters(message, overlay);
        });

        ClientSendMessageEvents.MODIFY_CHAT.register(ChatModifications::prepareOutgoingChat);
        ClientSendMessageEvents.MODIFY_COMMAND.register(ChatModifications::prepareOutgoingCommand);
        ClientSendMessageEvents.COMMAND.register(command -> {
            PartyListParser.onCommandSent(command);
            GuildListParser.onCommandSent(command);
        });
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> overlay ? message : ChatModifications.fitToChat(EmojiRegistry.withEmojis(message)));

        UseBlockCallback.EVENT.register((_, level, _, hitResult) -> {
            EggClickFilter.handleClick(level, hitResult);
            return InteractionResult.PASS;
        });

        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, _) -> SkullRefresher.handleBELoad(be));
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, _) -> SkullRefresher.handleBEUnload(be));
        ClientChunkEvents.CHUNK_LOAD.register(SkullRefresher::handleChunkLoad);
    }

    private static boolean passesChatFilters(Component message, boolean overlay) {
        if (!isInSkyblock) return true;

        String cleanMessage = message.getString().replaceAll("§.", "").trim();
        boolean spookyFestMessage = SpookyMessageHandler.handleMessage(message);
        boolean chatFilter = !ChatFilter.filterMessages(cleanMessage);
        boolean eggMessage = EggClickFilter.handleMessage(cleanMessage);
        if (!chatFilter || !eggMessage || !spookyFestMessage) return false;

        return SeparatedChat.handleMessage(message, overlay);
    }

    private static void handleGameMessage(Component message) {
        String cleanMessage = message.getString().replaceAll("§.", "").trim();
        PartyCommands.handlePartyCommands(cleanMessage);
        PartyInfo.handlePartyMessages(cleanMessage);
        CoopListParser.handleMessage(cleanMessage);
        if (!isInSkyblock) return;
        DowntimeTracker.trackDowntime(cleanMessage);
        DungeonPartyCommands.handleDungeonPartyCommands(cleanMessage);
        AutoRejoin.autoRejoin(cleanMessage);
        ChatCommands.handleCommands(cleanMessage);
    }
}