package com.skyblockutils.features.party;

import com.skyblockutils.utils.SSUIndicator;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PartyInfo {
    public static boolean isInParty = false;
    public static String leader = Minecraft.getInstance().getUser().getName();
    public static List<String> members = new ArrayList<>();
    public static List<UUID> memberUuids = new ArrayList<>();

    public static void handlePartyMessages(String message) {
        if (message.startsWith("You'll be partying with:")) {
            PartyTracker.request();
            return;
        }

        if (message.contains(":")) return;

        if (message.contains("has disbanded the party") ||
                message.contains("You left the party") ||
                message.contains("You have been kicked from the party by") ||
                message.contains("The party was disbanded")) {
            isInParty = false;
            leader = null;
            members.clear();
            memberUuids.clear();
            SSUIndicator.setPartyMembers(List.of());
        }

        if (message.contains("has left the party") ||
                message.contains("has been removed from the party") ||
                message.contains("joined the party") ||
                (message.startsWith("You have joined") && message.contains("'s party")) ||
                message.contains("joined the dungeon group!") ||
                message.contains("was removed from your party because they disconnected.") ||
                message.contains("The party was transferred to")) {

            PartyTracker.request();
        }
    }
}