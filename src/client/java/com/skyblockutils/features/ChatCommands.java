package com.skyblockutils.features;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.utils.PlayerLookup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import java.util.List;

public class ChatCommands {
    public static void handleCommands(String message) {
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == null) return;

        String[] parts = message.split(": ", 2);
        if (parts.length < 2) return;

        String messageContent = parts[1];

        if (!messageContent.startsWith("!")) return;

        if (messageContent.equalsIgnoreCase("!tps"))
            networkHandler.sendChatMessage("Tps: " + String.format("%.1f", ModFunctions.tps));
        if (messageContent.equalsIgnoreCase("!ping")) networkHandler.sendChatMessage("Ping: " + ModFunctions.ping);

        // Silly messages
        List<String> allowedCommands = List.of("gay", "lesbian", "trans", "femboy", "racist");
        if (allowedCommands.contains(messageContent.replaceFirst("!", "").split(" ")[0])) {
            int randomPercentage = (int) (Math.random() * 100) + 1;

            if (messageContent.split(" ").length == 1) {
                String cleanMessageHeader = parts[0].replaceAll("\\[.*?]", "").trim();
                String messageSender = cleanMessageHeader.split(" ")[cleanMessageHeader.split(" ").length - 1];

                networkHandler.sendChatMessage(messageSender + " is " + randomPercentage + "% " + messageContent.replaceFirst("!", ""));
            } else {
                String username = messageContent.split(" ")[1];

                PlayerLookup.getFormattedUsername(username).thenAccept(formattedName ->
                        MinecraftClient.getInstance().execute(() -> {
                            if (formattedName == null) {
                                networkHandler.sendChatMessage(username + " is not a valid username");
                                return;
                            }

                            networkHandler.sendChatMessage(
                                    formattedName + " is " + randomPercentage + "% " +
                                            messageContent.replaceFirst("!", "").split(" ")[0]
                            );
                        })
                );
            }
        }
    }
}
