package com.kittytalents.talent;

import com.kittytalents.core.tuning.Tuning;
import com.kittytalents.core.tuning.Tuning.Key;

/** Tunable numbers of talents whose logic lives outside talent/effect (combat engine, pounce goal, TalentedCat#hurt). */
public final class TalentTuning {
    private TalentTuning() {}

    public static final Key POUNCE_COOLDOWN = Tuning.talent("pounce", "cooldown_ticks", 100, "Cooldown ticks");
    public static final Key POUNCE_COOLDOWN_PER_RANK = Tuning.talent("pounce", "cooldown_ticks_per_rank", 10, "Cooldown cut per rank");
    public static final Key POUNCE_MIN_COOLDOWN = Tuning.talent("pounce", "min_cooldown_ticks", 40, "Shortest cooldown ticks");
    public static final Key POUNCE_RANGE = Tuning.talent("pounce", "range", 8.0, "Range");
    public static final Key POUNCE_RANGE_PER_RANK = Tuning.talent("pounce", "range_per_rank", 0.75, "Range per rank");

    public static final Key CRIT_CHANCE = Tuning.talent("critical_claws", "crit_chance", 0.10, "Crit chance");
    public static final Key CRIT_CHANCE_PER_RANK = Tuning.talent("critical_claws", "crit_chance_per_rank", 0.08, "Crit chance per rank");
    public static final Key CRIT_MULT = Tuning.talent("critical_claws", "crit_damage_mult", 1.5, "Crit damage x");
    public static final Key CRIT_MULT_PER_RANK = Tuning.talent("critical_claws", "crit_damage_mult_per_rank", 0.2, "Crit damage x per rank");

    public static final Key REFLEX_DODGE_PER_RANK = Tuning.talent("feline_reflexes", "dodge_chance_per_rank", 0.07, "Dodge chance per rank");

    public static final Key NINE_LIVES_RECHARGE = Tuning.talent("nine_lives", "recharge_ticks", 12000, "Recharge ticks");
    public static final Key NINE_LIVES_RECHARGE_PER_RANK = Tuning.talent("nine_lives", "recharge_ticks_per_rank", 1200, "Recharge cut per rank");
    public static final Key NINE_LIVES_HEAL_PER_RANK = Tuning.talent("nine_lives", "revive_health_per_rank", 0.18, "Revive health per rank");

    /** Loads this class so its keys exist (Tuning.bootstrap). */
    public static void init() {}
}
