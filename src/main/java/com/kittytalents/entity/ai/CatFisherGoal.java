package com.kittytalents.entity.ai;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumSet;

public class CatFisherGoal extends Goal {
    private final TalentedCat cat;
    private BlockPos waterPos;
    private int fishingTimer = 0;

    public CatFisherGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) {
            return false;
        }
        int fisherLevel = cat.getTalentLevel(Talents.FISHER_CAT);
        boolean isFishMode = cat.getCatMode() == CatMode.FISH;
        if (fisherLevel <= 0 && !isFishMode) {
            return false;
        }

        int bound = Math.max(1, 100 - (fisherLevel * 10));
        if (!isFishMode && cat.getRandom().nextInt(bound) != 0) {
            return false;
        }

        BlockPos current = cat.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(current.offset(-3, -1, -3), current.offset(3, 0, 3))) {
            if (cat.level().getBlockState(pos).is(Blocks.WATER)) {
                this.waterPos = pos.immutable();
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        this.fishingTimer = 40 + cat.getRandom().nextInt(40);
    }

    @Override
    public boolean canContinueToUse() {
        return fishingTimer > 0 && waterPos != null && !cat.isIncapacitated() && !cat.isOrderedToSit() && !cat.isSleeping() && !cat.isTwined();
    }

    @Override
    public void tick() {
        fishingTimer--;
        if (waterPos != null) {
            cat.getLookControl().setLookAt(waterPos.getX() + 0.5D, waterPos.getY() + 0.5D, waterPos.getZ() + 0.5D);
        }

        if (fishingTimer == 0 && waterPos != null && !cat.level().isClientSide) {
            int level = Math.max(1, cat.getTalentLevel(Talents.FISHER_CAT));
            ItemStack caught;
            int roll = cat.getRandom().nextInt(100);
            if (roll < 50) {
                caught = new ItemStack(Items.COD);
            } else if (roll < 80) {
                caught = new ItemStack(Items.SALMON);
            } else if (roll < 95 && level >= 3) {
                caught = new ItemStack(Items.TROPICAL_FISH);
            } else if (level >= 4) {
                caught = new ItemStack(Items.PRISMARINE_SHARD);
            } else {
                caught = new ItemStack(Items.PUFFERFISH);
            }

            if (cat.getTalentLevel(Talents.KITTY_PACK) > 0) {
                caught = cat.getCatPack().addItem(caught);
            }

            if (!caught.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(cat.level(), cat.getX(), cat.getY() + 0.5D, cat.getZ(), caught);
                itemEntity.setDefaultPickUpDelay();
                cat.level().addFreshEntity(itemEntity);
            }

            cat.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.6F, 1.2F);
            cat.increaseHunger(10);
        }
    }

    @Override
    public void stop() {
        this.fishingTimer = 0;
        this.waterPos = null;
    }
}
