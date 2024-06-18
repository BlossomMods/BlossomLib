package dev.codedsakura.blossom.lib.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.codedsakura.blossom.lib.BlossomGlobals;
import dev.codedsakura.blossom.lib.utils.CubicBezierCurve;
import dev.codedsakura.blossom.lib.utils.gson.CubicBezierCurveSerializer;
import dev.codedsakura.blossom.lib.utils.gson.DeprecatedExclusionStrategy;
import dev.codedsakura.blossom.lib.utils.gson.SerializeNullConverter;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.core.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

public class ConfigFileController {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(CubicBezierCurve.class, new CubicBezierCurveSerializer())
            .registerTypeAdapterFactory(new SerializeNullConverter())
            .addSerializationExclusionStrategy(new DeprecatedExclusionStrategy())
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static File getFile(String filename) {
        return FabricLoader.getInstance().getConfigDir().resolve("BlossomMods/" + filename).toFile();
    }

    public static <T extends BlossomConfig> @NotNull T load(ConfigManager.Config<T> configData) {
        Optional<Logger> optionalLogger = Optional.ofNullable(BlossomGlobals.LOGGER);
        optionalLogger
                .ifPresent(l -> l.debug("loading config {}", configData.filename()));

        var file = getFile(configData.filename());
        T config = null;

        if (file.exists()) {
            try (var reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(file)
                    )
            )) {
                config = GSON.fromJson(reader, configData.clazz());
            } catch (IOException e) {
                optionalLogger.ifPresentOrElse(
                        l -> l.error("Failed to load config {}", configData.filename(), e),
                        e::printStackTrace
                );
            }
        }

        if (config != null && !Objects.equals(config.version, config.getLatestVersion())) {
            if (config.update()) {
                config.version = config.getLatestVersion();
                ConfigFileController.save(config, configData.filename());
                return config;
            }

            var nowAsIso = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            ConfigFileController.save(config, "%s.bak-%s".formatted(configData.filename(), nowAsIso));
            config = null;
        }

        if (config == null) {
            try {
                config = configData.clazz().getDeclaredConstructor().newInstance();
                config.version = config.getLatestVersion();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                optionalLogger.ifPresentOrElse(
                        l -> l.error("Failed to load config class {}", configData.filename(), e),
                        e::printStackTrace
                );
                System.exit(1);
            }
        }

        ConfigFileController.save(config, configData.filename());
        return config;
    }

    public static <T> void save(T config, String filename) {
        Optional<Logger> optionalLogger = Optional.ofNullable(BlossomGlobals.LOGGER);
        optionalLogger.ifPresent(l -> l.debug("saving config {}", filename));

        File file = getFile(filename);
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdir()) {
                optionalLogger.ifPresentOrElse(
                        l -> l.error("Failed to create a directory for {}", filename),
                        () -> System.err.println("Failed to create a directory for " + file)
                );
            }
        }

        try (var writer = new OutputStreamWriter(
                new FileOutputStream(file)
        )) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            optionalLogger.ifPresentOrElse(
                    l -> l.error("Failed to save config {}", filename, e),
                    e::printStackTrace
            );
        }
    }
}
