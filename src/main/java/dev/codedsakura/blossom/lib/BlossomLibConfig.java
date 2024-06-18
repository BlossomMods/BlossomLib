package dev.codedsakura.blossom.lib;

import dev.codedsakura.blossom.lib.config.BlossomConfig;
import dev.codedsakura.blossom.lib.teleport.TeleportConfig;
import org.apache.logging.log4j.Level;

import java.util.Map;

public class BlossomLibConfig extends BlossomConfig {
    public LoggingConfig logging = new LoggingConfig();

    public static class LoggingConfig {
        public String consoleLogLevel = Level.INFO.name();
        public String fileLogLevel = Level.TRACE.name();
        public String fileLogPath = "logs/BlossomMods.log";
        public boolean fileLogAppend = true;
        public boolean disableCustomLogger = false;
    }

    public TeleportConfig baseTeleportation = TeleportConfig.DEFAULT;

    public Colors colors = new Colors();

    public static class Colors {
        public String base = "light_purple";
        public String warn = "yellow";
        public String error = "red";
        public String success = "green";
        public String variable = "gold";
        public String player = "aqua";
        public String command = "gold";
        public String commandDescription = "white";
    }

    public Map<String, String> dimNameOverrides = null;

    public boolean enableMC124177Fix = true;

    @Override
    protected int getLatestVersion() {
        return 1;
    }

    @Override
    public boolean update() {
        if (version == null) {
            baseTeleportation = baseTeleportation.cloneMergeDefault();
            return true;
        }
        return false;
    }
}
