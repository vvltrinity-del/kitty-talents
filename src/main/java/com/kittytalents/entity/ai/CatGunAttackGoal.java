package com.kittytalents.entity.ai;

import com.kittytalents.compat.tacz.TacZCompat;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class CatGunAttackGoal extends Goal {
    private final TalentedCat cat;
    private final double speedModifier;
    private final float attackRadiusMin;
    private final float attackRadiusMax;
    private int shootCooldown = 0;
    private int strafeTime = 0;
    private boolean strafeClockwise = false;
    private int navCooldown = 0;
    private int noLosTicks = 0;

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
            if (!TacZCompat.tryReEquipFirearmIfAmmoAvailable(cat)) {
                return false;
            }
        }
        LivingEntity target = cat.getTarget();
        if (target == null || !target.isAlive()) {
            // Out-of-combat tactical reload if magazine < 50%
            ItemStack gun = cat.getEquippedWeapon();
            if (TacZCompat.isGun(gun) && !TacZCompat.isReloading(cat)) {
                int current = TacZCompat.getAmmoCount(gun);
                int max = TacZCompat.getMaxAmmoCount(gun);
                if (current < max / 2 && TacZCompat.hasAmmoInPack(cat, gun)) {
                    TacZCompat.triggerReload(cat, gun);
                }
            }
            return false;
        }

        ItemStack gun = cat.getEquippedWeapon();
        if (!TacZCompat.hasAmmoInPack(cat, gun) && TacZCompat.getAmmoCount(gun) <= 0) {
            boolean swapped = TacZCompat.handleEmptyAmmoSwapOrStow(cat);
            if (swapped) {
                return false;
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

        ItemStack gun = cat.getEquippedWeapon();
        if (!TacZCompat.hasAmmoInPack(cat, gun) && TacZCompat.getAmmoCount(gun) <= 0) {
            TacZCompat.handleEmptyAmmoSwapOrStow(cat);
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.shootCooldown = 5;
        this.strafeTime = 0;
        this.noLosTicks = 0;
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
            TacZCompat.handleEmptyAmmoSwapOrStow(cat);
            return;
        }

        double maxDistSq = attackRadiusMax * attackRadiusMax;
        double minDistSq = attackRadiusMin * attackRadiusMin;

        if (distSq > maxDistSq) {
            navCooldown++;
            if (navCooldown >= 15 || cat.getNavigation().isDone()) {
                navCooldown = 0;
                cat.getNavigation().moveTo(target, speedModifier);
            }
        } else if (distSq < minDistSq) {
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
                cat.getMoveControl().strafe(0.0F, strafeClockwise ? 0.6F : -0.6F);
            }
        } else {
            strafeTime++;
            if (strafeTime > 40) {
                strafeTime = 0;
                strafeClockwise = cat.getRandom().nextBoolean();
            }
            cat.getMoveControl().strafe(0.0F, strafeClockwise ? 0.6F : -0.6F);
        }

        // Firing & line-of-fire safety logic
        if (shootCooldown > 0) {
            shootCooldown--;
        } else {
            if (cat.getSensing().hasLineOfSight(target)) {
                noLosTicks = 0;
                if (!isAllyInLineOfFire(target)) {
                    boolean fired = TacZCompat.shootGunAtTarget(cat, target);
                    if (fired) {
                        shootCooldown = TacZCompat.getShootCooldownTicks(gun);
                    } else {
                        shootCooldown = 8;
                    }
                } else {
                    // Reposition laterally when an ally is in the firing line
                    strafeClockwise = !strafeClockwise;
                    cat.getMoveControl().strafe(0.0F, strafeClockwise ? 0.85F : -0.85F);
                    shootCooldown = 6;
                }
            } else {
                noLosTicks++;
                if (noLosTicks >= 40) {
                    int current = TacZCompat.getAmmoCount(gun);
                    int max = TacZCompat.getMaxAmmoCount(gun);
                    if (current < max / 2 && TacZCompat.hasAmmoInPack(cat, gun)) {
                        TacZCompat.triggerReload(cat, gun);
                    }
                }
            }
        }
    }

    private boolean isAllyInLineOfFire(LivingEntity target) {
        Vec3 start = cat.getEyePosition();
        Vec3 end = target.getEyePosition();
        AABB searchBox = new AABB(start, end).inflate(1.0D);
        List<LivingEntity> entities = cat.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != cat && e != target && e.isAlive());
        for (LivingEntity e : entities) {
            boolean isAlly = (e == cat.getOwner())
                    || (e instanceof Player p && (cat.isOwnedBy(p) || cat.isTrusted(p)))
                    || (e instanceof TalentedCat other && cat.getOwnerUUID() != null && Objects.equals(cat.getOwnerUUID(), other.getOwnerUUID()))
                    || cat.isAlliedTo(e);
            if (isAlly && e.getBoundingBox().inflate(0.5D).clip(start, end).isPresent()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void stop() {
        this.shootCooldown = 0;
        this.strafeTime = 0;
        this.noLosTicks = 0;
        TacZCompat.setAiming(cat, false);
    }
}
