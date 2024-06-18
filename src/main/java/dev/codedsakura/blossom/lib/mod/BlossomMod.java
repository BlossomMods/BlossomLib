package dev.codedsakura.blossom.lib.mod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.codedsakura.blossom.lib.config.BlossomConfig;
import dev.codedsakura.blossom.lib.config.ConfigManager;
import dev.codedsakura.blossom.lib.utils.CustomLogger;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.logging.log4j.core.Logger;

import java.util.ArrayList;
import java.util.function.Consumer;

public abstract class BlossomMod<T extends BlossomConfig> {
    private final Class<T> type;
    private final ArrayList<Consumer<CommandDispatcher<ServerCommandSource>>> commands = new ArrayList<>();
    public T config;
    public Logger logger;

    public BlossomMod(Class<T> type) {
        this.type = type;
    }

    abstract public String getName();

    protected void register() {
        ModController.register(this);
        this.initLogger();
        this.initConfig();
    }

    protected void initConfig() {
        ConfigManager.register(type, this.getName() + ".json", newConfig -> config = newConfig);
    }

    protected void initLogger() {
        logger = CustomLogger.createLogger(this.getName());
    }

    protected BlossomMod<T> addCommand(LiteralArgumentBuilder<ServerCommandSource> command) {
        return this.registerCommand(dispatch -> dispatch.register(command));
    }

    protected BlossomMod<T> registerCommand(Consumer<CommandDispatcher<ServerCommandSource>> callback) {
        commands.add(callback);
        return this;
    }

    protected ArrayList<Consumer<CommandDispatcher<ServerCommandSource>>> getCommands() {
        return commands;
    }
}
