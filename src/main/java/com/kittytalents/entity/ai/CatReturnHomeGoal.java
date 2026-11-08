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

    /** Home Whiskers: +8% travel speed per rank. */
    private double speed() {
        return 1.35D * (1.0D + 0.08D * cat.getTalentLevel(com.kittytalents.talent.Talents.HOME_WHISKERS));
    }

    @Override
    public void start() {
        BlockPos home = cat.getHomeBedPos();
        if (home != null) {
            cat.setOrderedToSit(false);
            // Home Whiskers rank 3+: a cat far from home (same dimension) finds its way back instantly.
            boolean sameDim = cat.getHomeBedDimension() == null || cat.getHomeBedDimension().isEmpty()
                    || cat.getHomeBedDimension().equals(cat.level().dimension().location().toString());
            if (sameDim && cat.getTalentLevel(com.kittytalents.talent.Talents.HOME_WHISKERS) >= 3
                    && cat.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D) > 64.0D * 64.0D) {
                cat.teleportTo(home.getX() + 0.5D, home.getY() + 1.0D, home.getZ() + 0.5D);
            }
            cat.getNavigation().moveTo(home.getX() + 0.5D, home.getY() + 0.5D, home.getZ() + 0.5D, speed());
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
            cat.getNavigation().moveTo(home.getX() + 0.5D, home.getY() + 0.5D, home.getZ() + 0.5D, speed());
        }
    }

    @Override
    public void stop() {
        cat.getNavigation().stop();
        BlockPos home = cat.getHomeBedPos();
        if (home != null && cat.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D) <= 3.0D) {
            // Arrived: CatRestOnBedGoal takes over and curls the cat up on its bed (ordering it to sit here would
            // block that goal, so the cat would only sit).
            if (com.kittytalents.talent.effect.TalentEngine.isMastered(cat, com.kittytalents.talent.Talents.HOME_WHISKERS)) {
                cat.setHealth(cat.getMaxHealth()); // mastery "Hearth": arrives home fully healed
            }
        }
    }
}
