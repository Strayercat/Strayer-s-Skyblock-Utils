package com.skyblockutils.features.coop;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.features.guild.GuildListParser;
import com.skyblockutils.features.party.PartyListParser;
import com.skyblockutils.utils.SSUIndicator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoopListParser {
    private static final long JOIN_DELAY_MS = 1_500;
    private static final long CHANGE_DELAY_MS = 1_000;
    private static final long MIN_INTERVAL_MS = 5_000;
    private static final long OPEN_WINDOW_MS = 2_500;
    private static final long CONTENT_WINDOW_MS = 3_000;
    private static final long SOUND_GRACE_MS = 300;
    private static final Pattern MEMBER = Pattern.compile("^(?:\\[[^]]+] )*(\\w{3,16})$");

    public static final List<String> members = new ArrayList<>();

    private static ClientLevel lastLevel;
    private static long pendingAt = -1;
    private static long lastRequest = 0;
    private static long expectOpenUntil = 0;
    private static long expectContentUntil = 0;
    private static int containerId = -1;
    private static long muteSoundsUntil = 0;

    public static void tick(Minecraft client) {
        long now = System.currentTimeMillis();

        if (client.level != lastLevel) {
            lastLevel = client.level;
            if (client.level != null) schedule(now + JOIN_DELAY_MS);
        }

        if (containerId != -1 && now > expectContentUntil) closeHidden(client);
        if (containerId == -1 && expectOpenUntil != 0 && now > expectOpenUntil) {
            expectOpenUntil = 0;
            muteSoundsUntil = 0;
        }

        if (pendingAt < 0 || now < pendingAt) return;
        if (now - lastRequest < MIN_INTERVAL_MS) return;
        if (client.gui.screen() != null || client.getConnection() == null) return;
        if (ModFunctions.isWorldLoaded()) return;
        if (Boolean.TRUE.equals(ModFunctions.isInDungeons(client))) return;
        if (PartyListParser.isExpecting() || GuildListParser.isExpecting()) return;

        request(client, now);
    }

    public static void handleMessage(String message) {
        if (message.contains(":")) return;
        String lower = message.toLowerCase(Locale.ROOT);
        if (!lower.contains("co-op") && !lower.contains("coop")) return;
        if (lower.contains("joined") || lower.contains("left") || lower.contains("kicked") || lower.contains("removed")) {
            schedule(System.currentTimeMillis() + CHANGE_DELAY_MS);
        }
    }

    public static boolean onOpenScreen(int id, Component title) {
        if (System.currentTimeMillis() > expectOpenUntil) return false;
        if (!title.getString().replaceAll("§.", "").trim().startsWith("Profile:")) {
            expectOpenUntil = 0;
            muteSoundsUntil = 0;
            return false;
        }
        expectOpenUntil = 0;
        containerId = id;
        expectContentUntil = System.currentTimeMillis() + CONTENT_WINDOW_MS;
        return true;
    }

    public static boolean onContainerContent(int id, List<ItemStack> items) {
        if (containerId == -1 || id != containerId) return false;
        parse(items);
        closeHidden(Minecraft.getInstance());
        return true;
    }

    public static boolean shouldMuteSounds() {
        return System.currentTimeMillis() < muteSoundsUntil;
    }

    public static boolean isHiddenContainer(int id) {
        return containerId != -1 && id == containerId;
    }

    public static void reset() {
        members.clear();
        lastLevel = null;
        pendingAt = -1;
        expectOpenUntil = 0;
        containerId = -1;
        muteSoundsUntil = 0;
        SSUIndicator.setCoopMembers(List.of());
    }

    private static void schedule(long at) {
        if (pendingAt < 0 || at < pendingAt) pendingAt = at;
    }

    private static void request(Minecraft client, long now) {
        pendingAt = -1;
        lastRequest = now;
        expectOpenUntil = now + OPEN_WINDOW_MS;
        muteSoundsUntil = now + OPEN_WINDOW_MS + CONTENT_WINDOW_MS;
        client.getConnection().sendCommand("coopmanage");
    }

    private static void closeHidden(Minecraft client) {
        if (containerId == -1) return;
        if (client.getConnection() != null) client.getConnection().send(new ServerboundContainerClosePacket(containerId));
        containerId = -1;
        muteSoundsUntil = System.currentTimeMillis() + SOUND_GRACE_MS;
    }

    private static void parse(List<ItemStack> items) {
        String self = Minecraft.getInstance().getUser().getName();
        Set<String> found = new LinkedHashSet<>();

        for (ItemStack stack : items) {
            if (stack.isEmpty() || !stack.is(Items.PLAYER_HEAD) || !hasStatusLine(stack)) continue;
            String name = stack.getHoverName().getString().replaceAll("§.", "").trim();
            Matcher m = MEMBER.matcher(name);
            if (!m.matches()) continue;
            String member = m.group(1);
            if (!member.equalsIgnoreCase(self)) found.add(member);
        }

        members.clear();
        members.addAll(found);
        SSUIndicator.setCoopMembers(members);
    }

    private static boolean hasStatusLine(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        for (Component line : lore.lines()) {
            if (line.getString().replaceAll("§.", "").trim().startsWith("Status:")) return true;
        }
        return false;
    }
}
