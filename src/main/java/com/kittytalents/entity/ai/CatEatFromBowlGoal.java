package com.kittytalents.entity.ai;

import com.kittytalents.block.CatFoodBowlBlock;
import com.kittytalents.block.CatFoodBowlBlockEntity;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.util.CatFoodHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;

public class CatEatFromBowlGoal extends Goal {
    private final TalentedCat cat;
    private BlockPos bowlPos = null;
    private int checkCooldown = 0;
    private int eatingTicks = 0;

    public CatEatFromBowlGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping() || cat.isTwined() || cat.isPassenger()) return false;
        // Target bowl if hunger < 80% (96/120) OR if damaged (can heal from food)
        boolean needsFood = cat.getHunger() < 96 || cat.getHealth() < cat.getMaxHealth();
        if (!needsFood) return false;

        if (checkCooldown > 0) {
            checkCooldown--;
            return false;
        }

        checkCooldown = 30; // Check every 1.5s
        BlockPos catPos = cat.blockPosition();
        int searchRadius = cat.getHunger() < 40 ? 18 : 12;

        for (BlockPos pos : BlockPos.betweenClosed(catPos.offset(-searchRadius, -2, -searchRadius), catPos.offset(searchRadius, 2, searchRadius))) {
            if (cat.level().getBlockState(pos).getBlock() instanceof CatFoodBowlBlock) {
                BlockEntity be = cat.level().getBlockEntity(pos);
                if (be instanceof CatFoodBowlBlockEntity bowl && !bowl.getFirstEdibleFood(cat).isEmpty()) {
                    bowlPos = pos.immutable();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void start() {
        if (bowlPos != null) {
            double speed = cat.getHunger() < 40 ? 1.35D : 1.15D;
            cat.getNavigation().moveTo(bowlPos.getX() + 0.5D, bowlPos.getY(), bowlPos.getZ() + 0.5D, speed);
            eatingTicks = 0;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping() || cat.isTwined() || cat.isPassenger() || bowlPos == null) return false;
        if (cat.getHunger() >= 118 && cat.getHealth() >= cat.getMaxHealth()) return false;
        BlockEntity be = cat.level().getBlockEntity(bowlPos);
        return be instanceof CatFoodBowlBlockEntity bowl && !bowl.getFirstEdibleFood(cat).isEmpty();
    }

    @Override
    public void tick() {
        if (bowlPos == null) return;

        double distSq = cat.distanceToSqr(bowlPos.getX() + 0.5D, bowlPos.getY(), bowlPos.getZ() + 0.5D);
        cat.getLookControl().setLookAt(bowlPos.getX() + 0.5D, bowlPos.getY() + 0.2D, bowlPos.getZ() + 0.5D, 30.0F, 30.0F);

        if (distSq <= 2.25D) { // Within ~1.5 blocks
            cat.getNavigation().stop();
            eatingTicks++;

            if (eatingTicks % 6 == 0) {
                cat.playSound(SoundEvents.CAT_EAT, 0.7F, 1.0F);
            }

            if (eatingTicks >= 20) { // After 1 second of eating
                eatingTicks = 0;
                BlockEntity be = cat.level().getBlockEntity(bowlPos);
                if (be instanceof CatFoodBowlBlockEntity bowl) {
                    ItemStack consumed = bowl.consumeOneFood(cat);
                    if (!consumed.isEmpty()) {
                        CatFoodHelper.CatFoodValue val = CatFoodHelper.getFoodValues(consumed, cat);
                        cat.applyFood(val.hunger, val.saturation, val.heal);

                        // Visual particles
                        if (cat.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, consumed),
                                    bowlPos.getX() + 0.5D, bowlPos.getY() + 0.35D, bowlPos.getZ() + 0.5D,
                                    6, 0.15D, 0.1D, 0.15D, 0.05D);
                            serverLevel.sendParticles(ParticleTypes.HEART,
                                    cat.getX(), cat.getY() + 0.5D, cat.getZ(),
                                    2, 0.2D, 0.2D, 0.2D, 0.02D);
                        }

                        cat.playSound(SoundEvents.CAT_PURREOW, 0.8F, 1.2F);
                    }
                }
            }
        } else {
            eatingTicks = 0;
            if (cat.getNavigation().isDone() && cat.tickCount % 15 == 0) {
                double speed = cat.getHunger() < 40 ? 1.35D : 1.15D;
                cat.getNavigation().moveTo(bowlPos.getX() + 0.5D, bowlPos.getY(), bowlPos.getZ() + 0.5D, speed);
            }
        }
    }

    @Override
    public void stop() {
        bowlPos = null;
        eatingTicks = 0;
    }
}
