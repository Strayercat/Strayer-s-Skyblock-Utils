package com.skyblockutils.utils;

public class ModStyle {
    public static final int COLOR_TITLE = 0xFFFFFFFF;
    public static final int COLOR_SUBTITLE = 0xFFAAAAAA;
    public static final int COLOR_BACKGROUND = 0xAA000000;

    public enum ColorType {
        TITLE_START, TITLE_END, MAIN, TEXT, WAYPOINT
    }

    public enum ColorStyle {
        ORIGINAL, OCEAN, SUNSET, CANDY, SHARK;

        public int getColor(ColorType type) {
            return switch (this) {
                case ORIGINAL -> switch (type) {
                    case TITLE_START -> 0x4CFF00;
                    case TITLE_END -> 0x93E27D;
                    case MAIN -> 0xFF00AA00;
                    case TEXT -> 0xFFAAFFAA;
                    case WAYPOINT -> 0x4CFF01;
                };
                case OCEAN -> switch (type) {
                    case TITLE_START -> 0x0077FF;
                    case TITLE_END -> 0x00CCFF;
                    case MAIN -> 0xFF0055AA;
                    case TEXT -> 0xFFAAEEFF;
                    case WAYPOINT -> 0x1EE3D7;
                };
                case SUNSET -> switch (type) {
                    case TITLE_START -> 0xFF6600;
                    case TITLE_END -> 0xFFCC00;
                    case MAIN -> 0xFFAA3300;
                    case TEXT -> 0xFFFFDDAA;
                    case WAYPOINT -> 0xFF6601;
                };
                case CANDY -> switch (type) {
                    case TITLE_START -> 0xFF69B4;
                    case TITLE_END -> 0xFF1493;
                    case MAIN -> 0xFFFF69B4;
                    case TEXT -> 0xFFFFD1DC;
                    case WAYPOINT -> 0xFF69B5;
                };
                case SHARK -> switch (type) {
                    case TITLE_START -> 0x8A43FF;
                    case TITLE_END -> 0xB87DE2;
                    case MAIN -> 0xFF7200BA;
                    case TEXT -> 0xFFC1A4DB;
                    case WAYPOINT -> 0x913BE3;
                };
            };
        }
    }

    public static int getColor(ColorStyle style, ColorType type) {
        return style.getColor(type);
    }
}