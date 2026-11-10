package com.kittytalents.core.persist;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Versioned save format for {@link com.kittytalents.entity.TalentedCat}.
 *
 * <p>Every saved cat carries {@value #KEY_SCHEMA}. On load, {@link #migrate} upgrades older tags in place,
 * one version step at a time, before any field is read. Cats are never silently reset: unknown or missing
 * values fall back to defaults only for the specific field affected.</p>
 *
 * <h2>Versions</h2>
 * <ul>
 *   <li><b>0</b> (no {@value #KEY_SCHEMA}): 1.0, 2.0 and partial-3.0 saves. CatMode stored as an enum ordinal,
 *       ability cooldowns / Spirit Catnip flag / combat scratch state kept in Forge persistent data.</li>
 *   <li><b>1</b>: CatMode stored by name ({@value #KEY_MODE_NAME}); ability cooldowns in
 *       {@value #KEY_ABILITY_COOLDOWNS}; Spirit Catnip flag in {@value #KEY_SPIRIT_PRIMED}; transient
 *       combat keys removed from persistent data.</li>
 *   <li><b>2</b>: Pounce is every cat's built-in move, no longer an equippable ability: a saved "pounce" in an
 *       ability slot is cleared so the slot falls back to its default. Mode names may be legacy (mapped by
 *       CatMode#fromSavedName after the 21 -> 12 mode merge).</li>
 *   <li><b>3</b> (4.0): the skin is saved by name ({@value #KEY_SKIN}, e.g. "kittytalents:tiger"; empty = the breed's
 *       default) instead of the number {@value #KEY_LEGACY_SKIN_ID}, so packs can add skins.</li>
 * </ul>
 *
 * <p>This class is pure NBT logic (no world / entity access) so it can be unit-tested without a Minecraft bootstrap.</p>
 */
public final class CatSchema {

    public static final int CURRENT = 3;

    public static final String KEY_SCHEMA = "KTSchema";
    public static final String KEY_MODE_NAME = "CatModeName";
    public static final String KEY_LEGACY_MODE_ORDINAL = "CatMode";
    public static final String KEY_ABILITY_COOLDOWNS = "AbilityCooldowns";
    public static final String KEY_SPIRIT_PRIMED = "SpiritCatnipPrimed";
    public static final String KEY_MUTATION = "MutationType";
    public static final String KEY_SKIN = "Skin";
    public static final String KEY_LEGACY_SKIN_ID = "SkinId";

    /** The 3.0 skin numbers in order. FROZEN: decodes old saves. 0 meant "the breed's default look". */
    public static final List<String> LEGACY_SKINS = List.of(
            "", "kittytalents:black", "kittytalents:red", "kittytalents:siamese", "kittytalents:british_shorthair",
            "kittytalents:calico", "kittytalents:persian", "kittytalents:ragdoll", "kittytalents:white", "kittytalents:jellie",
            "kittytalents:all_black", "kittytalents:tiger", "kittytalents:snow_leopard", "kittytalents:void_ender",
            "kittytalents:golden_maneki", "kittytalents:ghost_phantom", "kittytalents:panther");

    /** A 3.0 skin number as a skin name ("" = breed default for 0 or anything unknown). */
    public static String legacySkin(int id) {
        return id > 0 && id < LEGACY_SKINS.size() ? LEGACY_SKINS.get(id) : "";
    }

    /**
     * The CatMode enum order as it was in every pre-schema save. FROZEN: never edit, it decodes old ordinals.
     */
    public static final List<String> LEGACY_MODE_ORDER = List.of(
            "DOCILE", "FOLLOW", "STAY", "WANDER", "HUNTING", "GUARD", "AGGRESSIVE", "BERSERKER", "PATROL",
            "TACTICAL", "DEFENSIVE", "ESCORT", "FETCH", "MINE", "FARM", "FISH", "SUPPORT", "PROTECT",
            "RETURN_HOME", "RETREAT", "WORK");

    /** Persistent-data keys that were only ever transient scratch state and must not live on disk. */
    static final List<String> TRANSIENT_PERSISTENT_KEYS = List.of(
            "kt_ai_initialized", "KT_LastRmbPounce", "KT_DodgeEnd", "KT_PerfectDodgeEnd", "KT_ComboStep",
            "KT_ComboExpiry", "KT_LastAttackTime", "KT_PounceActive", "KT_PounceStart");

    private static final String LEGACY_COOLDOWN_PREFIX = "cooldown_";

    private CatSchema() {}

    public static int versionOf(CompoundTag root) {
        return root.contains(KEY_SCHEMA) ? root.getInt(KEY_SCHEMA) : 0;
    }

    /**
     * Upgrades {@code root} (the entity save tag) and {@code forgeData} (the entity's Forge persistent data,
     * already loaded from {@code root.ForgeData}) to {@link #CURRENT}. Idempotent for current-version tags.
     *
     * @return the version the tag was at before migration
     */
    public static int migrate(CompoundTag root, CompoundTag forgeData) {
        int from = versionOf(root);
        if (from < 1) migrate0to1(root, forgeData);
        if (from < 2) migrate1to2(root);
        if (from < 3) migrate2to3(root);
        root.putInt(KEY_SCHEMA, CURRENT);
        return from;
    }

    static final List<String> ABILITY_SLOT_KEYS = List.of("AbilitySlot1", "AbilitySlot2", "AbilitySlot3", "AbilityUltimate");

    private static void migrate2to3(CompoundTag root) {
        if (!root.contains(KEY_SKIN) && root.contains(KEY_LEGACY_SKIN_ID)) {
            root.putString(KEY_SKIN, legacySkin(root.getInt(KEY_LEGACY_SKIN_ID)));
        }
        root.remove(KEY_LEGACY_SKIN_ID);
    }

    private static void migrate1to2(CompoundTag root) {
        for (String key : ABILITY_SLOT_KEYS) {
            if ("pounce".equalsIgnoreCase(root.getString(key))) root.remove(key);
        }
    }

    private static void migrate0to1(CompoundTag root, CompoundTag forgeData) {
        // 1. CatMode ordinal -> name (prerequisite for the planned mode merge, which reorders the enum).
        if (!root.contains(KEY_MODE_NAME) && root.contains(KEY_LEGACY_MODE_ORDINAL)) {
            int ordinal = root.getInt(KEY_LEGACY_MODE_ORDINAL);
            String name = ordinal >= 0 && ordinal < LEGACY_MODE_ORDER.size() ? LEGACY_MODE_ORDER.get(ordinal) : "DOCILE";
            root.putString(KEY_MODE_NAME, name);
        }

        // 2. Ability cooldowns: ForgeData "cooldown_<id>" longs -> root AbilityCooldowns compound.
        CompoundTag cooldowns = root.getCompound(KEY_ABILITY_COOLDOWNS);
        List<String> moved = new ArrayList<>();
        for (String key : forgeData.getAllKeys()) {
            if (key.startsWith(LEGACY_COOLDOWN_PREFIX)) {
                cooldowns.putLong(key.substring(LEGACY_COOLDOWN_PREFIX.length()), forgeData.getLong(key));
                moved.add(key);
            }
        }
        moved.forEach(forgeData::remove);
        if (!cooldowns.isEmpty()) root.put(KEY_ABILITY_COOLDOWNS, cooldowns);

        // 3. Spirit Catnip flag (fed but not yet bred) -> root field.
        if (forgeData.getBoolean("SpiritCatnipUsed")) root.putBoolean(KEY_SPIRIT_PRIMED, true);
        forgeData.remove("SpiritCatnipUsed");

        // 4. Mutation was double-written to ForgeData; root is authoritative, ForgeData only fills a gap.
        if (!root.contains(KEY_MUTATION) && forgeData.contains(KEY_MUTATION)) {
            root.putString(KEY_MUTATION, forgeData.getString(KEY_MUTATION));
        }
        forgeData.remove(KEY_MUTATION);

        // 5. Write-only counter that never fed into progression.
        forgeData.remove("KT_CombatXP");

        // 6. Transient scratch state that should never have been persisted.
        TRANSIENT_PERSISTENT_KEYS.forEach(forgeData::remove);
    }
}
