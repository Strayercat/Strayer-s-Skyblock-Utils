package com.skyblockutils.features.chat;

public class FancyEmotes {
    public static String fancyEmotes(String message) {
        return message.replace("<3", "❤")
                .replace("\\o/", "¯\\_(ツ)_/¯")
                .replace("o/", "( ﾟ◡ﾟ)/")
                .replace("O/", "( ﾟ◡ﾟ)/")
                .replace(":skull:", "☠");
    }
}
