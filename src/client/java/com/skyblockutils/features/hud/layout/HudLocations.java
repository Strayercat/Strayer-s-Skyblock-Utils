package com.skyblockutils.features.hud.layout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HudLocations {
    public static final String DEFAULT = "";

    private static final Map<String, String> NAMES = new LinkedHashMap<>();

    static {
        NAMES.put("hub", "Hub");
        NAMES.put("dynamic", "Private Island");
        NAMES.put("garden", "Garden");
        NAMES.put("farming_1", "The Farming Islands");
        NAMES.put("foraging_1", "The Park");
        NAMES.put("foraging_2", "Galatea");
        NAMES.put("combat_1", "Spider's Den");
        NAMES.put("combat_3", "The End");
        NAMES.put("crimson_isle", "Crimson Isle");
        NAMES.put("kuudra", "Kuudra");
        NAMES.put("mining_1", "Gold Mine");
        NAMES.put("mining_2", "Deep Caverns");
        NAMES.put("mining_3", "Dwarven Mines");
        NAMES.put("crystal_hollows", "Crystal Hollows");
        NAMES.put("mineshaft", "Glacite Mineshafts");
        NAMES.put("fishing_1", "Backwater Bayou");
        NAMES.put("winter", "Jerry's Workshop");
        NAMES.put("dungeon_hub", "Dungeon Hub");
        NAMES.put("dungeon", "Catacombs");
        NAMES.put("rift", "The Rift");
        NAMES.put("dark_auction", "Dark Auction");
    }

    private HudLocations() {
    }

    public static String name(String key) {
        if (key == null || key.isEmpty()) return "All Locations";
        String name = NAMES.get(key);
        return name != null ? name : prettify(key);
    }

    public static List<String> options(String current) {
        List<String> options = new ArrayList<>();
        options.add(DEFAULT);
        if (current != null && !current.isEmpty() && !NAMES.containsKey(current)) options.add(current);
        options.addAll(NAMES.keySet());
        return options;
    }

    private static String prettify(String key) {
        StringBuilder out = new StringBuilder();
        for (String word : key.split("_")) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }
}
