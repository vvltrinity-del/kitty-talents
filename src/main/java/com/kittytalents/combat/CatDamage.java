package com.kittytalents.combat;

import com.kittytalents.ai.CatTargeting;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Damage a cat deals other than with its claws, and target picking for the Twine claw and pounce. */
public final class CatDamage {
    private CatDamage() {}

    /**
     * Ability damage of the given type (freeze, fire, lightning...) that is credited to the cat: the kill counts for
     * XP, kill stats, bond, on-kill talents and drops. The cat is the causing entity but not the direct one, so the hurt
     * event's claw rules (combos, crits, role and element multipliers) don't apply and knockback stays as it was.
     */
    public static DamageSource ability(TalentedCat cat, ResourceKey<DamageType> type) {
        return new DamageSource(cat.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), null, cat);
    }

    /**
     * What a Twined cat's claw or pounce hits: the closest creature in front of it (within {@code minFacing} of the
     * view direction) and inside {@code reach}, never the rider, the cat's friends (owner, co-owners, the owner's pets)
     * or anything behind it.
     */
    @Nullable
    public static LivingEntity twineStrikeTarget(TalentedCat cat, @Nullable Entity rider, double reach, double minFacing) {
        Vec3 look = cat.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0, cat.getYRot()) : flat.normalize();
        Vec3 eye = cat.position().add(0, cat.getBbHeight() * 0.5, 0);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        AABB box = cat.getBoundingBox().inflate(reach);
        for (LivingEntity e : cat.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != cat && e != rider && e.isAlive()
                && !e.isSpectator() && !CatTargeting.isFriendly(cat, e))) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            Vec3 toFlat = new Vec3(to.x, 0, to.z);
            double dist = toFlat.length();
            if (dist > 0.6 && toFlat.normalize().dot(flat) < minFacing) continue; // behind or beside the cat
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }
        return best;
    }
}
