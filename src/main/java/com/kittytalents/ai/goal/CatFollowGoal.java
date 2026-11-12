package com.kittytalents.ai.goal;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.ai.CatCliffSafetyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Owner tether for FOLLOW / TACTICAL / SUPPORT (the only follow goal; vanilla FollowOwnerGoal is no longer added).
 * FOLLOW stays at the owner's side, SUPPORT closer, TACTICAL at a flanking offset. While fighting, the cat only
 * breaks off to follow if the owner gets far away.
 */
public class CatFollowGoal extends Goal {
    private final TalentedCat cat;
    private LivingEntity owner;
    private int repath;
    /** Stuck detection: position sampled every 20 ticks; two samples without progress = stuck. */
    private net.minecraft.world.phys.Vec3 lastSample;
    private int stuckSamples;

    public CatFollowGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private static double startDist(CatMode mode) {
        return mode.followStartDistance();
    }

    private static double stopDist(CatMode mode) {
        return mode.followStopDistance();
    }

    /** While a fight is on, only follow once the owner is this far away. */
    private static final double COMBAT_LEASH = 16.0D;

    private boolean blocked() {
        return cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping() || cat.isPassenger()
                || !cat.getCatMode().followsOwner();
    }

    private boolean fighting() {
        return cat.getTarget() != null && cat.getTarget().isAlive();
    }

    @Override
    public boolean canUse() {
        if (blocked()) return false;
        LivingEntity o = cat.getOwner();
        if (o == null || o.isSpectator() || o.level() != cat.level()) return false;
        double d = cat.distanceTo(o);
        return fighting() ? d > COMBAT_LEASH : d > startDist(cat.getCatMode());
    }

    @Override
    public boolean canContinueToUse() {
        if (blocked() || owner == null || !owner.isAlive() || owner.level() != cat.level()) return false;
        double d = cat.distanceTo(owner);
        return fighting() ? d > COMBAT_LEASH * 0.6D : d > stopDist(cat.getCatMode());
    }

    @Override
    public void start() {
        owner = cat.getOwner();
        repath = 0;
        pathedTo = null;
        lastSample = cat.position();
        stuckSamples = 0;
    }

    @Override
    public void stop() {
        owner = null;
        cat.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) return;
        cat.getLookControl().setLookAt(owner, 10.0F, cat.getMaxHeadXRot());
        if (cat.distanceToSqr(owner) > 24.0D * 24.0D && !cat.isLeashed()) {
            teleportNearOwner();
            return;
        }
        // Stuck behind foliage or a dead-end path: the vanilla pathfinder keeps retrying the same blocked route,
        // so if the cat has barely moved for 2 seconds while the owner is 6+ blocks away, hop to the owner.
        if (cat.tickCount % 20 == 0) {
            boolean noProgress = cat.position().distanceToSqr(lastSample) < 0.25D;
            stuckSamples = noProgress && cat.distanceToSqr(owner) > 36.0D ? stuckSamples + 1 : 0;
            lastSample = cat.position();
            if (stuckSamples >= 2 && !cat.isLeashed()) {
                stuckSamples = 0;
                teleportNearOwner();
                return;
            }
        }
        if (--repath > 0) return;
        repath = 10;
        // 5.0 performance: a new path only when the owner has moved, or the old path is done. Re-pathing every
        // 10 ticks to a standing owner was the largest server cost of a group of following cats.
        Vec3 dest = destination();
        boolean moved = pathedTo == null || pathedTo.distanceToSqr(dest) > 1.5D * 1.5D;
        if (!moved && !cat.getNavigation().isDone()) return;
        pathedTo = dest;
        double speed = owner instanceof Player p && p.isSprinting() ? 1.6D : 1.25D;
        cat.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
    }

    /** Where the current path leads (null: none yet). */
    private Vec3 pathedTo;

    private Vec3 destination() {
        Vec3 o = owner.position();
        if (cat.getCatMode() == CatMode.TACTICAL) {
            Vec3 look = owner.getLookAngle();
            Vec3 flank = o.add(-look.z * 5.0D, 0, look.x * 5.0D);
            if (CatCliffSafetyHelper.isSafeToLeap(cat, flank)) return flank;
        }
        return o;
    }

    private void teleportNearOwner() {
        BlockPos base = owner.blockPosition();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (Math.abs(dx) < 1 && Math.abs(dz) < 1) continue; // not inside the owner
                    BlockPos p = base.offset(dx, dy, dz);
                    if (cat.level().getBlockState(p).isAir() && cat.level().getBlockState(p.above()).isAir()
                            && !cat.level().getBlockState(p.below()).isAir()
                            && !CatCliffSafetyHelper.isHazardous(cat.level(), p.below())) {
                        cat.teleportTo(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D);
                        cat.getNavigation().stop();
                        return;
                    }
                }
            }
        }
    }
}
