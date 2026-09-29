package com.skyblockutils.utils;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.DailyReminders;
import com.skyblockutils.features.party.PartyInfo;
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
import java.util.stream.Stream;

public class OnScreenNotification {
    private static final int WIDTH = 120;
    private static final int MARGIN = 2;
    private static final int PADDING = 5;
    private static final int CORNER_RADIUS = 4;
    private static final int DIVIDER_HEIGHT = 1;
    private static final int DIVIDER_MARGIN = 2;
    private static final int DIVIDER_SPACE = DIVIDER_HEIGHT + DIVIDER_MARGIN * 2;

    private static final int SCREEN_MARGIN_X = 3;
    private static final int SCREEN_MARGIN_Y = 3;

    private static final float TEXT_SCALE = 0.8f;

    private static final int INVITE_EXPIRE_TICKS = 100;
    private static final String EXPIRED_TITLE = "Invite Expired";
    private static final int ERROR_ACCENT_COLOR = 0xFF5555;
    private static final int ERROR_BACKGROUND_COLOR = 0x3a1414;
    private static final int BACKGROUND_COLOR = 0x1a1a1a;

    private static final List<String> HOVER_TEXT = List.of("Right click to dismiss", "Middle click to copy");
    private static final List<String> ALREADY_IN_PARTY_TEXT = List.of("Already in a party");
    private static final List<String> COPIED_TEXT = List.of("Copied ✓");

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
        boolean error = false;
        long errorTimestamp = 0;
        Object metadata = null;

        Notification(String title, String subtitle, int tickTime, int titleColor, Object metadata) {
            this.title = title;
            this.subtitle = subtitle;
            this.ticks = tickTime;
            this.maxTicks = tickTime;
            this.height = calculateHeight(title, subtitle);
            this.titleColor = titleColor;
            this.metadata = metadata;
        }

        private int calculateHeight(String title, String subtitle) {
            int lineStep = scaledLineStep();
            int titleLines = wrapText(title).size();
            int subtitleLines = wrapText(subtitle).size();
            return titleLines * lineStep + DIVIDER_SPACE + subtitleLines * lineStep + PADDING * 2;
        }

        boolean isClicked(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + height;
        }

        boolean isPartyInvite() {
            return title.contains("PARTY INVITE");
        }

        boolean isExpiredInvite() {
            return isPartyInvite() && ticks <= INVITE_EXPIRE_TICKS;
        }

        String inviter() {
            return subtitle.split(" ")[0];
        }

        String displayTitle() {
            return isExpiredInvite() ? EXPIRED_TITLE : title;
        }

        int displayTitleColor() {
            return isExpiredInvite() ? ERROR_ACCENT_COLOR : titleColor;
        }

