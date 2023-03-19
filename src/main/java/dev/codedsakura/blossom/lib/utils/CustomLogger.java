package dev.codedsakura.blossom.lib.utils;

import dev.codedsakura.blossom.lib.BlossomGlobals;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import static org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory.newConfigurationBuilder;

public class CustomLogger {
    private static LoggerContext CONTEXT = null;

    private static void initialize() {
        Configurator.reconfigure();

        if (BlossomGlobals.CONFIG.logging.disableCustomLogger) {
            CONTEXT = LoggerContext.getContext(true);
            return;
        }

        Appender sysOut = LoggerContext.getContext(false).getConfiguration().getAppender("SysOut");
        ConfigurationBuilder<BuiltConfiguration> builder = newConfigurationBuilder();

        builder.add(
                builder.newAppender("fileLog", "File")
                        .addAttribute("fileName", BlossomGlobals.CONFIG.logging.fileLogPath)
                        .addAttribute("append", BlossomGlobals.CONFIG.logging.fileLogAppend)
                        .add(builder
                                .newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.DENY)
                                .addAttribute("level", BlossomGlobals.CONFIG.logging.fileLogLevel))
                        .add(builder
                                .newLayout("PatternLayout")
                                .addAttribute("pattern", "[%d{yyyy-MM-dd HH:mm:ss}] [%t/%5level] (%logger{1}): %msg%n%throwable"))
        );

        builder.add(
                builder.newRootLogger(Level.TRACE)
                        .add(builder.newAppenderRef("fileLog"))
        );

        CONTEXT = LoggerContext.getContext(true);
        CONTEXT.setConfiguration(builder.build(false));

        Configuration configuration = CONTEXT.getConfiguration();
        configuration.addAppender(sysOut);
        configuration.getRootLogger().addAppender(sysOut, Level.getLevel(BlossomGlobals.CONFIG.logging.consoleLogLevel), null);
    }

    /**
     * A {@link Logger} which writes to both STDOUT and a log file
     *
     * @param name the name of the custom {@link Logger}
     * @return a custom {@link Logger} with the provided name
     */
    public static Logger createLogger(String name) {
        if (CONTEXT == null) {
            initialize();
        }
        return CONTEXT.getLogger(name);
    }
}
