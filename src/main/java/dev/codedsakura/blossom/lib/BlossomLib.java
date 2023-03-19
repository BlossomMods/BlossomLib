package dev.codedsakura.blossom.lib;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.codedsakura.blossom.lib.config.ConfigManager;
import dev.codedsakura.blossom.lib.permissions.Permissions;
import dev.codedsakura.blossom.lib.teleport.TeleportUtils;
import dev.codedsakura.blossom.lib.text.DimName;
import dev.codedsakura.blossom.lib.text.TextUtils;
import dev.codedsakura.blossom.lib.utils.PlayerSetFoV;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec2f;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;


public class BlossomLib implements ModInitializer {
    private static final ArrayList<LiteralArgumentBuilder<ServerCommandSource>> COMMANDS = new ArrayList<>();
    private static final ArrayList<Consumer<CommandDispatcher<ServerCommandSource>>> COMMAND_CONSUMERS = new ArrayList<>();

    @Override
    public void onInitialize() {
        BlossomGlobals.LOGGER.info("BlossomLib is starting...");

        ServerTickEvents.END_SERVER_TICK.register(_server -> TeleportUtils.tick());

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
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
                            .requires(Permissions.require("blossom.lib.base-command.clear.cooldowns", 2))
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
                                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                if (player == null) {
                                                    return 1;
                                                }
                                                TextUtils.send(ctx, "blossom.debug.countdown.start", standStill);

                                                TeleportUtils.genericCountdown(
                                                        null,
                                                        standStill,
                                                        player,
                                                        () -> {
                                                            BlossomGlobals.LOGGER.info("debug countdown done");
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
                                                                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                                        if (player == null) {
                                                                            return 1;
                                                                        }
                                                                        TextUtils.send(ctx, "blossom.debug.teleport.cooldown", standStill, cooldown);
                                                                        return TeleportUtils.teleport(
                                                                                null,
                                                                                standStill,
                                                                                cooldown,
                                                                                BlossomLib.class,
                                                                                player,
                                                                                () -> new TeleportUtils.TeleportDestination(
                                                                                        ctx.getSource().getWorld(),
                                                                                        Vec3ArgumentType.getVec3(ctx, "pos"),
                                                                                        rot.y, rot.x
                                                                                )
                                                                        ) ? 1 : 0;
                                                                    }))))))
                            .then(literal("fov")
                                    .then(argument("multiplier", FloatArgumentType.floatArg())
                                            .executes(ctx -> {
                                                PlayerSetFoV.setPlayerFoV(
                                                        Objects.requireNonNull(ctx.getSource().getPlayer()),
                                                        FloatArgumentType.getFloat(ctx, "multiplier")
                                                );
                                                return Command.SINGLE_SUCCESS;
                                            })))
                            .then(literal("dimName")
                                    .then(argument("identifier", IdentifierArgumentType.identifier())
                                            .executes(ctx -> {
                                                TextUtils.send(ctx, "Result: %s", DimName.get(IdentifierArgumentType.getIdentifier(ctx, "identifier")));
                                                return Command.SINGLE_SUCCESS;
                                            })))));

            dispatcher.register(literal("tpcancel")
                    .requires(
                            Permissions.require("blossom.tpcancel", true))
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();

                        if (TeleportUtils.hasCountdowns(player.getUuid())) {
                            TeleportUtils.cancelCountdowns(player.getUuid());
                            TextUtils.send(ctx, "blossom.tpcancel");
                        } else {
                            TextUtils.send(ctx, "blossom.tpcancel.fail");
                        }
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
