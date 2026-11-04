package com.skyblockutils.features.hud.layout;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.hud.CustomSidebar;
import com.skyblockutils.features.hud.SeparatedChatHud;
import com.skyblockutils.features.hud.SsuHud;
import com.skyblockutils.features.slayer.CarryTracker;
import com.skyblockutils.features.voice.VoiceHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class HudManager {
    public static final float MIN_SCALE = 0.25f;
    public static final float MAX_SCALE = 4f;

    private static final Map<HudElement, Bounds> lastBounds = new IdentityHashMap<>();
    private static List<HudElement> elements;

    private HudManager() {
    }

    public record Bounds(float x, float y, float scale, int width, int height) {
        public float scaledWidth() {
            return width * scale;
        }

        public float scaledHeight() {
            return height * scale;
        }

        public float right() {
            return x + scaledWidth();
        }

        public float bottom() {
            return y + scaledHeight();
        }

        public boolean contains(double mx, double my) {
            return mx >= x && mx < right() && my >= y && my < bottom();
        }

        public boolean intersects(Bounds other) {
            return x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
        }
    }

    public static List<HudElement> elements() {
        if (elements == null) {
            elements = List.of(
                    SsuHud.ELEMENT,
                    CarryTracker.ELEMENT,
                    CustomSidebar.ELEMENT,
                    VoiceHud.ELEMENT,
                    SeparatedChatHud.ELEMENT
            );
        }
        return elements;
    }

    private static Map<String, HudLayoutEntry> store() {
        if (ModConfig.INSTANCE.hudLayouts == null) ModConfig.INSTANCE.hudLayouts = new HashMap<>();
        return ModConfig.INSTANCE.hudLayouts;
    }

    public static HudLayoutEntry layout(HudElement element) {
        HudLayoutEntry entry = store().get(element.id);
        return entry != null ? entry.sanitize() : element.defaultLayout().sanitize();
    }

    public static HudLayoutEntry editable(HudElement element) {
        return store().computeIfAbsent(element.id, _ -> element.defaultLayout().copy()).sanitize();
    }

    public static void reset(HudElement element) {
        store().remove(element.id);
    }

    public static void resetAll() {
        store().clear();
    }

    public static int layoutWidth(HudElement element, boolean preview) {
        HudLayoutEntry layout = layout(element);
        int width = layout.width > 0 ? layout.width : element.autoWidth(preview);
        return Math.clamp(width, element.minWidth(), Math.max(element.minWidth(), element.maxWidth()));
    }

    public static Bounds bounds(HudElement element, boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        HudLayoutEntry layout = layout(element);

        int width = Math.max(1, element.contentWidth(layoutWidth(element, preview), preview));
        int height = Math.max(1, element.height(width, preview));

        float scale = Math.min(layout.scale, Math.min(screenWidth / (float) width, screenHeight / (float) height));
        float x = position(layout.anchorX, layout.offsetX, screenWidth, width * scale);
        float y = position(layout.anchorY, layout.offsetY, screenHeight, height * scale);
        return new Bounds(x, y, scale, width, height);
    }

    private static float position(HudLayoutEntry.Anchor anchor, float offset, int screen, float size) {
        float max = Math.max(0f, screen - size);
        float pos = switch (anchor) {
            case START -> offset;
            case CENTER -> max / 2f + offset;
            case END -> max - offset;
        };
        return Math.clamp(pos, 0f, max);
    }

    public static boolean isEditing() {
        return Minecraft.getInstance().gui.screen() instanceof HudEditScreen;
    }

    public static void render(HudElement element, GuiGraphicsExtractor graphics) {
        if (isEditing() || !element.isEnabled() || element.isActive()) return;
        draw(element, graphics, false);
    }

    public static Bounds draw(HudElement element, GuiGraphicsExtractor graphics, boolean preview) {
        Bounds bounds = bounds(element, preview);
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(bounds.x(), bounds.y());
            graphics.pose().scale(bounds.scale(), bounds.scale());
            element.render(new HudContext(graphics, bounds, preview));
        } finally {
            graphics.pose().popMatrix();
        }
        lastBounds.put(element, bounds);
        return bounds;
    }
}
