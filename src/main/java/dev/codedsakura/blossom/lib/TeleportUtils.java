package dev.codedsakura.blossom.lib;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static dev.codedsakura.blossom.lib.BlossomLib.CONFIG;
import static dev.codedsakura.blossom.lib.BlossomLib.LOGGER;

public class TeleportUtils {
    private static final ArrayList<CounterRunnable> TASKS = new ArrayList<>();
    private static final HashMap<HashablePair<UUID, Class<?>>, Long> COOLDOWNS = new HashMap<>();
    private static final HashMap<UUID, TeleportDestination> LAST_TELEPORT = new HashMap<>();
    private static final String IDENTIFIER = "blossom:standstill";

    static void tick() {
        TASKS.forEach(CounterRunnable::run);
        TASKS.removeIf(CounterRunnable::shouldRemove);
    }

    public static void genericCountdown(@Nullable TeleportConfig customConfig, double standStillTime, ServerPlayerEntity who, Runnable onDone) {
        LOGGER.debug("Create new genericCountdown for {} ({} s)", who.getUuid(), standStillTime);
        MinecraftServer server = who.getServer();
        assert server != null;
        final Vec3d[] lastPos = {who.getPos()};

        final TeleportConfig config = customConfig == null ? BlossomLib.CONFIG.baseTeleportation : customConfig.cloneMerge();

        CommandBossBar commandBossBar = null;
        int standTicks = (int) (standStillTime * 20);
        if (config.bossBar.enabled) {
            commandBossBar = server.getBossBarManager().add(
                    new Identifier(IDENTIFIER + "_" + who.getUuidAsString()),
                    new TranslatableText(
                            "blossom.countdown.boss_bar.name",
                            new LiteralText(Integer.toString(standTicks))
                                    .styled(style -> style.withColor(TextColor.parse(CONFIG.colors.variable)))
                    ).styled(style -> style.withColor(TextColor.parse(config.bossBar.textColor)))
            );
            commandBossBar.setColor(BossBar.Color.byName(config.bossBar.color));
            commandBossBar.addPlayer(who);
        }
        who.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 10, 5));

        CommandBossBar finalCommandBossBar = commandBossBar;
        TASKS.add(new CounterRunnable(standTicks, who.getUuid()) {
            @Override
            void run() {
                if (counter <= 0) {
                    LOGGER.debug("genericCountdown for {} has ended", player);
                    if (finalCommandBossBar != null) {
                        finalCommandBossBar.removePlayer(who);
                        server.getBossBarManager().remove(finalCommandBossBar);
                    }
                    if (config.titleMessage.enabled) {
                        who.networkHandler.sendPacket(new SubtitleS2CPacket(
                                config.titleMessage.subtitleDone.getText("blossom.countdown.title.done.subtitle")
                        ));
                        who.networkHandler.sendPacket(new TitleS2CPacket(
                                config.titleMessage.titleDone.getText("blossom.countdown.title.done.title")
                        ));
                    }

                    if (counter == 0) {
                        if (config.actionBarMessageEnabled) {
                            who.sendMessage(
                                    TextUtils.translation("blossom.countdown.action_bar.done"),
                                    true
                            );
                        }
                        onDone.run();
                    }

                    counter = -1;
                    return;
                }

                Vec3d pos = who.getPos();
                double dist = lastPos[0].distanceTo(pos);
                if (dist < .05) {
                    if (dist != 0) lastPos[0] = pos;
                    counter--;
                } else {
                    LOGGER.debug("genericCountdown for {} has been reset after {} ticks", player, standTicks);
                    lastPos[0] = pos;
                    counter = standTicks;
                }

                int remaining = (int) Math.floor((counter / 20f) + 1);

                if (finalCommandBossBar != null) {
                    finalCommandBossBar.setPercent((float) counter / standTicks);
                    finalCommandBossBar.setName(
                            new TranslatableText(
                                    "blossom.countdown.boss_bar.name",
                                    new LiteralText(Integer.toString(remaining))
                                            .styled(style -> style.withColor(TextColor.parse(CONFIG.colors.variable)))
                            ).styled(style -> style.withColor(TextColor.parse(config.bossBar.textColor)))
                    );
                }
                if (config.actionBarMessageEnabled) {
                    who.sendMessage(
                            TextUtils.translation("blossom.countdown.action_bar.counting", remaining),
                            true
                    );
                }
                if (config.titleMessage.enabled) {
                    who.networkHandler.sendPacket(new SubtitleS2CPacket(
                            config.titleMessage.subtitleCounting.getText("blossom.countdown.title.counting.subtitle", remaining)
                    ));
                    who.networkHandler.sendPacket(new TitleS2CPacket(
                            config.titleMessage.titleCounting.getText("blossom.countdown.title.counting.title", remaining)
                    ));
                }
            }
        });
    }

    public static void cancelCountdowns(UUID player) {
        TASKS.stream()
                .filter(task -> task.player.compareTo(player) == 0)
                .forEach(task -> task.counter = -1);
    }

    public static boolean hasCountdowns(UUID player) {
        return TASKS.stream().anyMatch(task -> task.player.compareTo(player) == 0);
    }

    static void clearAll() {
        TASKS.forEach(task -> task.counter = -1);
    }

    private static abstract class CounterRunnable {
        int counter;
        UUID player;

        public CounterRunnable(int counter, UUID player) {
            this.counter = counter;
            this.player = player;
        }

        abstract void run();

        boolean shouldRemove() {
            return counter < 0;
        }
    }


    public static boolean teleport(@Nullable TeleportConfig customConfig, double standStillTime, ServerPlayerEntity who, GetDestination getWhere) {
        return teleport(customConfig, standStillTime, 0, TeleportUtils.class, who, getWhere);
    }

    public static boolean teleport(@Nullable TeleportConfig customConfig, double standStillTime, long cooldownTime,
                                   Class<?> cooldownClass, ServerPlayerEntity who, GetDestination getWhere) {
        if (hasCountdowns(who.getUuid())) {
            who.sendMessage(TextUtils.fTranslation("blossom.error.has-countdown", TextUtils.Type.ERROR), false);
            return false;
        }
        HashablePair<UUID, Class<?>> pair = new HashablePair<>(who.getUuid(), cooldownClass);
        if (COOLDOWNS.containsKey(pair)) {
            long timeLeft = COOLDOWNS.get(pair) - new Date().getTime() / 1000;
            if (timeLeft > 0) {
                who.sendMessage(TextUtils.fTranslation("blossom.error.has-cooldown", TextUtils.Type.ERROR, timeLeft), false);
                return false;
            }
        }


        genericCountdown(customConfig, standStillTime, who, () -> {
            final TeleportConfig config = customConfig == null ? BlossomLib.CONFIG.baseTeleportation : customConfig.cloneMerge();
            if (config.allowBack) {
                LAST_TELEPORT.put(who.getUuid(), new TeleportDestination(who));
            }
            TeleportDestination where = getWhere.get();
            who.teleport(where.world, where.x, where.y, where.z, where.yaw, where.pitch);
            COOLDOWNS.put(pair, new Date().getTime() / 1000 + cooldownTime);
        });
        return true;
    }


    public static void cancelCooldown(UUID player, Class<?> targetClass) {
        HashablePair<UUID, Class<?>> key = new HashablePair<>(player, targetClass);
        COOLDOWNS.remove(key);
    }

    public static void cancelCooldowns(UUID player) {
        COOLDOWNS.keySet().removeIf(key -> key.getLeft().compareTo(player) == 0);
    }

    public static void cancelAllCooldowns() {
        COOLDOWNS.clear();
    }


    public static TeleportDestination getLastTeleport(UUID player) {
        return LAST_TELEPORT.get(player);
    }


    public static boolean hasCooldown(UUID player, Class<?> targetClass) {
        HashablePair<UUID, Class<?>> key = new HashablePair<>(player, targetClass);
        return COOLDOWNS.containsKey(key);
    }

    public static List<Class<?>> getCooldowns(UUID player) {
        return COOLDOWNS.keySet()
                .stream()
                .filter(key -> key.getLeft().compareTo(player) == 0)
                .map(HashablePair::getRight)
                .collect(Collectors.toList());
    }


    public interface GetDestination {
        TeleportDestination get();
    }

    public static class TeleportDestination {
        final public ServerWorld world;
        final public double x, y, z;
        final public float yaw, pitch;

        public TeleportDestination(ServerWorld world, Vec3d pos, float yaw, float pitch) {
            this(world, pos.x, pos.y, pos.z, yaw, pitch);
        }

        public TeleportDestination(ServerWorld world, double x, double y, double z, Vec2f rotation) {
            this(world, x, y, z, rotation.x, rotation.y);
        }

        public TeleportDestination(ServerWorld world, Vec3d pos, Vec2f rotation) {
            this(world, pos.x, pos.y, pos.z, rotation.x, rotation.y);
        }

        public TeleportDestination(PlayerEntity player) {
            this((ServerWorld) player.world, player.getPos(), player.getYaw(), player.getPitch());
        }

        public TeleportDestination(ServerWorld world, double x, double y, double z, float yaw, float pitch) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
