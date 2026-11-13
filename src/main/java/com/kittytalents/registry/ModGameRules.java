package com.kittytalents.registry;

import net.minecraft.world.level.GameRules;

/**
 * Per-world game rules (5.0).
 * <ul>
 * <li>{@code kittyTalentsCatDeath} (default false): a downed cat bleeds out unless revived in time. Off keeps
 * cats safe as before.</li>
 * <li>{@code kittyTalentsBleedOutSeconds} (default 60): how long that is.</li>
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

    /** Seconds a downed cat has before it bleeds out, with kittyTalentsCatDeath on (default 60, at least 5). */
    public static final GameRules.Key<GameRules.IntegerValue> BLEED_OUT_SECONDS =
            GameRules.register("kittyTalentsBleedOutSeconds", GameRules.Category.MOBS, GameRules.IntegerValue.create(60));

    public static int bleedOutTicks(net.minecraft.world.level.Level level) {
        return Math.max(5, level.getGameRules().getInt(BLEED_OUT_SECONDS)) * 20;
    }

    /** Forces class loading so the rules exist before any world loads. */
    public static void init() {}
}
