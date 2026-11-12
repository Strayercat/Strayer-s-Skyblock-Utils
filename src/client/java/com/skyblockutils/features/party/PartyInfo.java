package com.skyblockutils.features.party;

import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PartyInfo {
    public static boolean isInParty = false;
    public static @Nullable String leader = null;
    public static List<String> members = new ArrayList<>();

    public static boolean isLeader() {
        return !isInParty || leader == null || !leader.equalsIgnoreCase(Minecraft.getInstance().getUser().getName());
    }

    public static void handlePartyMessages(String message) {
        if (message.startsWith("You'll be partying with:")) {
            PartyListParser.requestListDelayed();
            return;
        }

        if (message.contains(":")) return;

        if (message.contains("has disbanded the party") ||
                message.contains("You left the party") ||
                message.contains("You have been kicked from the party by") ||
                message.contains("The party was disbanded") ||
                message.contains("You are not currently in a party")) {
            PartyListParser.clear();
            return;
        }

        if (message.contains("has left the party") ||
                message.contains("has been removed from the party") ||
                message.contains("joined the party") ||
                (message.startsWith("You have joined") && message.contains("'s party")) ||
                message.contains("joined the dungeon group!") ||
                message.contains("was removed from your party because they disconnected.") ||
                message.contains("The party was transferred to") ||
                message.contains("to Party Leader")) {

            PartyListParser.requestListDelayed();
        }
    }
}
