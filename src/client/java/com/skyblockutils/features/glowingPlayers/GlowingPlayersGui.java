package com.skyblockutils.features.glowingPlayers;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.glowingPlayers.GlowingPlayers.GlowingPlayer;
import com.skyblockutils.utils.CustomEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public class GlowingPlayersGui {
    public static boolean configScreenRequested = false;

    private static final Map<Screen, List<GlowingPlayer>> snapshots = new WeakHashMap<>();
    private static List<GlowingPlayer> sessionStart = List.of();
    private static Screen root;
    private static boolean saving = false;

    public static Screen createConfigScreen(Screen parent) {
        if (!snapshots.containsKey(parent)) startSession(parent);

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(new ReturnScreen(parent))
                .setTransparentBackground(false)
                .setDefaultBackgroundTexture(Identifier.fromNamespaceAndPath("skyblockutils", "textures/gui/background.png"))
                .setTitle(Component.literal("Glowing Players Config"))
                .setDoesConfirmSave(false)
                .setSavingRunnable(() -> saving = true);

        buildGlowingPlayersGui(builder);
        Screen screen = builder.build();
        snapshots.put(screen, copy(ModConfig.INSTANCE.getGlowingPlayers()));
        return screen;
    }

    public static void handleConfigScreen(Minecraft client) {
        if (configScreenRequested) client.setScreenAndShow(createConfigScreen(client.gui.screen()));
        configScreenRequested = false;
    }

    public static void refreshScreen(Minecraft client) {
        if (client.gui.screen() == null) return;
        Screen parent = client.gui.screen();
        client.setScreenAndShow(createConfigScreen(parent));
    }

    public static boolean hasUnsavedChanges() {
        return !sameList(ModConfig.INSTANCE.getGlowingPlayers(), sessionStart);
    }

    private static void buildGlowingPlayersGui(ConfigBuilder builder) {
        var category = builder.getOrCreateCategory(Component.literal(""));

        category.addEntry(new CustomEntry() {
                    @Override
                    public boolean isEdited() {
                        return hasUnsavedChanges();
                    }
                }
                        .addText("Glowing Players", CustomEntry.Alignment.LEFT, 0xFFFFFFFF)
                        .addButton(Component.literal("+"), 20, CustomEntry.Alignment.RIGHT, () -> GlowingPlayerCreationScreen.openScreen(Minecraft.getInstance().gui.screen()), Component.literal("Add Glowing Player"))
                        .addButton(Component.literal("⟳"), 20, CustomEntry.Alignment.RIGHT, () -> refreshScreen(Minecraft.getInstance()), Component.literal("Reload GUI"))
        );

        for (GlowingPlayer p : ModConfig.INSTANCE.getGlowingPlayers()) {
            category.addEntry(new GlowingPlayerEntry(p, () -> {
                GlowingPlayers.remove(p.username, true);
                refreshScreen(Minecraft.getInstance());
            }));
        }
    }

    private static void startSession(Screen parent) {
        snapshots.clear();
        root = parent;
        sessionStart = copy(ModConfig.INSTANCE.getGlowingPlayers());
        saving = false;
    }

    private static void returnTo(Screen target) {
        Minecraft client = Minecraft.getInstance();

        if (saving) {
            Screen exit = root;
            ModConfig.save();
            endSession();
            client.setScreenAndShow(exit);
            return;
        }

        List<GlowingPlayer> snapshot = snapshots.get(target);
        if (snapshot != null) {
            apply(snapshot);
            client.setScreenAndShow(target);
            return;
        }

        apply(sessionStart);
        endSession();
        client.setScreenAndShow(target);
    }

    private static void endSession() {
        snapshots.clear();
        sessionStart = List.of();
        root = null;
        saving = false;
    }

    private static void apply(List<GlowingPlayer> players) {
        List<GlowingPlayer> current = ModConfig.INSTANCE.getGlowingPlayers();
        current.clear();
        current.addAll(copy(players));
    }

    private static List<GlowingPlayer> copy(List<GlowingPlayer> players) {
        List<GlowingPlayer> copy = new ArrayList<>(players.size());
        for (GlowingPlayer p : players) copy.add(new GlowingPlayer(p.username, p.uuid, p.color));
        return copy;
    }

    private static boolean sameList(List<GlowingPlayer> a, List<GlowingPlayer> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            GlowingPlayer x = a.get(i);
            GlowingPlayer y = b.get(i);
            if (!x.username.equalsIgnoreCase(y.username) || !Objects.equals(x.uuid, y.uuid) || x.color != y.color) return false;
        }
        return true;
    }

    private static class ReturnScreen extends Screen {
        private final Screen target;
        private boolean handled = false;

        ReturnScreen(Screen target) {
            super(Component.empty());
            this.target = target;
        }

        @Override
        protected void init() {
            if (handled) return;
            handled = true;
            Minecraft.getInstance().execute(() -> returnTo(target));
        }

        @Override
        public void removed() {
            handled = false;
        }
    }
}