package com.kittytalents.entity.ai;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * R11: Retreat Companion Mode Goal.
 * Causes the cat to disengage combat, clear its target, and sprint back to its owner or home bed.
 */
public class CatRetreatGoal extends Goal {
    private final TalentedCat cat;
    private int recalcTimer = 0;

    public CatRetreatGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        if (cat.getCatMode() != CatMode.RETREAT) return false;

        LivingEntity owner = cat.getOwner();
        BlockPos home = cat.getHomeBedPos();
        if (owner == null && home == null) return false;

        double distSq = owner != null ? cat.distanceToSqr(owner) : cat.distanceToSqr(home.getX(), home.getY(), home.getZ());
        return distSq > 9.0D;
    }

    @Override
    public boolean canContinueToUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        if (cat.getCatMode() != CatMode.RETREAT) return false;

        LivingEntity owner = cat.getOwner();
        BlockPos home = cat.getHomeBedPos();
        if (owner == null && home == null) return false;

        double distSq = owner != null ? cat.distanceToSqr(owner) : cat.distanceToSqr(home.getX(), home.getY(), home.getZ());
        return distSq > 6.0D;
    }

    @Override
    public void start() {
        cat.setTarget(null);
        cat.setOrderedToSit(false);
        this.recalcTimer = 0;
    }

    @Override
    public void tick() {
        cat.setTarget(null); // Never attack while retreating

        if (--recalcTimer <= 0) {
            recalcTimer = 15;
            LivingEntity owner = cat.getOwner();
            if (owner != null) {
                cat.getNavigation().moveTo(owner, 1.5D); // High sprint speed
            } else if (cat.getHomeBedPos() != null) {
                BlockPos home = cat.getHomeBedPos();
                cat.getNavigation().moveTo(home.getX() + 0.5D, home.getY() + 0.5D, home.getZ() + 0.5D, 1.5D);
            }
        }
    }

    @Override
    public void stop() {
        cat.getNavigation().stop();
    }
}
