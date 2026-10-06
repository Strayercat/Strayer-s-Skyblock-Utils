package com.skyblockutils.utils;

public interface SSURenderState {
    float SPECIAL_SCALE = 0.75F;
    float BIG_HEAD_SCALE = 1.75F;

    boolean ssu$isBigHead();

    void ssu$setBigHead(boolean bigHead);

    boolean ssu$isGroucho();

    void ssu$setGroucho(boolean groucho);
}