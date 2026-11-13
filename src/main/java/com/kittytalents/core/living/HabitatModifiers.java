package com.kittytalents.core.living;

import com.kittytalents.block.KittyHabitatBlockEntity;
import com.kittytalents.core.tuning.Tuning;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Personality and bond inside the Kitty Habitat (5.0, "Habitat Traits Plan"). A housed cat's trait changes how it
 * works, and its neighbours matter, so laying out the colony is a choice:
 * <ul>
 * <li>Shy: +20% speed with no other working Habitat within 4 blocks, -10% with 3 or more.</li>
 * <li>Brave (foreman): other working Habitats within 6 blocks get +5% speed (the bonus does not stack).</li>
 * <li>Lazy: 10% slower, but never gets hungry while working.</li>
 * <li>Curious: each cycle has a 10% chance of a bonus item from the tier below.</li>
 * <li>Playful: +15% with another Playful Habitat within 4 blocks, -10% with no working Habitat near.</li>
 * <li>Greedy: +25% output, +50% hunger.</li>
 * <li>Bond: +1% speed per bond level (max +10%) while the owner is within 32 blocks.</li>
 * </ul>
 * Numbers are Tuning keys (scope {@code traits}, owner {@code habitat}). The neighbour scan is cached per Habitat and
 * refreshed every 100 ticks.
 */
public final class HabitatModifiers {
    private HabitatModifiers() {}

    private static final String SCOPE = "traits", OWNER = "habitat";
    private static Tuning.Key key(String name, double def, String label) {
        return Tuning.define(SCOPE, OWNER, name, def, 0, Math.max(1.0, Math.abs(def) * 10), label);
    }
    public static final Tuning.Key SHY_ALONE = key("shy_alone_speed", 0.20, "Shy: speed bonus when alone");
    public static final Tuning.Key SHY_CROWD = key("shy_crowded_speed", 0.10, "Shy: speed loss when crowded");
    public static final Tuning.Key BRAVE = key("brave_foreman_speed", 0.05, "Brave: foreman bonus to neighbours");
    public static final Tuning.Key LAZY = key("lazy_speed", 0.10, "Lazy: speed loss");
    public static final Tuning.Key CURIOUS = key("curious_bonus_chance", 0.10, "Curious: bonus item chance");
    public static final Tuning.Key PLAYFUL = key("playful_pair_speed", 0.15, "Playful: speed bonus with a playmate");
    public static final Tuning.Key PLAYFUL_ALONE = key("playful_alone_speed", 0.10, "Playful: speed loss when alone");
    public static final Tuning.Key GREEDY_OUT = key("greedy_output", 0.25, "Greedy: extra output");
    public static final Tuning.Key GREEDY_FOOD = key("greedy_hunger", 0.50, "Greedy: extra hunger");
    public static final Tuning.Key BOND = key("bond_speed_per_level", 0.01, "Bond: speed per bond level");
    public static final Tuning.Key BOND_RANGE = key("bond_owner_range", 32, "Bond: owner range");
    public static final Tuning.Key CAPTAIN = key("captain_rally_speed", 0.05, "Captain Rally: speed bonus");

    /** Neighbour radii (blocks). */
    public static final double NEAR = 4.0, FOREMAN = 6.0;
    public static final int REFRESH_TICKS = 100;

    public static void init() {}

    /** speed/output/hunger multipliers, the Curious flag, and the lines shown on the Habitat screen. */
    public record Mods(float speed, float output, float hunger, boolean curious, List<String> lines) {
        public static final Mods NONE = new Mods(1, 1, 1, false, List.of());
    }

    // ---------------------------------------------------------------- loaded Habitats per level

    private static final Map<Level, Set<BlockPos>> LOADED = new WeakHashMap<>();

    public static void loaded(Level level, BlockPos pos) {
        if (level != null && !level.isClientSide) LOADED.computeIfAbsent(level, l -> new HashSet<>()).add(pos.immutable());
    }

    public static void unloaded(Level level, BlockPos pos) {
        Set<BlockPos> set = level == null ? null : LOADED.get(level);
        if (set != null) set.remove(pos);
    }

    // ---------------------------------------------------------------- the calculation

    public static Mods compute(Level level, BlockPos pos, KittyHabitatBlockEntity h) {
        if (!h.hasWorker()) return Mods.NONE;
        CatTrait trait = h.workerTrait();
        int occupiedNear = 0, playfulNear = 0;
        boolean foreman = false, captain = false;
        for (BlockPos other : LOADED.getOrDefault(level, Set.of())) {
            if (other.equals(pos)) continue;
            double d = other.distSqr(pos);
            if (d > FOREMAN * FOREMAN) continue;
            if (!(level.getBlockEntity(other) instanceof KittyHabitatBlockEntity o) || !o.hasWorker()) continue;
            CatTrait ot = o.workerTrait();
            if (ot == CatTrait.BRAVE) foreman = true;
            if (d <= NEAR * NEAR) {
                occupiedNear++;
                if (ot == CatTrait.PLAYFUL) playfulNear++;
            }
        }
        float speed = 1, output = 1, hunger = 1;
        boolean curious = false;
        List<String> lines = new ArrayList<>();
        switch (trait) {
            case SHY -> {
                if (occupiedNear == 0) { speed += SHY_ALONE.f(); lines.add(line("Shy: alone", SHY_ALONE.f())); }
                else if (occupiedNear >= 3) { speed -= SHY_CROWD.f(); lines.add(line("Shy: crowded", -SHY_CROWD.f())); }
            }
            case PLAYFUL -> {
                if (playfulNear > 0) { speed += PLAYFUL.f(); lines.add(line("Playful: playmate", PLAYFUL.f())); }
                else if (occupiedNear == 0) { speed -= PLAYFUL_ALONE.f(); lines.add(line("Playful: lonely", -PLAYFUL_ALONE.f())); }
            }
            case LAZY -> { speed -= LAZY.f(); hunger = 0; lines.add(line("Lazy: naps, no hunger", -LAZY.f())); }
            case GREEDY -> {
                output += GREEDY_OUT.f();
                hunger += GREEDY_FOOD.f();
                lines.add("Greedy: +" + pct(GREEDY_OUT.f()) + " output, +" + pct(GREEDY_FOOD.f()) + " hunger");
            }
            case CURIOUS -> { curious = true; lines.add("Curious: " + pct(CURIOUS.f()) + " bonus finds"); }
            default -> {}
        }
        if (foreman) { speed += BRAVE.f(); lines.add(line("Brave foreman nearby", BRAVE.f())); }
        if (CaptainRally.boostsHabitat(level, pos)) { speed += CAPTAIN.f(); lines.add(line("Captain's Rally", CAPTAIN.f())); }
        int bond = h.workerBondLevel();
        UUID owner = h.workerOwner();
        if (bond > 0 && owner != null) {
            Player p = level.getPlayerByUUID(owner);
            if (p != null && p.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) <= BOND_RANGE.get() * BOND_RANGE.get()) {
                float b = bond * BOND.f();
                speed += b;
                lines.add(line("Bond " + bond + ", owner near", b));
            }
        }
        return new Mods(Math.max(0.1F, speed), output, hunger, curious, lines);
    }

    private static String line(String what, float delta) {
        return what + ", " + (delta >= 0 ? "+" : "") + pct(delta);
    }

    private static String pct(float f) {
        return Math.round(f * 100) + "%";
    }
}
