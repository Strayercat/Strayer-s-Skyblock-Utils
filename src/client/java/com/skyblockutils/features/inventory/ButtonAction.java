package com.skyblockutils.features.inventory;

import com.skyblockutils.ModCommands;
import com.skyblockutils.config.ClothConfigHandler;
import com.skyblockutils.features.browser.InGameBrowser;
import com.skyblockutils.features.glowingPlayers.GlowingPlayersGui;
import com.skyblockutils.features.hud.layout.HudEditScreen;
import com.skyblockutils.features.wiki.WikiLookup;
import com.skyblockutils.utils.ScreenOpener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.Locale;

public record ButtonAction(Type type, String value) {
    public static final ButtonAction NONE = new ButtonAction(Type.COMMAND, "");

    public enum Type {
        COMMAND("Command", ""),
        CHAT("Chat", "chat:"),
        URL("URL", "url:"),
        SSU("SSU", "ssu:");

        public final String label;
        public final String prefix;

        Type(String label, String prefix) {
            this.label = label;
            this.prefix = prefix;
        }
    }

    public enum Ssu {
        WIKI("wiki", "Wiki"),
        SKYCRYPT("skycrypt", "My SkyCrypt"),
        HUD("hud", "HUD Editor"),
        CONFIG("config", "SSU Config"),
        BUTTONS("buttons", "Button Editor"),
        GLOWING_PLAYERS("glowingplayers", "Glowing Players");

        public final String id;
        public final String label;

        Ssu(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public static Ssu byId(String id) {
            for (Ssu action : values()) if (action.id.equalsIgnoreCase(id.trim())) return action;
            return null;
        }

        void run() {
            switch (this) {
                case WIKI -> WikiLookup.openHomeFromCommand();
                case SKYCRYPT -> ModCommands.openSkyCrypt(Minecraft.getInstance().getUser().getName());
                case HUD -> ScreenOpener.openNextTick(HudEditScreen::new);
                case CONFIG -> ScreenOpener.openNextTick(ClothConfigHandler::createConfigScreen);
                case BUTTONS -> ScreenOpener.openNextTick(ButtonEditScreen::new);
                case GLOWING_PLAYERS -> ScreenOpener.openNextTick(GlowingPlayersGui::createConfigScreen);
            }
        }
    }

    public static ButtonAction parse(String raw) {
        if (raw == null || raw.isBlank()) return NONE;
        String trimmed = raw.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        for (Type type : Type.values()) {
            if (!type.prefix.isEmpty() && lower.startsWith(type.prefix)) {
                return new ButtonAction(type, trimmed.substring(type.prefix.length()).trim());
            }
        }
        return new ButtonAction(Type.COMMAND, trimmed.startsWith("/") ? trimmed.substring(1) : trimmed);
    }

    public static String serialize(Type type, String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) return type.prefix;
        if (type == Type.COMMAND && trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        return type.prefix + trimmed;
    }

    public boolean isEmpty() {
        return value.isBlank();
    }

    public static boolean isEmpty(String raw) {
        return parse(raw).isEmpty();
    }

    public String describe() {
        return switch (type) {
            case COMMAND -> "/" + value;
            case CHAT -> "Say \"" + value + "\"";
            case URL -> value;
            case SSU -> {
                Ssu action = Ssu.byId(value);
                yield action == null ? "SSU: " + value : action.label;
            }
        };
    }

    public void run(Screen screen) {
        if (isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        switch (type) {
            case COMMAND -> {
                if (client.getConnection() == null) return;
                if (screen != null) screen.clearFocus();
                client.getConnection().sendCommand(value);
            }
            case CHAT -> {
                if (client.getConnection() == null) return;
                if (screen != null) screen.clearFocus();
                client.getConnection().sendChat(value);
            }
            case URL -> InGameBrowser.openFromCommand(value.contains("://") ? value : "https://" + value);
            case SSU -> {
                Ssu action = Ssu.byId(value);
                if (action != null) action.run();
            }
        }
    }
}
