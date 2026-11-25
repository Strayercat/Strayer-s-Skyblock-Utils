package com.skyblockutils;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.DailyReminders;
import com.skyblockutils.features.NpcFinder;
import com.skyblockutils.features.PuffTracker;
import com.skyblockutils.features.Zoom;
import com.skyblockutils.features.chat.SeparatedChat;
import com.skyblockutils.features.coop.CoopListParser;
import com.skyblockutils.features.dungeons.AutoRejoin;
import com.skyblockutils.features.dungeons.DowntimeTracker;
import com.skyblockutils.features.guild.GuildListParser;
import com.skyblockutils.features.hud.BestiaryHud;
import com.skyblockutils.features.hud.SeparatedChatHud;
import com.skyblockutils.features.hud.SsuHud;
import com.skyblockutils.features.hud.StashHud;
import com.skyblockutils.features.inventory.InventoryButtons;
import com.skyblockutils.features.mining.CorlTimer;
import com.skyblockutils.features.party.PartyListParser;
import com.skyblockutils.features.browser.InGameBrowser;
import com.skyblockutils.features.wiki.WikiLookup;
import com.skyblockutils.hypixel.HypixelLocation;
import com.skyblockutils.mixin.client.BossHealthOverlayAccessor;
import com.skyblockutils.mixin.client.PingDebugMonitorAccessor;
import com.skyblockutils.mixin.client.PingDebugMonitorInvoker;
import com.skyblockutils.utils.ChatBlockRouter;
import com.skyblockutils.utils.GuiBlocker;
import com.skyblockutils.utils.ModStyle;
import com.skyblockutils.utils.SSU;
import com.skyblockutils.utils.SSUIndicator;
import com.skyblockutils.utils.SideBarUtils;
import com.skyblockutils.utils.TextUtils;
import com.skyblockutils.utils.UpdateChecker;
import com.skyblockutils.utils.ZoomState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.debugchart.LocalSampleLogger;

public class ModFunctions {
    public static int ping = 0;
    public static float tps = 0;

    private static long lastPingMeasure = 0;

    public static void onJoin() {
        GuiBlocker.shouldHideScreen = false;
        UpdateChecker.init(Minecraft.getInstance());
        resetSession();
    }

    public static void onLeave() {
        StrayersSkyblockUtilsClient.isInSkyblock = false;
        AutoRejoin.resetAutoRejoin();
        BestiaryHud.save();
        SSUIndicator.disconnect();
        PartyListParser.INSTANCE.reset();
        HypixelLocation.reset();
        GuildListParser.INSTANCE.reset();
        CoopListParser.reset();
        BestiaryHud.reset();
        StashHud.reset();
        InventoryButtons.resetHead();
        resetSession();
    }

    private static void resetSession() {
        SsuHud.funFactHandled = false;
        DowntimeTracker.resetDowntimeTracker();
        serverChangeDataReset();
        NpcFinder.clear();
        SideBarUtils.resetLocation();
        ChatBlockRouter.reset();
        DailyReminders.reset();
    }

    public static void serverChangeDataReset() {
        CorlTimer.resetCorlTimer();
        PuffTracker.reset();
    }

    public static void calculatePing(Minecraft client, ClientPacketListener listener) {
        ((PingDebugMonitorInvoker) ((PingDebugMonitorAccessor) listener).getPingDebugMonitor()).ssu$tick();

        long now = System.currentTimeMillis();
        if (lastPingMeasure + 1000 > now) return;
        lastPingMeasure = now;

        LocalSampleLogger pingLog = client.getDebugOverlay().getPingLogger();
        if (pingLog.size() > 0) ping = (int) pingLog.get(pingLog.size() - 1);
    }

    public static void handleSkyblockExclusiveKeybinds() {
        while (ModKeyBindings.CORLEONE_TIMER_KEY.consumeClick()) CorlTimer.toggleCorlTimer();
        while (ModKeyBindings.PUFF_TIMER_KEY.consumeClick()) PuffTracker.togglePuffTimer();
        SsuHud.setVisible(ModKeyBindings.HUD_KEY.isDown());
        WikiLookup.tick(Minecraft.getInstance());
        InGameBrowser.tick();
    }

    public static void handleNonSkyblockExclusiveKeybinds(Minecraft client) {
        while (ModKeyBindings.PRINT_COORDINATES_KEY.consumeClick()) sendCoordinates(client, ModConfig.INSTANCE.coordinatesSendLocation);
        boolean zoomPressed = ModKeyBindings.ZOOM_KEY.isDown();
        if (zoomPressed && !ZoomState.isZooming) Zoom.enter(client);
        else if (!zoomPressed && ZoomState.isZooming) Zoom.exit(client);
    }

    public static void sendCoordinates(Minecraft client, boolean withLocation) {
        if (client.player == null || client.getConnection() == null) return;

        String message = "x:" + (int) client.player.getX()
                + " y:" + (int) client.player.getY()
                + " z:" + (int) client.player.getZ();
        if (withLocation && !SideBarUtils.location.isEmpty()) message += " | ⏣ " + SideBarUtils.location;

        client.getConnection().sendChat(message);
    }

    public static Component getFormattedCoordinates() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return Component.empty();

        int color = ModStyle.main();
        return Component.empty()
                .append(Component.literal("X: ").withColor(color)).append(String.valueOf((int) client.player.getX()))
                .append(Component.literal(" Y: ").withColor(color)).append(String.valueOf((int) client.player.getY()))
                .append(Component.literal(" Z: ").withColor(color)).append(String.valueOf((int) client.player.getZ()));
    }

    public static void showTitle(Minecraft client, String title, int displayTime, boolean withSound) {
        showTitle(client, Component.literal(title), displayTime, withSound);
    }

    public static void showTitle(Minecraft client, Component title, int displayTime, boolean withSound) {
        if (!client.isSameThread()) {
            client.execute(() -> showTitle(client, title, displayTime, withSound));
            return;
        }
        if (client.player == null) return;
        client.gui.hud.setTimes(10, displayTime, 10);
        client.gui.hud.setTitle(title);
        if (withSound) client.player.playSound(SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value());
    }

    public static void sendSystemMessage(String message, boolean fullName) {
        sendSystemMessage(Component.literal(message).withColor(ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TEXT)), fullName);
    }

    public static void sendSystemMessage(Component message, boolean fullName) {
        Component full = Component.empty().append(fullName ? SSU.getFullName() : SSU.getName()).append(message);

        if (SeparatedChatHud.separatingHere() && StrayersSkyblockUtilsClient.isInSkyblock) {
            SeparatedChat.addMessage(full);
        } else {
            Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(full);
        }
    }

    public static boolean isWorldNotReady() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null || client.getConnection() == null || SideBarUtils.isNotReady()) return true;
        return !level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(player.getBlockX()), SectionPos.blockToSectionCoord(player.getBlockZ()));
    }

    public static boolean isInDungeons(Minecraft client) {
        String location = SideBarUtils.location;
        if (location != null && location.contains("The Catacombs")) return true;
        return ((BossHealthOverlayAccessor) client.gui.hud.getBossOverlay())
                .getEvents().values().stream().findFirst()
                .map(bossBar -> TextUtils.strip(bossBar.getName()).contains("The Professor"))
                .orElse(false);
    }
}
