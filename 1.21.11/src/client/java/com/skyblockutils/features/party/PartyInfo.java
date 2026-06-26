package com.skyblockutils.features.party;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PartyInfo {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static boolean isInParty = false;
    public static String leader = MinecraftClient.getInstance().getSession().getUsername();
    public static List<String> members = new ArrayList<>();
    public static List<UUID> memberUuids = new ArrayList<>();

    public static void handlePartyMessages(String message) {
        if (message.contains(":")) return;

        if (message.contains("has disbanded the party") ||
                message.contains("You left the party") ||
                message.contains("You have been kicked from the party by") ||
                message.contains("The party was disbanded")) {
            isInParty = false;
            leader = null;
            members.clear();
            memberUuids.clear();
        }

        if (message.contains("has left the party") ||
                message.contains("has been removed from the party") ||
                message.contains("joined the party") ||
                message.contains("joined the dungeon group!") ||
                message.contains("was removed from your party because they disconnected.") ||
                message.contains("The party was transferred to")) {

            scheduler.schedule(() -> {
                PartyListParser.expectingPartyList = true;
                Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("party list");
            }, 500, TimeUnit.MILLISECONDS);
        }
    }
}