package com.skyblockutils.features;

public class ChatModifications {
    public static String modifiedChat(String message) {
        return message.replace("<3", "❤")
                .replace("\\o/", "¯\\_(ツ)_/¯")
                .replace("o/", "( ﾟ◡ﾟ)/")
                .replace("O/", "( ﾟ◡ﾟ)/")
                .replace(":skull:", "☠");
    }
}
