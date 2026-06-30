package com.skyblockutils.utils;

import com.skyblockutils.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

public class OnScreenNotification {
    private static final int WIDTH = 145;
    private static final int MARGIN = 2;
    private static final int PADDING = 5;

    private static final List<String> HOVER_TEXT = List.of("Right click to dismiss", "Middle click to copy");

    private static class Notification {
        String title;
        String subtitle;
        int ticks;
        int maxTicks;
        int height;
        int x, y;
        int titleColor;
        boolean copied = false;
        long copiedTimestamp = 0;

        Notification(String title, String subtitle, int tickTime, int titleColor) {
            this.title = title;
            this.subtitle = subtitle;
            this.ticks = tickTime;
            this.maxTicks = tickTime;
            this.height = calculateHeight(title, subtitle);
            this.titleColor = titleColor;
        }

        private int calculateHeight(String title, String subtitle) {
            Minecraft mc = Minecraft.getInstance();
            int lineHeight = mc.font.lineHeight + 2;
            int titleLines = wrapText(title).size();
            int subtitleLines = wrapText(subtitle).size();
            return (titleLines + subtitleLines) * lineHeight + PADDING * 2;
        }

        boolean isClicked(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + height;
        }
    }

    private static final Queue<Notification> notifications = new LinkedList<>();

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title = "";
        private String subtitle = "";
        private int tickTime = 60;
        private boolean withSound = false;
        private int titleColor = 0xFFFFFF;

