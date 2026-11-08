package com.kittytalents.talent.effect;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Gameplay of one talent, as hooks (Phase 4). A talent implements only the hooks it needs; {@link TalentEngine}
 * calls them for every talent the cat has at rank >= 1. All hooks run server-side only.
 *
 * <p><b>Mastery:</b> every effect also defines what happens at max rank ({@link TalentContext#mastered()}); hooks
 * branch on it. The mastery's player-facing text is the lang key {@code talent.kittytalents.<id>.mastery}.</p>
 */
public interface TalentEffect {

    /** Ticks between {@link #onTick} calls; 0 = this talent has no periodic behavior. */
    default int tickInterval() { return 0; }

    /** Periodic behavior while the cat is awake and active. */
    default void onTick(TalentContext ctx) {}

    /** Adjust the cat's outgoing melee damage before it is dealt. */
    default float modifyOutgoingDamage(TalentContext ctx, LivingEntity target, float damage) { return damage; }

    /** After the cat landed a melee hit. */
    default void onHitDealt(TalentContext ctx, LivingEntity target, float damage) {}

    /** Adjust damage the cat takes. Return <= 0 to cancel the hit entirely. */
    default float modifyIncomingDamage(TalentContext ctx, DamageSource source, float amount) { return amount; }

    /** Contribute to the cat's attribute values (recomputed from scratch each time, so additive and idempotent). */
    default void applyAttributes(TalentContext ctx, AttributeTotals totals) {}

    /** The cat killed {@code victim} (any means: melee, pounce, abilities, guns). */
    default void onKill(TalentContext ctx, LivingEntity victim) {}

    /** The cat's owner is about to take damage while near the cat. Return the (possibly reduced) amount. */
    default float onOwnerHurt(TalentContext ctx, Player owner, DamageSource source, float amount) { return amount; }

    /** The cat's owner is about to take fall damage while near the cat. Return the damage multiplier to apply. */
    default float onOwnerFall(TalentContext ctx, Player owner, float distance) { return 1.0F; }
}
