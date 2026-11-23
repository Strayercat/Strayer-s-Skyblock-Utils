package com.skyblockutils.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class Resources {
    private Resources() {
    }

    public static JsonObject json(String path) {
        try (InputStream in = Resources.class.getResourceAsStream("/assets/" + SSU.NAMESPACE + "/" + path)) {
            if (in == null) return new JsonObject();
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            return new JsonObject();
        }
    }
}
