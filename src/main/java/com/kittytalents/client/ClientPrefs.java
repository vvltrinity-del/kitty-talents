package com.kittytalents.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kittytalents.KittyTalents;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Small per-player UI choices that are not worth a config screen (4.0 Phase 7): the whistle's scope and pinned
 * commands. Saved as config/kittytalents-client-prefs.json; a missing or broken file just means defaults.
 */
public final class ClientPrefs {
    private ClientPrefs() {}

    public static final int MAX_FAVOURITES = 6;

    private static final class Data {
        boolean whistleMineOnly = false;
        List<String> whistleFavourites = new ArrayList<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data data;

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("kittytalents-client-prefs.json");
    }

    private static Data data() {
        if (data == null) {
            try {
                data = Files.exists(file()) ? GSON.fromJson(Files.readString(file()), Data.class) : new Data();
            } catch (Exception e) {
                KittyTalents.LOGGER.warn("[Prefs] could not read {}: {}", file(), e.toString());
            }
            if (data == null) data = new Data();
            if (data.whistleFavourites == null) data.whistleFavourites = new ArrayList<>();
        }
        return data;
    }

    private static void save() {
        try {
            Files.writeString(file(), GSON.toJson(data()));
        } catch (Exception e) {
            KittyTalents.LOGGER.warn("[Prefs] could not write {}: {}", file(), e.toString());
        }
    }

    /** Whistle only your own cats (true), or also cats you co-own (false, the default). */
    public static boolean whistleMineOnly() {
        return data().whistleMineOnly;
    }

    public static void setWhistleMineOnly(boolean mineOnly) {
        data().whistleMineOnly = mineOnly;
        save();
    }

    /** Pinned whistle commands (enum names), in the order they were pinned. */
    public static List<String> whistleFavourites() {
        return List.copyOf(data().whistleFavourites);
    }

    /** Pins or unpins a command. @return false if the row is full. */
    public static boolean toggleWhistleFavourite(String command) {
        List<String> favs = data().whistleFavourites;
        if (!favs.remove(command)) {
            if (favs.size() >= MAX_FAVOURITES) return false;
            favs.add(command);
        }
        save();
        return true;
    }
}
