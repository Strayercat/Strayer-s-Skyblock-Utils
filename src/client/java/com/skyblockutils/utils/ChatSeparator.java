package com.skyblockutils.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.Optional;

public class ChatSeparator {
    public static boolean is(Component message) {
        String text = message.getString().replaceAll("§.", "").trim();
        if (!text.isEmpty()) return text.length() >= 5 && text.chars().allMatch(c -> c == '-' || c == '▬');

        return message.<Boolean>visit((style, segment) ->
                style.isStrikethrough() && !segment.isEmpty() ? Optional.of(true) : Optional.<Boolean>empty(),
                Style.EMPTY
        ).orElse(false);
    }
}
