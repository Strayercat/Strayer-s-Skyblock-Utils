package com.skyblockutils.features.hud.layout;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.hud.BestiaryHud;
import com.skyblockutils.features.hud.CustomSidebar;
import com.skyblockutils.features.hud.SeparatedChatHud;
import com.skyblockutils.features.hud.SsuHud;
import com.skyblockutils.features.hud.StashHud;
import com.skyblockutils.features.slayer.CarryTracker;
import com.skyblockutils.features.slayer.SlayerTracker;
import com.skyblockutils.features.voice.VoiceHud;
import com.skyblockutils.hypixel.HypixelLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HudManager {
    public static final float MIN_SCALE = 0.25f;
    public static final float MAX_SCALE = 4f;
    public static final float SNAP_DISTANCE = 6f;
    private static final float TOUCH = 0.75f;
    private static final int MAX_CHAIN = 16;

    private static final Set<HudElement> resolving = new HashSet<>();

    private static List<HudElement> elements;
    private static String editLocation = HudLocations.DEFAULT;

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

    public record Attachment(boolean hideLeft, boolean hideTop, boolean hideRight, boolean hideBottom,
                             boolean squareTopLeft, boolean squareTopRight, boolean squareBottomLeft, boolean squareBottomRight) {
    }

    public record Snap(HudElement parent, HudLayoutEntry.Side side, float offset, float x, float y) {
    }

    public static List<HudElement> elements() {
        if (elements == null) {
            elements = List.of(
                    SsuHud.ELEMENT,
                    CarryTracker.ELEMENT,
                    SlayerTracker.ELEMENT,
                    CustomSidebar.ELEMENT,
                    VoiceHud.ELEMENT,
                    StashHud.ELEMENT,
                    BestiaryHud.ELEMENT,
                    SeparatedChatHud.ELEMENT
            );
        }
        return elements;
    }

    public static String currentLocation() {
        if (!HypixelLocation.isSkyBlock()) return HudLocations.DEFAULT;
        return HypixelLocation.mode();
    }

    public static String activeLocation() {
        return isEditing() ? editLocation : currentLocation();
    }

    public static String editLocation() {
        return editLocation;
    }

    public static void setEditLocation(String location) {
        editLocation = location == null ? HudLocations.DEFAULT : location;
    }

    private static Map<String, HudLayoutEntry> defaults() {
        if (ModConfig.INSTANCE.hudLayouts == null) ModConfig.INSTANCE.hudLayouts = new HashMap<>();
        return ModConfig.INSTANCE.hudLayouts;
    }

    private static Map<String, Map<String, HudLayoutEntry>> locationLayouts() {
        if (ModConfig.INSTANCE.hudLocationLayouts == null) ModConfig.INSTANCE.hudLocationLayouts = new HashMap<>();
        return ModConfig.INSTANCE.hudLocationLayouts;
    }

    private static Map<String, Map<String, Boolean>> visibility() {
        if (ModConfig.INSTANCE.hudVisibility == null) ModConfig.INSTANCE.hudVisibility = new HashMap<>();
        return ModConfig.INSTANCE.hudVisibility;
    }

    private static Map<String, Boolean> globalOptions() {
        if (ModConfig.INSTANCE.hudGlobalOptions == null) ModConfig.INSTANCE.hudGlobalOptions = new HashMap<>();
        return ModConfig.INSTANCE.hudGlobalOptions;
    }

    private static Map<String, HudLayoutEntry> store(String location) {
        if (location.isEmpty()) return defaults();
        return locationLayouts().computeIfAbsent(location, _ -> new HashMap<>());
    }

    public static HudLayoutEntry layout(HudElement element) {
        return layout(element, activeLocation());
    }

    public static HudLayoutEntry layout(HudElement element, String location) {
        if (!location.isEmpty()) {
            Map<String, HudLayoutEntry> overrides = locationLayouts().get(location);
            HudLayoutEntry entry = overrides == null ? null : overrides.get(element.id);
            if (entry != null) return entry.sanitize();
        }
        HudLayoutEntry entry = defaults().get(element.id);
        return entry != null ? entry.sanitize() : element.defaultLayout().sanitize();
    }

    public static boolean hasOverride(HudElement element, String location) {
        if (location.isEmpty()) return false;
        Map<String, HudLayoutEntry> overrides = locationLayouts().get(location);
        return overrides != null && overrides.containsKey(element.id);
    }

    public static HudLayoutEntry editable(HudElement element) {
        String location = activeLocation();
        if (location.isEmpty()) return defaults().computeIfAbsent(element.id, _ -> element.defaultLayout().copy()).sanitize();
        return store(location).computeIfAbsent(element.id, _ -> layout(element, HudLocations.DEFAULT).copy()).sanitize();
    }

    public static boolean isVisible(HudElement element, String location) {
        Map<String, Boolean> here = visibility().get(location);
        if (here != null && here.get(element.id) != null) return here.get(element.id);
        if (!location.isEmpty()) {
            Map<String, Boolean> fallback = visibility().get(HudLocations.DEFAULT);
            if (fallback != null && fallback.get(element.id) != null) return fallback.get(element.id);
        }
        return true;
    }

    public static boolean optionEnabled(HudElement element, HudElement.Option option) {
        if (!option.available().getAsBoolean()) return false;
        if (option.global()) {
            Boolean value = globalOptions().get(element.id + "." + option.key());
            return value != null ? value : option.defaultValue().getAsBoolean();
        }
        Map<String, Boolean> options = layout(element).options;
        Boolean value = options == null ? null : options.get(option.key());
        return value != null ? value : option.defaultValue().getAsBoolean();
    }

    public static void toggleOption(HudElement element, HudElement.Option option) {
        boolean enabled = optionEnabled(element, option);
        if (option.global()) {
            globalOptions().put(element.id + "." + option.key(), !enabled);
            return;
        }
        HudLayoutEntry layout = editable(element);
        if (layout.options == null) layout.options = new HashMap<>();
        layout.options.put(option.key(), !enabled);
    }

    public static boolean isShownInEditor(HudElement element) {
        return element.isEnabled() && isVisible(element, editLocation);
    }

    public static void setVisible(HudElement element, boolean visible) {
        visibility().computeIfAbsent(activeLocation(), _ -> new HashMap<>()).put(element.id, visible);
    }

    private static HudLayoutEntry base(HudElement element) {
        return activeLocation().isEmpty() ? element.defaultLayout().sanitize() : layout(element, HudLocations.DEFAULT);
    }

    public static void resetPosition(HudElement element) {
        HudLayoutEntry base = base(element);
        HudLayoutEntry layout = editable(element);
        layout.anchorX = base.anchorX;
        layout.anchorY = base.anchorY;
        layout.offsetX = base.offsetX;
        layout.offsetY = base.offsetY;
        layout.attachTo = base.attachTo;
        layout.attachSide = base.attachSide;
        layout.attachOffset = base.attachOffset;
    }

    public static void resetSize(HudElement element) {
        HudLayoutEntry base = base(element);
        HudLayoutEntry layout = editable(element);
        layout.scale = base.scale;
        layout.width = base.width;
    }

    public static boolean isSizeDefault(HudElement element) {
        HudLayoutEntry base = base(element);
        HudLayoutEntry layout = layout(element);
        return layout.scale == base.scale && layout.width == base.width;
    }

    public static void makeDefault(HudElement element) {
        String location = activeLocation();
        if (location.isEmpty()) return;
        defaults().put(element.id, layout(element).copy());
        Map<String, HudLayoutEntry> overrides = locationLayouts().get(location);
        if (overrides != null) {
            overrides.remove(element.id);
            if (overrides.isEmpty()) locationLayouts().remove(location);
        }
    }

    public static void useDefault(HudElement element) {
        String location = activeLocation();
        if (location.isEmpty()) return;
        Map<String, HudLayoutEntry> overrides = locationLayouts().get(location);
        if (overrides == null) return;
        overrides.remove(element.id);
        if (overrides.isEmpty()) locationLayouts().remove(location);
    }

    public static void reset(HudElement element) {
        String location = activeLocation();
        if (location.isEmpty()) {
            defaults().remove(element.id);
        } else {
            Map<String, HudLayoutEntry> overrides = locationLayouts().get(location);
            if (overrides != null) {
                overrides.remove(element.id);
                if (overrides.isEmpty()) locationLayouts().remove(location);
            }
        }
        Map<String, Boolean> here = visibility().get(location);
        if (here != null) {
            here.remove(element.id);
            if (here.isEmpty()) visibility().remove(location);
        }
    }

    public static void resetAll() {
        String location = activeLocation();
        if (location.isEmpty()) defaults().clear();
        else locationLayouts().remove(location);
        visibility().remove(location);
    }

    public static int layoutWidth(HudElement element, boolean preview) {
        HudLayoutEntry layout = layout(element);
        int width = layout.width > 0 ? layout.width : element.autoWidth(preview);
        return Math.clamp(width, element.minWidth(), Math.max(element.minWidth(), element.maxWidth()));
    }

    public static HudElement byId(String id) {
        if (id == null) return null;
        for (HudElement element : elements()) {
            if (element.id.equals(id)) return element;
        }
        return null;
    }

    public static HudElement parentOf(HudElement element) {
        HudLayoutEntry layout = layout(element);
        if (!layout.isAttached()) return null;
        HudElement parent = byId(layout.attachTo);
        return parent == element ? null : parent;
    }

    public static boolean dependsOn(HudElement element, HudElement target) {
        HudElement current = element;
        for (int i = 0; i < MAX_CHAIN && current != null; i++) {
            if (current == target) return true;
            current = parentOf(current);
        }
        return false;
    }

    public static boolean isPresent(HudElement element, boolean preview) {
        if (preview && isEditing()) return isShownInEditor(element);
        return isShowing(element);
    }

    public static Bounds bounds(HudElement element, boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        HudLayoutEntry layout = layout(element);

        int width = Math.max(1, element.contentWidth(layoutWidth(element, preview)));
        int height = Math.max(1, element.height(width, preview));
        float scale = Math.min(layout.scale, Math.min(screenWidth / (float) width, screenHeight / (float) height));
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;

        HudElement parent = parentOf(element);
        if (parent != null && !resolving.contains(parent) && resolving.add(element)) {
            try {
                Bounds p = bounds(parent, preview);
                boolean present = isPresent(parent, preview);
                float x;
                float y;
                switch (layout.attachSide) {
                    case BELOW -> {
                        x = p.x() + layout.attachOffset;
                        y = present ? p.bottom() : p.y();
                    }
                    case ABOVE -> {
                        x = p.x() + layout.attachOffset;
                        y = (present ? p.y() : p.bottom()) - scaledHeight;
                    }
                    case RIGHT -> {
                        x = present ? p.right() : p.x();
                        y = p.y() + layout.attachOffset;
                    }
                    default -> {
                        x = (present ? p.x() : p.right()) - scaledWidth;
                        y = p.y() + layout.attachOffset;
                    }
                }
                x = Math.clamp(x, 0f, Math.max(0f, screenWidth - scaledWidth));
                y = Math.clamp(y, 0f, Math.max(0f, screenHeight - scaledHeight));
                return new Bounds(x, y, scale, width, height);
            } finally {
                resolving.remove(element);
            }
        }

        float x = position(layout.anchorX, layout.offsetX, screenWidth, scaledWidth);
        float y = position(layout.anchorY, layout.offsetY, screenHeight, scaledHeight);
        return new Bounds(x, y, scale, width, height);
    }

    public static Snap findSnap(HudElement dragged, float x, float y, float w, float h) {
        Snap best = null;
        float bestGap = SNAP_DISTANCE;
        for (HudElement other : elements()) {
            if (other == dragged || !isPresent(other, true) || dependsOn(other, dragged)) continue;
            Bounds p = bounds(other, true);
            boolean xNear = x < p.right() + SNAP_DISTANCE && x + w > p.x() - SNAP_DISTANCE;
            boolean yNear = y < p.bottom() + SNAP_DISTANCE && y + h > p.y() - SNAP_DISTANCE;

            if (xNear) {
                float offset = alignOffset(x - p.x(), w, p.scaledWidth());
                float below = Math.abs(y - p.bottom());
                if (below <= bestGap) {
                    bestGap = below;
                    best = new Snap(other, HudLayoutEntry.Side.BELOW, offset, p.x() + offset, p.bottom());
                }
                float above = Math.abs(y + h - p.y());
                if (above <= bestGap) {
                    bestGap = above;
                    best = new Snap(other, HudLayoutEntry.Side.ABOVE, offset, p.x() + offset, p.y() - h);
                }
            }
            if (yNear) {
                float offset = alignOffset(y - p.y(), h, p.scaledHeight());
                float right = Math.abs(x - p.right());
                if (right <= bestGap) {
                    bestGap = right;
                    best = new Snap(other, HudLayoutEntry.Side.RIGHT, offset, p.right(), p.y() + offset);
                }
                float left = Math.abs(x + w - p.x());
                if (left <= bestGap) {
                    bestGap = left;
                    best = new Snap(other, HudLayoutEntry.Side.LEFT, offset, p.x() - w, p.y() + offset);
                }
            }
        }
        return best;
    }

    private static float alignOffset(float raw, float size, float parentSize) {
        if (Math.abs(raw) <= SNAP_DISTANCE) return 0f;
        if (Math.abs(raw + size - parentSize) <= SNAP_DISTANCE) return parentSize - size;
        if (Math.abs(raw + size / 2f - parentSize / 2f) <= SNAP_DISTANCE) return (parentSize - size) / 2f;
        return raw;
    }

    public static Attachment attachment(HudElement self, float x1, float y1, float x2, float y2, boolean preview) {
        boolean hideLeft = false, hideTop = false, hideRight = false, hideBottom = false;
        boolean topLeft = false, topRight = false, bottomLeft = false, bottomRight = false;
        if (self == null) return new Attachment(false, false, false, false, false, false, false, false);

        for (HudElement other : elements()) {
            if (other == self || !isPresent(other, preview)) continue;
            Bounds n = bounds(other, preview);
            boolean xOverlap = n.x() < x2 - TOUCH && n.right() > x1 + TOUCH;
            boolean yOverlap = n.y() < y2 - TOUCH && n.bottom() > y1 + TOUCH;
            boolean spansX = n.x() <= x1 + TOUCH && n.right() >= x2 - TOUCH;
            boolean spansY = n.y() <= y1 + TOUCH && n.bottom() >= y2 - TOUCH;
            boolean coversThemX = x1 <= n.x() + TOUCH && x2 >= n.right() - TOUCH;
            boolean coversThemY = y1 <= n.y() + TOUCH && y2 >= n.bottom() - TOUCH;

            if (xOverlap && Math.abs(n.bottom() - y1) <= TOUCH) {
                if (n.x() <= x1 + TOUCH) topLeft = true;
                if (n.right() >= x2 - TOUCH) topRight = true;
                if (spansX && !coversThemX) hideTop = true;
            }
            if (xOverlap && Math.abs(n.y() - y2) <= TOUCH) {
                if (n.x() <= x1 + TOUCH) bottomLeft = true;
                if (n.right() >= x2 - TOUCH) bottomRight = true;
                if (spansX) hideBottom = true;
            }
            if (yOverlap && Math.abs(n.right() - x1) <= TOUCH) {
                if (n.y() <= y1 + TOUCH) topLeft = true;
                if (n.bottom() >= y2 - TOUCH) bottomLeft = true;
                if (spansY && !coversThemY) hideLeft = true;
            }
            if (yOverlap && Math.abs(n.x() - x2) <= TOUCH) {
                if (n.y() <= y1 + TOUCH) topRight = true;
                if (n.bottom() >= y2 - TOUCH) bottomRight = true;
                if (spansY) hideRight = true;
            }
        }
        return new Attachment(hideLeft, hideTop, hideRight, hideBottom, topLeft, topRight, bottomLeft, bottomRight);
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

    public static boolean isShowing(HudElement element) {
        return element.isEnabled() && !element.isActive() && isVisible(element, currentLocation());
    }

    public static void render(HudElement element, GuiGraphicsExtractor graphics) {
        if (isEditing() || !isShowing(element)) return;
        draw(element, graphics, false);
    }

    public static Bounds draw(HudElement element, GuiGraphicsExtractor graphics, boolean preview) {
        Bounds bounds = bounds(element, preview);
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(bounds.x(), bounds.y());
            graphics.pose().scale(bounds.scale(), bounds.scale());
            element.render(new HudContext(graphics, element, bounds, preview));
        } finally {
            graphics.pose().popMatrix();
        }
        return bounds;
    }
}
