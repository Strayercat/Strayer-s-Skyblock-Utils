package com.skyblockutils.features.glowingPlayers;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.Tooltip;
import me.shedaniel.math.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GlowingPlayerEntry extends AbstractConfigListEntry<GlowingPlayers.GlowingPlayer> {
    private final GlowingPlayers.GlowingPlayer player;
    private final List<Map.Entry<String, Integer>> colors;
    private int colorIndex;
    private final String colorName;

    private final Button removeButton;
    private final Button editColorButton;

    public GlowingPlayerEntry(GlowingPlayers.GlowingPlayer player, Runnable onRemove) {
        super(Component.literal(player.username), false);
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

        this.removeButton = Button.builder(Component.literal("✕"), b -> onRemove.run())
                .size(20, 20)
                .build();

        this.editColorButton = Button.builder(
                Component.empty(),
                b -> GlowingPlayerCreationScreen.openScreenWithInfo(
                        Minecraft.getInstance().screen, player
                )
        ).size(80, 20).build();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.extractRenderState(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        Minecraft client = Minecraft.getInstance();

        context.text(client.font, player.getUsername(), x + 10, y + 6, 0xFFFFFFFF);

        editColorButton.setX(x + entryWidth - 24 - 4 - 80);
        editColorButton.setY(y + 2);
        editColorButton.extractRenderState(context, mouseX, mouseY, delta);
        if (mouseX >= editColorButton.getX() && mouseX <= editColorButton.getX() + editColorButton.getWidth()
                && mouseY >= editColorButton.getY() && mouseY <= editColorButton.getY() + editColorButton.getHeight()) {
            addTooltip(Tooltip.of(new Point(mouseX, mouseY), Component.literal("Edit color")));
        }

        int color = colors.get(colorIndex).getValue();
        int textX = editColorButton.getX() + (editColorButton.getWidth() - client.font.width(colorName)) / 2;
        int textY = editColorButton.getY() + (editColorButton.getHeight() - client.font.lineHeight) / 2;
        context.text(client.font, Component.literal(colorName), textX, textY, 0xFF000000 | color);

        removeButton.setX(x + entryWidth - 24);
        removeButton.setY(y + 2);
        removeButton.extractRenderState(context, mouseX, mouseY, delta);
        if (mouseX >= removeButton.getX() && mouseX <= removeButton.getX() + removeButton.getWidth()
                && mouseY >= removeButton.getY() && mouseY <= removeButton.getY() + removeButton.getHeight()) {
            addTooltip(Tooltip.of(new Point(mouseX, mouseY), Component.literal("Remove Player")));
        }
    }

    public List<Button> aJ_() {
        return List.of(editColorButton, removeButton);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
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

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of(editColorButton, removeButton);
    }
}