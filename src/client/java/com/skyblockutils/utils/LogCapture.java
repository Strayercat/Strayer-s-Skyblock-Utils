package com.skyblockutils.utils;

import com.skyblockutils.features.hud.LogHud;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;

public final class LogCapture {
    private static final String PREFIX = "SSU";
    private static boolean installed = false;

    private LogCapture() {
    }

    public static void install() {
        if (installed) return;
        if (!(LogManager.getContext(false) instanceof LoggerContext context)) return;
        Configuration config = context.getConfiguration();
        AbstractAppender appender = new AbstractAppender("SSULogHud", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                String logger = event.getLoggerName();
                if (logger == null || !logger.startsWith(PREFIX)) return;
                String message = event.getMessage().getFormattedMessage();
                Throwable thrown = event.getThrown();
                if (thrown != null) message += " (" + thrown + ")";
                LogHud.addLog(event.getLevel().name(), logger, message);
            }
        };
        appender.start();
        config.addAppender(appender);
        config.getRootLogger().addAppender(appender, null, null);
        context.updateLoggers();
        installed = true;
    }
}
