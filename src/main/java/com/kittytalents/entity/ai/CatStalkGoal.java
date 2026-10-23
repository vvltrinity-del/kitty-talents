package com.kittytalents.entity.ai;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * R13: Feline Stalking Behavior.
 * When approaching a target or prey from 8-18 blocks away, the cat enters
 * a crouching stalking pose, moving cautiously and silently until in pounce range.
 */
public class CatStalkGoal extends Goal {
    private final TalentedCat cat;
    private LivingEntity target;
    private int stalkPauseTicks = 0;

    public CatStalkGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping() || !cat.getCatMode().canAttack()) {
            return false;
        }
        LivingEntity currentTarget = cat.getTarget();
        if (currentTarget == null || !currentTarget.isAlive()) {
            return false;
        }

        double distSq = cat.distanceToSqr(currentTarget);
        // Stalk when between 8 and 18 blocks away
        return distSq >= 64.0D && distSq <= 324.0D && cat.onGround();
    }

    @Override
    public boolean canContinueToUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        LivingEntity current = cat.getTarget();
        if (current == null || !current.isAlive()) return false;
        double distSq = cat.distanceToSqr(current);
        return distSq > 36.0D && distSq <= 360.0D; // Continue until within pounce range (< 6 blocks)
    }

    @Override
    public void start() {
        this.target = cat.getTarget();
        this.stalkPauseTicks = 0;
        cat.setPose(Pose.CROUCHING);
        if (target != null) {
            cat.getNavigation().moveTo(target, 0.75D); // Slow stalking pace
        }
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) return;

        cat.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Feline cautious stalking: brief pauses to calculate pounce trajectory
        if (stalkPauseTicks > 0) {
            stalkPauseTicks--;
            cat.getNavigation().stop();
            return;
        }

        if (cat.getRandom().nextInt(50) == 0) {
            stalkPauseTicks = 10 + cat.getRandom().nextInt(15);
            return;
        }

        if (cat.getNavigation().isDone() || cat.tickCount % 15 == 0) {
            cat.getNavigation().moveTo(target, 0.75D);
        }
    }

    @Override
    public void stop() {
        this.target = null;
        this.stalkPauseTicks = 0;
        if (cat.getPose() == Pose.CROUCHING) {
            cat.setPose(Pose.STANDING);
        }
    }
}
