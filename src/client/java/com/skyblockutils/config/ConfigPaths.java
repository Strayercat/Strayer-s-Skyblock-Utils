package com.skyblockutils.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigPaths {
    public static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("SSU");
    public static final Path CONFIG = ROOT.resolve("config.json");
    public static final Path CHAT_FILTERS = ROOT.resolve("ChatFilters.json");
    public static final Path BUTTONS_DIR = ROOT.resolve("buttons");
    public static final Path BUTTONS = BUTTONS_DIR.resolve("Buttons.json");
    public static final Path SAVED_BUTTONS = BUTTONS_DIR.resolve("saved");
    public static final Path CACHE = ROOT.resolve("cache");
    public static final Path BACKUP = ROOT.resolve("old");
    public static final Path LEGACY_CONFIG = FabricLoader.getInstance().getConfigDir().resolve("SkyblockUtils.json");

    private ConfigPaths() {
    }

    public static void ensureDirectories() {
        for (Path path : new Path[]{ROOT, BUTTONS_DIR, SAVED_BUTTONS, CACHE}) {
            try {
                Files.createDirectories(path);
            } catch (IOException ignored) {
            }
        }
    }
}
