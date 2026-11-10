package com.kittytalents.ai.goal;

import com.kittytalents.combat.FelineCombatEngine;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.ai.CatCliffSafetyHelper;
import com.kittytalents.talent.Talents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Pounce: every cat's built-in signature combat opener (Phase 3 design decision).
 *
 * <p>Stalk (crouch + rear wiggle) then a real physics leap through {@link FelineCombatEngine#launchPounce}, the same
 * code path Twine uses, so AI and player pounces behave identically. Landing damage is resolved by the engine and
 * only hits what {@link com.kittytalents.ai.CatTargeting#canHarm} allows. The Pounce talent only upgrades it:
 * longer reach and a shorter cooldown here, more height/damage/radius in the engine.</p>
 */
public class CatPounceGoal extends Goal {
    private static final int STALK_TICKS = 12;
    private static final int MAX_AIR_TICKS = 40;

    private final TalentedCat cat;
    private LivingEntity target;
    private int stalkTicks;
    private int airTicks;
    private boolean launched;
    private long readyAt;

    public CatPounceGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /** Ticks between pounces: 100 at rank 0, 10 fewer per Pounce rank, never under 40. */
    public static int cooldownTicks(TalentedCat cat) {
        return Math.max(com.kittytalents.talent.TalentTuning.POUNCE_MIN_COOLDOWN.i(), com.kittytalents.talent.TalentTuning.POUNCE_COOLDOWN.i() - com.kittytalents.talent.TalentTuning.POUNCE_COOLDOWN_PER_RANK.i() * cat.getTalentLevel(Talents.POUNCE));
    }

    /** Max pounce distance in blocks: 8 at rank 0, +0.75 per rank. */
    public static double maxRange(TalentedCat cat) {
        return com.kittytalents.talent.TalentTuning.POUNCE_RANGE.get() + com.kittytalents.talent.TalentTuning.POUNCE_RANGE_PER_RANK.get() * cat.getTalentLevel(Talents.POUNCE);
    }

    public static boolean isGrounded(TalentedCat cat) {
        return cat.onGround() || cat.verticalCollision || (cat.fallDistance < 0.2F && Math.abs(cat.getDeltaMovement().y) < 0.08D);
    }

    private boolean blocked() {
        CatMode mode = cat.getCatMode();
        return cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping() || cat.isVehicle()
                || cat.isRetreating() || !mode.canAttack() || mode == CatMode.SUPPORT || cat.hasEquippedGun();
    }

    @Override
    public boolean canUse() {
        if (blocked() || cat.level().getGameTime() < readyAt || !isGrounded(cat)) return false;
        LivingEntity t = cat.getTarget();
        if (t == null || !t.isAlive()) return false;
        double dist = Math.sqrt(cat.distanceToSqr(t));
        if (dist < 2.5D || dist > maxRange(cat)) return false;
        if (!cat.getSensing().hasLineOfSight(t)) return false;
        return CatCliffSafetyHelper.isSafeToLeap(cat, t.position());
    }

    /** Human-readable reason the pounce cannot start right now (null = it can). For debugging and audits. */
    public String whyNot() {
        if (blocked()) return "blocked(mode=" + cat.getCatMode() + ",gun=" + cat.hasEquippedGun() + ",sleep=" + cat.isSleeping()
                + ",vehicle=" + cat.isVehicle() + ",retreat=" + cat.isRetreating() + ")";
        if (cat.level().getGameTime() < readyAt) return "cooldown";
        if (!isGrounded(cat)) return "airborne";
        LivingEntity t = cat.getTarget();
        if (t == null || !t.isAlive()) return "no target";
        double dist = Math.sqrt(cat.distanceToSqr(t));
        if (dist < 2.5D || dist > maxRange(cat)) return "range " + dist;
        if (!cat.getSensing().hasLineOfSight(t)) return "no line of sight";
        if (!CatCliffSafetyHelper.isSafeToLeap(cat, t.position())) return "unsafe landing";
        return null;
    }

    @Override
    public void start() {
        target = cat.getTarget();
        stalkTicks = STALK_TICKS;
        airTicks = 0;
        launched = false;
        cat.getNavigation().stop();
        cat.setPose(Pose.CROUCHING);
        cat.playSound(SoundEvents.CAT_HISS, 0.5F, 1.5F);
    }

    @Override
    public boolean canContinueToUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit()) return false;
        if (!launched) return target != null && target.isAlive() && !blocked();
        // Once airborne, stay in the goal until the engine resolved the landing (or a safety timeout).
        return cat.combat().pounceActive && airTicks < MAX_AIR_TICKS;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target != null) cat.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (!launched) {
            cat.setDeltaMovement(0.0D, cat.getDeltaMovement().y, 0.0D);
            if (--stalkTicks > 0) return;
            // Re-check the landing zone at launch time: the target may have moved over a drop.
            if (target == null || !CatCliffSafetyHelper.isSafeToLeap(cat, target.position())) {
                launched = false;
                stalkTicks = Integer.MAX_VALUE; // canContinueToUse will end the goal via blocked()/target checks
                target = null;
                return;
            }
            cat.setPose(Pose.STANDING);
            launched = FelineCombatEngine.launchPounce(cat, target.position(), false);
            if (launched) cat.incrementPounces();
        } else {
            airTicks++;
        }
    }

    @Override
    public void stop() {
        cat.setPose(Pose.STANDING);
        // Full cooldown after a leap; a short one after an aborted stalk so the cat does not re-crouch every tick.
        readyAt = cat.level().getGameTime() + (launched ? cooldownTicks(cat) : 20);
        target = null;
        launched = false;
    }
}
