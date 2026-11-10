package com.kittytalents.core.living;

import com.kittytalents.core.tuning.Tuning;
import net.minecraft.network.chat.Component;

/**
 * Bond between a cat and its owner (4.0 Phase 6): points from time together, petting, feeding, playing and fighting
 * side by side become a level 0..10 that unlocks small perks. Every 3.0 cat starts at 0.
 * Numbers are tunable under {@code "bond"} in a tuning file.
 */
public final class CatBond {
    private CatBond() {}

    public static final String SCOPE = "bond";
    public static final int MAX_LEVEL = 10;

    /** Points for level n are {@code step * n * (n + 1) / 2}: 25, 75, 150, 250, ... 1375 by default. */
    public static final Tuning.Key LEVEL_STEP = key("levels", "points_step", 25, "Points step per level");

    public static final Tuning.Key TIME_POINTS = key("gain", "per_minute_together", 1, "Points per minute near the owner");
    public static final Tuning.Key TIME_RANGE = key("gain", "together_range", 16, "Blocks that count as together");
    public static final Tuning.Key PET_POINTS = key("gain", "petting", 2, "Points per petting");
    public static final Tuning.Key FEED_POINTS = key("gain", "feeding", 3, "Points per feeding");
    public static final Tuning.Key PLAY_POINTS = key("gain", "playing", 4, "Points per play");
    public static final Tuning.Key KILL_POINTS = key("gain", "kill_together", 2, "Points per kill near the owner");
    public static final Tuning.Key GAIN_COOLDOWN = key("gain", "cooldown_ticks", 600, "Ticks between gains of one kind");

    public static final Tuning.Key XP_MULT = key("perks", "xp_mult", 1.05, "Quick Learner: experience x");
    public static final Tuning.Key KINDRED_HEALTH = key("perks", "kindred_health", 2, "Kindred: max health");
    public static final Tuning.Key KINDRED_DAMAGE = key("perks", "kindred_damage", 1, "Kindred: attack damage");
    public static final Tuning.Key SOULMATE_DAMAGE = key("perks", "soulmate_damage_mult", 1.10, "Soulmates: damage x near the owner");
    public static final Tuning.Key SOULMATE_RANGE = key("perks", "soulmate_range", 12, "Soulmates: range");
    public static final Tuning.Key PLAY_ENERGY = key("perks", "play_energy", 20, "Play: Energy restored");

    private static Tuning.Key key(String owner, String name, double def, String label) {
        return Tuning.define(SCOPE, owner, name, def, 0, def * 10, label);
    }

    /** A perk and the bond level that unlocks it. */
    public enum Perk {
        COMFORT(2, "Comfort", "Petting also gives you Regeneration."),
        PLAY(3, "Playtime", "Right-click with String to play: restores Energy."),
        QUICK_LEARNER(5, "Quick Learner", "+5% experience."),
        KINDRED(7, "Kindred", "+2 max health and +1 attack damage."),
        SOULMATES(10, "Soulmates", "+10% damage while you are close by.");

        public final int level;
        public final String displayName, description;

        Perk(int level, String displayName, String description) {
            this.level = level;
            this.displayName = displayName;
            this.description = description;
        }

        public Component title() {
            return Component.translatableWithFallback("bond.kittytalents." + name().toLowerCase(), displayName);
        }

        public Component text() {
            return Component.translatableWithFallback("bond.kittytalents." + name().toLowerCase() + ".desc", description);
        }
    }

    /** Total points needed to reach {@code level}. */
    public static int pointsFor(int level) {
        return (int) Math.round(LEVEL_STEP.get() * level * (level + 1) / 2.0);
    }

    public static int levelFor(int points) {
        int level = 0;
        while (level < MAX_LEVEL && points >= pointsFor(level + 1)) level++;
        return level;
    }

    public static boolean has(int points, Perk perk) {
        return levelFor(points) >= perk.level;
    }

    public static void init() {}
}
