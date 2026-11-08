package com.kittytalents.client.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kittytalents.KittyTalents;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Data-driven cat skins: one JSON per skin in {@code assets/<namespace>/skins/}, so adding a skin (from this mod
 * or a resource pack) is adding a file, not editing the renderer.
 * <pre>
 * { "skin_id": 12, "texture": "kittytalents:textures/entity/cat/skin_12.png", "breed_default_for": ["snow_leopard"] }
 * </pre>
 * {@code skin_id} is the cosmetic skin a player picks (0 = "default": use the breed's skin if it has one).
 * {@code breed_default_for} lists {@link com.kittytalents.entity.CatVariant} ids drawn with this texture by default.
 * Lookups are plain map reads, rebuilt only on resource reload.
 */
public final class SkinRegistry implements ResourceManagerReloadListener {
    public static final SkinRegistry INSTANCE = new SkinRegistry();
    private static final String FOLDER = "skins";

    private volatile Map<Integer, ResourceLocation> bySkinId = Map.of();
    private volatile Map<String, ResourceLocation> byBreed = Map.of();

    private SkinRegistry() {}

    /** The texture a cat renders with. */
    public ResourceLocation resolve(TalentedCat cat) {
        int skinId = cat.getSkinId();
        if (skinId == 0) {
            ResourceLocation breedSkin = byBreed.get(cat.getCatVariant().getId());
            if (breedSkin != null) return breedSkin;
        }
        ResourceLocation tex = bySkinId.get(skinId);
        return tex != null ? tex : cat.getResourceLocation();
    }

    public int size() {
        return bySkinId.size();
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<Integer, ResourceLocation> skins = new HashMap<>();
        Map<String, ResourceLocation> breeds = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources(FOLDER, p -> p.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                ResourceLocation texture = new ResourceLocation(json.get("texture").getAsString());
                if (json.has("skin_id")) skins.put(json.get("skin_id").getAsInt(), texture);
                if (json.has("breed_default_for")) {
                    JsonArray list = json.getAsJsonArray("breed_default_for");
                    for (JsonElement breed : list) breeds.put(breed.getAsString(), texture);
                }
            } catch (Exception e) {
                KittyTalents.LOGGER.error("[SkinRegistry] bad skin file {}: {}", entry.getKey(), e.toString());
            }
        }
        this.bySkinId = Map.copyOf(skins);
        this.byBreed = Map.copyOf(breeds);
        KittyTalents.LOGGER.info("[SkinRegistry] loaded {} skins, {} breed defaults", skins.size(), breeds.size());
    }
}
