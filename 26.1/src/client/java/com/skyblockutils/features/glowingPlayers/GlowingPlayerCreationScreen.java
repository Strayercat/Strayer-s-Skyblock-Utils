package com.skyblockutils.features.glowingPlayers;

import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.utils.PlayerLookup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GlowingPlayerCreationScreen extends Screen {

    private final Screen parent;
    private EditBox playerNameField;

    private int selectedColor = 0xFFFFFF;
    private String selectedColorName = "WHITE";
    private final List<String> colorNames = new ArrayList<>();
    private final List<Integer> colorValues = new ArrayList<>();

    private final GlowingPlayers.GlowingPlayer editingPlayer;

    private static final int SWATCH_SIZE = 16;
    private static final int SWATCH_PADDING = 3;
    private static final int COLS = 8;

    public GlowingPlayerCreationScreen(Screen parent) {
        this(parent, null);
    }

    public GlowingPlayerCreationScreen(Screen parent, GlowingPlayers.GlowingPlayer player) {
        super(Component.literal(player == null ? "Add Glowing Player" : "Edit Glowing Player"));
        this.parent = parent;
        this.editingPlayer = player;

        if (player != null) {
            this.selectedColor = player.getColor();
            for (Map.Entry<String, Integer> entry : GlowingPlayers.MINECRAFT_COLORS.entrySet()) {
                if (entry.getValue() == player.getColor()) {
                    this.selectedColorName = entry.getKey();
                    break;
                }
            }
        }

        for (Map.Entry<String, Integer> entry : GlowingPlayers.MINECRAFT_COLORS.entrySet()) {
            colorNames.add(entry.getKey());
            colorValues.add(entry.getValue());
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int panelTop = this.height / 2 - 80;

        this.playerNameField = new EditBox(this.font, centerX - 100, panelTop + 20, 200, 20, Component.literal("Player Name"));
        this.playerNameField.setMaxLength(16);

        if (editingPlayer != null) {
            this.playerNameField.insertText(editingPlayer.getUsername());
            this.playerNameField.setEditable(false);
        } else {
            this.playerNameField.setHint(Component.literal("Enter username..."));
        }

        this.addRenderableWidget(this.playerNameField);
        if (editingPlayer == null) {
            this.setInitialFocus(this.playerNameField);
        }

        String confirmLabel = editingPlayer == null ? "Add Player" : "Save Changes";
        this.addRenderableWidget(Button.builder(
                Component.literal(confirmLabel),
                btn -> this.confirm()
        ).bounds(centerX - 105, panelTop + 155, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> this.close()
        ).bounds(centerX + 5, panelTop + 155, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (this.parent != null) {
            this.parent.extractRenderState(context, mouseX, mouseY, delta);
        }

        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        int centerX = this.width / 2;
        int panelTop = this.height / 2 - 80;
        int panelWidth = 250;
        int panelHeight = 185;

        context.fill(
                centerX - panelWidth / 2, panelTop,
                centerX + panelWidth / 2, panelTop + panelHeight,
                0xDD1a1a1a
        );
        drawBorder(context, centerX - panelWidth / 2, panelTop, panelWidth, panelHeight, 0xFF555555);

        context.centeredText(this.font, this.title, centerX, panelTop + 7, 0xFFFFFF);

        context.text(this.font, Component.literal("Username"), centerX - 100, panelTop + 10, 0xAAAAAA);

        context.text(this.font, Component.literal("Color:"), centerX - 100, panelTop + 52, 0xAAAAAA);
        context.fill(centerX + 58, panelTop + 50, centerX + 74, panelTop + 58, 0xFF000000 | selectedColor);
        drawBorder(context, centerX + 58, panelTop + 50, 16, 8, 0xFF888888);
        context.text(
                this.font,
                Component.literal(selectedColorName.replace("_", " ")),
                centerX - 74, panelTop + 52,
                0xFF000000 | selectedColor
        );

        renderColorGrid(context, centerX, panelTop + 65, mouseX, mouseY);

        super.extractRenderState(context, mouseX, mouseY, delta);

        OnScreenNotification.render(context, this.width, this.height);
    }

    private void renderColorGrid(GuiGraphicsExtractor context, int centerX, int gridTop, int mouseX, int mouseY) {
        int gridWidth = COLS * (SWATCH_SIZE + SWATCH_PADDING) - SWATCH_PADDING;
        int startX = centerX - gridWidth / 2;

        for (int i = 0; i < colorValues.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int x = startX + col * (SWATCH_SIZE + SWATCH_PADDING);
            int y = gridTop + row * (SWATCH_SIZE + SWATCH_PADDING);

            int color = colorValues.get(i);
            boolean isSelected = colorNames.get(i).equals(selectedColorName);
            boolean isHovered = mouseX >= x && mouseX < x + SWATCH_SIZE
                    && mouseY >= y && mouseY < y + SWATCH_SIZE;

            context.fill(x, y, x + SWATCH_SIZE, y + SWATCH_SIZE, 0xFF000000 | color);

            if (isSelected) {
                drawBorder(context, x - 1, y - 1, SWATCH_SIZE + 2, SWATCH_SIZE + 2, 0xFFFFFFFF);
            } else if (isHovered) {
                drawBorder(context, x - 1, y - 1, SWATCH_SIZE + 2, SWATCH_SIZE + 2, 0xFFAAAAAA);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int centerX = this.width / 2;
        int gridTop = this.height / 2 - 80 + 65;

        int gridWidth = COLS * (SWATCH_SIZE + SWATCH_PADDING) - SWATCH_PADDING;
        int startX = centerX - gridWidth / 2;

        for (int i = 0; i < colorValues.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int x = startX + col * (SWATCH_SIZE + SWATCH_PADDING);
            int y = gridTop + row * (SWATCH_SIZE + SWATCH_PADDING);

            if (event.x() >= x && event.x() < x + SWATCH_SIZE
                    && event.y() >= y && event.y() < y + SWATCH_SIZE) {
                selectedColor = colorValues.get(i);
                selectedColorName = colorNames.get(i);
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void confirm() {
        if (editingPlayer != null) {
            editingPlayer.color = selectedColor;
            GlowingPlayers.save();
            Minecraft.getInstance().execute(() -> {
                this.close();
                GlowingPlayersGui.refreshScreen(Minecraft.getInstance());
            });
        } else {
            String name = this.playerNameField.getValue().trim();
            if (name.isEmpty()) return;

            PlayerLookup.getFormattedUsername(name).thenAccept(formattedName -> {
                if (formattedName == null) {
                    Minecraft.getInstance().execute(() ->
                            OnScreenNotification.renderNotification("Player Not Found", "\"" + name + "\" doesn't exist.", 100)
                    );
                    return;
                }
                GlowingPlayers.add(formattedName, selectedColor, true, () ->
                        Minecraft.getInstance().execute(() -> {
                            this.close();
                            GlowingPlayersGui.refreshScreen(Minecraft.getInstance());
                        })
                );
            });
        }
    }

    public void close() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    public boolean shouldPause() {
        return false;
    }

    public static void openScreen(Screen parent) {
        Minecraft.getInstance().setScreen(new GlowingPlayerCreationScreen(parent));
    }

    public static void openScreenWithInfo(Screen parent, GlowingPlayers.GlowingPlayer player) {
        Minecraft.getInstance().setScreen(new GlowingPlayerCreationScreen(parent, player));
    }

    private static void drawBorder(GuiGraphicsExtractor context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }
}