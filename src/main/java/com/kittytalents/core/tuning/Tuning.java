package com.kittytalents.core.tuning;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kittytalents.KittyTalents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tunable numbers (4.0 Phase 4). Every talent and ability declares its numbers as {@link Key}s with the 3.0 value as
 * the default; a datapack overrides any of them, and the server sends the overrides to players so tooltips match.
 * <pre>
 * data/&lt;namespace&gt;/kittytalents/tuning/&lt;any name&gt;.json
 * { "talents":   { "hunter_cat": { "damage_per_rank": 4.0, "max_rank": 6 } },
 *   "abilities": { "frost_nova": { "cooldown_ticks": 60, "radius": 7 } } }
 * </pre>
 * Files are read in name order and merge per value (a later file wins); a pack replaces another pack's file by using the
 * same path. Unknown names are logged and ignored; values are clamped to
 * each key's allowed range. With no pack every value is its default, so behaviour matches 3.0 exactly.
 */
public final class Tuning {
    private Tuning() {}

    public static final String TALENTS = "talents";
    public static final String ABILITIES = "abilities";

    /** One tunable number. {@code get()} is a map read; safe on both sides. */
    public static final class Key {
        public final String scope, owner, name, label;
        public final double def, min, max;
        final String path;

        Key(String scope, String owner, String name, double def, double min, double max, String label) {
            this.scope = scope;
            this.owner = owner;
            this.name = name;
            this.def = def;
            this.min = min;
            this.max = max;
            this.label = label;
            this.path = path(scope, owner, name);
        }

        public double get() {
            Double v = overrides.get(path);
            return v != null ? v : def;
        }

        public float f() {
            return (float) get();
        }

        public int i() {
            return (int) Math.round(get());
        }

        public boolean isOverridden() {
            return overrides.containsKey(path);
        }

        /** "Damage per rank: 2.5" (lang key tuning.kittytalents.&lt;owner&gt;.&lt;name&gt;, English fallback). */
        public Component describe() {
            return Component.translatableWithFallback("tuning.kittytalents." + owner + "." + name, label)
                    .append(": " + format(get()));
        }
    }

    private static final Map<String, Key> KEYS = new LinkedHashMap<>();
    private static final Map<String, List<Key>> BY_OWNER = new HashMap<>();
    private static volatile Map<String, Double> overrides = Map.of();
    /** Every section a tuning file may have (talents, abilities, traits, growth, bond). */
    private static final java.util.Set<String> SCOPES = java.util.concurrent.ConcurrentHashMap.newKeySet();

    static String path(String scope, String owner, String name) {
        return scope + "/" + owner + "/" + name;
    }

    public static synchronized Key define(String scope, String owner, String name, double def, double min, double max, String label) {
        String path = path(scope, owner, name);
        Key existing = KEYS.get(path);
        if (existing != null) return existing;
        Key key = new Key(scope, owner, name, def, min, max, label);
        KEYS.put(path, key);
        SCOPES.add(scope);
        BY_OWNER.computeIfAbsent(scope + "/" + owner, k -> new ArrayList<>()).add(key);
        return key;
    }

    /** A talent number; allowed range 0 .. 10x the default (or 0..100 when the default is 0). */
    public static Key talent(String talent, String name, double def, String label) {
        return define(TALENTS, talent, name, def, 0, def == 0 ? 100 : Math.abs(def) * 10, label);
    }

    public static Key ability(String ability, String name, double def, String label) {
        return define(ABILITIES, ability, name, def, 0, def == 0 ? 100 : Math.abs(def) * 10, label);
    }

    public static List<Key> keysFor(String scope, String owner) {
        bootstrap();
        return Collections.unmodifiableList(BY_OWNER.getOrDefault(scope + "/" + owner, List.of()));
    }

    public static Collection<Key> all() {
        bootstrap();
        return Collections.unmodifiableCollection(KEYS.values());
    }

    /** Loads every class that declares keys, so lookups by name work before any cat has ticked. */
    public static void bootstrap() {
        com.kittytalents.talent.Talents.getAll();
        com.kittytalents.talent.TalentTuning.init();
        com.kittytalents.talent.effect.TalentEngine.get(null);
        com.kittytalents.ability.CatAbilities.getAll();
        com.kittytalents.core.living.CatTrait.init();
        com.kittytalents.core.living.GrowthStage.init();
        com.kittytalents.core.living.CatBond.init();
    }

