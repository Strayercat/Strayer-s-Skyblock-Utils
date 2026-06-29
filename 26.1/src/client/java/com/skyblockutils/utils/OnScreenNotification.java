package com.skyblockutils.utils;

import com.skyblockutils.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.LinkedList;
import java.util.Queue;

public class OnScreenNotification {
    private static final int WIDTH = 145;
    private static final int MARGIN = 2;
    private static final int PADDING = 5;

    private static class Notification {
        String title;
        String subtitle;
        int ticks;
        int maxTicks;
        int height;
        int x, y;

        Notification(String title, String subtitle, int tickTime) {
            this.title = title;
            this.subtitle = subtitle;
            this.ticks = tickTime;
            this.maxTicks = tickTime;
            this.height = calculateHeight(title, subtitle);
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

    public static void renderNotification(String title, String subtitle, int tickTime) {
        notifications.add(new Notification(title, subtitle, tickTime));
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

            int bgColor = isHovered ? 0x2a2a2a : 0x1a1a1a;
            context.fill(notif.x, notif.y, notif.x + WIDTH, notif.y + notif.height, bgColor | alphaInt);

            int borderColor = ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN);
            context.fill(notif.x, notif.y, notif.x + WIDTH, notif.y + 2, borderColor | alphaInt);

            var titleLines = wrapText(notif.title);
            var subtitleLines = wrapText(notif.subtitle);

            int lineY = notif.y + PADDING;

            for (String line : titleLines) {
                context.text(mc.font, line, notif.x + PADDING, lineY, 0xFFFFFF | alphaInt, false);
                lineY += mc.font.lineHeight + 2;
            }

            for (String line : subtitleLines) {
                context.text(mc.font, line, notif.x + PADDING, lineY, 0xAAAAAA | alphaInt, false);
                lineY += mc.font.lineHeight + 2;
            }

            yOffset += notif.height + MARGIN;
        }
    }

    private static java.util.List<String> wrapText(String text) {
        java.util.List<String> lines = new java.util.ArrayList<>();
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

    public static boolean handleNotificationClicks(int mouseX, int mouseY, int button, int screenWidth, int screenHeight) {
        int yOffset = 0;
        Notification toRemove = null;

        for (Notification notif : notifications) {
            int x = screenWidth - WIDTH;
            int y = screenHeight - notif.height - yOffset;

            boolean hit = mouseX >= x && mouseX <= x + WIDTH
                    && mouseY >= y && mouseY <= y + notif.height;

            if (hit) {
                if (button == 0 && notif.title.contains("PARTY INVITE")) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.connection.sendChat("/p accept " + notif.subtitle.split(" ")[0]);
                    }
                }
                toRemove = notif;
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

    public static int[] getNotificationPosition(Notification notif, int screenWidth, int screenHeight, int yOffset) {
        int x = screenWidth - WIDTH;
        int y = screenHeight - notif.height - yOffset;
        return new int[]{x, y};
    }
}