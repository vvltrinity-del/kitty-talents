package com.kittytalents.entity.ai;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * R11: Multi-mode Follow, Escort, Defensive, and Bodyguard Behavior.
 * Dynamically adjusts tether distance, positioning, and speed based on active CatMode.
 */
public class CatFollowAndEscortGoal extends Goal {
    private final TalentedCat cat;
    private LivingEntity owner;
    private int timeToRecalcPath = 0;

    public CatFollowAndEscortGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) {
            return false;
        }

        CatMode mode = cat.getCatMode();
        if (!mode.shouldFollowOwner() && mode != CatMode.GUARD) {
            return false;
        }

        LivingEntity currentOwner = cat.getOwner();
        if (currentOwner == null || currentOwner.isSpectator()) {
            return false;
        }

        double distSq = cat.distanceToSqr(currentOwner);
        double minTriggerDist = switch (mode) {
            case DEFENSIVE, PROTECT -> 16.0D; // Keep tight tether (4 blocks)
            case ESCORT -> 20.0D;
            case SUPPORT -> 25.0D;
            case TACTICAL -> 36.0D; // 6 blocks
            default -> 49.0D; // 7 blocks
        };

        return distSq >= minTriggerDist;
    }

    @Override
    public boolean canContinueToUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        if (owner == null || !owner.isAlive()) return false;
        if (!cat.getCatMode().shouldFollowOwner() && cat.getCatMode() != CatMode.GUARD) return false;

        double distSq = cat.distanceToSqr(owner);
        double stopDistSq = switch (cat.getCatMode()) {
            case DEFENSIVE, PROTECT -> 9.0D; // Stop within 3 blocks
            case ESCORT -> 16.0D;
            case TACTICAL -> 25.0D;
            default -> 12.0D;
        };

        return distSq > stopDistSq;
    }

    @Override
    public void start() {
        this.owner = cat.getOwner();
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.cat.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) return;

        cat.getLookControl().setLookAt(owner, 10.0F, cat.getMaxHeadXRot());

        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = 10;

            Vec3 dest = calculateTargetPosition();

            // Match owner sprinting speed
            double speed = 1.25D;
            if (owner instanceof Player p && p.isSprinting()) {
                speed = 1.6D;
            }

            cat.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
        }

        // Emergency teleport if excessively far (> 24 blocks) and safe
        if (cat.distanceToSqr(owner) > 576.0D && !cat.isLeashed() && !cat.isPassenger()) {
            teleportToOwner();
        }
    }

    private Vec3 calculateTargetPosition() {
        CatMode mode = cat.getCatMode();
        Vec3 ownerPos = owner.position();

        switch (mode) {
            case ESCORT -> {
                // Walk 4 blocks ahead of owner's look direction
                Vec3 look = owner.getLookAngle();
                Vec3 ahead = ownerPos.add(look.x * 4.0D, 0, look.z * 4.0D);
                if (CatCliffSafetyHelper.isSafeToLeap(cat, ahead)) {
                    return ahead;
                }
            }
            case PROTECT, DEFENSIVE -> {
                // Position between owner and owner's last attacker, or close at side
                LivingEntity threat = owner.getLastHurtByMob();
                if (threat != null && threat.isAlive()) {
                    Vec3 between = ownerPos.add(threat.position().subtract(ownerPos).normalize().scale(2.5D));
                    if (CatCliffSafetyHelper.isSafeToLeap(cat, between)) {
                        return between;
                    }
                }
                return ownerPos.add(1.5D, 0, 1.5D);
            }
            case TACTICAL -> {
                // Flanking position 6 blocks to the side
                Vec3 look = owner.getLookAngle();
                Vec3 flank = ownerPos.add(-look.z * 5.0D, 0, look.x * 5.0D);
                if (CatCliffSafetyHelper.isSafeToLeap(cat, flank)) {
                    return flank;
                }
            }
            default -> {}
        }

        return ownerPos;
    }

    private void teleportToOwner() {
        BlockPos ownerBlock = owner.blockPosition();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos candidate = ownerBlock.offset(dx, dy, dz);
                    if (cat.level().getBlockState(candidate).isAir() &&
                        cat.level().getBlockState(candidate.above()).isAir() &&
                        !cat.level().getBlockState(candidate.below()).isAir() &&
                        !CatCliffSafetyHelper.isHazardous(cat.level(), candidate.below())) {
                        cat.teleportTo(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
                        cat.getNavigation().stop();
                        return;
                    }
                }
            }
        }
    }
}
