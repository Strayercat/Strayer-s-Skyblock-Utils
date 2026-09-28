package com.skyblockutils.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerLookup {
    private static final HttpClient client = HttpClient.newHttpClient();

    public record Profile(UUID uuid, String name) {}

    public static CompletableFuture<Profile> getProfile(String username) {
        String url = "https://api.mojang.com/users/profiles/minecraft/" + username;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) return null;
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    return new Profile(formatMojangUuid(json.get("id").getAsString()), json.get("name").getAsString());
                })
                .exceptionally(_ -> null);
    }

    public static CompletableFuture<UUID> getUuidOffline(String username) {
        return getProfile(username).thenApply(profile -> profile == null ? null : profile.uuid());
    }

    public static CompletableFuture<String> getFormattedUsername(String username) {
        return getProfile(username).thenApply(profile -> profile == null ? null : profile.name());
    }

    public static CompletableFuture<String> getNameByUuid(UUID uuid) {
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "");
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) return null;
                    return JsonParser.parseString(response.body()).getAsJsonObject().get("name").getAsString();
                })
                .exceptionally(_ -> null);
    }

    public static UUID formatMojangUuid(String id) {
        return UUID.fromString(
                id.substring(0, 8) + "-" +
                        id.substring(8, 12) + "-" +
                        id.substring(12, 16) + "-" +
                        id.substring(16, 20) + "-" +
                        id.substring(20, 32)
        );
    }
}