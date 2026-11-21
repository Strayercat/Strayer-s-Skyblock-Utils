package com.skyblockutils.features.hud;

import com.skyblockutils.ModKeyBindings;
import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.chat.ChatLayout;
import com.skyblockutils.features.chat.SeparatedChat;
import com.skyblockutils.features.hud.layout.HudElement;
import com.skyblockutils.features.hud.layout.HudLayoutEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class SeparatedChatHud {
    public static final MessageHud HUD = new MessageHud(
            "system_chat", "System Messages", "System Chat", "No system messages yet", "System message copied to clipboard",
            SeparatedChat.FEED, () -> ModKeyBindings.SYSTEM_CHAT_HISTORY_KEY,
            () -> ModConfig.INSTANCE.separateMessage, () -> StrayersSkyblockUtilsClient.isInSkyblock,
            () -> {
                float scale = ChatLayout.chatScale();
                return new HudLayoutEntry(HudLayoutEntry.Anchor.END, HudLayoutEntry.Anchor.START, 0, 0, scale > 0 ? scale : 1f);
            });
    public static final HudElement ELEMENT = HUD.element;

    private SeparatedChatHud() {
    }

    public static boolean separatingHere() {
        return HUD.liveHere();
    }

    public static void tick() {
        HUD.tick();
    }

    public static void render(GuiGraphicsExtractor graphics) {
        HUD.render(graphics);
    }
}
