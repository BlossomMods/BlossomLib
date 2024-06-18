package dev.codedsakura.blossom.lib.config;

import dev.codedsakura.blossom.lib.BlossomGlobals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConfigManager {
    private static final ArrayList<Config<? extends BlossomConfig>> configs = new ArrayList<>();

    public static <T extends BlossomConfig> void register(Class<T> clazz, String filename, Consumer<T> apply) {
        Optional.ofNullable(BlossomGlobals.LOGGER)
                .ifPresent(l -> l.trace("register config manager {}", filename));

        Config<T> config = new Config<>(clazz, apply, filename);
        configs.add(config);
        apply.accept(ConfigFileController.load(config));
    }

    public static void unregister(Class<?> clazz) {
        configs.removeIf(conf -> conf.clazz().equals(clazz));
    }

    public static void refresh(Class<?> clazz) {
        configs.stream()
                .filter(conf -> conf.clazz().equals(clazz))
                .forEach(Config::refresh);
    }

    public static void refreshAll() {
        configs.forEach(Config::refresh);
    }

    public static List<Class<?>> getAllRegistered() {
        return configs.stream()
                .map(Config::clazz)
                .collect(Collectors.toList());
    }

    public record Config<T extends BlossomConfig>(Class<T> clazz, Consumer<T> apply, String filename) {
        void refresh() {
            apply.accept(ConfigFileController.load(this));
        }
    }
}
