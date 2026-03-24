package com.skyblockutils.features;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.utils.PlayerLookup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import java.util.List;

public class ChatCommands {
    public static void handleCommands(String message) {
        String[] parts = message.split(": ", 2);
        if (parts.length < 2) return;

        String messageContent = parts[1];
        if (!messageContent.startsWith("!")) return;

        String messageChannel;

        if (parts[0].contains(">")) {
            messageChannel = parts[0].split(">")[0].trim();
        } else if (message.startsWith("From")) {
            messageChannel = "From";
        } else {
            messageChannel = "all";
        }

        if (messageContent.equalsIgnoreCase("!tps"))
            sendMessageInChannel("Tps: " + String.format("%.1f", ModFunctions.tps), messageChannel);
        if (messageContent.equalsIgnoreCase("!ping"))
            sendMessageInChannel("Ping: " + ModFunctions.ping, messageChannel);

        // Silly messages
        List<String> allowedCommands = List.of("gay", "lesbian", "trans", "femboy", "racist", "sus");
        String command = messageContent.replaceFirst("!", "").split(" ")[0].trim().toLowerCase();
        System.out.println(command);
        if (allowedCommands.contains(command)) {
            int randomPercentage = (int) (Math.random() * 100) + 1;

            if (messageContent.split(" ").length == 1) {
                String cleanMessageHeader = parts[0].replaceAll("\\[.*?]", "").trim();
                String messageSender = cleanMessageHeader.split(" ")[cleanMessageHeader.split(" ").length - 1];
                sendMessageInChannel(messageSender + " is " + randomPercentage + "% " + command, messageChannel);
            } else {
                String username = messageContent.split(" ")[1];

                PlayerLookup.getFormattedUsername(username).thenAccept(formattedName ->
                        MinecraftClient.getInstance().execute(() -> {
                            if (formattedName == null) {
                                sendMessageInChannel(username + " is not a valid username", messageChannel);
                                return;
                            }

                            sendMessageInChannel(formattedName + " is " + randomPercentage + "% " + command, messageChannel);
                        })
                );
            }
        }
    }

    public static void sendMessageInChannel(String message, String channel) {
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == null) return;

        String commandPrefix;
        switch (channel) {
            case "Guild" -> commandPrefix = "gc";
            case "Party" -> commandPrefix = "pc";
            case "Co-op" -> commandPrefix = "cc";
            case "Officer" -> commandPrefix = "oc";
            case "From" -> commandPrefix = "r";
            default -> commandPrefix = "ac";
        }

        networkHandler.sendChatCommand(commandPrefix + " " + message);
    }
}
