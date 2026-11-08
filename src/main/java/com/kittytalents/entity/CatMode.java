package com.kittytalents.entity;

import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Companion modes (Phase 3: 21 legacy modes merged into 12, each with distinct behavior).
 *
 * <p>Each mode declares its behavior as data; goals ask the mode instead of hard-coding mode lists:
 * <ul>
 *   <li>{@link #followsOwner()}: tethers to the owner (CatFollowGoal).</li>
 *   <li>{@link #anchorRadius()}: holds an anchor set when the mode is chosen (CatHoldAnchorGoal); -1 = none.</li>
 *   <li>{@link #targetPolicy()}: what the cat may pick as a target (CatTargeting).</li>
 *   <li>{@link #retreatsWhenHurt()}: falls back to the owner at low health (CatRetreatGoal).</li>
 * </ul>
 *
 * <p>Saved by name. Legacy names are mapped by {@link #fromSavedName}. NEVER reorder for save reasons:
 * ordinals are only used for network packets, where both sides run the same code.
 */
public enum CatMode {
    /** Sits. Never fights. */
    DOCILE("mode.kittytalents.docile", false, -1, TargetPolicy.NONE, false),
    /** Holds its spot; defends itself and its owner if attacked there. */
    STAY("mode.kittytalents.stay", false, 2, TargetPolicy.SELF_DEFENSE, true),
    /** Follows the owner; defends owner, trusted players and itself. */
    FOLLOW("mode.kittytalents.follow", true, -1, TargetPolicy.DEFEND_OWNER, true),
    /** Roams freely; fights only back. */
    WANDER("mode.kittytalents.wander", false, -1, TargetPolicy.SELF_DEFENSE, true),
    /** Patrols around its post and attacks monsters that come within range of it. */
    GUARD("mode.kittytalents.guard", false, 10, TargetPolicy.AREA_DEFENSE, true),
    /** Stalks and pounces on prey and pests, plus hostiles nearby. */
    HUNTING("mode.kittytalents.hunt", false, -1, TargetPolicy.HUNT, true),
    /** Seeks out every monster in a wide radius. Never retreats. */
    AGGRESSIVE("mode.kittytalents.aggressive", false, -1, TargetPolicy.SEEK_HOSTILES, false),
    /** Follows at a flanking distance and fights at range (guns, ranged abilities). */
    TACTICAL("mode.kittytalents.tactical", true, -1, TargetPolicy.DEFEND_OWNER, true),
    /** Follows closely, prioritizes healing/buff abilities and avoids melee. */
    SUPPORT("mode.kittytalents.support", true, -1, TargetPolicy.SELF_DEFENSE, true),
    /** Plays with thrown toys and retrieves items. */
    FETCH("mode.kittytalents.fetch", false, -1, TargetPolicy.SELF_DEFENSE, true),
    /** Performs its assigned job (Farmer, Miner, ...). */
    WORK("mode.kittytalents.work", false, -1, TargetPolicy.SELF_DEFENSE, true),
    /** Goes to its bed and rests there. Never fights. */
    RETURN_HOME("mode.kittytalents.return_home", false, -1, TargetPolicy.NONE, false);

    /** What a mode allows the cat to target. */
    public enum TargetPolicy {
        /** Never targets anything. */
        NONE,
        /** Only things attacking the cat (or its owner while adjacent). */
        SELF_DEFENSE,
        /** Things threatening the owner, trusted players or the cat. */
        DEFEND_OWNER,
        /** DEFEND_OWNER plus hostiles within the anchor radius. */
        AREA_DEFENSE,
        /** DEFEND_OWNER plus prey, pests and nearby hostiles. */
        HUNT,
        /** DEFEND_OWNER plus every hostile within a wide radius. */
        SEEK_HOSTILES
    }

    /**
     * Legacy mode names (pre-Phase-3 saves and the frozen CatSchema ordinal table) -> current mode.
     * MINE/FARM/FISH also get a job assigned on load (see TalentedCat#readAdditionalSaveData).
     */
    private static final Map<String, CatMode> LEGACY = Map.of(
            "ESCORT", FOLLOW, "PROTECT", FOLLOW, "DEFENSIVE", FOLLOW, "RETREAT", FOLLOW,
            "PATROL", GUARD, "BERSERKER", AGGRESSIVE,
            "MINE", WORK, "FARM", WORK, "FISH", WORK);

    private final String key;
    private final boolean followsOwner;
    private final int anchorRadius;
    private final TargetPolicy targetPolicy;
    private final boolean retreatsWhenHurt;

    CatMode(String key, boolean followsOwner, int anchorRadius, TargetPolicy targetPolicy, boolean retreatsWhenHurt) {
        this.key = key;
        this.followsOwner = followsOwner;
        this.anchorRadius = anchorRadius;
        this.targetPolicy = targetPolicy;
        this.retreatsWhenHurt = retreatsWhenHurt;
    }

    public Component getDisplayName() { return Component.translatable(key); }
    public String getKey() { return key; }

    public boolean followsOwner() { return followsOwner; }
    /** Radius in blocks around the anchor the cat stays within, or -1 if the mode has no anchor. */
    public int anchorRadius() { return anchorRadius; }
    public boolean usesAnchor() { return anchorRadius >= 0; }
    public TargetPolicy targetPolicy() { return targetPolicy; }
    public boolean retreatsWhenHurt() { return retreatsWhenHurt; }

    /** May fight at all (including self-defense). */
    public boolean canAttack() { return targetPolicy != TargetPolicy.NONE; }

    /** Proactively engages targets (not just self-defense). Used for throttling and idle behaviors. */
    public boolean isCombatMode() {
        return targetPolicy == TargetPolicy.AREA_DEFENSE || targetPolicy == TargetPolicy.HUNT
                || targetPolicy == TargetPolicy.SEEK_HOSTILES;
    }

    public boolean isStationary() { return this == DOCILE || this == STAY; }
    public boolean isWorkMode() { return this == WORK; }

    // ---- Compatibility aliases for older call sites ----
    /** @deprecated use {@link #followsOwner()} */
    @Deprecated public boolean shouldFollowOwner() { return followsOwner; }

    public CatMode next() {
        CatMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public CatMode previous() {
        CatMode[] values = values();
        return values[(this.ordinal() - 1 + values.length) % values.length];
    }

    /** Network decode. Out-of-range ids fall back to DOCILE. */
    public static CatMode byId(int id) {
        CatMode[] values = values();
        return id < 0 || id >= values.length ? DOCILE : values[id];
    }

    /** Save / command decode: current names, then legacy names; unknown -> FOLLOW (safest active default). */
    public static CatMode fromSavedName(String name) {
        if (name == null || name.isEmpty()) return FOLLOW;
        String upper = name.toUpperCase(Locale.ROOT);
        for (CatMode mode : values()) {
            if (mode.name().equals(upper)) return mode;
        }
        return LEGACY.getOrDefault(upper, FOLLOW);
    }

    /** @deprecated use {@link #fromSavedName}; kept for existing callers. */
    @Deprecated
    public static CatMode byName(String name) { return fromSavedName(name); }

    /** True if {@code name} is a pre-Phase-3 mode that no longer exists. */
    public static boolean isLegacyName(String name) {
        return name != null && LEGACY.containsKey(name.toUpperCase(Locale.ROOT));
    }
}
