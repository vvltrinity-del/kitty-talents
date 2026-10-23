package com.kittytalents.entity.ai;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Priority 0 Safety Goal: Downed / Incapacitated Cat Lock.
 * Completely halts and zeroes movement, pathfinding, and combat targeting while
 * the cat is incapacitated, guaranteeing 100% resolution of the downed cat pathing bug.
 */
public class CatDownedSafetyGoal extends Goal {
    private final TalentedCat cat;

    public CatDownedSafetyGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP, Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        return cat.isIncapacitated();
    }

    @Override
    public boolean canContinueToUse() {
        return cat.isIncapacitated();
    }

    @Override
    public void start() {
        cat.getNavigation().stop();
        cat.setTarget(null);
        cat.setOrderedToSit(true);
        Vec3 motion = cat.getDeltaMovement();
        cat.setDeltaMovement(0.0D, motion.y, 0.0D);
    }

    @Override
    public void tick() {
        if (!cat.getNavigation().isDone()) {
            cat.getNavigation().stop();
        }
        if (cat.getTarget() != null) {
            cat.setTarget(null);
        }
        Vec3 motion = cat.getDeltaMovement();
        if (Math.abs(motion.x) > 0.001D || Math.abs(motion.z) > 0.001D) {
            cat.setDeltaMovement(0.0D, motion.y, 0.0D);
        }
    }
}
