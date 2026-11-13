package com.kittytalents.registry;

import net.minecraft.world.level.GameRules;

/**
 * Per-world game rules (5.0).
 * <ul>
 * <li>{@code kittyTalentsCatDeath} (default false): a downed cat bleeds out after 60 s unless revived. Off keeps
 * cats safe as before.</li>
 * <li>{@code kittyTalentsFollowerLimit} (default 4, 0 = no limit): how many cats can follow one player at once.
 * Only Follow, Tactical and Support count; working, guarding, wandering, staying and sitting cats don't.</li>
 * </ul>
 */
public final class ModGameRules {
    private ModGameRules() {}

    public static final GameRules.Key<GameRules.BooleanValue> CAT_DEATH =
            GameRules.register("kittyTalentsCatDeath", GameRules.Category.MOBS, GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.IntegerValue> FOLLOWER_LIMIT =
            GameRules.register("kittyTalentsFollowerLimit", GameRules.Category.MOBS, GameRules.IntegerValue.create(4));

    /** Seconds a downed cat has before it bleeds out (with kittyTalentsCatDeath on). */
    public static final int BLEED_OUT_TICKS = 60 * 20;

    /** Forces class loading so the rules exist before any world loads. */
    public static void init() {}
}