        int backgroundColor() {
            return isExpiredInvite() ? ERROR_BACKGROUND_COLOR : BACKGROUND_COLOR;
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
        private Object metadata = null;

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
            boolean first = true;

            for (Object line : lines) {
                String text = line instanceof Component c ? toLegacyString(c) : line.toString();
                if (text.isBlank()) continue;

                if (!first) sb.append("\n");
                sb.append(text);
                first = false;
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

        public Builder metadata(Object metadata) {
            this.metadata = metadata;
            return this;
        }

        public void send() {
            notifications.add(new Notification(title, subtitle, tickTime, titleColor, metadata));

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
        if (Minecraft.getInstance().level == null) return;

        if (ModConfig.INSTANCE.notificationStyle == ModStyle.NotificationStyle.LEGACY) {
            renderLegacy(context, screenWidth, screenHeight);
        } else {
            renderRounded(context, screenWidth, screenHeight);
        }
    }

    private static List<String> hoverTextFor(Notification notif) {
        if (notif.isExpiredInvite()) {
            return Stream.concat(Stream.of("Click to invite " + ChatFormatting.stripFormatting(notif.inviter())), HOVER_TEXT.stream()).toList();
        } else if (notif.isPartyInvite()) {
            return Stream.concat(Stream.of("Click to join"), HOVER_TEXT.stream()).toList();
        } else if (notif.title.contains("Daily Reminder")) {
            return Stream.concat(Stream.of("Click to ignore for the day"), HOVER_TEXT.stream()).toList();
        } else if (notif.title.contains("Boop")) {
            return Stream.concat(Stream.of("Click to party them"), HOVER_TEXT.stream()).toList();
        }
        return HOVER_TEXT;
    }

    private static void renderLegacy(GuiGraphicsExtractor context, int screenWidth, int screenHeight) {
        if (notifications.isEmpty()) return;

        int yOffset = 0;
        Minecraft mc = Minecraft.getInstance();

        boolean guiOpen = mc.gui.screen() != null;

        double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getHeight();

        int accentColor = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);

        for (Notification notif : notifications) {
            notif.x = screenWidth - WIDTH;
            notif.y = screenHeight - notif.height - yOffset;

            float alpha = Math.min(1.0f, notif.ticks / 20.0f);
            int alphaInt = (int) (alpha * 255) << 24;

            boolean isHovered = guiOpen && notif.isClicked((int) mouseX, (int) mouseY);
            boolean expired = notif.isExpiredInvite();

            context.fill(notif.x, notif.y, notif.x + WIDTH, notif.y + notif.height, notif.backgroundColor() | alphaInt);

            int borderColor = expired ? ERROR_ACCENT_COLOR : accentColor;
            context.fill(notif.x, notif.y, notif.x + WIDTH, notif.y + 2, borderColor | alphaInt);

            var titleLines = wrapText(notif.displayTitle());
            var subtitleLines = wrapText(notif.subtitle);

            int lineY = notif.y + PADDING;
            int lineStep = mc.font.lineHeight + 2;

            for (String line : titleLines) {
                context.text(mc.font, line, notif.x + PADDING, lineY, notif.displayTitleColor() | alphaInt, false);
                lineY += lineStep;
            }

            for (String line : subtitleLines) {
                context.text(mc.font, line, notif.x + PADDING, lineY, 0xAAAAAA | alphaInt, false);
                lineY += lineStep;
            }

            int overlayTop = notif.y + 2;
            int overlayBottom = notif.y + notif.height;
            int overlayAlphaInt = (int) (alpha * 0.85f * 255) << 24;

            if (isHovered && !notif.copied) {
                context.fill(notif.x, overlayTop, notif.x + WIDTH, overlayBottom, 0x2a2a2a | overlayAlphaInt);
                drawCenteredOverlayText(context, mc, hoverTextFor(notif), notif.x, overlayTop, overlayBottom, alphaInt);
            }

            if (notif.error && notif.errorTimestamp + 2000 > System.currentTimeMillis()) {
                context.fill(notif.x, overlayTop, notif.x + WIDTH, overlayBottom, 0x5a1a1a | overlayAlphaInt);
                drawCenteredOverlayText(context, mc, ALREADY_IN_PARTY_TEXT, notif.x, overlayTop, overlayBottom, alphaInt);
            } else if (notif.copied && notif.copiedTimestamp + 2000 > System.currentTimeMillis()) {
                context.fill(notif.x, overlayTop, notif.x + WIDTH, overlayBottom, 0x2a2a2a | overlayAlphaInt);
                drawCenteredOverlayText(context, mc, COPIED_TEXT, notif.x, overlayTop, overlayBottom, alphaInt);
            }

            yOffset += notif.height + MARGIN;
        }
    }

    private static void renderRounded(GuiGraphicsExtractor context, int screenWidth, int screenHeight) {
        if (notifications.isEmpty()) return;

        int yOffset = 0;
        Minecraft mc = Minecraft.getInstance();

        boolean guiOpen = mc.gui.screen() != null;

        double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getHeight();

        int accentColor = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);

        for (Notification notif : notifications) {
            notif.x = screenWidth - WIDTH - SCREEN_MARGIN_X;
            notif.y = screenHeight - notif.height - yOffset - SCREEN_MARGIN_Y;

            float alpha = Math.min(1.0f, notif.ticks / 20.0f);
            int alphaInt = (int) (alpha * 255) << 24;

            boolean isHovered = guiOpen && notif.isClicked((int) mouseX, (int) mouseY);
            boolean expired = notif.isExpiredInvite();
            int accent = expired ? ERROR_ACCENT_COLOR : accentColor;

            fillRounded(context, notif.x, notif.y, notif.x + WIDTH, notif.y + notif.height,
                    accent | alphaInt, CORNER_RADIUS);
            fillRounded(context, notif.x + 1, notif.y + 1, notif.x + WIDTH - 1, notif.y + notif.height - 1,
                    notif.backgroundColor() | alphaInt, Math.max(0, CORNER_RADIUS - 1));

            var titleLines = wrapText(notif.displayTitle());
            var subtitleLines = wrapText(notif.subtitle);

            int lineY = notif.y + PADDING;
            int lineStep = scaledLineStep();

            for (String line : titleLines) {
                int scaledWidth = Math.round(mc.font.width(line) * TEXT_SCALE);
                int lineX = notif.x + (WIDTH - scaledWidth) / 2;
                drawScaledText(context, mc, line, lineX, lineY, notif.displayTitleColor() | alphaInt);
                lineY += lineStep;
            }

            lineY += DIVIDER_MARGIN;
            context.fill(notif.x + 1, lineY, notif.x + WIDTH - 1, lineY + DIVIDER_HEIGHT, accent | alphaInt);
            lineY += DIVIDER_HEIGHT + DIVIDER_MARGIN;

            for (String line : subtitleLines) {
                drawScaledText(context, mc, line, notif.x + PADDING, lineY, 0xAAAAAA | alphaInt);
                lineY += lineStep;
            }

            int overlayTop = notif.y + 1;
            int overlayBottom = notif.y + notif.height - 1;
            int overlayAlphaInt = (int) (alpha * 0.85f * 255) << 24;

            if (isHovered && !notif.copied) {
                context.fill(notif.x + 1, overlayTop, notif.x + WIDTH - 1, overlayBottom, 0x2a2a2a | overlayAlphaInt);
                drawCenteredOverlayText(context, mc, hoverTextFor(notif), notif.x, overlayTop, overlayBottom, alphaInt);
            }

            if (notif.error && notif.errorTimestamp + 2000 > System.currentTimeMillis()) {
                context.fill(notif.x + 1, overlayTop, notif.x + WIDTH - 1, overlayBottom, 0x5a1a1a | overlayAlphaInt);
                drawCenteredOverlayText(context, mc, ALREADY_IN_PARTY_TEXT, notif.x, overlayTop, overlayBottom, alphaInt);
            } else if (notif.copied && notif.copiedTimestamp + 2000 > System.currentTimeMillis()) {
                context.fill(notif.x + 1, overlayTop, notif.x + WIDTH - 1, overlayBottom, 0x2a2a2a | overlayAlphaInt);
                drawCenteredOverlayText(context, mc, COPIED_TEXT, notif.x, overlayTop, overlayBottom, alphaInt);
            }

            yOffset += notif.height + MARGIN;
        }
    }

