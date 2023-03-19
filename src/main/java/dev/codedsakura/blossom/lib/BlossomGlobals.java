package dev.codedsakura.blossom.lib;

import dev.codedsakura.blossom.lib.config.ConfigManager;
import dev.codedsakura.blossom.lib.utils.CustomLogger;
import org.apache.logging.log4j.core.Logger;

public class BlossomGlobals {
    public static BlossomLibConfig CONFIG = ConfigManager.register(BlossomLibConfig.class, "BlossomLib.json", newConf -> CONFIG = newConf);
    public static final Logger LOGGER = CustomLogger.createLogger("BlossomLib");
}
