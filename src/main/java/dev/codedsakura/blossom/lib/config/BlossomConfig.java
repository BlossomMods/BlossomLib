package dev.codedsakura.blossom.lib.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.codedsakura.blossom.lib.BlossomGlobals;
import dev.codedsakura.blossom.lib.utils.CubicBezierCurve;
import dev.codedsakura.blossom.lib.utils.gson.CubicBezierCurveSerializer;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class BlossomConfig {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(CubicBezierCurve.class, new CubicBezierCurveSerializer())
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private static File getFile(String filename) {
        return FabricLoader.getInstance().getConfigDir().resolve("BlossomMods/" + filename).toFile();
    }

    public static <T> @NotNull T load(Class<T> clazz, String filename) {
        Optional.ofNullable(BlossomGlobals.LOGGER)
                .ifPresent(l -> l.trace("loading config {}", filename));

        var file = getFile(filename);
        T config = null;

        if (file.exists()) {
            try (var reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(file)
                    )
            )) {
                config = GSON.fromJson(reader, clazz);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (config == null) {
            try {
                config = clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        BlossomConfig.save(config, filename);
        return config;
    }

    public static <T> void save(T config, String filename) {
        Optional.ofNullable(BlossomGlobals.LOGGER)
                .ifPresent(l -> l.trace("saving config {}", filename));

        File file = getFile(filename);
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdir()) {
                System.err.println("Failed to create a directory for " + file);
            }
        }

        try (var writer = new OutputStreamWriter(
                new FileOutputStream(file)
        )) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
