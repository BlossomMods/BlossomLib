package dev.codedsakura.blossom.lib.mod;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

import java.util.ArrayList;
import java.util.List;

public class ModController {
    private static final ArrayList<ModState> MODS = new ArrayList<>();

    public static <T> void register(BlossomMod<T> mod) {
        MODS.add(new ModState(mod));
    }

    public static void init(CommandDispatcher<ServerCommandSource> dispatcher) {
        MODS.forEach(state -> {
            if (!state.enabled) {
                state.mod.logger.warn("Loaded, but not enabled, skipping initialization");
                return;
            }
            try {
                state.mod.logger.info("Register commands");
                state.mod.getCommands().forEach(consumer -> consumer.accept(dispatcher));
                state.initialized = true;
            } catch (Exception e) {
                state.mod.logger.error("Failed to register commands", e);
                state.enabled = false;
            }
        });
    }

    public static List<String> getMods() {
        return MODS.stream()
                .filter(v -> v.enabled && v.initialized)
                .map(v -> v.name)
                .toList();
    }

    public static List<PublicModState> getAllModStates() {
        return MODS.stream()
                .map(v -> new PublicModState(v.name, v.initialized, v.enabled))
                .toList();
    }

    public static boolean isEnabled(String modName) {
        return MODS.stream().anyMatch(v -> v.name.equals(modName));
    }


    public record PublicModState(String name, boolean initialized, boolean enabled) {
    }

    static private class ModState {
        String name;
        boolean initialized;
        boolean enabled;
        BlossomMod<?> mod;

        ModState(BlossomMod<?> mod) {
            this.name = mod.getName();
            this.initialized = false;
            this.enabled = true;
            this.mod = mod;
        }
    }
}