    /** Tooltip lines for one talent/ability: every key except {@code skip}, with the live (synced) value. */
    public static List<Component> describeAll(String scope, String owner, java.util.Set<String> skip) {
        List<Component> out = new ArrayList<>();
        for (Key k : keysFor(scope, owner)) {
            if (skip.contains(k.name) && !k.isOverridden()) continue;
            out.add(k.describe());
        }
        return out;
    }

    public static String format(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e7) return Long.toString((long) v);
        String s = String.format(java.util.Locale.ROOT, "%.2f", v);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    // ------------------------------------------------------------------ overrides

    public static Map<String, Double> overrides() {
        return overrides;
    }

    public static void setOverrides(Map<String, Double> values) {
        overrides = Map.copyOf(values);
    }

    /** Parses tuning files (in pack order) into overrides. Public for GameTests. */
    public static Map<String, Double> parse(Map<ResourceLocation, JsonElement> files) {
        bootstrap();
        Map<String, Double> out = new LinkedHashMap<>();
        List<Map.Entry<ResourceLocation, JsonElement>> sorted = new ArrayList<>(files.entrySet());
        sorted.sort(Map.Entry.comparingByKey());
        for (Map.Entry<ResourceLocation, JsonElement> file : sorted) {
            try {
                JsonObject root = file.getValue().getAsJsonObject();
                for (String scope : root.keySet()) {
                    if (!SCOPES.contains(scope)) {
                        KittyTalents.LOGGER.warn("[Tuning] {}: unknown section {}", file.getKey(), scope);
                        continue;
                    }
                    for (Map.Entry<String, JsonElement> owner : root.getAsJsonObject(scope).entrySet()) {
                        for (Map.Entry<String, JsonElement> value : owner.getValue().getAsJsonObject().entrySet()) {
                            Key key = KEYS.get(path(scope, owner.getKey(), value.getKey()));
                            if (key == null) {
                                KittyTalents.LOGGER.warn("[Tuning] {}: unknown {} value {}.{}", file.getKey(), scope, owner.getKey(), value.getKey());
                                continue;
                            }
                            double v = value.getValue().getAsDouble();
                            if (!Double.isFinite(v)) continue;
                            double clamped = Math.max(key.min, Math.min(key.max, v));
                            if (clamped != v) {
                                KittyTalents.LOGGER.warn("[Tuning] {}: {} = {} is outside {}..{}; using {}", file.getKey(), key.path, v, key.min, key.max, clamped);
                            }
                            out.put(key.path, clamped);
                        }
                    }
                }
            } catch (Exception e) {
                KittyTalents.LOGGER.error("[Tuning] bad tuning file {}: {}", file.getKey(), e.toString());
            }
        }
        return out;
    }

    public static void write(FriendlyByteBuf buf, Map<String, Double> values) {
        buf.writeMap(values, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeDouble);
    }

    public static Map<String, Double> read(FriendlyByteBuf buf) {
        return buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readDouble);
    }

    /** The default of every key, as a tuning file (for /kittytalents tuning export). */
    public static JsonObject defaultsAsJson() {
        JsonObject root = new JsonObject();
        for (Key k : all()) {
            JsonObject scope = root.has(k.scope) ? root.getAsJsonObject(k.scope) : new JsonObject();
            root.add(k.scope, scope);
            JsonObject owner = scope.has(k.owner) ? scope.getAsJsonObject(k.owner) : new JsonObject();
            scope.add(k.owner, owner);
            owner.addProperty(k.name, k.def);
        }
        return root;
    }

    /** Server loader for data/&lt;ns&gt;/kittytalents/tuning/*.json. */
    public static final class Loader extends SimpleJsonResourceReloadListener {
        public Loader() {
            super(new Gson(), "kittytalents/tuning");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<String, Double> parsed = parse(files);
            setOverrides(parsed);
            KittyTalents.LOGGER.info("[Tuning] {} values changed by datapacks", parsed.size());
        }
    }
}
