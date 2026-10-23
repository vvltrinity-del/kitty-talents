package com.kittytalents.entity.ai;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * R11: Guard and Patrol Companion Mode Goal.
 * Anchors the cat to a territorial post (GUARD) or patrols a perimeter around it (PATROL).
 */
public class CatPatrolAndGuardGoal extends Goal {
    private final TalentedCat cat;
    private BlockPos anchorPos = null;
    private int patrolAngle = 0;
    private int patrolCooldown = 0;

    public CatPatrolAndGuardGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        CatMode mode = cat.getCatMode();
        if (mode != CatMode.GUARD && mode != CatMode.PATROL && mode != CatMode.STAY) return false;

        if (anchorPos == null) {
            anchorPos = cat.blockPosition();
        }

        // If in STAY mode, simply remain at anchor position
        if (mode == CatMode.STAY) {
            return cat.distanceToSqr(anchorPos.getX() + 0.5D, anchorPos.getY(), anchorPos.getZ() + 0.5D) > 4.0D;
        }

        // In GUARD mode, if drifted too far from anchor and no current target
        if (mode == CatMode.GUARD && cat.getTarget() == null) {
            return cat.distanceToSqr(anchorPos.getX() + 0.5D, anchorPos.getY(), anchorPos.getZ() + 0.5D) > 64.0D;
        }

        // In PATROL mode, periodic patrol steps around anchor
        if (mode == CatMode.PATROL && cat.getTarget() == null) {
            if (patrolCooldown > 0) {
                patrolCooldown--;
                return false;
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        if (cat.getTarget() != null) return false; // Yield to combat
        if (anchorPos == null) return false;
        CatMode mode = cat.getCatMode();
        return (mode == CatMode.GUARD || mode == CatMode.PATROL || mode == CatMode.STAY);
    }

    @Override
    public void start() {
        if (anchorPos == null) return;
        CatMode mode = cat.getCatMode();

        if (mode == CatMode.GUARD || mode == CatMode.STAY) {
            cat.getNavigation().moveTo(anchorPos.getX() + 0.5D, anchorPos.getY() + 0.5D, anchorPos.getZ() + 0.5D, 1.25D);
        } else if (mode == CatMode.PATROL) {
            patrolAngle = (patrolAngle + 45) % 360;
            double rad = Math.toRadians(patrolAngle);
            double targetX = anchorPos.getX() + 0.5D + Math.cos(rad) * 10.0D;
            double targetZ = anchorPos.getZ() + 0.5D + Math.sin(rad) * 10.0D;
            Vec3 dest = new Vec3(targetX, anchorPos.getY(), targetZ);
            if (CatCliffSafetyHelper.isSafeToLeap(cat, dest)) {
                cat.getNavigation().moveTo(targetX, anchorPos.getY(), targetZ, 1.15D);
            }
            patrolCooldown = 100;
        }
    }

    @Override
    public void stop() {
        cat.getNavigation().stop();
    }
}
