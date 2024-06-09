package dev.codedsakura.blossom.lib;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.codedsakura.blossom.lib.config.ConfigManager;
import dev.codedsakura.blossom.lib.mod.BlossomMod;
import dev.codedsakura.blossom.lib.mod.ModController;
import dev.codedsakura.blossom.lib.permissions.Permissions;
import dev.codedsakura.blossom.lib.teleport.TeleportUtils;
import dev.codedsakura.blossom.lib.text.DimName;
import dev.codedsakura.blossom.lib.text.TextUtils;
import dev.codedsakura.blossom.lib.utils.PlayerSetFoV;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.argument.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec2f;

import java.util.Objects;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;


public class BlossomLib implements ModInitializer {
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
                                TextUtils.sendOps(ctx, "blossom.lib.configs-reload");
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
                                        TextUtils.sendOps(ctx, "blossom.lib.config-reload", target);
                                        return 1;
                                    })))
                    .then(literal("clear-countdowns")
                            .requires(Permissions.require("blossom.lib.base-command.clear.countdowns", 2))
                            .executes(ctx -> {
                                TeleportUtils.clearAll();
                                TextUtils.sendOps(ctx, "blossom.lib.clear-countdowns.all");
                                return 1;
                            })
                            .then(argument("player", EntityArgumentType.player())
                                    .executes(ctx -> {
                                        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
                                        TeleportUtils.cancelCountdowns(player.getUuid());
                                        TextUtils.sendOps(ctx, "blossom.lib.clear-countdowns.one", player);
                                        return 1;
                                    })))
                    .then(literal("clear-cooldowns")
                            .requires(Permissions.require("blossom.lib.base-command.clear.cooldowns", 2))
                            .executes(ctx -> {
                                TeleportUtils.cancelAllCooldowns();
                                TextUtils.sendOps(ctx, "blossom.lib.clear-cooldowns.all");
                                return 1;
                            })
                            .then(argument("player", EntityArgumentType.player())
                                    .executes(ctx -> {
                                        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
                                        TeleportUtils.cancelCooldowns(player.getUuid());
                                        TextUtils.sendOps(ctx, "blossom.lib.clear-cooldowns.one", player);
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
                                                TextUtils.sendOps(ctx, "blossom.lib.clear-cooldowns.type", player, type);
                                                return 1;
                                            }))))
                    .then(literal("debug")
                            .requires(Permissions.require("blossom.lib.base-command.debug", 4))
                            .then(literal("countdown")
                                            .executes(ctx -> {
                                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                if (player == null) {
                                                    return 1;
                                                }
                                                TextUtils.send(ctx, "blossom.lib.debug.countdown.start");

                                                TeleportUtils.genericCountdown(
                                                        null,
                                                        player,
                                                        () -> {
                                                            BlossomGlobals.LOGGER.info("debug countdown done");
                                                            TextUtils.send(ctx, "blossom.lib.debug.countdown.end");
                                                        }
                                                );
                                                return 1;
                                            }))
                            .then(literal("teleport")
                                    .then(argument("pos", Vec3ArgumentType.vec3(true))
                                            .then(argument("rot", RotationArgumentType.rotation())
                                                    .executes(ctx -> {
                                                        Vec2f rot = RotationArgumentType.getRotation(ctx, "rot")
                                                                .toAbsoluteRotation(ctx.getSource());
                                                        TextUtils.send(ctx, "blossom.lib.debug.teleport");
                                                        return TeleportUtils.teleport(
                                                                null,
                                                                ctx.getSource().getPlayer(),
                                                                () -> new TeleportUtils.TeleportDestination(
                                                                        ctx.getSource().getWorld(),
                                                                        Vec3ArgumentType.getVec3(ctx, "pos"),
                                                                        rot.y, rot.x
                                                                )
                                                        ) ? 1 : 0;
                                                    })
                                                    .then(argument("dim", DimensionArgumentType.dimension())
                                                            .executes(ctx -> {
                                                                Vec2f rot = RotationArgumentType.getRotation(ctx, "rot")
                                                                        .toAbsoluteRotation(ctx.getSource());
                                                                var world = DimensionArgumentType.getDimensionArgument(ctx, "dim");
                                                                TextUtils.send(ctx, "blossom.lib.debug.teleport");
                                                                return TeleportUtils.teleport(
                                                                        null,
                                                                        ctx.getSource().getPlayer(),
                                                                        () -> new TeleportUtils.TeleportDestination(
                                                                                world,
                                                                                Vec3ArgumentType.getVec3(ctx, "pos"),
                                                                                rot.y, rot.x
                                                                        )
                                                                ) ? 1 : 0;
                                                            })))))
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

        });

        BlossomGlobals.LOGGER.info("BlossomLib has started");
    }

    public static <T> void registerSubMod(BlossomMod<T> mod) {
        ModController.register(mod);
    }
}
