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

/** The datapack skin JSONs read by skin/CatSkins: every 3.0 skin exists, names are translated, mod textures exist. */
public class SkinFilesTest {
    private static final File SKINS = new File("src/main/resources/data/kittytalents/kittytalents/skins");
    private static final File ASSETS = new File("src/main/resources/assets/kittytalents");

    @Test
    public void skinFilesAreCompleteAndPointAtRealTextures() throws Exception {
        Set<String> ids = new HashSet<>();
        Set<Integer> orders = new HashSet<>();
        JsonObject lang;
        try (Reader r = new FileReader(new File(ASSETS, "lang/en_us.json"))) { lang = JsonParser.parseReader(r).getAsJsonObject(); }
        Set<String> breeds = new HashSet<>();
        for (File f : Objects.requireNonNull(SKINS.listFiles((d, n) -> n.endsWith(".json")))) {
            try (Reader r = new FileReader(f)) {
                JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
                ids.add("kittytalents:" + f.getName().replace(".json", ""));
                assertTrue(orders.add(json.get("order").getAsInt()), "duplicate order in " + f.getName());
                assertTrue(lang.has(json.get("name").getAsString()), f.getName() + " name has no en_us entry");
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
        for (String legacy : com.kittytalents.core.persist.CatSchema.LEGACY_SKINS) {
            assertTrue(legacy.isEmpty() || ids.contains(legacy), "no skin file for legacy skin " + legacy);
        }
        assertTrue(ids.contains("kittytalents:tabby"), "tabby skin missing");
        assertEquals(Set.of("tiger", "snow_leopard", "void_ender", "golden_maneki", "ghost_phantom", "panther"), breeds);
    }
}
