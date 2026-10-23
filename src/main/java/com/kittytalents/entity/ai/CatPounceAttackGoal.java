package com.kittytalents.entity.ai;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CatPounceAttackGoal extends Goal {
    private final TalentedCat cat;
    private LivingEntity target;
    private int attackCooldown = 0;

    public CatPounceAttackGoal(TalentedCat cat) {
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping() || !cat.getCatMode().canAttack()) {
            return false;
        }
        if (cat.getTalentLevel(Talents.POUNCE) <= 0 && cat.getCatMode() != CatMode.HUNTING) {
            return false;
        }
        LivingEntity currentTarget = cat.getTarget();
        if (currentTarget == null || !currentTarget.isAlive()) {
            return false;
        }
        this.target = currentTarget;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && !cat.isTwined() && !cat.isIncapacitated() &&
               !cat.isOrderedToSit() && !cat.isSleeping() && cat.distanceToSqr(target) < 256.0D;
    }

    @Override
    public void start() {
        cat.getNavigation().moveTo(target, 1.25D);
    }

    @Override
    public void stop() {
        this.target = null;
        this.attackCooldown = 0;
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) return;

        cat.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distSq = cat.distanceToSqr(target);

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        int pounceLevel = cat.getTalentLevel(Talents.POUNCE);
        // Leap attack if between 2 and 8 blocks away, on ground, cooldown ready, AND safe
        if (distSq >= 4.0D && distSq <= 49.0D && cat.onGround() && attackCooldown <= 0) {
            // Cliff leaping safety check: do not leap into chasms, lava, or off high drops
            if (CatCliffSafetyHelper.isSafeToLeap(cat, target.position())) {
                Vec3 diff = target.position().subtract(cat.position());
                if (diff.lengthSqr() > 0.001D) {
                    Vec3 dir = diff.normalize();
                    cat.setDeltaMovement(dir.x * (0.8D + 0.1D * pounceLevel), 0.45D + 0.05D * pounceLevel, dir.z * (0.8D + 0.1D * pounceLevel));
                    cat.hasImpulse = true;
                    attackCooldown = Math.max(10, 40 - (pounceLevel * 4));
                }
            }
        }

        // Claw attack in melee range
        if (distSq <= 4.0D && attackCooldown <= 10) {
            float baseDamage = 3.0F + (pounceLevel * 2.0F);
            target.hurt(cat.damageSources().mobAttack(cat), baseDamage);
            if (pounceLevel >= 3) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, pounceLevel - 2));
            }
            attackCooldown = 20;
        } else {
            if (cat.getNavigation().isDone() || cat.tickCount % 10 == 0) {
                cat.getNavigation().moveTo(target, 1.25D);
            }
        }
    }
}
