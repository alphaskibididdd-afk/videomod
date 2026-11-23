package de.videomod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Position, Größe und letzte URL des Videofensters (wird gespeichert). */
public class VideoConfig {
    public int x = 20, y = 20, w = 320, h = 180;
    public String url = "";
    public boolean visible = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("videomod.json");
    public static VideoConfig I = load();

    private static VideoConfig load() {
        try {
            if (Files.exists(FILE)) return GSON.fromJson(Files.readString(FILE), VideoConfig.class);
        } catch (Exception ignored) {}
        return new VideoConfig();
    }

    public static void save() {
        try { Files.writeString(FILE, GSON.toJson(I)); } catch (Exception ignored) {}
    }
}
