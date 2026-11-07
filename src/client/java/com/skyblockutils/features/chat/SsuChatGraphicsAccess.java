package com.skyblockutils.features.chat;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.ModStyle;
import com.skyblockutils.utils.RoundedRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2f;

import java.util.function.Consumer;

public final class SsuChatGraphicsAccess implements ChatComponent.ChatGraphicsAccess {
    private static final int CORNER_RADIUS = 4;
    private static final int VERTICAL_PADDING = 2;

    private final ChatComponent.ChatGraphicsAccess delegate;
    private final GuiGraphicsExtractor graphics;

    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int maxAlpha = 0;
    private boolean flushed = false;

    public SsuChatGraphicsAccess(ChatComponent.ChatGraphicsAccess delegate, GuiGraphicsExtractor graphics) {
        this.delegate = delegate;
        this.graphics = graphics;
    }

    @Override
    public void updatePose(@NotNull Consumer<Matrix3x2f> updater) {
        delegate.updatePose(updater);
    }

    @Override
    public void fill(int x0, int y0, int x1, int y1, int color) {
        if (!flushed && (color & 0xFFFFFF) == 0) {
            minX = Math.min(minX, Math.min(x0, x1));
            maxX = Math.max(maxX, Math.max(x0, x1));
            minY = Math.min(minY, Math.min(y0, y1));
            maxY = Math.max(maxY, Math.max(y0, y1));
            maxAlpha = Math.max(maxAlpha, color >>> 24);
            return;
        }
        delegate.fill(x0, y0, x1, y1, color);
    }

    @Override
    public boolean handleMessage(int textTop, float opacity, @NotNull FormattedCharSequence message) {
        flush();
        return delegate.handleMessage(textTop, opacity, message);
    }

    @Override
    public void handleTag(int x0, int y0, int x1, int y1, float opacity, @NotNull GuiMessageTag tag) {
        flush();
        delegate.handleTag(x0, y0, x1, y1, opacity, tag);
    }

    @Override
    public void handleTagIcon(int left, int bottom, boolean forceVisible, @NotNull GuiMessageTag tag, GuiMessageTag.@NotNull Icon icon) {
        flush();
        delegate.handleTagIcon(left, bottom, forceVisible, tag, icon);
    }

    private void flush() {
        if (flushed) return;
        flushed = true;
        if (maxX <= minX || maxY <= minY || maxAlpha == 0) return;

        float backgroundOpacity = Minecraft.getInstance().options.textBackgroundOpacity().get().floatValue();
        float visibility = backgroundOpacity <= 0f ? 1f : Math.min(1f, maxAlpha / (backgroundOpacity * 255f));
        int borderAlpha = Math.round(visibility * 255f) << 24;
        int border = borderAlpha | (ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN) & 0xFFFFFF);

        RoundedRect.panel(graphics, minX, minY - VERTICAL_PADDING, maxX, maxY + VERTICAL_PADDING, CORNER_RADIUS, maxAlpha << 24, border);
    }
}
