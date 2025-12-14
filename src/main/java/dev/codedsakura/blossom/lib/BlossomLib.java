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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;


public class BlossomLib implements ModInitializer {
    private static final ArrayList<LiteralArgumentBuilder<CommandSourceStack>> COMMANDS = new ArrayList<>();
    private static final ArrayList<Consumer<CommandDispatcher<CommandSourceStack>>> COMMAND_CONSUMERS = new ArrayList<>();

    @Override
    public void onInitialize() {
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
                            .then(argument("player", EntityArgument.player())
                                    .executes(ctx -> {
                                        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                        TeleportUtils.cancelCountdowns(player.getUUID());
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
                            .then(argument("player", EntityArgument.player())
                                    .executes(ctx -> {
                                        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                        TeleportUtils.cancelCooldowns(player.getUUID());
                                        TextUtils.sendOps(ctx, "blossom.clear-cooldowns.one", player);
                                        return 1;
                                    })
                                    .then(argument("type", StringArgumentType.greedyString())
                                            .suggests((ctx, builder) -> {
                                                String start = builder.getRemaining().toLowerCase();
                                                TeleportUtils.getCooldowns(EntityArgument.getPlayer(ctx, "player").getUUID())
                                                        .stream()
                                                        .map(Class::getSimpleName)
                                                        .sorted(String::compareToIgnoreCase)
                                                        .filter(c -> c.toLowerCase().startsWith(start))
                                                        .forEach(builder::suggest);
                                                return builder.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                String type = StringArgumentType.getString(ctx, "type");
                                                UUID player = EntityArgument.getPlayer(ctx, "player").getUUID();
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
                                                ServerPlayer player = ctx.getSource().getPlayer();
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
                                            .then(argument("pos", Vec3Argument.vec3(true))
                                                    .then(argument("rot", RotationArgument.rotation())
                                                            .executes(ctx -> {
                                                                Vec2 rot = RotationArgument.getRotation(ctx, "rot")
                                                                        .getRotation(ctx.getSource());
                                                                int standStill = IntegerArgumentType.getInteger(ctx, "standStill");
                                                                TextUtils.send(ctx, "blossom.debug.teleport.no-cooldown", standStill);
                                                                return TeleportUtils.teleport(
                                                                        null,
                                                                        standStill,
                                                                        ctx.getSource().getPlayer(),
                                                                        () -> new TeleportUtils.TeleportDestination(
                                                                                ctx.getSource().getLevel(),
                                                                                Vec3Argument.getVec3(ctx, "pos"),
                                                                                rot.y, rot.x
                                                                        )
                                                                ) ? 1 : 0;
                                                            })))
                                            .then(argument("cooldown", IntegerArgumentType.integer())
                                                    .then(argument("pos", Vec3Argument.vec3(true))
                                                            .then(argument("rot", RotationArgument.rotation())
                                                                    .executes(ctx -> {
                                                                        Vec2 rot = RotationArgument.getRotation(ctx, "rot")
                                                                                .getRotation(ctx.getSource());
                                                                        int standStill = IntegerArgumentType.getInteger(ctx, "standStill");
                                                                        int cooldown = IntegerArgumentType.getInteger(ctx, "cooldown");
                                                                        ServerPlayer player = ctx.getSource().getPlayer();
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
                                                                                        ctx.getSource().getLevel(),
                                                                                        Vec3Argument.getVec3(ctx, "pos"),
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
                                    .then(argument("identifier", IdentifierArgument.id())
                                            .executes(ctx -> {
                                                TextUtils.send(ctx, "Result: %s", DimName.get(IdentifierArgument.getId(ctx, "identifier")));
                                                return Command.SINGLE_SUCCESS;
                                            })))));

            dispatcher.register(literal("tpcancel")
                    .requires(
                            Permissions.require("blossom.tpcancel", true))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();

                        if (TeleportUtils.hasCountdowns(player.getUUID())) {
                            TeleportUtils.cancelCountdowns(player.getUUID());
                            TextUtils.send(ctx, "blossom.tpcancel");
                        } else {
                            TextUtils.send(ctx, "blossom.tpcancel.fail");
                        }
                        return 1;
                    }));

            COMMANDS.forEach(dispatcher::register);

            COMMAND_CONSUMERS.forEach(consumer -> consumer.accept(dispatcher));
        });

        BlossomGlobals.LOGGER.info("BlossomLib has starting");
    }

    public static void addCommand(LiteralArgumentBuilder<CommandSourceStack> command) {
        COMMANDS.add(command);
    }

    public static void registerCommand(Consumer<CommandDispatcher<CommandSourceStack>> callback) {
        COMMAND_CONSUMERS.add(callback);
    }
}
