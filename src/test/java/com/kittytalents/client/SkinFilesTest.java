package com.kittytalents.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The skin JSONs read by client/skin/SkinRegistry: valid, unique ids 0..16, and every mod texture exists. */
public class SkinFilesTest {
    private static final File SKINS = new File("src/main/resources/assets/kittytalents/skins");
    private static final File ASSETS = new File("src/main/resources/assets/kittytalents");

    @Test
    public void skinFilesAreCompleteAndPointAtRealTextures() throws Exception {
        Set<Integer> ids = new HashSet<>();
        Set<String> breeds = new HashSet<>();
        for (File f : Objects.requireNonNull(SKINS.listFiles((d, n) -> n.endsWith(".json")))) {
            try (Reader r = new FileReader(f)) {
                JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
                assertTrue(ids.add(json.get("skin_id").getAsInt()), "duplicate skin_id in " + f.getName());
                String tex = json.get("texture").getAsString();
                if (tex.startsWith("kittytalents:")) {
                    File png = new File(ASSETS, tex.substring("kittytalents:".length()));
                    assertTrue(png.isFile(), f.getName() + " points at missing " + png);
                }
                if (json.has("breed_default_for")) {
                    json.getAsJsonArray("breed_default_for").forEach(b -> assertTrue(breeds.add(b.getAsString()), "breed listed twice: " + b));
                }
            }
        }
        for (int i = 0; i <= 16; i++) assertTrue(ids.contains(i), "no skin file for skin_id " + i);
        assertEquals(Set.of("tiger", "snow_leopard", "void_ender", "golden_maneki", "ghost_phantom", "panther"), breeds);
    }
}