        public Builder title(String title) {
            this.title = title;
            this.titleColor = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.TITLE_END) & 0xFFFFFF;
            return this;
        }

        public Builder title(Component title) {
            this.title = toLegacyString(title);
            this.titleColor = 0xFFFFFF;
            return this;
        }

        public Builder subtitle(String subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public Builder subtitle(Component subtitle) {
            this.subtitle = toLegacyString(subtitle);
            return this;
        }

        public Builder subtitle(List<?> lines) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) sb.append("\n");
                Object line = lines.get(i);
                sb.append(line instanceof Component c ? toLegacyString(c) : line.toString());
            }
            this.subtitle = sb.toString();
            return this;
        }

        public Builder tickTime(int tickTime) {
            this.tickTime = tickTime;
            return this;
        }

        public Builder withSound(boolean withSound) {
            this.withSound = withSound;
            return this;
        }

        public void send() {
            notifications.add(new Notification(title, subtitle, tickTime, titleColor));

            if (withSound) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.playSound(SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value());
                }
            }
        }
    }

    public static void tick() {
        notifications.removeIf(n -> n.ticks <= 0);
        notifications.forEach(n -> n.ticks--);
    }

    public static void render(GuiGraphicsExtractor context, int screenWidth, int screenHeight) {
        if (notifications.isEmpty()) return;

        int yOffset = 0;
        Minecraft mc = Minecraft.getInstance();

        double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getHeight();

        for (Notification notif : notifications) {
            notif.x = screenWidth - WIDTH;
            notif.y = screenHeight - notif.height - yOffset;

            float alpha = Math.min(1.0f, notif.ticks / 20.0f);
            int alphaInt = (int) (alpha * 255) << 24;

            boolean isHovered = notif.isClicked((int) mouseX, (int) mouseY);

            context.fill(notif.x, notif.y, notif.x + WIDTH, notif.y + notif.height, 0x1a1a1a | alphaInt);

            int borderColor = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
            context.fill(notif.x, notif.y, notif.x + WIDTH, notif.y + 2, borderColor | alphaInt);

            var titleLines = wrapText(notif.title);
            var subtitleLines = wrapText(notif.subtitle);

            int lineY = notif.y + PADDING;
            int lineStep = mc.font.lineHeight + 2;

            for (String line : titleLines) {
                context.text(mc.font, line, notif.x + PADDING, lineY, notif.titleColor | alphaInt, false);
                lineY += lineStep;
            }

            for (String line : subtitleLines) {
                context.text(mc.font, line, notif.x + PADDING, lineY, 0xAAAAAA | alphaInt, false);
                lineY += lineStep;
            }

            if (isHovered && !notif.copied) {
                int overlayTop = notif.y + 2;
                int overlayBottom = notif.y + notif.height;

                float overlayOpacity = 0.85f;
                int overlayAlphaInt = (int) (alpha * overlayOpacity * 255) << 24;
                context.fill(notif.x, overlayTop, notif.x + WIDTH, overlayBottom, 0x2a2a2a | overlayAlphaInt);

                drawCenteredOverlayText(context, mc, HOVER_TEXT, notif.x, overlayTop, overlayBottom, alphaInt);
            }

            if (notif.copied && notif.copiedTimestamp + 2000 > System.currentTimeMillis()) {
                int overlayTop = notif.y + 2;
                int overlayBottom = notif.y + notif.height;

                float overlayOpacity = 0.85f;
                int overlayAlphaInt = (int) (alpha * overlayOpacity * 255) << 24;
                context.fill(notif.x, overlayTop, notif.x + WIDTH, overlayBottom, 0x2a2a2a | overlayAlphaInt);

                drawCenteredOverlayText(context, mc, List.of("Copied ✓"), notif.x, overlayTop, overlayBottom, alphaInt);
            }

            yOffset += notif.height + MARGIN;
        }
    }

    private static void drawCenteredOverlayText(GuiGraphicsExtractor context, Minecraft mc, List<String> lines,
                                                int x, int top, int bottom, int alphaInt) {
        int lineStep = mc.font.lineHeight + 2;
        int blockHeight = lines.size() * lineStep - 2;

        int lineY = top + (bottom - top - blockHeight) / 2;
        for (String line : lines) {
            int lineWidth = mc.font.width(line);
            int lineX = x + (OnScreenNotification.WIDTH - lineWidth) / 2;
            context.text(mc.font, line, lineX, lineY, 16777215 | alphaInt, false);
            lineY += lineStep;
        }
    }

    private static List<String> wrapText(String text) {
        List<String> lines = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();

        String[] paragraphs = text.split("\n", -1);

        for (String paragraph : paragraphs) {
            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                if (mc.font.width(testLine) <= 135) {
                    currentLine = new StringBuilder(testLine);
                } else {
                    if (!currentLine.isEmpty()) {
                        lines.add(currentLine.toString());
                    }
                    currentLine = new StringBuilder(word);
                }
            }

            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }
        }

        return lines;
    }

    public static String toLegacyString(Component component) {
        StringBuilder sb = new StringBuilder();
        component.visit((style, text) -> {
            sb.append(styleToLegacyCodes(style)).append(text);
            return Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }

    private static String styleToLegacyCodes(Style style) {
        StringBuilder sb = new StringBuilder();

        TextColor color = style.getColor();
        if (color != null) {
            try {
                sb.append(ChatFormatting.valueOf(color.serialize().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (style.isBold()) sb.append(ChatFormatting.BOLD);
        if (style.isItalic()) sb.append(ChatFormatting.ITALIC);
        if (style.isUnderlined()) sb.append(ChatFormatting.UNDERLINE);
        if (style.isStrikethrough()) sb.append(ChatFormatting.STRIKETHROUGH);
        if (style.isObfuscated()) sb.append(ChatFormatting.OBFUSCATED);

        return sb.toString();
    }

    public static Component removeText(Component component, String target) {
        MutableComponent result = Component.empty();
        component.visit((style, text) -> {
            String replaced = text.replace(target, "");
            if (!replaced.isEmpty()) {
                result.append(Component.literal(replaced).setStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    public static boolean handleNotificationClicks(int mouseX, int mouseY, int button, int screenWidth, int screenHeight) {
        int yOffset = 0;
        Notification toRemove = null;

        for (Notification notif : notifications) {
            int x = screenWidth - WIDTH;
            int y = screenHeight - notif.height - yOffset;

            boolean hit = mouseX >= x && mouseX <= x + WIDTH
                    && mouseY >= y && mouseY <= y + notif.height;

            if (hit) {
                if (button == 2) {
                    ClipboardUtils.copyTextToClipboard(notif.subtitle.replaceAll("\n", " "));
                    notif.copied = true;
                    notif.copiedTimestamp = System.currentTimeMillis();
                }

                if (button == 1) {
                    toRemove = notif;
                }

                if (button == 0) {
                    if (notif.title.contains("PARTY INVITE")) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.connection.sendChat("/p accept " + notif.subtitle.split(" ")[0]);
                        }
                    }
                    toRemove = notif;
                }
                break;
            }

            yOffset += notif.height + MARGIN;
        }

        if (toRemove != null) {
            notifications.remove(toRemove);
            return true;
        }
        return false;
    }
}