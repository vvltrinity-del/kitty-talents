package com.kittytalents.core.living;

import com.kittytalents.core.tuning.Tuning;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/**
 * A cat's personality (4.0 Phase 6). Each trait nudges idle behaviour (ai/goal/CatTraitGoal) and gives one small
 * bonus and one small quirk. {@link #NONE} is "not rolled yet" and has no effect.
 * Numbers are tunable under {@code "traits"} in a tuning file.
 */
public enum CatTrait {
    NONE("none", "None", "", "", 0xFFB8B2A8),
    BRAVE("brave", "Brave", "+10% attack damage", "Never retreats when hurt; stares down nearby monsters", 0xFFF87171),
    LAZY("lazy", "Lazy", "Heals while resting", "10% slower; naps when idle", 0xFF93C5FD),
    CURIOUS("curious", "Curious", "+15% experience", "Wanders off to inspect dropped items", 0xFFFDE68A),
    PLAYFUL("playful", "Playful", "+25% Energy recovery", "Gets hungry 10% faster; hops around when idle", 0xFFF9A8D4),
    SHY("shy", "Shy", "15% less damage from projectiles", "Backs away from strangers", 0xFFC4B5FD),
    GREEDY("greedy", "Greedy", "Food restores 50% more", "Gets hungry 25% faster; begs when you hold food", 0xFF86EFAC);

    public static final String SCOPE = "traits";

    public static final Tuning.Key BRAVE_DAMAGE = key("brave", "damage_mult", 1.10, "Attack damage x");
    public static final Tuning.Key LAZY_SPEED = key("lazy", "speed_mult", 0.90, "Move speed x");
    public static final Tuning.Key LAZY_REST_HEAL_TICKS = key("lazy", "rest_heal_ticks", 100, "Ticks per 1 HP while resting");
    public static final Tuning.Key CURIOUS_XP = key("curious", "xp_mult", 1.15, "Experience x");
    public static final Tuning.Key PLAYFUL_ENERGY = key("playful", "energy_regen_mult", 1.25, "Energy recovery x");
    public static final Tuning.Key PLAYFUL_HUNGER = key("playful", "hunger_mult", 1.10, "Hunger x");
    public static final Tuning.Key SHY_PROJECTILE = key("shy", "projectile_damage_taken", 0.85, "Projectile damage taken x");
    public static final Tuning.Key SHY_DISTANCE = key("shy", "stranger_distance", 5.0, "Keeps this far from strangers");
    public static final Tuning.Key GREEDY_FOOD = key("greedy", "food_mult", 1.5, "Food restored x");
    public static final Tuning.Key GREEDY_HUNGER = key("greedy", "hunger_mult", 1.25, "Hunger x");
    public static final Tuning.Key INHERIT_CHANCE = key("inheritance", "parent_chance", 0.8, "Chance a kitten takes a parent's trait");

    private static Tuning.Key key(String owner, String name, double def, String label) {
        return Tuning.define(SCOPE, owner, name, def, 0, def * 10, label);
    }

    public final String id, displayName, bonus, quirk;
    public final int color;

    CatTrait(String id, String displayName, String bonus, String quirk, int color) {
        this.id = id;
        this.displayName = displayName;
        this.bonus = bonus;
        this.quirk = quirk;
        this.color = color;
    }

    public Component title() {
        return Component.translatableWithFallback("trait.kittytalents." + id, displayName);
    }

    public Component bonusText() {
        return Component.translatableWithFallback("trait.kittytalents." + id + ".bonus", bonus);
    }

    public Component quirkText() {
        return Component.translatableWithFallback("trait.kittytalents." + id + ".quirk", quirk);
    }

    public static CatTrait byId(String id) {
        for (CatTrait t : values()) if (t.id.equals(id)) return t;
        return NONE;
    }

    /** A random real trait. */
    public static CatTrait roll(RandomSource random) {
        return values()[1 + random.nextInt(values().length - 1)];
    }

    /** A kitten's trait: usually one parent's (either, equal odds), otherwise a fresh roll. */
    public static CatTrait inherit(CatTrait a, CatTrait b, RandomSource random) {
        CatTrait pick = random.nextBoolean() ? a : b;
        if (pick == NONE) pick = a != NONE ? a : b;
        return pick != NONE && random.nextDouble() < INHERIT_CHANCE.get() ? pick : roll(random);
    }

    public static void init() {}
}
