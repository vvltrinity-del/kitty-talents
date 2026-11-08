package com.kittytalents.entity.ai;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CatComfortGoal extends Goal {
    private final TalentedCat cat;
    private int purrTimer = 0;

    public CatComfortGoal(TalentedCat cat) {
        this.cat = cat;
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated()) {
            return false;
        }
        int pillowLevel = cat.getTalentLevel(Talents.PILLOW_CAT);
        if (pillowLevel <= 0) {
            return false;
        }
        BlockPos pos = cat.blockPosition();
        BlockState stateOn = cat.level().getBlockState(pos);
        BlockState stateBelow = cat.level().getBlockState(pos.below());
        return isComfortable(stateOn) || isComfortable(stateBelow);
    }

    private boolean isComfortable(BlockState state) {
        return state.is(BlockTags.BEDS) || state.is(Blocks.CHEST) || state.is(Blocks.BARREL) || state.is(Blocks.FURNACE);
    }

    @Override
    public void tick() {
        int pillowLevel = cat.getTalentLevel(Talents.PILLOW_CAT);
        purrTimer++;
        if (purrTimer % (60 / Math.max(1, pillowLevel)) == 0) {
            if (cat.getHealth() < cat.getMaxHealth()) {
                cat.heal(1.0F * pillowLevel);
            }
        }
        if (purrTimer % 80 == 0) {
            cat.playSound(SoundEvents.CAT_PURREOW, 0.5F, 1.0F);
        }
    }
}
