package com.kittytalents.ai.goal;

import com.kittytalents.block.CatTreeBlock;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Cat Tree perching (4.0). A wandering or guarding cat now and then walks to a nearby Cat Tree, hops onto the perch,
 * sits and later curls up. While perched it keeps watch: hostile mobs within 16 blocks glow so the owner sees them.
 * Any order, target or mode change ends it.
 */
public class CatPerchGoal extends Goal {
    private static final int SEARCH_RADIUS = 12;
    private static final int WATCH_RADIUS = 16;
    /** One search attempt per this many canUse calls on average (tests set it to 1). */
    static int searchChance = 240;

    private final TalentedCat cat;
    private BlockPos treeUpper;
    private Vec3 perch;
    private boolean onPerch;
    private int ticks, duration, cooldownUntil;

    public CatPerchGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    /** For GameTests: make the next canUse search right away (1) or restore the normal rate. */
    public static void setSearchChanceForTests(int chance) {
        searchChance = Math.max(1, chance);
    }

    private boolean allowed() {
        CatMode mode = cat.getCatMode();
        return (mode == CatMode.WANDER || mode == CatMode.GUARD) && !cat.isOrderedToSit() && !cat.isTwined()
                && !cat.isIncapacitated() && !cat.isPassenger() && !cat.isVehicle() && cat.getTarget() == null
                && !cat.isBaby();
    }

    @Override
    public boolean canUse() {
        if (cat.level().isClientSide || !allowed() || cat.tickCount < cooldownUntil) return false;
        if (searchChance > 1 && cat.getRandom().nextInt(searchChance) != 0) return false;
        BlockPos found = findFreeTree();
        if (found == null) {
            cooldownUntil = cat.tickCount + 200;
            return false;
        }
        treeUpper = found;
        perch = CatTreeBlock.perchOf(cat.level().getBlockState(found), found);
        return true;
    }

    private BlockPos findFreeTree() {
        BlockPos origin = cat.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-SEARCH_RADIUS, -3, -SEARCH_RADIUS), origin.offset(SEARCH_RADIUS, 4, SEARCH_RADIUS))) {
            BlockState state = cat.level().getBlockState(p);
            if (!state.is(ModBlocks.CAT_TREE.get()) || state.getValue(CatTreeBlock.HALF) != DoubleBlockHalf.UPPER) continue;
            Vec3 spot = CatTreeBlock.perchOf(state, p);
            if (occupied(spot)) continue;
            double d = cat.distanceToSqr(spot);
            if (d < bestDist) {
                bestDist = d;
                best = p.immutable();
            }
        }
        return best;
    }

    private boolean occupied(Vec3 spot) {
        return !cat.level().getEntitiesOfClass(TalentedCat.class, new AABB(spot, spot).inflate(0.6D, 0.8D, 0.6D), c -> c != cat).isEmpty();
    }

    private boolean treeStillThere() {
        BlockState state = cat.level().getBlockState(treeUpper);
        return state.is(ModBlocks.CAT_TREE.get()) && state.getValue(CatTreeBlock.HALF) == DoubleBlockHalf.UPPER;
    }

    @Override
    public void start() {
        ticks = 0;
        onPerch = false;
        duration = 600 + cat.getRandom().nextInt(600);
        cat.getNavigation().moveTo(treeUpper.getX() + 0.5D, treeUpper.getY() - 1, treeUpper.getZ() + 0.5D, 1.0D);
    }

    @Override
    public boolean canContinueToUse() {
        return allowed() && treeUpper != null && treeStillThere() && ticks < duration && (onPerch || ticks < 240);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        if (!onPerch) {
            double dx = cat.getX() - perch.x, dz = cat.getZ() - perch.z;
            if (dx * dx + dz * dz < 2.6D && Math.abs(cat.getY() - (perch.y - 1)) < 2.5D) {
                hopUp();
            } else if (ticks % 20 == 0 && cat.getNavigation().isDone()) {
                cat.getNavigation().moveTo(perch.x, treeUpper.getY() - 1, perch.z, 1.0D);
            }
            return;
        }
        // Stay put on the perch.
        cat.getNavigation().stop();
        if (cat.distanceToSqr(perch) > 0.09D) cat.moveTo(perch.x, perch.y, perch.z, cat.getYRot(), cat.getXRot());
        cat.setDeltaMovement(0.0D, Math.min(0.0D, cat.getDeltaMovement().y), 0.0D);
        if (ticks % 200 == 100) cat.setLying(true);
        if (ticks % 40 == 0) keepWatch();
    }

    private void hopUp() {
        if (occupied(perch)) {
            ticks = duration; // someone took it; give up
            return;
        }
        onPerch = true;
        cat.getNavigation().stop();
        cat.moveTo(perch.x, perch.y, perch.z, cat.getYRot(), 0.0F);
        cat.setInSittingPose(true);
        cat.playSound(SoundEvents.CAT_PURR, 0.6F, 1.0F);
    }

    /** "Watching over the base": hostiles nearby glow for a few seconds. */
    private void keepWatch() {
        for (LivingEntity e : cat.level().getEntitiesOfClass(LivingEntity.class, cat.getBoundingBox().inflate(WATCH_RADIUS),
                e -> e instanceof Enemy && e.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false));
        }
    }

    public boolean isPerched() {
        return onPerch;
    }

    @Override
    public void stop() {
        if (onPerch) {
            cat.setLying(false);
            cat.setInSittingPose(false);
        }
        onPerch = false;
        treeUpper = null;
        cooldownUntil = cat.tickCount + 1200;
    }
}
