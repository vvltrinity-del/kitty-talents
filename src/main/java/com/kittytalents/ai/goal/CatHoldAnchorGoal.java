package com.kittytalents.ai.goal;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.ai.CatCliffSafetyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * STAY and GUARD: keeps the cat at the anchor set when the mode was chosen (saved with the cat).
 * STAY returns to the spot; GUARD patrols a ring around its post when idle and returns after chasing.
 */
public class CatHoldAnchorGoal extends Goal {
    private final TalentedCat cat;
    private int patrolAngle;
    private long nextPatrol;

    public CatHoldAnchorGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean blocked() {
        return cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()
                || !cat.getCatMode().usesAnchor() || cat.getModeAnchor() == null;
    }

    private double distToAnchor() {
        BlockPos a = cat.getModeAnchor();
        return Math.sqrt(cat.distanceToSqr(a.getX() + 0.5D, a.getY(), a.getZ() + 0.5D));
    }

    /** How far a cat may chase from its anchor before giving up and returning. */
    private double leash() {
        return cat.getCatMode().anchorRadius() + 8.0D;
    }

    @Override
    public boolean canUse() {
        if (blocked()) return false;
        double d = distToAnchor();
        if (cat.getTarget() != null) {
            if (d <= leash()) return false; // let combat goals work inside the leash
            cat.setTarget(null);           // chased too far: give up and come back
            return true;
        }
        if (d > Math.max(1.5D, cat.getCatMode().anchorRadius())) return true;
        return cat.getCatMode() == CatMode.GUARD && cat.level().getGameTime() >= nextPatrol;
    }

    @Override
    public boolean canContinueToUse() {
        return !blocked() && cat.getTarget() == null && !cat.getNavigation().isDone();
    }

    @Override
    public void start() {
        BlockPos a = cat.getModeAnchor();
        Vec3 dest = new Vec3(a.getX() + 0.5D, a.getY(), a.getZ() + 0.5D);
        if (cat.getCatMode() == CatMode.GUARD && distToAnchor() <= cat.getCatMode().anchorRadius()) {
            patrolAngle = (patrolAngle + 70) % 360;
            double r = cat.getCatMode().anchorRadius() * 0.7D;
            Vec3 point = dest.add(Math.cos(Math.toRadians(patrolAngle)) * r, 0, Math.sin(Math.toRadians(patrolAngle)) * r);
            if (CatCliffSafetyHelper.isSafeToLeap(cat, point)) dest = point;
            nextPatrol = cat.level().getGameTime() + 80 + cat.getRandom().nextInt(60);
        }
        cat.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.1D);
    }

    @Override
    public void stop() {
        cat.getNavigation().stop();
    }
}
