package com.skyblockutils.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

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
            MinecraftClient mc = MinecraftClient.getInstance();
            int lineHeight = mc.textRenderer.fontHeight + 2;
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

    public static void render(DrawContext context, int screenWidth, int screenHeight) {
        if (notifications.isEmpty()) return;

        int yOffset = 0;
        MinecraftClient mc = MinecraftClient.getInstance();

        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();

        for (Notification notif : notifications) {
            notif.x = screenWidth - WIDTH;
            notif.y = screenHeight - notif.height - yOffset;

            float alpha = Math.min(1.0f, notif.ticks / 20.0f);
            int alphaInt = (int) (alpha * 255) << 24;

            boolean isHovered = notif.isClicked((int) mouseX, (int) mouseY);

            int bgColor = isHovered ? 0x2a2a2a : 0x1a1a1a;
            context.fill(notif.x, notif.y, notif.x + WIDTH, notif.y + notif.height, bgColor | alphaInt);

            int borderColor = isHovered ? 0x00FF00 : 0x00AA00;
            context.fill(notif.x, notif.y, notif.x + WIDTH, notif.y + 2, borderColor | alphaInt);

            var titleLines = wrapText(notif.title);
            var subtitleLines = wrapText(notif.subtitle);

            int lineY = notif.y + PADDING;

            for (String line : titleLines) {
                context.drawText(mc.textRenderer, line, notif.x + PADDING, lineY, 0xFFFFFF | alphaInt, false);
                lineY += mc.textRenderer.fontHeight + 2;
            }

            for (String line : subtitleLines) {
                context.drawText(mc.textRenderer, line, notif.x + PADDING, lineY, 0xAAAAAA | alphaInt, false);
                lineY += mc.textRenderer.fontHeight + 2;
            }

            yOffset += notif.height + MARGIN;
        }
    }

    private static java.util.List<String> wrapText(String text) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();

        String[] paragraphs = text.split("\n", -1);

        for (String paragraph : paragraphs) {
            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                if (mc.textRenderer.getWidth(testLine) <= 135) {
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

    public static void handleNotificationClicks(int mouseX, int mouseY, int button) {
        Notification toRemove = null;

        for (Notification notif : notifications) {
            if (notif.isClicked(mouseX, mouseY)) {
                if (button == 0) {
                    if (notif.title.contains("PARTY INVITE")) {
                        MinecraftClient mc = MinecraftClient.getInstance();
                        if (mc.player != null) {
                            mc.player.networkHandler.sendChatMessage("/p accept " + notif.subtitle.split(" ")[0]);
                        }
                    }

                    if (notif.title.contains("Test")) {
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("Click!"));
                    }
                }
                toRemove = notif;
                break;
            }
        }

        if (toRemove != null) {
            notifications.remove(toRemove);
        }
    }
}