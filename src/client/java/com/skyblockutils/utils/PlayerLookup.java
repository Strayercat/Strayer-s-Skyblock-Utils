package com.skyblockutils.utils;

import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerLookup {
    private static final HttpClient client = HttpClient.newHttpClient();

    public static CompletableFuture<UUID> getUuidOffline(String username) {
        String url = "https://api.mojang.com/users/profiles/minecraft/" + username;

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        // This is the 32-character "dashless" string from Mojang
                        String dashlessId = JsonParser.parseString(response.body())
                                .getAsJsonObject()
                                .get("id").getAsString();

                        // FIX: Use your formatter to turn the Mojang string into a real UUID object
                        return formatMojangUuid(dashlessId);
                    }
                    return null;
                });
    }

    public static UUID formatMojangUuid(String id) {
        // Manually substring-ing is actually slightly faster than Regex!
        return UUID.fromString(
                id.substring(0, 8) + "-" +
                        id.substring(8, 12) + "-" +
                        id.substring(12, 16) + "-" +
                        id.substring(16, 20) + "-" +
                        id.substring(20, 32)
        );
    }
}