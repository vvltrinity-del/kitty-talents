package com.kittytalents.skin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kittytalents.KittyTalents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cat skins as datapack data (4.0 Phase 3): one JSON per skin in {@code data/<namespace>/kittytalents/skins/}.
 * The server loads them and sends the list to every player (PacketSyncSkins), so a datapack adds or changes skins
 * with no code; the texture itself comes from a resource pack.
 * <pre>
 * { "name": "Tiger", "texture": "kittytalents:textures/entity/cat/skin_11.png", "order": 110,
 *   "breed_default_for": ["tiger"] }
 * </pre>
 * {@code name} is shown in the Style tab (a translation key works too); {@code order} sorts the list;
 * {@code breed_default_for} lists breeds drawn with this skin when a cat's skin is "default" (empty).
 */
public final class CatSkins {
    private CatSkins() {}

    public record Skin(ResourceLocation id, String name, ResourceLocation texture, int order, List<String> breeds) {
        public Component displayName() {
            return Component.translatableWithFallback(name, name);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(id);
            buf.writeUtf(name);
            buf.writeResourceLocation(texture);
            buf.writeVarInt(order);
            buf.writeCollection(breeds, FriendlyByteBuf::writeUtf);
        }

        static Skin read(FriendlyByteBuf buf) {
            return new Skin(buf.readResourceLocation(), buf.readUtf(), buf.readResourceLocation(), buf.readVarInt(),
                    buf.readList(FriendlyByteBuf::readUtf));
        }
    }

    /** The "default" skin: use the breed's own look (or tabby if the breed has none). */
    public static final String DEFAULT = "";
    public static final ResourceLocation FALLBACK_TEXTURE = new ResourceLocation("minecraft", "textures/entity/cat/tabby.png");

    private static volatile Map<ResourceLocation, Skin> byId = Map.of();
    private static volatile Map<String, ResourceLocation> breedTexture = Map.of();
    private static volatile List<Skin> ordered = List.of();

    /** All skins in display order. */
    public static List<Skin> all() {
        return ordered;
    }

    @Nullable
    public static Skin get(String id) {
        if (id == null || id.isEmpty()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : byId.get(rl);
    }

    /** True for "" (default) and for any loaded skin id. */
    public static boolean isValid(String id) {
        return DEFAULT.equals(id) || get(id) != null;
    }

    /** The texture for a cat with this skin id and breed. Unknown ids (a removed pack) fall back to the default look. */
    public static ResourceLocation texture(String skinId, String breedId) {
        Skin skin = get(skinId);
        if (skin != null) return skin.texture();
        ResourceLocation breed = breedTexture.get(breedId);
        return breed != null ? breed : FALLBACK_TEXTURE;
    }

    /** Replaces the registry (server reload, or the client receiving the server's list). */
    public static void set(List<Skin> skins) {
        List<Skin> sorted = new ArrayList<>(skins);
        sorted.sort(Comparator.comparingInt(Skin::order).thenComparing(s -> s.id().toString()));
        Map<ResourceLocation, Skin> ids = new LinkedHashMap<>();
        Map<String, ResourceLocation> breeds = new HashMap<>();
        for (Skin s : sorted) {
            ids.put(s.id(), s);
            for (String b : s.breeds()) breeds.put(b, s.texture());
        }
        byId = Map.copyOf(ids);
        breedTexture = Map.copyOf(breeds);
        ordered = List.copyOf(sorted);
    }

    public static void write(FriendlyByteBuf buf) {
        buf.writeCollection(ordered, (b, s) -> s.write(b));
    }

    public static List<Skin> read(FriendlyByteBuf buf) {
        return buf.readList(Skin::read);
    }

    /** Server-side loader for data/<ns>/kittytalents/skins/*.json. */
    public static final class Loader extends SimpleJsonResourceReloadListener {
        public Loader() {
            super(new com.google.gson.Gson(), "kittytalents/skins");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            List<Skin> skins = new ArrayList<>();
            for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
                try {
                    JsonObject json = GsonHelper.convertToJsonObject(e.getValue(), "skin");
                    List<String> breeds = new ArrayList<>();
                    if (json.has("breed_default_for")) {
                        GsonHelper.getAsJsonArray(json, "breed_default_for").forEach(b -> breeds.add(b.getAsString()));
                    }
                    skins.add(new Skin(e.getKey(), GsonHelper.getAsString(json, "name", e.getKey().getPath()),
                            new ResourceLocation(GsonHelper.getAsString(json, "texture")),
                            GsonHelper.getAsInt(json, "order", 1000), List.copyOf(breeds)));
                } catch (Exception ex) {
                    KittyTalents.LOGGER.error("[Skins] bad skin file {}: {}", e.getKey(), ex.toString());
                }
            }
            set(skins);
            KittyTalents.LOGGER.info("[Skins] loaded {} skins", skins.size());
        }
    }
}
