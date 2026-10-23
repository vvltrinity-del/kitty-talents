package com.kittytalents.entity.ai;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * R13: Feline Grooming & Resting Behavior.
 * When idle, peaceful cats groom their paws and coat, purring softly and cleansing debuffs.
 */
public class CatGroomGoal extends Goal {
    private final TalentedCat cat;
    private int groomTicks = 0;
    private int cooldown = 300;

    public CatGroomGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isSleeping() || cat.getTarget() != null) return false;
        if (cat.getCatMode().isCombatMode()) return false;
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        // Trigger randomly when idle, healthy, and on ground
        if (cat.getHealth() >= cat.getMaxHealth() * 0.8F && cat.onGround()) {
            return cat.getRandom().nextInt(200) == 0;
        }
        return false;
    }

    @Override
    public void start() {
        this.groomTicks = 60 + cat.getRandom().nextInt(40);
        cat.getNavigation().stop();
        cat.playSound(SoundEvents.CAT_PURREOW, 0.6F, 1.3F);
    }

    @Override
    public boolean canContinueToUse() {
        return groomTicks > 0 && !cat.isIncapacitated() && !cat.isSleeping() && cat.getTarget() == null && !cat.isTwined();
    }

    @Override
    public void tick() {
        groomTicks--;
        if (groomTicks % 25 == 0) {
            cat.playSound(ModSounds.CAT_GROOMING.get(), 0.7F, 1.0F);
            cat.playSound(SoundEvents.CAT_PURR, 0.6F, 1.1F);
            if (cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, cat.getX(), cat.getY() + 0.3D, cat.getZ(), 2, 0.15D, 0.1D, 0.15D, 0.02D);
            }
        }

        if (groomTicks == 0) {
            // Cleanse negative status effects (Poison, Slowness, Weakness)
            cat.removeEffect(MobEffects.POISON);
            cat.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            cat.removeEffect(MobEffects.WEAKNESS);
            cooldown = 600; // 30 seconds cooldown
        }
    }

    @Override
    public void stop() {
        this.groomTicks = 0;
    }
}
