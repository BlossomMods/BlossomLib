package dev.codedsakura.blossom.lib.permissions;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
    public static Predicate<CommandSourceStack> require(@NotNull String permission, boolean defaultValue) {
        if (isFPAPILoaded()) {
            return PermissionsExecutor.require(permission, defaultValue);
        }
        return _ignored -> defaultValue;
    }

    /**
     * @see me.lucko.fabric.api.permissions.v0.Permissions#require(String, int)
     */
    public static Predicate<CommandSourceStack> require(@NotNull String permission, int level) {
        if (isFPAPILoaded()) {
            return PermissionsExecutor.require(permission, level);
        }
        return player -> player.permissions()
                .hasPermission(
                        new Permission.HasCommandLevel(PermissionLevel.byId(level))
                );
    }

    /**
     * @see me.lucko.fabric.api.permissions.v0.Permissions#check(Entity, String, boolean)
     */
    public static boolean check(@NotNull CommandSourceStack source, @NotNull String permission, boolean fallback) {
        if (isFPAPILoaded()) {
            return PermissionsExecutor.check(source, permission, fallback);
        }
        return fallback;
    }

    /**
     * @see me.lucko.fabric.api.permissions.v0.Permissions#check(Entity, String, int)
     */
    public static boolean check(@NotNull CommandSourceStack source, @NotNull String permission, int level) {
        if (isFPAPILoaded()) {
            return PermissionsExecutor.check(source, permission, level);
        }
        return source.permissions()
                .hasPermission(
                        new Permission.HasCommandLevel(PermissionLevel.byId(level))
                );
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
        Level world = entity.level();
        return entity.createCommandSourceStackForNameResolution(world instanceof ServerLevel ? (ServerLevel)world : null)
                .permissions()
                .hasPermission(
                        new Permission.HasCommandLevel(PermissionLevel.byId(level))
                );
    }
}
