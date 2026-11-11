package com.kittytalents.entity;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
 * <p>Saved by name. Legacy names are mapped by {@link #fromSavedName}.
 *
 * <p>4.0 Phase 8: this is a registry, not an enum. The 12 built-in modes are the constants below; addons add more
 * with {@code KittyTalentsAPI.registerMode(CatMode.builder("mymod_scout")...build())}. {@link #values()} lists the
 * built-in modes in their fixed order, then addon modes sorted by name, so {@link #ordinal()} is the same on client
 * and server as long as both have the same addons. Ordinals are only used in packets and synced data, never in saves.
 */
public final class CatMode {
    private static final List<CatMode> REGISTERED = new ArrayList<>();
    private static volatile CatMode[] snapshot = new CatMode[0];

    /** Sits. Never fights. */
    public static final CatMode DOCILE = builtin("DOCILE", "mode.kittytalents.docile", false, -1, TargetPolicy.NONE, false);
    /** Holds its spot; defends itself and its owner if attacked there. */
    public static final CatMode STAY = builtin("STAY", "mode.kittytalents.stay", false, 2, TargetPolicy.SELF_DEFENSE, true);
    /** Follows the owner; defends owner, trusted players and itself. */
    public static final CatMode FOLLOW = builtin("FOLLOW", "mode.kittytalents.follow", true, -1, TargetPolicy.DEFEND_OWNER, true);
    /** Roams freely; fights only back. */
    public static final CatMode WANDER = builtin("WANDER", "mode.kittytalents.wander", false, -1, TargetPolicy.SELF_DEFENSE, true);
    /** Patrols around its post and attacks monsters that come within range of it. */
    public static final CatMode GUARD = builtin("GUARD", "mode.kittytalents.guard", false, 10, TargetPolicy.AREA_DEFENSE, true);
    /** Stalks and pounces on prey and pests, plus hostiles nearby. */
    public static final CatMode HUNTING = builtin("HUNTING", "mode.kittytalents.hunt", false, -1, TargetPolicy.HUNT, true);
    /** Seeks out every monster in a wide radius. Never retreats. */
    public static final CatMode AGGRESSIVE = builtin("AGGRESSIVE", "mode.kittytalents.aggressive", false, -1, TargetPolicy.SEEK_HOSTILES, false);
    /** Follows at a flanking distance and fights at range (guns, ranged abilities). */
    public static final CatMode TACTICAL = builtin("TACTICAL", "mode.kittytalents.tactical", true, -1, TargetPolicy.DEFEND_OWNER, true);
    /** Follows closely, prioritizes healing/buff abilities and avoids melee. */
    public static final CatMode SUPPORT = builtin("SUPPORT", "mode.kittytalents.support", true, -1, TargetPolicy.SELF_DEFENSE, true);
    /** Plays with thrown toys and retrieves items. */
    public static final CatMode FETCH = builtin("FETCH", "mode.kittytalents.fetch", false, -1, TargetPolicy.SELF_DEFENSE, true);
    /** Performs its assigned job (Farmer, Miner, ...). */
    public static final CatMode WORK = builtin("WORK", "mode.kittytalents.work", false, -1, TargetPolicy.SELF_DEFENSE, true);
    /** Goes to its bed and rests there. Never fights. */
    public static final CatMode RETURN_HOME = builtin("RETURN_HOME", "mode.kittytalents.return_home", false, -1, TargetPolicy.NONE, false);

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

    private final String name;
    private final String key;
    private final boolean builtin;
    private final boolean followsOwner;
    private final int anchorRadius;
    private final TargetPolicy targetPolicy;
    private final boolean retreatsWhenHurt;
    private final boolean stationary;
    private final double followStart;
    private final double followStop;

    private CatMode(String name, String key, boolean builtin, boolean followsOwner, int anchorRadius, TargetPolicy targetPolicy,
                    boolean retreatsWhenHurt, boolean stationary, double followStart, double followStop) {
        this.name = name;
        this.key = key;
        this.builtin = builtin;
        this.followsOwner = followsOwner;
        this.anchorRadius = anchorRadius;
        this.targetPolicy = targetPolicy;
        this.retreatsWhenHurt = retreatsWhenHurt;
        this.stationary = stationary;
        this.followStart = followStart;
        this.followStop = followStop;
    }

    private static CatMode builtin(String name, String key, boolean followsOwner, int anchorRadius, TargetPolicy targetPolicy,
                                   boolean retreatsWhenHurt) {
        boolean stationary = name.equals("DOCILE") || name.equals("STAY");
        double start = name.equals("SUPPORT") ? 4.0D : name.equals("TACTICAL") ? 7.0D : 6.0D;
        double stop = name.equals("SUPPORT") ? 2.5D : name.equals("TACTICAL") ? 5.0D : 3.0D;
        return register(new CatMode(name, key, true, followsOwner, anchorRadius, targetPolicy, retreatsWhenHurt, stationary, start, stop));
    }

    /**
     * Adds a mode to the registry. Addons go through {@code KittyTalentsAPI.registerMode}.
     *
     * @throws IllegalArgumentException if a mode with this name (or a legacy name) already exists
     */
    public static synchronized CatMode register(CatMode mode) {
        for (CatMode m : REGISTERED) {
            if (m.name.equals(mode.name)) throw new IllegalArgumentException("Kitty Talents: a mode named '" + mode.name + "' already exists");
        }
        if (LegacyNames.MAP.containsKey(mode.name)) {
            throw new IllegalArgumentException("Kitty Talents: '" + mode.name + "' is a legacy mode name");
        }
        REGISTERED.add(mode);
        List<CatMode> sorted = new ArrayList<>(REGISTERED);
        // Built-ins keep their declaration order (stable sort); addon modes follow, by name.
        sorted.sort(Comparator.<CatMode, Boolean>comparing(m -> !m.builtin).thenComparing(m -> m.builtin ? "" : m.name));
        snapshot = sorted.toArray(new CatMode[0]);
        return mode;
    }

    /** Every registered mode: built-ins in their fixed order, then addon modes by name. */
    public static CatMode[] values() { return snapshot.clone(); }

    /** Position in {@link #values()}. Packets and synced data only; saves use {@link #name()}. */
    public int ordinal() {
        CatMode[] all = snapshot;
        for (int i = 0; i < all.length; i++) if (all[i] == this) return i;
        return 0;
    }

    /** The save name, upper case ("FOLLOW", "MYMOD_SCOUT"). */
    public String name() { return name; }
    public boolean isBuiltin() { return builtin; }
    @Override public String toString() { return name; }

    /** Starts an addon mode. {@code id} is your mod id plus a name ("mymod_scout"); the save name is its upper case. */
    public static Builder builder(String id) { return new Builder(id); }

    /** Describes an addon mode. Defaults: does not follow, no anchor, self-defence only, retreats when hurt. */
    public static final class Builder {
        private final String name;
        private String key;
        private boolean followsOwner, stationary;
        private int anchorRadius = -1;
        private TargetPolicy targetPolicy = TargetPolicy.SELF_DEFENSE;
        private boolean retreatsWhenHurt = true;
        private double followStart = 6.0D, followStop = 3.0D;

        private Builder(String id) {
            if (id == null || !id.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Kitty Talents: mode ids use a-z, 0-9 and _: " + id);
            this.name = id.toUpperCase(Locale.ROOT);
            this.key = "mode.kittytalents." + id;
        }

        /** Lang key of the display name (default {@code mode.kittytalents.<id>}). */
        public Builder langKey(String key) { this.key = key; return this; }
        /** The cat tethers to its owner (cannot be combined with an anchor). */
        public Builder followsOwner() { this.followsOwner = true; return this; }
        /** Follow distances: start walking beyond {@code start} blocks, stop within {@code stop}. */
        public Builder followDistance(double start, double stop) { this.followStart = start; this.followStop = stop; return this; }
        /** The cat holds the spot where the mode was chosen, within this many blocks. */
        public Builder anchorRadius(int radius) { this.anchorRadius = radius; return this; }
        public Builder targetPolicy(TargetPolicy policy) { this.targetPolicy = policy; return this; }
        public Builder retreatsWhenHurt(boolean retreats) { this.retreatsWhenHurt = retreats; return this; }
        /** The cat sits in this mode, like Docile and Stay. */
        public Builder stationary() { this.stationary = true; return this; }

        public CatMode build() {
            if (followsOwner && anchorRadius >= 0) throw new IllegalArgumentException("Kitty Talents: a mode cannot both follow and hold an anchor");
            if (targetPolicy == null) throw new IllegalArgumentException("Kitty Talents: targetPolicy is required");
            return new CatMode(name, key, false, followsOwner, anchorRadius, targetPolicy, retreatsWhenHurt, stationary, followStart, followStop);
        }
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

    public boolean isStationary() { return stationary; }
    /** Follow distances in blocks (CatFollowGoal). */
    public double followStartDistance() { return followStart; }
    public double followStopDistance() { return followStop; }
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
        String current = LegacyNames.MAP.getOrDefault(upper, upper);
        for (CatMode mode : snapshot) {
            if (mode.name.equals(current)) return mode;
        }
        return FOLLOW; // also a mode from an addon that is no longer installed
    }

    /** @deprecated use {@link #fromSavedName}; kept for existing callers. */
    @Deprecated
    public static CatMode byName(String name) { return fromSavedName(name); }

    /** True if {@code name} is a pre-Phase-3 mode that no longer exists. */
    public static boolean isLegacyName(String name) {
        return name != null && LegacyNames.MAP.containsKey(name.toUpperCase(Locale.ROOT));
    }
    /**
     * Legacy mode names (pre-Phase-3 saves and the frozen CatSchema ordinal table) -> current mode name.
     * MINE/FARM/FISH also get a job assigned on load (see TalentedCat#readAdditionalSaveData).
     */
    private static final class LegacyNames {
        static final Map<String, String> MAP = Map.of(
                "ESCORT", "FOLLOW", "PROTECT", "FOLLOW", "DEFENSIVE", "FOLLOW", "RETREAT", "FOLLOW",
                "PATROL", "GUARD", "BERSERKER", "AGGRESSIVE",
                "MINE", "WORK", "FARM", "WORK", "FISH", "WORK");
    }
}
