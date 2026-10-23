package com.kittytalents.entity.ai;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * R11: Farm Companion Mode Goal.
 * Guards crops and farm plots, patrols agricultural perimeters, and repels crop-eating pests (e.g. rabbits).
 */
public class CatFarmGoal extends Goal {
    private final TalentedCat cat;
    private BlockPos farmPos = null;
    private int checkCooldown = 0;

    public CatFarmGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        if (cat.getCatMode() != CatMode.FARM) return false;

        if (checkCooldown > 0) {
            checkCooldown--;
            return false;
        }
        checkCooldown = 40;

        // Hunt crop-eating rabbits in farm area
        List<Rabbit> rabbits = cat.level().getEntitiesOfClass(Rabbit.class, cat.getBoundingBox().inflate(12.0D));
        if (!rabbits.isEmpty()) {
            cat.setTarget(rabbits.get(0));
            return false; // Target selector handles attack
        }

        // Search for nearby farmland/crops
        BlockPos current = cat.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(current.offset(-8, -2, -8), current.offset(8, 2, 8))) {
            if (cat.level().getBlockState(pos).is(Blocks.FARMLAND) || cat.level().getBlockState(pos).getBlock() instanceof CropBlock) {
                this.farmPos = pos.immutable();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        if (cat.getCatMode() != CatMode.FARM || farmPos == null) return false;
        return cat.distanceToSqr(farmPos.getX() + 0.5D, farmPos.getY(), farmPos.getZ() + 0.5D) > 4.0D;
    }

    @Override
    public void start() {
        if (farmPos != null) {
            cat.getNavigation().moveTo(farmPos.getX() + 0.5D, farmPos.getY() + 1.0D, farmPos.getZ() + 0.5D, 1.15D);
        }
    }

    @Override
    public void stop() {
        this.farmPos = null;
        this.cat.getNavigation().stop();
    }
}
