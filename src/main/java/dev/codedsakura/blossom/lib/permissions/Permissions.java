package dev.codedsakura.blossom.lib.permissions;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Helper class to make fabric-permissions-api an optional dependency
 */
public class Permissions {
    private static boolean fpaLoaded = false;
    private static boolean fpaLoadedChecked = false;

    private static boolean isFPAPILoaded() {
        if (!fpaLoadedChecked) {
            fpaLoadedChecked = true;
            fpaLoaded = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");
        }
        return fpaLoaded;
    }


    /**
     * @see me.lucko.fabric.api.permissions.v0.Permissions#require(String, boolean)
     */
    public static Predicate<ServerCommandSource> require(@NotNull String permission, boolean defaultValue) {
        if (isFPAPILoaded()) {
            return PermissionsExecutor.require(permission, defaultValue);
        }
        return _ignored -> defaultValue;
    }

    /**
     * @see me.lucko.fabric.api.permissions.v0.Permissions#require(String, int)
     */
    public static Predicate<ServerCommandSource> require(@NotNull String permission, int level) {
        if (isFPAPILoaded()) {
            return PermissionsExecutor.require(permission, level);
        }
        return player -> player.hasPermissionLevel(level);
    }

    /**
     * @see me.lucko.fabric.api.permissions.v0.Permissions#check(Entity, String, boolean)
     */
    public static boolean check(@NotNull ServerCommandSource source, @NotNull String permission, boolean fallback) {
        if (isFPAPILoaded()) {
            return PermissionsExecutor.check(source, permission, fallback);
        }
        return fallback;
    }

    /**
     * @see me.lucko.fabric.api.permissions.v0.Permissions#check(Entity, String, int)
     */
    public static boolean check(@NotNull ServerCommandSource source, @NotNull String permission, int level) {
        if (isFPAPILoaded()) {
            return PermissionsExecutor.check(source, permission, level);
        }
        return source.hasPermissionLevel(level);
    }

    /**
     * @see me.lucko.fabric.api.permissions.v0.Permissions#check(Entity, String, boolean)
     */
    public static boolean check(@NotNull Entity entity, @NotNull String permission, boolean fallback) {
        if (isFPAPILoaded()) {
            return PermissionsExecutor.check(entity, permission, fallback);
        }
        return fallback;
    }

    /**
     * @see me.lucko.fabric.api.permissions.v0.Permissions#check(Entity, String, int)
     */
    public static boolean check(@NotNull Entity entity, @NotNull String permission, int level) {
        if (isFPAPILoaded()) {
            return PermissionsExecutor.check(entity, permission, level);
        }
        World world = entity.getEntityWorld();
        return entity.getCommandSource(world instanceof ServerWorld ? (ServerWorld)world : null).hasPermissionLevel(level);
    }
}
