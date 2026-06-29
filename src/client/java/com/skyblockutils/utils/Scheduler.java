package com.skyblockutils.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Scheduler {
    public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
}
