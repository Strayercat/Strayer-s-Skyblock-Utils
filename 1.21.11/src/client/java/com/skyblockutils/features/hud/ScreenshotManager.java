package com.skyblockutils.features.hud;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.ClipboardUtils;
import com.skyblockutils.utils.ModStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ScreenshotManager {
    private static final int MAX_SCREENSHOTS = 4;
    private static final int THUMBNAIL_WIDTH = 100;
    private static final int GAP = 5;
    private static final int DISPLAY_MS = 5000;
    private static final int SLIDE_OUT_MS = 500;
    private static final int CHECKMARK_MS = 1000;

    private enum State { VISIBLE, SLIDING_OUT }

    private record ScreenshotEntry(
            Identifier textureId, int thumbHeight, long addedTime,
            long slideOutStartTime, State state,
            String filename,
            long checkmarkStartTime
    ) {
        float xOffset() {
            return switch (state) {
                case VISIBLE -> 0f;
                case SLIDING_OUT -> {
                    long elapsed = System.currentTimeMillis() - slideOutStartTime;
                    float progress = Math.min(elapsed / (float) SLIDE_OUT_MS, 1f);
                    yield (THUMBNAIL_WIDTH + GAP + 2) * progress;
                }
            };
        }

        boolean expired() {
            return state == State.SLIDING_OUT && System.currentTimeMillis() - slideOutStartTime >= SLIDE_OUT_MS;
        }

        boolean shouldStartSlideOut() {
            return state == State.VISIBLE && System.currentTimeMillis() - addedTime >= DISPLAY_MS;
        }

        boolean isShowingCheckmark() {
            return checkmarkStartTime > 0 && System.currentTimeMillis() - checkmarkStartTime < CHECKMARK_MS;
        }
    }

    private static final List<ScreenshotEntry> screenshots = new ArrayList<>();
    private static final List<Float> currentY = new ArrayList<>();
    private static final List<PendingScreenshot> pending = new ArrayList<>();
    private static int counter = 0;

    private record PendingScreenshot(NativeImage image, String filename) {}

    public static void addScreenshot(NativeImage image, String filename) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (screenshots.size() >= MAX_SCREENSHOTS) {
                if (pending.size() >= 2) return;
                NativeImage copy = new NativeImage(image.getWidth(), image.getHeight(), false);
                copy.copyFrom(image);
                pending.add(new PendingScreenshot(copy, filename));
                ScreenshotEntry oldest = screenshots.getFirst();
                if (oldest.state() == State.VISIBLE) {
                    screenshots.set(0, new ScreenshotEntry(
                            oldest.textureId(), oldest.thumbHeight(), oldest.addedTime(),
                            System.currentTimeMillis(), State.SLIDING_OUT,
                            oldest.filename(), oldest.checkmarkStartTime()
                    ));
                }
            } else {
                registerAndAdd(image, filename, client);
            }
        });
    }

    private static void registerAndAdd(NativeImage image, String filename, MinecraftClient client) {
        int thumbHeight = (int) ((float) image.getHeight() / image.getWidth() * THUMBNAIL_WIDTH);
        Identifier id = Identifier.of("strayers-skyblock-utils", "screenshot_" + counter++);
        client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> id.toString(), image));
        screenshots.add(new ScreenshotEntry(id, thumbHeight, System.currentTimeMillis(), 0L, State.VISIBLE, filename, 0L));
        currentY.add(0f);
    }

    public static void onMouseClick(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();

        for (int i = 0; i < screenshots.size(); i++) {
            ScreenshotEntry entry = screenshots.get(i);
            if (entry.state() == State.SLIDING_OUT) continue;

            int x = (int) (screenWidth - THUMBNAIL_WIDTH - GAP + entry.xOffset());
            int y = currentY.get(i).intValue();

            if (mouseX >= x && mouseX <= x + THUMBNAIL_WIDTH && mouseY >= y && mouseY <= y + entry.thumbHeight()) {
                ClipboardUtils.copyImageToClipboard(entry.filename);
                screenshots.set(i, new ScreenshotEntry(
                        entry.textureId(), entry.thumbHeight(), entry.addedTime(),
                        entry.slideOutStartTime(), entry.state(),
                        entry.filename(), System.currentTimeMillis()
                ));
                return;
            }
        }
    }

    public static void buildScreenshotHud(DrawContext context) {
        if (screenshots.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int mouseX = (int) (client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth());
        int mouseY = (int) (client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight());

        for (int i = 0; i < screenshots.size(); i++) {
            ScreenshotEntry entry = screenshots.get(i);
            int x = (int) (screenWidth - THUMBNAIL_WIDTH - GAP + entry.xOffset());
            int y = currentY.get(i).intValue();

            boolean hovered = mouseX >= x && mouseX <= x + THUMBNAIL_WIDTH
                    && mouseY >= y && mouseY <= y + entry.thumbHeight()
                    && entry.state() == State.VISIBLE;

            context.enableScissor(screenWidth - THUMBNAIL_WIDTH - GAP - 2, 0, screenWidth, client.getWindow().getScaledHeight());

            context.fill(x - 1, y - 1, x + THUMBNAIL_WIDTH + 1, y + entry.thumbHeight() + 1,
                    ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.SCREENSHOT));

            context.drawTexturedQuad(entry.textureId(), x, y, x + THUMBNAIL_WIDTH, y + entry.thumbHeight(), 0f, 1f, 0f, 1f);

            if (hovered) {
                context.fill(x, y, x + THUMBNAIL_WIDTH, y + entry.thumbHeight(), 0xAA000000);
                if (entry.isShowingCheckmark()) {
                    context.drawCenteredTextWithShadow(client.textRenderer,
                            "✔", x + THUMBNAIL_WIDTH / 2, y + entry.thumbHeight() / 2 - 4, 0xFF55FF55);
                } else {
                    context.drawCenteredTextWithShadow(client.textRenderer,
                            "⎘", x + THUMBNAIL_WIDTH / 2, y + entry.thumbHeight() / 2 - 4, 0xFFFFFFFF);
                }
            } else if (entry.isShowingCheckmark()) {
                context.fill(x, y, x + THUMBNAIL_WIDTH, y + entry.thumbHeight(), 0x66000000);
                context.drawCenteredTextWithShadow(client.textRenderer,
                        "✔", x + THUMBNAIL_WIDTH / 2, y + entry.thumbHeight() / 2 - 4, 0xFF55FF55);
            }

            context.disableScissor();
        }
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();

        for (int i = 0; i < screenshots.size(); i++) {
            ScreenshotEntry e = screenshots.get(i);
            if (e.shouldStartSlideOut()) {
                screenshots.set(i, new ScreenshotEntry(e.textureId(), e.thumbHeight(), e.addedTime(),
                        System.currentTimeMillis(), State.SLIDING_OUT, e.filename(), e.checkmarkStartTime()));
            }
        }

        for (int i = screenshots.size() - 1; i >= 0; i--) {
            if (screenshots.get(i).expired()) {
                client.getTextureManager().destroyTexture(screenshots.get(i).textureId());
                screenshots.remove(i);
                currentY.remove(i);
                if (!pending.isEmpty()) {
                    PendingScreenshot p = pending.removeFirst();
                    registerAndAdd(p.image(), p.filename(), client);
                }
            }
        }

        int targetY = GAP;
        for (int i = 0; i < screenshots.size(); i++) {
            currentY.set(i, (float) targetY);
            targetY += screenshots.get(i).thumbHeight() + GAP;
        }
    }
}