    private static void drawScaledText(GuiGraphicsExtractor context, Minecraft mc, String line, int x, int y, int color) {
        context.pose().pushMatrix();
        context.pose().translate(x, y);
        context.pose().scale(TEXT_SCALE, TEXT_SCALE);
        context.text(mc.font, line, 0, 0, color, false);
        context.pose().popMatrix();
    }

    private static int scaledLineStep() {
        Minecraft mc = Minecraft.getInstance();
        return Math.round(mc.font.lineHeight * TEXT_SCALE) + 2;
    }

    private static void fillRounded(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, int color, int radius) {
        int width = x2 - x1;
        int height = y2 - y1;
        radius = Math.min(radius, Math.min(width, height) / 2);

        if (radius <= 0) {
            context.fill(x1, y1, x2, y2, color);
            return;
        }

        context.fill(x1, y1 + radius, x2, y2 - radius, color);

        for (int i = 0; i < radius; i++) {
            double dy = radius - i - 0.5;
            int inset = (int) Math.round(radius - Math.sqrt(Math.max(0, radius * radius - dy * dy)));

            context.fill(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, color);
            context.fill(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, color);
        }
    }

    private static void drawCenteredOverlayText(GuiGraphicsExtractor context, Minecraft mc, List<String> lines,
                                                int x, int top, int bottom, int alphaInt) {
        int lineStep = scaledLineStep();
        int blockHeight = lines.size() * lineStep - 2;

        int lineY = top + (bottom - top - blockHeight) / 2;
        for (String line : lines) {
            int scaledWidth = Math.round(mc.font.width(line) * TEXT_SCALE);
            int lineX = x + (OnScreenNotification.WIDTH - scaledWidth) / 2;
            drawScaledText(context, mc, line, lineX, lineY, 16777215 | alphaInt);
            lineY += lineStep;
        }
    }

    private static List<String> wrapText(String text) {
        List<String> lines = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();

        int contentWidth = WIDTH - PADDING * 2;
        double wrapWidth = contentWidth / TEXT_SCALE;

        String[] paragraphs = text.split("\n", -1);

        for (String paragraph : paragraphs) {
            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                if (mc.font.width(testLine) <= wrapWidth) {
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
            if (!text.isEmpty()) {
                sb.append(styleToLegacyCodes(style)).append(text);
            }
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

    public static boolean handleNotificationClicks(int mouseX, int mouseY, int button) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() == null) return false;

        Notification toRemove = null;

        for (Notification notif : notifications) {
            boolean hit = mouseX >= notif.x && mouseX <= notif.x + WIDTH
                    && mouseY >= notif.y && mouseY <= notif.y + notif.height;

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
                    if (notif.isExpiredInvite()) {
                        if (client.getConnection() == null) return false;
                        client.getConnection().sendCommand("party invite " + ChatFormatting.stripFormatting(notif.inviter()));
                        toRemove = notif;
                    } else if (notif.isPartyInvite()) {
                        if (PartyInfo.isInParty) {
                            notif.error = true;
                            notif.errorTimestamp = System.currentTimeMillis();
                        } else {
                            if (client.player != null) {
                                client.player.connection.sendChat("/p accept " + notif.inviter());
                            }
                            toRemove = notif;
                        }
                    } else if (notif.title.contains("Daily Reminder")) {
                        DailyReminders.disable((DailyReminders.ReminderType) notif.metadata);
                        toRemove = notif;
                    } else if (notif.title.contains("Boop")) {
                        if (client.getConnection() == null) return false;
                        client.getConnection().sendCommand("party invite " + notif.inviter());
                        toRemove = notif;
                    } else {
                        toRemove = notif;
                    }
                }
                break;
            }
        }

        if (toRemove != null) {
            notifications.remove(toRemove);
            return true;
        }
        return false;
    }
}