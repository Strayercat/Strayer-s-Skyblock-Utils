package com.skyblockutils.utils;


import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class MarkCoordinates {
    private static final List<String> coordinatesList = new ArrayList<>();

    public static void addCoordinates() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        coordinatesList.add("x:" + (int) client.player.getX() + " y:" + (int) client.player.getY() + " z:" + (int) client.player.getZ());
    }

    public static void logCoordinatesList() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.inGameHud.getChatHud().addMessage(Text.literal(coordinatesList.toString()));
    }

    public static void clearCoordinates() {
        coordinatesList.clear();
    }
}
