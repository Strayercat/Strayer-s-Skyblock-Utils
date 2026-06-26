package com.skyblockutils.utils;



import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MarkCoordinates {
    private static final List<String> coordinatesList = new ArrayList<>();

    public static void addCoordinates() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        coordinatesList.add("x:" + (int) client.player.getX() + " y:" + (int) client.player.getY() + " z:" + (int) client.player.getZ());
    }

    public static void logCoordinatesList() {
        Minecraft client = Minecraft.getInstance();
        client.gui.getChat().addClientSystemMessage(Component.literal(coordinatesList.toString()));
    }

    public static void clearCoordinates() {
        coordinatesList.clear();
    }
}
