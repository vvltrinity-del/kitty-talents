package com.kittytalents.entity.ai;

import com.kittytalents.compat.tacz.TacZCompat;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CatGunAttackGoal extends Goal {
    private final TalentedCat cat;
    private final double speedModifier;
    private final float attackRadiusMin;
    private final float attackRadiusMax;
    private int shootCooldown = 0;
    private int strafeTime = 0;
    private boolean strafeClockwise = false;
    private int outOfAmmoTicks = 0;
    private int navCooldown = 0;

    public CatGunAttackGoal(TalentedCat cat, double speedModifier, float attackRadiusMin, float attackRadiusMax) {
        this.cat = cat;
        this.speedModifier = speedModifier;
        this.attackRadiusMin = attackRadiusMin;
        this.attackRadiusMax = attackRadiusMax;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public CatGunAttackGoal(TalentedCat cat) {
        this(cat, 1.25D, 8.0F, 22.0F);
    }

    @Override
    public boolean canUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping() || !cat.getCatMode().canAttack()) {
            return false;
        }
        if (!cat.hasEquippedGun()) {
            // Attempt to re-equip a firearm if ammo became available
            if (!TacZCompat.tryReEquipFirearmIfAmmoAvailable(cat)) {
                return false;
            }
        }
        LivingEntity target = cat.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Empty ammo check: if cat has no ammo in gun AND none in pack,
        // automatically swap for sword/axe or stow gun and yield to melee
        ItemStack gun = cat.getEquippedWeapon();
        if (!TacZCompat.hasAmmoInPack(cat, gun) && TacZCompat.getAmmoCount(gun) <= 0) {
            boolean swapped = TacZCompat.handleEmptyAmmoSwapOrStow(cat);
            if (swapped) {
                return false; // Swapped to melee or claws, yield to melee attack goal
            }
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (cat.isTwined() || cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping()) {
            return false;
        }
        if (!cat.hasEquippedGun()) {
            return false;
        }
        LivingEntity target = cat.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Empty ammo fallback: swap for melee weapon or stow gun
        ItemStack gun = cat.getEquippedWeapon();
        if (!TacZCompat.hasAmmoInPack(cat, gun) && TacZCompat.getAmmoCount(gun) <= 0) {
            TacZCompat.handleEmptyAmmoSwapOrStow(cat);
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.shootCooldown = 10;
        this.strafeTime = 0;
        this.outOfAmmoTicks = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = cat.getTarget();
        if (target == null || !target.isAlive()) return;

        double distSq = cat.distanceToSqr(target);
        cat.getLookControl().setLookAt(target, 30.0F, 30.0F);

        ItemStack gun = cat.getEquippedWeapon();
        boolean hasAmmo = TacZCompat.hasAmmoInPack(cat, gun) || TacZCompat.getAmmoCount(gun) > 0;

        if (!hasAmmo) {
            // Out of ammo: trigger swap to sword/melee or stow empty firearm
            TacZCompat.handleEmptyAmmoSwapOrStow(cat);
            return;
        }

        // Tactically maintain range between attackRadiusMin and attackRadiusMax blocks
        double maxDistSq = attackRadiusMax * attackRadiusMax;
        double minDistSq = attackRadiusMin * attackRadiusMin;

        if (distSq > maxDistSq) {
            // Advance towards target
            navCooldown++;
            if (navCooldown >= 15 || cat.getNavigation().isDone()) {
                navCooldown = 0;
                cat.getNavigation().moveTo(target, speedModifier);
            }
        } else if (distSq < minDistSq) {
            // Back away while checking cliff safety behind
            Vec3 backPos = new Vec3(
                    cat.getX() - (target.getX() - cat.getX()),
                    cat.getY(),
                    cat.getZ() - (target.getZ() - cat.getZ())
            );
            if (CatCliffSafetyHelper.isSafeToLeap(cat, backPos)) {
                navCooldown++;
                if (navCooldown >= 15 || cat.getNavigation().isDone()) {
                    navCooldown = 0;
                    cat.getNavigation().moveTo(backPos.x, backPos.y, backPos.z, speedModifier * 1.1D);
                }
            } else {
                // Cannot back up safely: strafe sideways instead
                cat.getMoveControl().strafe(0.0F, strafeClockwise ? 0.6F : -0.6F);
            }
        } else {
            // In optimal combat distance: lateral strafing & tactical positioning
            strafeTime++;
            if (strafeTime > 40) {
                strafeTime = 0;
                strafeClockwise = cat.getRandom().nextBoolean();
            }
            cat.getMoveControl().strafe(0.0F, strafeClockwise ? 0.6F : -0.6F);
        }

        // Firing logic
        if (shootCooldown > 0) {
            shootCooldown--;
        } else {
            if (cat.getSensing().hasLineOfSight(target)) {
                boolean fired = TacZCompat.shootGunAtTarget(cat, target);
                if (fired) {
                    shootCooldown = TacZCompat.getShootCooldownTicks(cat.getEquippedWeapon());
                } else {
                    shootCooldown = 20; // Reload or retry
                }
            }
        }
    }

    @Override
    public void stop() {
        this.shootCooldown = 0;
        this.strafeTime = 0;
        this.outOfAmmoTicks = 0;
    }
}
