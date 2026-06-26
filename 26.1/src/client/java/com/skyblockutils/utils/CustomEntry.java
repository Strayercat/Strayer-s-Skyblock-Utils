package com.skyblockutils.utils;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomEntry extends AbstractConfigListEntry<Object> {

    public enum ElementType {BUTTON, TEXT}

    public sealed interface Alignment permits Alignment.Left, Alignment.Right, Alignment.Center, Alignment.Fixed {
        record Left() implements Alignment {
        }

        record Right() implements Alignment {
        }

        record Center() implements Alignment {
        }

        record Fixed(int x) implements Alignment {
        }

        Alignment LEFT = new Left();
        Alignment RIGHT = new Right();
        Alignment CENTER = new Center();

        static Alignment fixed(int x) {
            return new Fixed(x);
        }
    }

    public record EntryElement(ElementType type, Object widget, Alignment alignment, int margin, Component[] tooltip) {
    }

    private final List<EntryElement> elements = new ArrayList<>();
    private final List<Button> buttons = new ArrayList<>();

    public CustomEntry() {
        super(Component.literal(""), false);
    }

    public CustomEntry addButton(Component label, int width, Alignment alignment, Runnable onClick) {
        return addButton(label, width, alignment, 4, onClick, new Component[0]);
    }

    public CustomEntry addButton(Component label, int width, Alignment alignment, Runnable onClick, Component... tooltip) {
        return addButton(label, width, alignment, 4, onClick, tooltip);
    }

    public CustomEntry addButton(Component label, int width, Alignment alignment, int margin, Runnable onClick, Component... tooltip) {
        Button button = Button.builder(label, b -> onClick.run())
                .size(Math.min(300, width), 20)
                .build();
        buttons.add(button);
        elements.add(new EntryElement(ElementType.BUTTON, button, alignment, margin, tooltip));
        return this;
    }

    public CustomEntry addText(String text, Alignment alignment, int color) {
        return addText(text, alignment, 4, color);
    }

    public CustomEntry addText(String text, Alignment alignment, int margin, int color) {
        elements.add(new EntryElement(ElementType.TEXT, new Object[]{text, color}, alignment, margin, new Component[0]));
        return this;
    }

    private int getElementWidth(EntryElement el) {
        return switch (el.widget()) {
            case Button btn -> btn.getWidth();
            case Object[] data -> Minecraft.getInstance().font.width((String) data[0]);
            default -> 0;
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.extractRenderState(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);

        Minecraft client = Minecraft.getInstance();
        int leftCursor = x;
        int rightCursor = x + entryWidth;

        int centerTotal = elements.stream()
                .filter(el -> el.alignment() instanceof Alignment.Center)
                .mapToInt(el -> getElementWidth(el) + el.margin())
                .sum();
        int centerCursor = x + (entryWidth - centerTotal) / 2;

        for (EntryElement el : elements) {
            int elWidth = getElementWidth(el);
            int elX = switch (el.alignment()) {
                case Alignment.Left a -> {
                    int pos = leftCursor;
                    leftCursor += elWidth + el.margin();
                    yield pos;
                }
                case Alignment.Right a -> {
                    rightCursor -= elWidth + el.margin();
                    yield rightCursor;
                }
                case Alignment.Center a -> {
                    int pos = centerCursor;
                    centerCursor += elWidth + el.margin();
                    yield pos;
                }
                case Alignment.Fixed a -> x + a.x();
            };

            switch (el.type()) {
                case BUTTON -> {
                    Button btn = (Button) el.widget();
                    btn.setX(elX);
                    btn.setY(y + 2);
                    btn.extractRenderState(context, mouseX, mouseY, delta);
                    if (el.tooltip().length > 0 && mouseX >= elX && mouseX <= elX + btn.getWidth() && mouseY >= y + 2 && mouseY <= y + 22) {
                        addTooltip(me.shedaniel.clothconfig2.api.Tooltip.of(
                                new me.shedaniel.math.Point(mouseX, mouseY),
                                el.tooltip()
                        ));
                    }
                }
                case TEXT -> {
                    Object[] data = (Object[]) el.widget();
                    context.text(client.font, (String) data[0], elX, y + 6, (int) data[1]);
                }
            }
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return buttons;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of();
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }
}