package com.kittytalents.entity.ai;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public class CatMoleMiningGoal extends Goal {
    private final TalentedCat cat;
    private int miningCooldown = 0;
    private int digTicks = 0;

    public CatMoleMiningGoal(TalentedCat cat) {
        this.cat = cat;
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping() || cat.isVehicle()) {
            return false;
        }
        int moleLvl = cat.getTalentLevel(Talents.MOLE_CAT);
        boolean isMineMode = cat.getCatMode() == CatMode.MINE;
        if (moleLvl <= 0 && !isMineMode) {
            return false;
        }
        if (miningCooldown > 0) {
            miningCooldown--;
            return false;
        }

        // Clamp random bound to prevent IllegalArgumentException when moleLvl >= 10
        int bound = Math.max(1, 150 - (moleLvl * 15));
        if (!isMineMode && cat.getRandom().nextInt(bound) != 0) {
            return false;
        }
        return cat.onGround();
    }

    @Override
    public void start() {
        this.digTicks = 40;
        cat.playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 1.2F);
    }

    @Override
    public boolean canContinueToUse() {
        return digTicks > 0 && !cat.isIncapacitated() && !cat.isOrderedToSit() && !cat.isSleeping() && !cat.isTwined();
    }

    @Override
    public void tick() {
        digTicks--;
        if (cat.level() instanceof ServerLevel serverLevel) {
            BlockPos ground = cat.blockPosition().below();
            BlockState state = cat.level().getBlockState(ground);
            if (!state.isAir()) {
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), cat.getX(), cat.getY() + 0.1D, cat.getZ(), 5, 0.2D, 0.1D, 0.2D, 0.05D);
            }
        }

        if (digTicks == 0 && !cat.level().isClientSide) {
            int moleLvl = Math.max(1, cat.getTalentLevel(Talents.MOLE_CAT));
            ItemStack ore;
            int roll = cat.getRandom().nextInt(100);

            if (roll < 40) {
                ore = new ItemStack(Items.COAL, 1 + cat.getRandom().nextInt(2));
            } else if (roll < 70) {
                ore = new ItemStack(Items.RAW_IRON, 1);
            } else if (roll < 85) {
                ore = new ItemStack(Items.RAW_COPPER, 1 + cat.getRandom().nextInt(2));
            } else if (roll < 93 && moleLvl >= 2) {
                ore = new ItemStack(Items.RAW_GOLD, 1);
            } else if (roll < 98 && moleLvl >= 3) {
                ore = new ItemStack(Items.LAPIS_LAZULI, 2);
            } else if (moleLvl >= 4 && roll < 99) {
                ore = new ItemStack(Items.DIAMOND, 1);
            } else if (moleLvl >= 5) {
                ore = new ItemStack(Items.NETHERITE_SCRAP, 1);
            } else {
                ore = new ItemStack(Items.COBBLESTONE, 2);
            }

            if (cat.getTalentLevel(Talents.KITTY_PACK) > 0) {
                ore = cat.getCatPack().addItem(ore);
            }

            if (!ore.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(cat.level(), cat.getX(), cat.getY() + 0.3D, cat.getZ(), ore);
                cat.level().addFreshEntity(itemEntity);
            }

            cat.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 1.5F);
            cat.increaseHunger(5);
            this.miningCooldown = Math.max(40, 300 - (moleLvl * 30));
        }
    }

    @Override
    public void stop() {
        this.digTicks = 0;
    }
}
