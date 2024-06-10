package dev.codedsakura.blossom.lib.teleport;

import dev.codedsakura.blossom.lib.BlossomGlobals;
import dev.codedsakura.blossom.lib.permissions.Permissions;
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
    private static final ArrayList<LastTeleportAddHook> LAST_TELEPORT_ADD_HOOKS = new ArrayList<>();
    private static final String IDENTIFIER = "blossom:standstill";

    public static void tick() {
        TASKS.forEach(CounterRunnable::run);
        TASKS.removeIf(CounterRunnable::shouldRemove);
    }

    public static void genericCountdown(@Nullable TeleportConfig customConfig, ServerPlayerEntity who, Runnable onDone) {
        final TeleportConfig config = customConfig == null ? BlossomGlobals.CONFIG.baseTeleportation : getPlayerSpecific(customConfig, who);

        LOGGER.debug("Create new genericCountdown for {} ({} s)", who.getUuid(), config.standStill);
        MinecraftServer server = who.getServer();
        assert server != null;
        final Vec3d[] lastPos = {who.getPos()};

        CommandBossBar commandBossBar = null;
        int standTicks = config.standStill * 20;
        if (config.bossBar.enabled) {
            Identifier id = new Identifier(IDENTIFIER + "_" + who.getUuidAsString());
            commandBossBar = Optional.ofNullable(server.getBossBarManager().get(id))
                    .orElseGet(() -> server.getBossBarManager().add(
                            id,
                            TextUtils.translation(
                                    "blossom.countdown.boss_bar.name",
                                    Text.literal(Integer.toString(standTicks))
                                            .styled(style -> style.withColor(TextUtils.parseColor(CONFIG.colors.variable)))
                            ).styled(style -> style.withColor(TextUtils.parseColor(config.bossBar.textColor)))
                    ));

            commandBossBar.setColor(BossBar.Color.byName(config.bossBar.color));
            commandBossBar.clearPlayers();
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
                    PlayerSetFoV.resetPlayerFoV(who);
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
                                            .styled(style -> style.withColor(TextUtils.parseColor(CONFIG.colors.variable)))
                            ).styled(style -> style.withColor(TextUtils.parseColor(config.bossBar.textColor)))
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

                PlayerSetFoV.resetPlayerFoV(who);
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


    public static boolean teleport(@Nullable TeleportConfig customConfig, ServerPlayerEntity who, GetDestination getWhere) {
        return teleport(customConfig, TeleportUtils.class, who, getWhere);
    }

    public static boolean teleport(@Nullable TeleportConfig customConfig,
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

        final TeleportConfig config = customConfig == null ? BlossomGlobals.CONFIG.baseTeleportation : getPlayerSpecific(customConfig, who);

        var destinationWorld = getWhere.get().world.getRegistryKey().getValue().toString();
        if (!teleportCheckAndInform(config, who, destinationWorld)) {
            return false;
        }

        genericCountdown(customConfig, who, () -> {
            if (config.allowBack) {
                TeleportUtils.addLastTeleport(who.getUuid(), new TeleportDestination(who));
            }
            TeleportDestination where = getWhere.get();
            who.teleport(where.world, where.x, where.y, where.z, where.yaw, where.pitch);
            COOLDOWNS.put(pair, new Date().getTime() / 1000 + config.cooldown);
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


    public static TeleportConfig getPlayerSpecific(TeleportConfig config, ServerPlayerEntity player) {
        if (TeleportConfig.DEFAULT.permissionOverrides.isEmpty()) {
            return config.cloneMergeDefault();
        }

        Optional<String> firstPerm = TeleportConfig.DEFAULT.permissionOverrides
                .keySet()
                .stream()
                .filter(perm -> Permissions.check(player, perm, false))
                .findFirst();

        if (firstPerm.isEmpty()) {
            return config.cloneMergeDefault();
        }

        return TeleportConfig.DEFAULT.permissionOverrides
                .get(firstPerm.get())
                .cloneMergeDefault();
    }


    private enum DimTeleportCheck {
        ALLOWED(""),
        SOURCE_IN_BLACKLIST("source-blacklist"),
        DEST_IN_BLACKLIST("dest-blacklist"),
        SOURCE_AND_DEST_IN_BLACKLIST("source-dest-blacklist"),
        SOURCE_NOT_IN_WHITELIST("source-whitelist"),
        DEST_NOT_IN_WHITELIST("dest-whitelist"),
        SOURCE_AND_DEST_NOT_IN_WHITELIST("source-dest-whitelist"),
        ;

        public final String localeKey;

        DimTeleportCheck(String localeKey) {
            this.localeKey = localeKey;
        }
    }

    public static boolean teleportAllowed(TeleportConfig config, ServerPlayerEntity player, String dest) {
        config = getPlayerSpecific(config, player);
        var source = player.getWorld().getRegistryKey().getValue().toString();
        return dimTeleportCheck(config, source, dest) == DimTeleportCheck.ALLOWED;
    }

    public static boolean teleportCheckAndInform(TeleportConfig config, ServerPlayerEntity player, String dest) {
        config = getPlayerSpecific(config, player);
        var source = player.getWorld().getRegistryKey().getValue().toString();
        var check = dimTeleportCheck(config, source, dest);

        if (check == DimTeleportCheck.ALLOWED) {
            return true;
        }

        player.sendMessage(TextUtils.fTranslation("blossom.error.teleport." + check.localeKey, TextUtils.Type.ERROR, source, dest), false);

        return false;
    }

    private static DimTeleportCheck dimTeleportCheck(TeleportConfig config, String source, String dest) {
        boolean sourceInList = config.dimensionBlacklist.contains(source);
        boolean destInList = config.dimensionBlacklist.contains(dest);

        if (config.useDimBlacklistAsWhitelist) {
            switch (config.dimBlacklistBehavior) {
                case SOURCE:
                    if (!sourceInList) {
                        return DimTeleportCheck.SOURCE_NOT_IN_WHITELIST;
                    }
                    break;

                case DEST:
                    if (!destInList) {
                        return DimTeleportCheck.DEST_NOT_IN_WHITELIST;
                    }
                    break;

                case SOURCE_AND_DEST:
                    if (!sourceInList) {
                        if (!destInList) {
                            return DimTeleportCheck.SOURCE_AND_DEST_NOT_IN_WHITELIST;
                        } else {
                            return DimTeleportCheck.SOURCE_NOT_IN_WHITELIST;
                        }
                    }
                    if (!destInList) {
                        return DimTeleportCheck.DEST_NOT_IN_WHITELIST;
                    }
                    break;

                case SOURCE_OR_DEST:
                    if (!sourceInList && !destInList) {
                        return DimTeleportCheck.SOURCE_AND_DEST_NOT_IN_WHITELIST;
                    }
                    break;
            }
        } else {
            switch (config.dimBlacklistBehavior) {
                case SOURCE:
                    if (sourceInList) {
                        return DimTeleportCheck.SOURCE_IN_BLACKLIST;
                    }
                    break;

                case DEST:
                    if (destInList) {
                        return DimTeleportCheck.DEST_IN_BLACKLIST;
                    }
                    break;

                case SOURCE_AND_DEST:
                    if (sourceInList && destInList) {
                        return DimTeleportCheck.SOURCE_AND_DEST_IN_BLACKLIST;
                    }
                    break;

                case SOURCE_OR_DEST:
                    if (sourceInList) {
                        if (destInList) {
                            return DimTeleportCheck.SOURCE_AND_DEST_IN_BLACKLIST;
                        } else {
                            return DimTeleportCheck.SOURCE_IN_BLACKLIST;
                        }
                    }
                    if (destInList) {
                        return DimTeleportCheck.DEST_IN_BLACKLIST;
                    }
                    break;
            }
        }

        return DimTeleportCheck.ALLOWED;
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
            this((ServerWorld) player.getWorld(), player.getPos(), player.getYaw(), player.getPitch());
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

    public interface LastTeleportAddHook {
        void run(UUID player, TeleportDestination destination, HashMap<UUID, TeleportDestination> lastTeleportMap);
    }

    public static void addLastTeleportAddHook(LastTeleportAddHook hook) {
        LAST_TELEPORT_ADD_HOOKS.add(hook);
    }

    private static void addLastTeleport(UUID player, TeleportDestination destination) {
        LAST_TELEPORT.put(player, destination);
        LAST_TELEPORT_ADD_HOOKS.forEach(hook -> hook.run(player, destination, LAST_TELEPORT));
    }

    public static TeleportDestination getLastTeleport(UUID player) {
        return LAST_TELEPORT.get(player);
    }

    public static HashMap<UUID, TeleportDestination> getAllLastTeleports() {
        return LAST_TELEPORT;
    }
}
