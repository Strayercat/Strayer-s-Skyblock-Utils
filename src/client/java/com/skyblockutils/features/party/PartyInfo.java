package com.skyblockutils.features.party;

import java.util.ArrayList;
import java.util.List;

public class PartyInfo {
    public static boolean isInParty = false;
    public static String leader;
    public static List<String> members = new ArrayList<>();

    public static void handlePartyMessages(String message) {
        if (message.contains(":")) return;

        if (message.contains("has disbanded the party") || message.contains("You left the party") || message.contains("You have been kicked from the party by") || message.contains("The party was disbanded")) {
            isInParty = false;
            leader = null;
            members.clear();
        }

        if (message.contains("has left the party") || message.contains("has been removed from the party")) {
            members.remove(extractPlayerNameFromFirstPosition(message));
        }

        if (message.contains("joined the party") || message.contains("joined the dungeon group!")) {
            members.add(extractPlayerNameFromFirstPosition(message));
        }

        if (message.contains("The party was transfered to")) {
            //TODO handle party leader leaving
            leader = message.replaceFirst("The party was transfered to", "").replaceAll("\\[[^]]+]", "").trim().split(" ")[0];
        }
    }

    public static String extractPlayerNameFromFirstPosition(String message) {
        String cleaned = message.replaceAll("\\[[^]]+]", "").replaceAll("Party Finder >", "").trim();
        return cleaned.split(" ")[0];
    }
}