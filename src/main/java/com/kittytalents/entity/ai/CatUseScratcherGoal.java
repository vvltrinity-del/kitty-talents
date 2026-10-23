package com.kittytalents.entity.ai;

import com.kittytalents.block.CatScratcherBlock;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class CatUseScratcherGoal extends Goal {
    private final TalentedCat cat;
    private BlockPos scratcherPos = null;
    private int scratchTicks = 0;
    private int approachTicks = 0;
    private int cooldown = 200;

    public CatUseScratcherGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        if (cat.getCatMode() == CatMode.DOCILE || cat.getCatMode() == CatMode.STAY) return false;
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        BlockPos catPos = cat.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(catPos.offset(-8, -2, -8), catPos.offset(8, 2, 8))) {
            if (cat.level().getBlockState(pos).getBlock() instanceof CatScratcherBlock) {
                scratcherPos = pos.immutable();
                return true;
            }
        }
        cooldown = 100;
        return false;
    }

    @Override
    public void start() {
        if (scratcherPos != null) {
            cat.getNavigation().moveTo(scratcherPos.getX() + 0.5D, scratcherPos.getY(), scratcherPos.getZ() + 0.5D, 1.15D);
            scratchTicks = 0;
            approachTicks = 0;
        }
    }

    @Override
    public void tick() {
        if (scratcherPos == null) return;
        cat.getLookControl().setLookAt(scratcherPos.getX() + 0.5D, scratcherPos.getY() + 0.5D, scratcherPos.getZ() + 0.5D);

        double distSq = cat.distanceToSqr(scratcherPos.getX() + 0.5D, scratcherPos.getY(), scratcherPos.getZ() + 0.5D);

        if (distSq < 3.5D) {
            scratchTicks++;
            if (scratchTicks % 15 == 0) {
                cat.playSound(SoundEvents.WOOD_HIT, 0.8F, 1.6F);
                cat.playSound(SoundEvents.CAT_PURREOW, 0.7F, 1.2F);
                if (cat.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, scratcherPos.getX() + 0.5D, scratcherPos.getY() + 0.8D, scratcherPos.getZ() + 0.5D, 5, 0.2D, 0.2D, 0.2D, 0.05D);
                }
            }

            if (scratchTicks >= 60) {
                // Sharpened claws buff!
                cat.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 1, false, false, true));
                cat.playSound(SoundEvents.PLAYER_LEVELUP, 0.5F, 1.8F);
                if (cat.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, cat.getX(), cat.getY() + 0.5D, cat.getZ(), 6, 0.25D, 0.25D, 0.25D, 0.05D);
                }
                cooldown = 1200; // 1 minute before scratching again
                scratcherPos = null;
            }
        } else {
            // Approaching scratcher: increment timeout counter
            approachTicks++;
            if (approachTicks > 80) { // Scratcher timeout fix: abort if unreachable after 4 seconds
                cooldown = 300;
                stop();
                return;
            }
            if (cat.getNavigation().isDone() && approachTicks % 15 == 0) {
                cat.getNavigation().moveTo(scratcherPos.getX() + 0.5D, scratcherPos.getY(), scratcherPos.getZ() + 0.5D, 1.15D);
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) return false;
        if (scratcherPos == null || scratchTicks >= 60) return false;
        // Scratcher timeout fix: abort if approach takes too long
        if (approachTicks > 80 && scratchTicks == 0) return false;
        return true;
    }

    @Override
    public void stop() {
        scratcherPos = null;
        scratchTicks = 0;
        approachTicks = 0;
    }
}
