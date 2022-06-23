package dev.codedsakura.blossom.lib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec2f;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;


class BlossomLibConfig {
    LoggingConfig logging = new LoggingConfig();

    static class LoggingConfig {
        String consoleLogLevel = Level.INFO.name();
        String fileLogLevel = Level.WARN.name();
        String fileLogPath = "logs/BlossomMods.log";
        boolean fileLogAppend = true;
    }

    TeleportConfig baseTeleportation = new TeleportConfig(true);

    Colors colors = new Colors();

    static class Colors {
        String base = "light_purple";
        String warn = "yellow";
        String error = "red";
        String success = "green";
        String variable = "gold";
        String player = "aqua";
        String command = "gold";
        String commandDescription = "white";
    }
}

public class BlossomLib implements ModInitializer {
    static BlossomLibConfig CONFIG = ConfigManager.register(BlossomLibConfig.class, "BlossomLib.json", newConf -> CONFIG = newConf);
    public static final Logger LOGGER = CustomLogger.createLogger("BlossomLib");

    private static final ArrayList<LiteralArgumentBuilder<ServerCommandSource>> COMMANDS = new ArrayList<>();
    private static final ArrayList<Consumer<CommandDispatcher<ServerCommandSource>>> COMMAND_CONSUMERS = new ArrayList<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(_server -> TeleportUtils.tick());

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("blossomlib")
                    .requires(Permissions.require("blossom.lib.base-command", 2))
                    .then(literal("reload-configs")
                            .requires(Permissions.require("blossom.lib.base-command.reload-configs", 3))
                            .executes(ctx -> {
                                ConfigManager.refreshAll();
                                TextUtils.sendOps(ctx, "blossom.configs-reload");
                                return 1;
                            })
                            .then(argument("module", StringArgumentType.string())
                                    .suggests((ctx, builder) -> {
                                        String start = builder.getRemaining().toLowerCase();
                                        ConfigManager.getAllRegistered()
                                                .stream()
                                                .map(Class::getSimpleName)
                                                .sorted(String::compareToIgnoreCase)
                                                .filter(c -> c.toLowerCase().startsWith(start))
                                                .forEach(builder::suggest);
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        String module = StringArgumentType.getString(ctx, "module");
                                        Class<?> target = ConfigManager.getAllRegistered()
                                                .stream()
                                                .filter(c -> c.getSimpleName().equals(module))
                                                .findFirst().orElseThrow();
                                        ConfigManager.refresh(target);
                                        TextUtils.sendOps(ctx, "blossom.config-reload", target);
                                        return 1;
                                    })))
                    .then(literal("clear-countdowns")
                            .requires(Permissions.require("blossom.lib.base-command.clear.countdowns", 2))
                            .executes(ctx -> {
                                TeleportUtils.clearAll();
                                TextUtils.sendOps(ctx, "blossom.clear-countdowns.all");
                                return 1;
                            })
                            .then(argument("player", EntityArgumentType.player())
                                    .executes(ctx -> {
                                        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
                                        TeleportUtils.cancelCountdowns(player.getUuid());
                                        TextUtils.sendOps(ctx, "blossom.clear-countdowns.one", player);
                                        return 1;
                                    })))
                    .then(literal("clear-cooldowns")
                            .requires(Permissions.require("bolssom.lib.base-command.clear.cooldowns", 2))
                            .executes(ctx -> {
                                TeleportUtils.cancelAllCooldowns();
                                TextUtils.sendOps(ctx, "blossom.clear-cooldowns.all");
                                return 1;
                            })
                            .then(argument("player", EntityArgumentType.player())
                                    .executes(ctx -> {
                                        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
                                        TeleportUtils.cancelCooldowns(player.getUuid());
                                        TextUtils.sendOps(ctx, "blossom.clear-cooldowns.one", player);
                                        return 1;
                                    })
                                    .then(argument("type", StringArgumentType.greedyString())
                                            .suggests((ctx, builder) -> {
                                                String start = builder.getRemaining().toLowerCase();
                                                TeleportUtils.getCooldowns(EntityArgumentType.getPlayer(ctx, "player").getUuid())
                                                        .stream()
                                                        .map(Class::getSimpleName)
                                                        .sorted(String::compareToIgnoreCase)
                                                        .filter(c -> c.toLowerCase().startsWith(start))
                                                        .forEach(builder::suggest);
                                                return builder.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                String type = StringArgumentType.getString(ctx, "type");
                                                UUID player = EntityArgumentType.getPlayer(ctx, "player").getUuid();
                                                Class<?> target = TeleportUtils.getCooldowns(player)
                                                        .stream()
                                                        .filter(c -> c.getSimpleName().equals(type))
                                                        .findFirst().orElseThrow();
                                                TeleportUtils.cancelCooldown(player, target);
                                                TextUtils.sendOps(ctx, "blossom.clear-cooldowns.type", player, type);
                                                return 1;
                                            }))))
                    .then(literal("debug")
                            .requires(Permissions.require("blossom.lib.base-command.debug", 4))
                            .then(literal("countdown")
                                    .then(argument("standStill", IntegerArgumentType.integer(0))
                                            .executes(ctx -> {
                                                int standStill = IntegerArgumentType.getInteger(ctx, "standStill");
                                                TextUtils.send(ctx, "blossom.debug.countdown.start", standStill);
                                                TeleportUtils.genericCountdown(
                                                        null,
                                                        standStill,
                                                        ctx.getSource().getPlayer(),
                                                        () -> {
                                                            LOGGER.info("debug countdown done");
                                                            TextUtils.send(ctx, "blossom.debug.countdown.end");
                                                        }
                                                );
                                                return 1;
                                            })))
                            .then(literal("teleport")
                                    .then(argument("standStill", IntegerArgumentType.integer(0))
                                            .then(argument("pos", Vec3ArgumentType.vec3(true))
                                                    .then(argument("rot", RotationArgumentType.rotation())
                                                            .executes(ctx -> {
                                                                Vec2f rot = RotationArgumentType.getRotation(ctx, "rot")
                                                                        .toAbsoluteRotation(ctx.getSource());
                                                                int standStill = IntegerArgumentType.getInteger(ctx, "standStill");
                                                                TextUtils.send(ctx, "blossom.debug.teleport.no-cooldown", standStill);
                                                                return TeleportUtils.teleport(
                                                                        null,
                                                                        standStill,
                                                                        ctx.getSource().getPlayer(),
                                                                        () -> new TeleportUtils.TeleportDestination(
                                                                                ctx.getSource().getWorld(),
                                                                                Vec3ArgumentType.getVec3(ctx, "pos"),
                                                                                rot.y, rot.x
                                                                        )
                                                                ) ? 1 : 0;
                                                            })))
                                            .then(argument("cooldown", IntegerArgumentType.integer())
                                                    .then(argument("pos", Vec3ArgumentType.vec3(true))
                                                            .then(argument("rot", RotationArgumentType.rotation())
                                                                    .executes(ctx -> {
                                                                        Vec2f rot = RotationArgumentType.getRotation(ctx, "rot")
                                                                                .toAbsoluteRotation(ctx.getSource());
                                                                        int standStill = IntegerArgumentType.getInteger(ctx, "standStill");
                                                                        int cooldown = IntegerArgumentType.getInteger(ctx, "cooldown");
                                                                        TextUtils.send(ctx, "blossom.debug.teleport.cooldown", standStill, cooldown);
                                                                        return TeleportUtils.teleport(
                                                                                null,
                                                                                standStill,
                                                                                cooldown,
                                                                                BlossomLib.class,
                                                                                ctx.getSource().getPlayer(),
                                                                                () -> new TeleportUtils.TeleportDestination(
                                                                                        ctx.getSource().getWorld(),
                                                                                        Vec3ArgumentType.getVec3(ctx, "pos"),
                                                                                        rot.y, rot.x
                                                                                )
                                                                        ) ? 1 : 0;
                                                                    }))))))));

            dispatcher.register(literal("tpcancel")
                    .requires(
                            Permissions.require("blossom.tpcancel", true)
                                    .and(source -> {
                                        try {
                                            return TeleportUtils.hasCountdowns(source.getPlayer().getUuid());
                                        } catch (CommandSyntaxException e) {
                                            return false;
                                        }
                                    }))
                    .executes(ctx -> {
                        TeleportUtils.cancelCountdowns(ctx.getSource().getPlayer().getUuid());
                        TextUtils.send(ctx, "blossom.tpcancel");
                        return 1;
                    }));

            COMMANDS.forEach(dispatcher::register);

            COMMAND_CONSUMERS.forEach(consumer -> consumer.accept(dispatcher));
        });
    }

    public static void addCommand(LiteralArgumentBuilder<ServerCommandSource> command) {
        COMMANDS.add(command);
    }

    public static void registerCommand(Consumer<CommandDispatcher<ServerCommandSource>> callback) {
        COMMAND_CONSUMERS.add(callback);
    }
}
