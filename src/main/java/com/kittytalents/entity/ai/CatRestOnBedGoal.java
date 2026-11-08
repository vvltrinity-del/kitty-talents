package com.kittytalents.entity.ai;

import com.kittytalents.block.CatBedBlock;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class CatRestOnBedGoal extends Goal {
    private final TalentedCat cat;
    private int restTicks = 0;
    private int checkCooldown = 0;
    private int navThrottle = 0;
    private boolean wasOrderedToSitBeforeResting = false;

    public CatRestOnBedGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined()) return false;
        if (cat.getHomeBedPos() == null) return false;

        // Downed cat pathing fix: incapacitated cats can NEVER pathfind across the world to bed.
        // They can only receive bed nursing if already placed directly on/adjacent to the bed.
        BlockPos bedPos = cat.getHomeBedPos();
        double distSq = cat.distanceToSqr(bedPos.getX() + 0.5D, bedPos.getY(), bedPos.getZ() + 0.5D);
        if (cat.isIncapacitated()) {
            return distSq <= 2.5D && cat.level().getBlockState(bedPos).getBlock() instanceof CatBedBlock;
        }

        // If explicitly told to sit elsewhere in STAY or DOCILE mode far from bed, don't run off to bed
        if (cat.isOrderedToSit() && distSq > 36.0D) {
            return false;
        }

        if (checkCooldown > 0) {
            checkCooldown--;
            return false;
        }
        checkCooldown = 40;

        // Bed rest trigger conditions:
        // 1. Injured (needs healing)
        // 2. Nighttime
        // 3. RETURN_HOME mode
        // 4. DOCILE / STAY mode near bed when dusk/night or damaged
        boolean needsHealing = cat.getHealth() < cat.getMaxHealth();
        boolean isNight = cat.level().isNight();
        boolean isHomeMode = cat.getCatMode() == CatMode.RETURN_HOME;
        boolean shouldRest = needsHealing || isNight || isHomeMode ||
                ((cat.getCatMode() == CatMode.DOCILE || cat.getCatMode() == CatMode.STAY) && (isNight || needsHealing));

        if (shouldRest && distSq < 400.0D) {
            return cat.level().getBlockState(bedPos).getBlock() instanceof CatBedBlock;
        }

        return false;
    }

    @Override
    public void start() {
        this.wasOrderedToSitBeforeResting = cat.isOrderedToSit();
        BlockPos bedPos = cat.getHomeBedPos();
        if (bedPos != null && !cat.isIncapacitated()) {
            cat.getNavigation().moveTo(bedPos.getX() + 0.5D, bedPos.getY(), bedPos.getZ() + 0.5D, 1.2D);
            restTicks = 0;
            navThrottle = 0;
        }
    }

    @Override
    public void tick() {
        BlockPos bedPos = cat.getHomeBedPos();
        if (bedPos == null) return;

        double distSqr = cat.distanceToSqr(bedPos.getX() + 0.5D, bedPos.getY(), bedPos.getZ() + 0.5D);
        if (distSqr < 2.5D) {
            cat.getNavigation().stop();
            cat.setOrderedToSit(true);
            restTicks++;

            // Rest & Healing bonus
            if (restTicks % 40 == 0) {
                cat.heal(1.5F);
                cat.playSound(SoundEvents.CAT_PURR, 0.8F, 1.0F);
                if (cat.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART, cat.getX(), cat.getY() + 0.4D, cat.getZ(), 2, 0.2D, 0.1D, 0.2D, 0.02D);
                }

                // If incapacitated, bed nurses back to health
                if (cat.isIncapacitated() && cat.getHealth() >= 8.0F) {
                    cat.setIncapacitated(false);
                    cat.playSound(SoundEvents.CAT_PURREOW, 1.0F, 1.0F);
                }
            }
        } else if (!cat.isIncapacitated()) {
            navThrottle++;
            if (navThrottle >= 20 || cat.getNavigation().isDone()) {
                navThrottle = 0;
                cat.getNavigation().moveTo(bedPos.getX() + 0.5D, bedPos.getY(), bedPos.getZ() + 0.5D, 1.2D);
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (cat.isTwined()) return false;
        BlockPos bedPos = cat.getHomeBedPos();
        if (bedPos == null) return false;
        if (!(cat.level().getBlockState(bedPos).getBlock() instanceof CatBedBlock)) return false;

        // Incapacitated cats continue nursing if at bed
        if (cat.isIncapacitated()) {
            return cat.distanceToSqr(bedPos.getX() + 0.5D, bedPos.getY(), bedPos.getZ() + 0.5D) <= 2.5D;
        }

        // Needs healing
        if (cat.getHealth() < cat.getMaxHealth()) return true;

        // Nighttime sleeping
        if (cat.level().isNight()) return true;

        // RETURN_HOME mode stays at home
        if (cat.getCatMode() == CatMode.RETURN_HOME) return true;

        // Docile loop fix: Do NOT stay in bed during daytime once healthy!
        // If daytime, full health, and mode is DOCILE or FOLLOW, cat should wake up and stand.
        return false;
    }

    @Override
    public void stop() {
        restTicks = 0;
        navThrottle = 0;

        // Bed resting sit-lock fix:
        // If the cat was NOT originally ordered to sit by the player, and mode is not DOCILE/STAY,
        // restore standing state so the cat can resume following or normal activity.
        if (!wasOrderedToSitBeforeResting && !cat.isIncapacitated() &&
                cat.getCatMode() != CatMode.DOCILE && cat.getCatMode() != CatMode.STAY) {
            cat.setOrderedToSit(false);
        }
    }
}
