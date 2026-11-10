package com.kittytalents.ai.goal;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Stalking (playtest request): when the cat's target is out of pounce range and hasn't noticed the cat, it creeps up
 * crouched and slow until it is close enough to pounce; CatPounceGoal then takes over. If the target turns on the cat,
 * the stalk is dropped and normal combat (charge / melee) resumes.
 */
public class CatStalkGoal extends Goal {
    private static final double MAX_STALK_DISTANCE = 24.0D;
    private static final int MAX_TICKS = 300;

    private final TalentedCat cat;
    private LivingEntity target;
    private int ticks;
    private int repath;

    public CatStalkGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean allowed() {
        CatMode mode = cat.getCatMode();
        return !cat.isTwined() && !cat.isIncapacitated() && !cat.isOrderedToSit() && !cat.isRetreating()
                && mode.canAttack() && mode != CatMode.SUPPORT && !cat.hasEquippedGun() && !cat.isVehicle();
    }

    /** The target hasn't noticed the cat (isn't hunting it). */
    public static boolean isUnaware(TalentedCat cat, LivingEntity t) {
        return !(t instanceof Mob m && m.getTarget() == cat) && cat.getLastHurtByMob() != t;
    }

    private boolean unaware(LivingEntity t) {
        return isUnaware(cat, t);
    }

    private double stalkStopDistance() {
        return 2.5D; // stalk right up to minimum pounce range; CatPounceGoal (priority POUNCE) preempts when in range
    }

    @Override
    public boolean canUse() {
        if (!allowed()) return false;
        LivingEntity t = cat.getTarget();
        if (t == null || !t.isAlive() || !unaware(t)) return false;
        double d = cat.distanceTo(t);
        return d > CatPounceGoal.maxRange(cat) && d < MAX_STALK_DISTANCE;
    }

    @Override
    public boolean canContinueToUse() {
        return allowed() && target != null && target.isAlive() && target == cat.getTarget() && unaware(target)
                && ticks < MAX_TICKS && cat.distanceTo(target) > stalkStopDistance();
    }

    @Override
    public void start() {
        target = cat.getTarget();
        ticks = 0;
        repath = 0;
        cat.setPose(Pose.CROUCHING);
    }

    @Override
    public void tick() {
        ticks++;
        cat.setPose(Pose.CROUCHING);
        cat.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (--repath <= 0) {
            repath = 10;
            cat.getNavigation().moveTo(target, 0.6D); // slow, low creep
        }
    }

    @Override
    public void stop() {
        cat.setPose(Pose.STANDING);
        cat.getNavigation().stop();
        target = null;
    }
}
