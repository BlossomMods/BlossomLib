package dev.codedsakura.blossom.lib.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.codedsakura.blossom.lib.BlossomGlobals;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.*;

public abstract class DataController<T> {
    protected static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    protected T data = null;
    private MinecraftServer server;

    public DataController() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            this.server = server;
            data = read(server);
            if (data == null) {
                data = defaultData();
            }
            write(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(this::write);
    }

    public abstract T defaultData();

    public abstract String getFilename();

    protected Class<T> getType() {
        //noinspection unchecked
        return (Class<T>) defaultData().getClass();
    }

    protected T readJson(InputStreamReader reader) {
        return GSON.fromJson(new BufferedReader(reader), getType());
    }

    protected void writeJson(OutputStreamWriter writer) {
        GSON.toJson(data, writer);
    }

    private File getFile(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT)
                .resolve("data")
                .resolve(getFilename() + ".json")
                .toFile();
    }

    private InputStreamReader getReader(MinecraftServer server) throws FileNotFoundException {
        return new InputStreamReader(new FileInputStream(this.getFile(server)));
    }

    private OutputStreamWriter getWriter(MinecraftServer server) throws FileNotFoundException {
        return new OutputStreamWriter(new FileOutputStream(this.getFile(server)));
    }

    public void write() {
        write(server);
    }

    public void write(MinecraftServer server) {
        BlossomGlobals.LOGGER.trace("writing");
        try {
            OutputStreamWriter writer = getWriter(server);
            writeJson(writer);
            writer.close();
        } catch (Exception e) {
            BlossomGlobals.LOGGER.throwing(e);
        }
    }

    public T read() {
        return read(server);
    }

    public T read(MinecraftServer server) {
        BlossomGlobals.LOGGER.trace("reading");
        try {
            return readJson(getReader(server));
        } catch (Exception e) {
            BlossomGlobals.LOGGER.info(e.getMessage());
            return null;
        }
    }
}
