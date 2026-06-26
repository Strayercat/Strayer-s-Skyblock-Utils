package com.skyblockutils.features.glowingPlayers;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GlowingPlayerEntry extends AbstractConfigListEntry<GlowingPlayers.GlowingPlayer> {
    private final GlowingPlayers.GlowingPlayer player;
    private final List<Map.Entry<String, Integer>> colors;
    private int colorIndex;
    private final String colorName;

    private final ButtonWidget removeButton;
    private final ButtonWidget editColorButton;

    public GlowingPlayerEntry(GlowingPlayers.GlowingPlayer player, Runnable onRemove) {
        super(Text.literal(player.username), false);
        this.player = player;
        this.colors = new ArrayList<>(GlowingPlayers.MINECRAFT_COLORS.entrySet());

        this.colorIndex = 0;
        for (int i = 0; i < colors.size(); i++) {
            if (colors.get(i).getValue().equals(player.color)) {
                this.colorIndex = i;
                break;
            }
        }

        this.colorName = colors.get(colorIndex).getKey().replace("_", " ");

        this.removeButton = ButtonWidget.builder(Text.literal("✕"), b -> onRemove.run())
                .size(20, 20)
                .build();

        this.editColorButton = ButtonWidget.builder(
                Text.empty(),
                b -> GlowingPlayerCreationScreen.openScreenWithInfo(
                        MinecraftClient.getInstance().currentScreen, player
                )
        ).size(80, 20).build();
        this.editColorButton.setTooltip(Tooltip.of(Text.literal("Edit color")));
    }

    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();

        context.drawTextWithShadow(client.textRenderer, player.getUsername(), x + 10, y + 6, 0xFFFFFFFF);

        editColorButton.setX(x + entryWidth - 24 - 4 - 80);
        editColorButton.setY(y + 2);
        editColorButton.render(context, mouseX, mouseY, delta);

        int color = colors.get(colorIndex).getValue();
        int textX = editColorButton.getX() + (editColorButton.getWidth() - client.textRenderer.getWidth(colorName)) / 2;
        int textY = editColorButton.getY() + (editColorButton.getHeight() - client.textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(client.textRenderer, Text.literal(colorName), textX, textY, 0xFF000000 | color);

        removeButton.setX(x + entryWidth - 24);
        removeButton.setY(y + 2);
        removeButton.setTooltip(Tooltip.of(Text.literal("Remove Player")));
        removeButton.render(context, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends Element> children() {
        return List.of(editColorButton, removeButton);
    }

    @Override
    public List<? extends Selectable> narratables() {
        return List.of(editColorButton, removeButton);
    }

    @Override
    public GlowingPlayers.GlowingPlayer getValue() {
        return player;
    }

    @Override
    public Optional<GlowingPlayers.GlowingPlayer> getDefaultValue() {
        return Optional.empty();
    }
}