package com.kittytalents.ai.goal;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Automatic tactical retreat (replaces the old RETREAT mode). In modes that allow it, a badly hurt cat breaks off,
 * drops its target and runs to its owner (or away from its attacker), then re-engages once it has recovered.
 */
public class CatRetreatGoal extends Goal {
    public static final float RETREAT_BELOW = 0.30F;
    public static final float RECOVER_AT = 0.55F;
    private static final int MAX_TICKS = 200;
    private static final int COOLDOWN = 200;

    private final TalentedCat cat;
    private int ticks;
    private int repath;
    private long readyAt;

    public CatRetreatGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private float healthFraction() {
        return cat.getHealth() / Math.max(1.0F, cat.getMaxHealth());
    }

    private boolean inDanger() {
        return cat.getTarget() != null || (cat.getLastHurtByMob() != null
                && cat.tickCount - cat.getLastHurtByMobTimestamp() < 100);
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || !cat.getCatMode().retreatsWhenHurt()) return false;
        if (cat.getTrait() == com.kittytalents.core.living.CatTrait.BRAVE) return false; // brave cats stand their ground
        if (cat.level().getGameTime() < readyAt) return false;
        return healthFraction() < RETREAT_BELOW && inDanger();
    }

    @Override
    public boolean canContinueToUse() {
        return !cat.isTwined() && !cat.isIncapacitated() && ticks < MAX_TICKS && healthFraction() < RECOVER_AT;
    }

    @Override
    public void start() {
        ticks = 0;
        repath = 0;
        cat.setRetreating(true);
        cat.setTarget(null);
    }

    @Override
    public void tick() {
        ticks++;
        if (--repath > 0) return;
        repath = 10;
        LivingEntity owner = cat.getOwner();
        if (owner != null && owner.level() == cat.level()) {
            if (cat.distanceToSqr(owner) > 9.0D) cat.getNavigation().moveTo(owner, 1.5D);
            return;
        }
        LivingEntity threat = cat.getLastHurtByMob();
        if (threat != null) {
            Vec3 away = DefaultRandomPos.getPosAway(cat, 12, 4, threat.position());
            if (away != null) cat.getNavigation().moveTo(away.x, away.y, away.z, 1.5D);
            return;
        }
        BlockPos home = cat.getHomeBedPos();
        if (home != null && com.kittytalents.util.CatTeleport.isHomeBedHere(cat)) { // 5.1.1: not to a bed in another dimension
            cat.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 1.5D);
        }
    }

    @Override
    public void stop() {
        cat.setRetreating(false);
        cat.getNavigation().stop();
        readyAt = cat.level().getGameTime() + COOLDOWN;
    }
}
