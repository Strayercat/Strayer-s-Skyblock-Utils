package com.skyblockutils.features.browser;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2f;
import org.lwjgl.glfw.GLFW;

public class BrowserScreen extends Screen {
    private static final String HINT = "Esc: close  •  Ctrl+O: open in your browser  •  Ctrl+Scroll / Ctrl+-/=/0: zoom";
    private static final double ZOOM_STEP = 0.5;
    private static final double MIN_ZOOM = -4;
    private static final double MAX_ZOOM = 6;

    private static double zoomLevel = 0;

    private final Screen parent;
    private final String url;
    private MCEFBrowser browser;
    private MCEFApi.Initialization initialization;

    BrowserScreen(String url, Screen parent) {
        super(Component.literal("Browser"));
        this.url = url;
        this.parent = parent;
    }

    private String currentUrl() {
        if (browser == null) return url;
        String current = browser.getCefBrowser().getURL();
        return current == null || current.isBlank() ? url : current;
    }

    private double pixelScale() {
        return width <= 0 ? 1 : (double) Minecraft.getInstance().getWindow().getWidth() / width;
    }

    private void resizeBrowser() {
        Window window = Minecraft.getInstance().getWindow();
        browser.resize(Math.max(1, window.getWidth()), Math.max(1, window.getHeight()));
    }

    private void applyZoom() {
        if (browser != null) browser.getCefBrowser().setZoomLevel(zoomLevel);
    }

    private void zoom(double delta) {
        zoomLevel = delta == 0 ? 0 : Math.clamp(zoomLevel + delta, MIN_ZOOM, MAX_ZOOM);
        applyZoom();
    }

    @Override
    protected void init() {
        initialization = MCEFApi.initialize();
        if (browser != null) resizeBrowser();
    }

    @Override
    public void tick() {
        if (browser != null || initialization == null || !initialization.isDone()) return;
        browser = MCEFApi.getInstance().createBrowser(url, false);
        resizeBrowser();
        browser.setFocus(true);
        applyZoom();
    }

    @Override
    public void extractBackground(final @NotNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        graphics.fill(0, 0, width, height, 0xFF101010);
    }

    @Override
    public void extractRenderState(final @NotNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        if (browser == null) {
            String text = getString();
            graphics.centeredText(font, text, width / 2, height / 2 - 4, 0xFFFFFFFF);
            graphics.centeredText(font, HINT, width / 2, height / 2 + 10, 0xFFAAAAAA);
            return;
        }

        GpuTextureView view = browser.getTextureView();
        if (view != null) {
            graphics.guiRenderState.addGuiElement(new BlitRenderState(
                    RenderPipelines.GUI_TEXTURED,
                    TextureSetup.singleTexture(view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),
                    new Matrix3x2f(graphics.pose()),
                    0, 0, width, height,
                    0.0F, 1.0F, 0.0F, 1.0F,
                    0xFFFFFFFF,
                    graphics.scissorStack.peek()
            ));
        }
        graphics.requestCursor(browser.getCursorType());
    }

    private @NotNull String getString() {
        String stage = initialization == null ? "Starting" : switch (initialization.getStage()) {
            case DOWNLOADING -> "Downloading the browser (first time only)";
            case EXTRACTING -> "Extracting the browser";
            case INSTALL -> "Installing the browser";
            default -> "Starting the browser";
        };
        float percentage = initialization == null ? -1 : initialization.getPercentage();
        return stage + (percentage >= 0 ? " " + Math.round(percentage) + "%" : "...");
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
    }

    private MouseButtonEvent toPixels(MouseButtonEvent event) {
        double scale = pixelScale();
        return new MouseButtonEvent(event.x() * scale, event.y() * scale, event.buttonInfo());
    }

    @Override
    public boolean mouseClicked(final @NotNull MouseButtonEvent event, final boolean doubled) {
        if (browser != null) browser.onMouseClicked(toPixels(event), doubled);
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(final @NotNull MouseButtonEvent event) {
        if (browser != null) browser.onMouseReleased(toPixels(event));
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (browser != null) {
            if (hasControlDown()) {
                zoom(Math.signum(scrollY) * ZOOM_STEP);
            } else {
                double scale = pixelScale();
                browser.onMouseScrolled((int) (mouseX * scale), (int) (mouseY * scale), scrollY);
            }
        }
        return true;
    }

    @Override
    public void mouseMoved(final double x, final double y) {
        double scale = pixelScale();
        if (browser != null) browser.onMouseMoved((int) (x * scale), (int) (y * scale));
        super.mouseMoved(x, y);
    }

    private static boolean hasControlDown() {
        long handle = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    @Override
    public boolean keyPressed(final @NotNull KeyEvent event) {
        if (event.hasControlDownWithQuirk()) {
            switch (event.key()) {
                case GLFW.GLFW_KEY_O -> {
                    InGameBrowser.openExternal(currentUrl());
                    return true;
                }
                case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> {
                    zoom(ZOOM_STEP);
                    return true;
                }
                case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> {
                    zoom(-ZOOM_STEP);
                    return true;
                }
                case GLFW.GLFW_KEY_0, GLFW.GLFW_KEY_KP_0 -> {
                    zoom(0);
                    return true;
                }
                default -> {
                }
            }
        }
        if (browser != null) {
            browser.onKeyPressed(event);
            int typed = switch (event.key()) {
                case GLFW.GLFW_KEY_BACKSPACE -> '\b';
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> '\r';
                case GLFW.GLFW_KEY_TAB -> '\t';
                default -> -1;
            };
            if (typed != -1 && Util.getPlatform() == Util.OS.WINDOWS) browser.onCharTyped(new CharacterEvent(typed));
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(final @NotNull KeyEvent event) {
        if (browser != null) browser.onKeyReleased(event);
        return super.keyReleased(event);
    }

    @Override
    public boolean charTyped(final @NotNull CharacterEvent event) {
        if (browser != null) browser.onCharTyped(event);
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
