package com.kittytalents.entity.ai;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * R11: Return Home Companion Mode Goal.
 * Navigates back to the cat's linked bed when set to RETURN_HOME mode.
 */
public class CatReturnHomeGoal extends Goal {
    private final TalentedCat cat;
    private int recalcPathTimer = 0;

    public CatReturnHomeGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        if (cat.getCatMode() != CatMode.RETURN_HOME) return false;
        BlockPos home = cat.getHomeBedPos();
        if (home == null) return false;

        return cat.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D) > 4.0D;
    }

    @Override
    public boolean canContinueToUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        if (cat.getCatMode() != CatMode.RETURN_HOME) return false;
        BlockPos home = cat.getHomeBedPos();
        if (home == null) return false;

        return cat.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D) > 2.5D;
    }

    @Override
    public void start() {
        BlockPos home = cat.getHomeBedPos();
        if (home != null) {
            cat.setOrderedToSit(false);
            cat.getNavigation().moveTo(home.getX() + 0.5D, home.getY() + 0.5D, home.getZ() + 0.5D, 1.35D);
            recalcPathTimer = 0;
        }
    }

    @Override
    public void tick() {
        BlockPos home = cat.getHomeBedPos();
        if (home == null) return;

        cat.getLookControl().setLookAt(home.getX() + 0.5D, home.getY() + 0.5D, home.getZ() + 0.5D, 20.0F, 20.0F);

        if (--recalcPathTimer <= 0) {
            recalcPathTimer = 20;
            cat.getNavigation().moveTo(home.getX() + 0.5D, home.getY() + 0.5D, home.getZ() + 0.5D, 1.35D);
        }
    }

    @Override
    public void stop() {
        cat.getNavigation().stop();
        BlockPos home = cat.getHomeBedPos();
        if (home != null && cat.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D) <= 3.0D) {
            cat.setOrderedToSit(true); // Rest at home
        }
    }
}
