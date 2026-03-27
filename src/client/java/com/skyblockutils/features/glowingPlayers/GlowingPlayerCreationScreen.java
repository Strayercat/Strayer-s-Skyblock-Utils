package com.skyblockutils.features.glowingPlayers;

import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.utils.PlayerLookup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GlowingPlayerCreationScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget playerNameField;

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
        super(Text.literal(player == null ? "Add Glowing Player" : "Edit Glowing Player"));
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

        this.playerNameField = new TextFieldWidget(
                this.textRenderer,
                centerX - 100, panelTop + 20,
                200, 20,
                Text.literal("Player Name")
        );
        this.playerNameField.setMaxLength(16);

        if (editingPlayer != null) {
            this.playerNameField.setText(editingPlayer.getUsername());
            this.playerNameField.setEditable(false);
            this.playerNameField.setPlaceholder(Text.literal(""));
        } else {
            this.playerNameField.setPlaceholder(Text.literal("Enter username..."));
        }

        this.addDrawableChild(this.playerNameField);
        if (editingPlayer == null) {
            this.setInitialFocus(this.playerNameField);
        }

        String confirmLabel = editingPlayer == null ? "Add Player" : "Save Changes";
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(confirmLabel),
                btn -> this.confirm()
        ).dimensions(centerX - 105, panelTop + 155, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Cancel"),
                btn -> this.close()
        ).dimensions(centerX + 5, panelTop + 155, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.parent != null) {
            this.parent.render(context, mouseX, mouseY, delta);
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

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, panelTop + 7, 0xFFFFFF);

        context.drawTextWithShadow(this.textRenderer, Text.literal("Username"), centerX - 100, panelTop + 10, 0xAAAAAA);

        context.drawTextWithShadow(this.textRenderer, Text.literal("Color:"), centerX - 100, panelTop + 52, 0xAAAAAA);
        context.fill(centerX + 58, panelTop + 50, centerX + 74, panelTop + 58, 0xFF000000 | selectedColor);
        drawBorder(context, centerX + 58, panelTop + 50, 16, 8, 0xFF888888);
        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal(selectedColorName.replace("_", " ")),
                centerX - 74, panelTop + 52,
                0xFF000000 | selectedColor
        );

        renderColorGrid(context, centerX, panelTop + 65, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);

        OnScreenNotification.render(context, this.width, this.height);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private void renderColorGrid(DrawContext context, int centerX, int gridTop, int mouseX, int mouseY) {
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
    public boolean mouseClicked(Click click, boolean doubled) {
        int centerX = this.width / 2;
        int gridTop = this.height / 2 - 80 + 65;

        int gridWidth = COLS * (SWATCH_SIZE + SWATCH_PADDING) - SWATCH_PADDING;
        int startX = centerX - gridWidth / 2;

        for (int i = 0; i < colorValues.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int x = startX + col * (SWATCH_SIZE + SWATCH_PADDING);
            int y = gridTop + row * (SWATCH_SIZE + SWATCH_PADDING);

            if (click.x() >= x && click.x() < x + SWATCH_SIZE
                    && click.y() >= y && click.y() < y + SWATCH_SIZE) {
                selectedColor = colorValues.get(i);
                selectedColorName = colorNames.get(i);
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    private void confirm() {
        if (editingPlayer != null) {
            editingPlayer.color = selectedColor;
            GlowingPlayers.save();
            MinecraftClient.getInstance().execute(() -> {
                this.close();
                GlowingPlayersGui.refreshScreen(MinecraftClient.getInstance());
            });
        } else {
            String name = this.playerNameField.getText().trim();
            if (name.isEmpty()) return;

            PlayerLookup.getFormattedUsername(name).thenAccept(formattedName -> {
                if (formattedName == null) {
                    MinecraftClient.getInstance().execute(() ->
                            OnScreenNotification.renderNotification("Player Not Found", "\"" + name + "\" doesn't exist.", 100)
                    );
                    return;
                }
                GlowingPlayers.add(formattedName, selectedColor, true, () ->
                        MinecraftClient.getInstance().execute(() -> {
                            this.close();
                            GlowingPlayersGui.refreshScreen(MinecraftClient.getInstance());
                        })
                );
            });
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(this.parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public static void openScreen(Screen parent) {
        MinecraftClient.getInstance().setScreen(new GlowingPlayerCreationScreen(parent));
    }

    public static void openScreenWithInfo(Screen parent, GlowingPlayers.GlowingPlayer player) {
        MinecraftClient.getInstance().setScreen(new GlowingPlayerCreationScreen(parent, player));
    }

    private static void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }
}