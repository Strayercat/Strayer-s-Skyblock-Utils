package com.skyblockutils.utils;

import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

public final class SkyblockAreas {
    private static final Map<String, String> AREAS = new HashMap<>();

    static {
        Resources.json("data/areas.json").entrySet().forEach(area -> {
            for (JsonElement location : area.getValue().getAsJsonArray()) AREAS.put(location.getAsString(), area.getKey());
        });
    }

    private SkyblockAreas() {
    }

    public static String of(String location) {
        if (location == null) return "Unknown";
        return AREAS.getOrDefault(location, "");
    }

    public static String current() {
        return of(SideBarUtils.location);
    }
}
