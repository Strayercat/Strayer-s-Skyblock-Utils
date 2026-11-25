package com.skyblockutils;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.*;
import com.skyblockutils.features.AboutScreen;
import com.skyblockutils.features.chat.ChatCommands;
import com.skyblockutils.features.chat.ChatFilter;
import com.skyblockutils.features.chat.ChatModifications;
import com.skyblockutils.features.chat.SeparatedChat;
import com.skyblockutils.features.chat.emoji.EmojiRegistry;
import com.skyblockutils.features.coop.CoopListParser;
import com.skyblockutils.features.dungeons.AutoRejoin;
import com.skyblockutils.features.dungeons.DowntimeTracker;
import com.skyblockutils.features.dungeons.DungeonPartyCommands;
import com.skyblockutils.features.events.spookyfest.SpookyMessageHandler;
import com.skyblockutils.features.foraging.TreeGiftNotifications;
import com.skyblockutils.features.guild.GuildListParser;
import com.skyblockutils.features.hud.*;
import com.skyblockutils.features.hud.layout.HudElement;
import com.skyblockutils.features.hud.layout.HudManager;
import com.skyblockutils.features.mining.CorlTimer;
import com.skyblockutils.features.mining.GlaciteTunnelsWaypoints;
import com.skyblockutils.features.mining.PowderChestNotifications;
import com.skyblockutils.features.party.PartyCommands;
import com.skyblockutils.features.party.PartyInfo;
import com.skyblockutils.features.party.PartyInviteNotifications;
import com.skyblockutils.features.party.PartyListParser;
import com.skyblockutils.features.slayer.CarryTracker;
import com.skyblockutils.features.slayer.SlayerTracker;
import com.skyblockutils.features.textures.CustomBreakingAnimation;
import com.skyblockutils.features.textures.F7VoidLava;
import com.skyblockutils.features.textures.ObviousGemstones;
import com.skyblockutils.features.voice.VoiceChat;
import com.skyblockutils.features.voice.VoiceHud;
import com.skyblockutils.features.wiki.WikiLookup;
import com.skyblockutils.hypixel.HypixelApi;
import com.skyblockutils.hypixel.HypixelLocation;
import com.skyblockutils.render.EggClickFilter;
import com.skyblockutils.render.HeadRestorer;
import com.skyblockutils.render.PawPrints;
import com.skyblockutils.utils.*;
import com.skyblockutils.utils.ChatBlockRouter;
import com.skyblockutils.utils.SSU;
import com.skyblockutils.utils.TextUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

import java.util.function.Supplier;

public class StrayersSkyblockUtilsClient implements ClientModInitializer {
    public static boolean isInSkyblock = false;
    public static boolean isOnHypixel = false;

    @Override
    public void onInitializeClient() {
        ModKeyBindings.init();
        ChatBlockRouter.setReleaseSink(message -> {
            if (passesChatFilters(message, false)) Minecraft.getInstance().gui.hud.getChat().addServerSystemMessage(EmojiRegistry.withEmojis(message));
        });
        ChatBlockRouter.setHoldAll(() -> isInSkyblock);
        ChatBlockRouter.register(
                GuildListParser.INSTANCE.blockHandler,
                PartyListParser.INSTANCE.blockHandler,
                PartyInviteNotifications.BLOCK_HANDLER,
                PowderChestNotifications.BLOCK_HANDLER,
                TreeGiftNotifications.BLOCK_HANDLER
        );
        ModConfig.load();
        HypixelApi.init();
        GuiBlocker.init();
        BestiaryHud.register();
        WikiLookup.register();
        AboutScreen.registerInfoButton();
        DailyReminders.init();
        F7VoidLava.register();
        ObviousGemstones.register();

        ClientCommandRegistrationCallback.EVENT.register(ModCommands::register);

        ClientPlayConnectionEvents.JOIN.register((_, _, _) -> isOnHypixel = false);

        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> {
            isOnHypixel = false;
            ModFunctions.onLeave();
        });

