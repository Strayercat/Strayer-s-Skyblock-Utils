package com.skyblockutils.features.foraging;


import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.ChatBlockRouter;
import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.utils.SideBarUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TreeGiftNotifications {
    private static final Pattern PERCENT_SUFFIX = Pattern.compile("\\s*\\(\\d+(?:\\.\\d+)?%\\)");

    public static final ChatBlockRouter.Handler BLOCK_HANDLER = new ChatBlockRouter.Handler() {
        @Override
        public boolean isActive() {
            if (!ModConfig.INSTANCE.treeGiftNotification) return false;
            String area = ModFunctions.mapLocationToGeneralArea(SideBarUtils.location);
            return "Galatea".equals(area) || "Torrhus Canyon".equals(area);
        }

        @Override
        public boolean matches(String header) {
            return header.equals("TREE GIFT");
        }

        @Override
        public void accept(ChatBlockRouter.Block block) {
            parseLines(block.lines());
        }
    };

    private static void parseLines(List<Component> lines) {
        List<Component> fullList = new ArrayList<>();

        for (Component el : lines) {
            String text = el.getString().trim();
            if (text.isEmpty() || text.equals("TREE GIFT") || text.equals("BONUS GIFT")) continue;

            if (text.contains("rewards gained!")) {
                findHoverEvent(el).ifPresent(hoverEvent -> {
                    if (hoverEvent instanceof HoverEvent.ShowText(Component value)) {
                        extractHoverLines(value).forEach(line -> fullList.add(format(line)));
                    }
                });
            } else {
                if (text.contains("You helped cut") || text.contains("fell from the Tree!")) continue;
                fullList.add(format(el));
            }
        }

        OnScreenNotification.builder().title("§2TREE GIFT").subtitle(fullList).tickTime(ModConfig.INSTANCE.treeGiftNotificationTime).send();

        if (!ModConfig.INSTANCE.phantomTitle) return;

        Minecraft client = Minecraft.getInstance();

        if (lines.stream().anyMatch(e -> e.getString().contains("Phanflare")))
            ModFunctions.showTitle(client, "Phanflare", 40, false);
        if (lines.stream().anyMatch(e -> e.getString().contains("Dreadwing")))
            ModFunctions.showTitle(client, "Dreadwing", 40, false);
        if (lines.stream().anyMatch(e -> e.getString().contains("Phanpyre")))
            ModFunctions.showTitle(client, "Phanpyre", 40, false);
    }

    private static Optional<HoverEvent> findHoverEvent(Component component) {
        if (component.getStyle().getHoverEvent() != null) {
            return Optional.of(component.getStyle().getHoverEvent());
        }
        for (Component sibling : component.getSiblings()) {
            Optional<HoverEvent> found = findHoverEvent(sibling);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    private static List<Component> extractHoverLines(Component hoverText) {
        List<Component> lines = new ArrayList<>();
        final MutableComponent[] currentLine = {null};

        hoverText.visit((style, text) -> {
            String[] parts = text.split("\n", -1);

            for (int i = 0; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    Component segment = Component.literal(parts[i]).setStyle(style);
                    if (currentLine[0] == null) {
                        currentLine[0] = segment.copy();
                    } else {
                        currentLine[0].append(segment);
                    }
                }
                if (i < parts.length - 1) {
                    if (currentLine[0] != null) {
                        lines.add(currentLine[0]);
                        currentLine[0] = null;
                    }
                }
            }

            return Optional.<Void>empty();
        }, Style.EMPTY);

        if (currentLine[0] != null) lines.add(currentLine[0]);

        return lines;
    }

    private static Component format(Component line) {
        record Seg(String text, Style style) {
        }
        List<Seg> segs = new ArrayList<>();
        StringBuilder full = new StringBuilder();

        line.visit((style, text) -> {
            segs.add(new Seg(text, style));
            full.append(text);
            return Optional.<Void>empty();
        }, Style.EMPTY);

        String fullText = full.toString();
        Matcher matcher = PERCENT_SUFFIX.matcher(fullText);
        List<int[]> removeRanges = new ArrayList<>();
        while (matcher.find()) {
            removeRanges.add(new int[]{matcher.start(), matcher.end()});
        }

        MutableComponent result = null;
        int pos = 0;

        for (Seg seg : segs) {
            int segStart = pos;
            int segEnd = pos + seg.text().length();
            pos = segEnd;

            int cursor = segStart;
            for (int[] range : removeRanges) {
                int rStart = Math.max(range[0], segStart);
                int rEnd = Math.min(range[1], segEnd);
                if (rStart >= rEnd) continue;

                if (cursor < rStart) {
                    result = appendKept(result, seg.text().substring(cursor - segStart, rStart - segStart), seg.style());
                }
                cursor = Math.max(cursor, rEnd);
            }
            if (cursor < segEnd) {
                result = appendKept(result, seg.text().substring(cursor - segStart, segEnd - segStart), seg.style());
            }
        }

        return result != null ? result : Component.empty();
    }

    private static MutableComponent appendKept(MutableComponent result, String text, Style style) {
        if (text.isEmpty()) return result;
        text = text.replace("Experience", "XP");
        Component piece = Component.literal(text).setStyle(style);
        return result == null ? piece.copy() : result.append(piece);
    }
}