package dev.codedsakura.blossom.lib.teleport;

import dev.codedsakura.blossom.lib.BlossomGlobals;
import dev.codedsakura.blossom.lib.text.TextUtils;
import dev.codedsakura.blossom.lib.utils.HashablePair;
import dev.codedsakura.blossom.lib.utils.PlayerSetFoV;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static dev.codedsakura.blossom.lib.BlossomGlobals.CONFIG;
import static dev.codedsakura.blossom.lib.BlossomGlobals.LOGGER;

public class TeleportUtils {
    private static final ArrayList<CounterRunnable> TASKS = new ArrayList<>();
    private static final HashMap<HashablePair<UUID, Class<?>>, Long> COOLDOWNS = new HashMap<>();
    private static final HashMap<UUID, TeleportDestination> LAST_TELEPORT = new HashMap<>();
    private static final String IDENTIFIER = "blossom:standstill";

    public static void tick() {
        TASKS.forEach(CounterRunnable::run);
        TASKS.removeIf(CounterRunnable::shouldRemove);
    }

    public static void genericCountdown(@Nullable TeleportConfig customConfig, double standStillTime, ServerPlayerEntity who, Runnable onDone) {
        LOGGER.debug("Create new genericCountdown for {} ({} s)", who.getUuid(), standStillTime);
        MinecraftServer server = who.getServer();
        assert server != null;
        final Vec3d[] lastPos = {who.getPos()};

        final TeleportConfig config = customConfig == null ? BlossomGlobals.CONFIG.baseTeleportation : customConfig.cloneMerge();

        CommandBossBar commandBossBar = null;
        int standTicks = (int) (standStillTime * 20);
        if (config.bossBar.enabled) {
            commandBossBar = server.getBossBarManager().add(
                    new Identifier(IDENTIFIER + "_" + who.getUuidAsString()),
                    TextUtils.translation(
                            "blossom.countdown.boss_bar.name",
                            Text.literal(Integer.toString(standTicks))
                                    .styled(style -> style.withColor(TextColor.parse(CONFIG.colors.variable)))
                    ).styled(style -> style.withColor(TextColor.parse(config.bossBar.textColor)))
            );
            commandBossBar.setColor(BossBar.Color.byName(config.bossBar.color));
            commandBossBar.addPlayer(who);
        }
        who.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 10, 5));

        CommandBossBar finalCommandBossBar = commandBossBar;
        float[] lastFovMultiplier = new float[]{1f};
        int endAt = config.fovEffectAfter.isEnabled() ? -config.fovEffectAfter.getStepCount() - 1 : 0;

        TASKS.add(new CounterRunnable(standTicks, endAt, who.getUuid()) {
            private void handleZero() {
                if (counter != 0) {
                    return;
                }

                LOGGER.debug("genericCountdown for {} has ended", player);
                cleanUp();
                if (config.titleMessage.enabled) {
                    who.networkHandler.sendPacket(new SubtitleS2CPacket(
                            config.titleMessage.subtitleDone.getText("blossom.countdown.title.done.subtitle")
                    ));
                    who.networkHandler.sendPacket(new TitleS2CPacket(
                            config.titleMessage.titleDone.getText("blossom.countdown.title.done.title")
                    ));
                }

                if (config.actionBarMessageEnabled) {
                    who.sendMessage(
                            TextUtils.translation("blossom.countdown.action_bar.done"),
                            true
                    );
                }
                onDone.run();
            }

            private void changeFov() {
                int stepIndex = config.fovEffectBefore.getStepCount() + 1 - counter;
                if (config.fovEffectBefore.isEnabled() && counter > 0 && stepIndex >= 0) {
                    float newFov = (float) (double) config.fovEffectBefore.getData().get(stepIndex);
                    PlayerSetFoV.setPlayerFoV(who, 2 * newFov - lastFovMultiplier[0]);
                    lastFovMultiplier[0] = newFov;
                }
                if (config.fovEffectAfter.isEnabled() && counter < 0) {
                    stepIndex = -counter - 1;
                    float newFov = (float) (double) config.fovEffectAfter.getData().get(stepIndex);
                    PlayerSetFoV.setPlayerFoV(who, 2 * newFov - lastFovMultiplier[0]);
                    lastFovMultiplier[0] = newFov;
                }
            }

            private boolean isStill() {
                if (counter < 1) {
                    return true;
                }

                Vec3d pos = who.getPos();
                double dist = lastPos[0].distanceTo(pos);
                if (dist < .05) {
                    if (dist != 0) lastPos[0] = pos;
                    return true;
                } else {
                    LOGGER.debug("genericCountdown for {} has been reset after {} ticks", player, standTicks - counter);
                    lastFovMultiplier[0] = 1f;
                    PlayerSetFoV.setPlayerFoV(who, 1);
                    lastPos[0] = pos;
                    return false;
                }
            }

            private void updateCountdown() {
                if (counter < 1) {
                    return;
                }

                int remaining = (int) Math.floor((counter / 20f) + 1);

                if (finalCommandBossBar != null) {
                    finalCommandBossBar.setPercent((float) counter / standTicks);
                    finalCommandBossBar.setName(
                            TextUtils.translation(
                                    "blossom.countdown.boss_bar.name",
                                    Text.literal(Integer.toString(remaining))
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

            @Override
            protected void run() {
                handleZero();

                changeFov();

                if (!isStill()) {
                    if (config.cancelOnMove) {
                        remove();
                        return;
                    }
                    counter = standTicks;
                    return;
                }

                updateCountdown();

                counter--;
            }

            @Override
            protected void cleanUp() {
                if (finalCommandBossBar != null) {
                    finalCommandBossBar.removePlayer(who);
                    server.getBossBarManager().remove(finalCommandBossBar);
                }

                if (counter != 0) {
                    PlayerSetFoV.setPlayerFoV(who, 1);
                }
            }
        });
    }

    private static abstract class CounterRunnable {
        int counter, endAt;
        UUID player;
        boolean forceRemove = false;

        public CounterRunnable(int counter, int endAt, UUID player) {
            this.counter = counter;
            this.endAt = endAt;
            this.player = player;
        }

        protected abstract void run();

        protected abstract void cleanUp();

        boolean shouldRemove() {
            return forceRemove || counter < endAt;
        }

        void remove() {
            cleanUp();
            forceRemove = true;
        }
    }

    public static void cancelCountdowns(UUID player) {
        TASKS.stream()
                .filter(task -> task.player.compareTo(player) == 0)
                .forEach(CounterRunnable::remove);
    }

    public static boolean hasCountdowns(UUID player) {
        return TASKS.stream().anyMatch(task -> task.player.compareTo(player) == 0);
    }

    public static void clearAll() {
        TASKS.forEach(CounterRunnable::remove);
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
            final TeleportConfig config = customConfig == null ? BlossomGlobals.CONFIG.baseTeleportation : customConfig.cloneMerge();
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

        @Override
        public String toString() {
            return "TeleportDestination{" +
                    "world=" + world +
                    ", x=" + x +
                    ", y=" + y +
                    ", z=" + z +
                    ", yaw=" + yaw +
                    ", pitch=" + pitch +
                    '}';
        }
    }
}
