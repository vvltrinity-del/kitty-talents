package com.kittytalents.entity.ai;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CatFoxPounceGoal extends Goal {
    private final TalentedCat cat;
    private LivingEntity target;
    private int phase = 0; // 0 = ready, 1 = crouching/preparing, 2 = airborne
    private int crouchTicks = 0;
    private int airborneTicks = 0;
    private int cooldown = 0;

    public CatFoxPounceGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        CatAIInitializer.initializeGoals(cat);
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping() || !cat.getCatMode().canAttack()) {
            return false;
        }
        if (cat.isVehicle()) {
            return false;
        }
        // Require POUNCE talent or HUNTING mode instinct
        if (cat.getTalentLevel(Talents.POUNCE) <= 0 && cat.getCatMode() != CatMode.HUNTING) {
            return false;
        }
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        LivingEntity currentTarget = cat.getTarget();
        if (currentTarget == null || !currentTarget.isAlive()) {
            return false;
        }

        double distSq = cat.distanceToSqr(currentTarget);
        // Pounce when between 3 and 14 blocks away on ground
        if (distSq < 9.0D || distSq > 196.0D || !cat.onGround()) {
            return false;
        }

        // Cliff leaping suicidal check: ensure leap trajectory and landing zone are safe!
        return CatCliffSafetyHelper.isSafeToLeap(cat, currentTarget.position());
    }

    @Override
    public void start() {
        this.target = cat.getTarget();
        this.phase = 1;
        this.crouchTicks = 15;
        this.airborneTicks = 0;
        cat.getNavigation().stop();
        cat.setPose(net.minecraft.world.entity.Pose.CROUCHING);
        cat.playSound(SoundEvents.CAT_HISS, 0.7F, 1.4F);
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && phase != 0 && !cat.isIncapacitated() && !cat.isOrderedToSit() && !cat.isSleeping() && !cat.isTwined();
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) {
            stop();
            return;
        }

        cat.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (phase == 1) {
            // Crouching / preparing
            crouchTicks--;
            cat.setDeltaMovement(0, cat.getDeltaMovement().y, 0);

            if (crouchTicks <= 0) {
                // Secondary safety check before launching: target didn't move over a cliff
                if (!CatCliffSafetyHelper.isSafeToLeap(cat, target.position())) {
                    stop();
                    return;
                }

                // Launch high into the air like a fox!
                this.phase = 2;
                this.airborneTicks = 0;
                cat.setPose(net.minecraft.world.entity.Pose.STANDING);
                Vec3 toTarget = target.position().subtract(cat.position());
                double dist = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
                if (dist < 0.1D) dist = 0.1D; // NaN guard

                double speed = Math.min(1.4D, 0.5D + (dist * 0.08D));
                int pounceLvl = cat.getTalentLevel(Talents.POUNCE);
                double ySpeed = 0.65D + (pounceLvl * 0.05D);

                cat.setDeltaMovement(toTarget.x / dist * speed, ySpeed, toTarget.z / dist * speed);
                cat.hasImpulse = true;
                cat.playSound(SoundEvents.FOX_BITE, 1.0F, 1.2F);
            }
        } else if (phase == 2) {
            // Airborne phase with safety timeout (max 60 ticks)
            airborneTicks++;
            if (airborneTicks > 60) {
                stop();
                return;
            }

            double distSq = cat.distanceToSqr(target);

            // If cat is landing or close to target
            if (distSq <= 4.0D || (cat.onGround() && cat.getDeltaMovement().y <= 0)) {
                int pounceLvl = cat.getTalentLevel(Talents.POUNCE);
                int weaponLvl = cat.getTalentLevel(Talents.CAT_ARMORY);
                float damage = 6.0F + (pounceLvl * 3.0F) + (weaponLvl * 2.5F);

                target.hurt(cat.damageSources().mobAttack(cat), damage);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));

                // Impact shockwave particles
                if (cat.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, cat.getX(), cat.getY() + 0.2D, cat.getZ(), 15, 0.4D, 0.2D, 0.4D, 0.1D);
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, cat.getX(), cat.getY() + 0.2D, cat.getZ(), 3, 0.2D, 0.1D, 0.2D, 0.0D);
                }

                cat.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.2F);
                cat.playSound(SoundEvents.CAT_PURREOW, 0.8F, 1.3F);

                this.stop();
            }
        }
    }

    @Override
    public void stop() {
        this.phase = 0;
        this.target = null;
        this.airborneTicks = 0;
        this.cooldown = 40;
        cat.setPose(net.minecraft.world.entity.Pose.STANDING);
    }
}
