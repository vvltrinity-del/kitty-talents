package com.kittytalents.ability;

import com.kittytalents.ai.CatTargeting;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Shared target picking for the 4.0 abilities. */
final class AbilityTargets {
    private AbilityTargets() {}

    /** The cat's target if alive and within {@code range}, else the nearest harmable foe in range (for manual casts). */
    static LivingEntity target(TalentedCat cat, double range) {
        LivingEntity t = cat.getTarget();
        if (t != null && t.isAlive() && cat.distanceTo(t) <= range && CatTargeting.canHarm(cat, t)) return t;
        List<LivingEntity> near = foes(cat, range);
        near.sort(Comparator.comparingDouble(cat::distanceToSqr));
        return near.isEmpty() ? null : near.get(0);
    }

    /** Harmable foes within {@code radius} of the cat (a mutable list). */
    static List<LivingEntity> foes(TalentedCat cat, double radius) {
        return foesAround(cat, cat, radius);
    }

    static List<LivingEntity> foesAround(TalentedCat cat, Entity centre, double radius) {
        AABB box = centre.getBoundingBox().inflate(radius);
        return new ArrayList<>(cat.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && CatTargeting.canHarm(cat, e) && e.distanceToSqr(centre) <= radius * radius));
    }

    /** Ultimates: worth it against a crowd, or a single tough foe; otherwise hold the Energy. */
    static float ultimatePriority(TalentedCat cat, int foes) {
        if (foes >= 3) return 2.5f;
        LivingEntity t = cat.getTarget();
        if (foes >= 1 && t != null && t.getMaxHealth() >= 40.0F) return 1.4f;
        return 0.4f;
    }

    /** A line of particles from the cat to the target. */
    static void trail(TalentedCat cat, LivingEntity t, SimpleParticleType type) {
        if (!(cat.level() instanceof ServerLevel sl)) return;
        Vec3 a = cat.position().add(0, 0.4, 0), b = t.position().add(0, t.getBbHeight() * 0.5, 0);
        int steps = (int) Math.max(4, a.distanceTo(b) * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, i / (double) steps);
            sl.sendParticles(type, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }
}