        registerHud("ssu_hud", () -> SsuHud.ELEMENT);
        registerHud("ssu_carry_hud", () -> CarryTracker.ELEMENT);
        registerHud("ssu_slayer_tracker", () -> SlayerTracker.ELEMENT);
        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, SSU.id("ssu_system_messages"), (context, _) -> SeparatedChatHud.render(context));
        registerHud("ssu_voice_chat", () -> VoiceHud.ELEMENT);
        registerHud("ssu_stash_hud", () -> StashHud.ELEMENT);
        registerHud("ssu_bestiary_hud", () -> BestiaryHud.ELEMENT);
        HudElementRegistry.attachElementBefore(VanillaHudElements.TITLE_AND_SUBTITLE, SSU.id("ssu_custom_scoreboard"), (context, _) -> HudManager.render(CustomSidebar.ELEMENT, context));

        LevelRenderEvents.END_MAIN.register(context -> {
            GlaciteTunnelsWaypoints.onWorldRender(context);
            NpcFinder.onWorldRender(context);
            PawPrints.onWorldRender(context);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            OnScreenNotification.tick();
            ObviousGemstones.tick(client);
            CustomBreakingAnimation.tick();
            ScreenOpener.tick();
            ModFunctions.handleNonSkyblockExclusiveKeybinds(client);
            ScreenshotManager.tick();
            ChatBlockRouter.tick();
            GuildListParser.INSTANCE.tickCapture();
            PartyListParser.INSTANCE.tickCapture();
            VoiceChat.tick(client);

            PawPrints.tick(client);
            SideBarUtils.tick();
            if (client.level == null) return;

            if (client.getConnection() instanceof ClientPacketListener listener) {
                ModFunctions.calculatePing(client, listener);
            }

            boolean locationKnown = HypixelLocation.isKnown();
            if (locationKnown) isInSkyblock = HypixelLocation.isSkyBlock();
            if (isOnHypixel || isInSkyblock) SSUIndicator.tick(client);
            if (!locationKnown) return;
            if (!isInSkyblock) return;
            BestiaryHud.tick(client);
            CorlTimer.corlTimerTick(client);
            PuffTracker.tick(client);
            CarryTracker.tick(client);
            SlayerTracker.tick();
            HeadRestorer.tick(client);
            ModFunctions.handleSkyblockExclusiveKeybinds();
            PartyListParser.INSTANCE.tick();
            GuildListParser.INSTANCE.tick();
            CoopListParser.tick(client);
            SeparatedChat.tickAllMessages();
            SeparatedChatHud.tick();
            DailyReminders.tick(client);
        });

        Identifier incomingPhase = SSU.id("incoming");
        ClientReceiveMessageEvents.ALLOW_GAME.addPhaseOrdering(incomingPhase, Event.DEFAULT_PHASE);
        ClientReceiveMessageEvents.ALLOW_GAME.register(incomingPhase, (message, overlay) -> {
            SeparatedChat.beginIncoming(message, overlay);
            return true;
        });

        ClientReceiveMessageEvents.GAME.register((message, _) -> {
            SeparatedChat.endIncoming();
            handleGameMessage(message);
        });
        ClientReceiveMessageEvents.GAME_CANCELED.register((message, _) -> {
            SeparatedChat.endIncoming();
            handleGameMessage(message);
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!overlay) {
                GuildListParser.INSTANCE.observeMessage(message);
                Filtered filtered = new Filtered(message);
                if (!ChatBlockRouter.handle(message, filtered::get)) return false;
                return passesChatFilters(message, false, filtered);
            }
            return passesChatFilters(message, overlay);
        });

        ClientSendMessageEvents.MODIFY_CHAT.register(ChatModifications::prepareOutgoingChat);
        ClientSendMessageEvents.MODIFY_COMMAND.register(ChatModifications::prepareOutgoingCommand);
        ClientSendMessageEvents.COMMAND.register(command -> {
            PartyListParser.INSTANCE.onCommandSent(command);
            GuildListParser.INSTANCE.onCommandSent(command);
        });
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> overlay ? message : EmojiRegistry.withEmojis(message));

        UseBlockCallback.EVENT.register((_, level, _, hitResult) -> {
            EggClickFilter.handleClick(level, hitResult);
            return InteractionResult.PASS;
        });

        ClientChunkEvents.CHUNK_LOAD.register(HeadRestorer::onChunkLoad);
    }

    private static void registerHud(String id, Supplier<HudElement> element) {
        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, SSU.id(id), (context, _) -> HudManager.render(element.get(), context));
    }

    private static final class Filtered {
        private final Component message;
        private Boolean value;

        private Filtered(Component message) {
            this.message = message;
        }

        private boolean get() {
            if (value == null) value = isFilteredOut(message);
            return value;
        }
    }

    private static boolean isFilteredOut(Component message) {
        if (!isInSkyblock) return false;

        String cleanMessage = TextUtils.clean(message);
        boolean spookyFestMessage = SpookyMessageHandler.handleMessage(message);
        boolean chatFilter = !ChatFilter.filterMessages(cleanMessage);
        boolean eggMessage = EggClickFilter.handleMessage(cleanMessage);
        boolean stashMessage = StashHud.handleMessage(cleanMessage);
        return !chatFilter || !eggMessage || !spookyFestMessage || !stashMessage;
    }

    private static boolean passesChatFilters(Component message, boolean overlay) {
        return passesChatFilters(message, overlay, new Filtered(message));
    }

    private static boolean passesChatFilters(Component message, boolean overlay, Filtered filtered) {
        if (!isInSkyblock) return true;
        if (filtered.get()) return false;
        return SeparatedChat.handleMessage(message, overlay);
    }

    private static void handleGameMessage(Component message) {
        String cleanMessage = TextUtils.clean(message);
        PartyCommands.handlePartyCommands(cleanMessage);
        PartyInfo.handlePartyMessages(cleanMessage);
        CoopListParser.handleMessage(cleanMessage);
        BestiaryHud.handleMessage(cleanMessage);
        if (!isInSkyblock) return;
        DowntimeTracker.trackDowntime(cleanMessage);
        CarryTracker.handleMessage(cleanMessage);
        SlayerTracker.handleMessage(cleanMessage);
        DungeonPartyCommands.handleDungeonPartyCommands(cleanMessage);
        AutoRejoin.autoRejoin(cleanMessage);
        ChatCommands.handleCommands(cleanMessage);
    }
}