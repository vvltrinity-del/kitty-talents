package com.kittytalents.entity.ai;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * Evaluates terrain safety for cat movement, jumping, and pouncing to eliminate
 * suicidal cliff diving, void leaps, and lava plunging.
 */
public class CatCliffSafetyHelper {

    /** Maximum safe fall distance without Cushion Paws or Sky Feline. */
    public static final int MAX_SAFE_DROP_BLOCKS = 3;

    /**
     * Checks if leaping or moving towards a target position is safe.
     * Validates both the landing position and the trajectory over chasms/lava.
     */
    public static boolean isSafeToLeap(TalentedCat cat, Vec3 targetPos) {
        if (cat == null || targetPos == null) return false;
        Level level = cat.level();

        // Sky Feline cats can glide/fly safely
        if (cat.getTalentLevel(Talents.SKY_FELINE) > 0) {
            return true;
        }

        BlockPos targetBlock = BlockPos.containing(targetPos);

        // Check if landing block or below is dangerous (lava, void, fire)
        if (isHazardous(level, targetBlock)) {
            return false;
        }

        // Find solid ground beneath the target position
        int dropDistance = findDropDistance(level, targetBlock);
        if (dropDistance < 0) {
            // No ground found (void or extreme abyss)
            return false;
        }

        int cushionLevel = cat.getTalentLevel(Talents.CUSHION_PAWS);
        int maxAllowedDrop = MAX_SAFE_DROP_BLOCKS + (cushionLevel * 3);

        if (dropDistance > maxAllowedDrop) {
            return false;
        }

        // Mid-point chasm check: make sure cat doesn't leap into a ravine
        Vec3 mid = cat.position().add(targetPos).scale(0.5D);
        BlockPos midBlock = BlockPos.containing(mid);
        if (isHazardous(level, midBlock)) {
            return false;
        }

        return true;
    }

    /**
     * Checks whether the ground directly ahead of the cat is safe for movement/jumping.
     * Prevents walking or leaping off tall cliffs or into hazards.
     */
    public static boolean isSafeStepAhead(TalentedCat cat) {
        if (cat == null) return false;
        Level level = cat.level();

        Direction dir = cat.getDirection();
        BlockPos current = cat.blockPosition();
        BlockPos ahead = current.relative(dir);

        // If block ahead is solid or has ground immediately beneath it, it is safe
        if (!level.isEmptyBlock(ahead) || !level.isEmptyBlock(ahead.below())) {
            return !isHazardous(level, ahead) && !isHazardous(level, ahead.below());
        }

        // Both ahead and ahead.below are empty: check if this is a 1-block gap jump with solid landing
        BlockPos acrossGap = ahead.relative(dir);
        if (!level.isEmptyBlock(acrossGap.below()) && !isHazardous(level, acrossGap.below())) {
            // Safe 1-block horizontal gap across which cat can jump
            return true;
        }

        // Otherwise check drop depth: if drop is dangerous, it is a suicidal cliff
        int drop = findDropDistance(level, ahead);
        if (drop < 0 || drop > MAX_SAFE_DROP_BLOCKS + (cat.getTalentLevel(Talents.CUSHION_PAWS) * 3)) {
            return false; // Suicidal cliff!
        }

        return !isHazardous(level, ahead.below(drop));
    }

    /**
     * Finds the drop distance to solid ground beneath pos. Returns -1 if abyss/void.
     */
    public static int findDropDistance(Level level, BlockPos startPos) {
        for (int dy = 0; dy <= 24; dy++) {
            BlockPos check = startPos.below(dy);
            if (check.getY() < level.getMinBuildHeight()) {
                return -1; // Void
            }
            BlockState state = level.getBlockState(check);
            if (isHazardous(level, check)) {
                return -1; // Lava or hazard beneath
            }
            if (!state.isAir() && state.isSolid()) {
                return dy;
            }
        }
        return -1; // Drop > 24 blocks
    }

    /**
     * Checks if a position contains lava, fire, or hazardous fluid/blocks.
     */
    public static boolean isHazardous(Level level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        if (fluid.is(Fluids.LAVA) || fluid.is(Fluids.FLOWING_LAVA)) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) ||
               state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.SWEET_BERRY_BUSH) ||
               state.is(Blocks.WITHER_ROSE);
    }
}
