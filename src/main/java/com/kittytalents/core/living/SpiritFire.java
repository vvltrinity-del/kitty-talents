package com.kittytalents.core.living;

import com.kittytalents.ai.CatTargeting;
import com.kittytalents.combat.CatDamage;
import com.kittytalents.core.tuning.Tuning;
import com.kittytalents.entity.CatBreedingManager;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Nekomata spirit fire (5.0). A Nekomata (the rare two-tailed mutation) keeps up to 3 blue will-o-wisps circling
 * it; in a fight it sends one at a time at its target, and they regrow over time. A Nekomata housed in a Kitty
 * Habitat wards it: hostile mobs near the Habitat are pushed back by ghostly flames. Numbers are Tuning keys
 * (scope traits, owner nekomata).
 */
public final class SpiritFire {
    private SpiritFire() {}

    private static Tuning.Key key(String name, double def, String label) {
        return Tuning.define("traits", "nekomata", name, def, 0, Math.max(1.0, Math.abs(def) * 10), label);
    }
    public static final Tuning.Key WISPS = key("wisps", 3, "Wisps");
    public static final Tuning.Key DAMAGE = key("wisp_damage", 5.0, "Wisp damage");
    public static final Tuning.Key RANGE = key("wisp_range", 12.0, "Wisp range");
    public static final Tuning.Key FIRE_TICKS = key("wisp_fire_interval_ticks", 30, "Ticks between wisp shots");
    public static final Tuning.Key REGROW_TICKS = key("wisp_regrow_ticks", 200, "Ticks to regrow one wisp");
    public static final Tuning.Key WARD_RADIUS = key("ward_radius", 6.0, "Spirit ward radius");

    public static void init() {}

    public static boolean isNekomata(TalentedCat cat) {
        return cat.getMutation() == CatBreedingManager.MutationType.NEKOMATA;
    }

    /** Server tick for a Nekomata: regrow wisps, and send one at the target when ready. */
    public static void tick(TalentedCat cat) {
        if (!isNekomata(cat) || !(cat.level() instanceof ServerLevel sl)) return;
        int max = WISPS.i();
        if (cat.getWisps() < max && cat.tickCount % Math.max(1, REGROW_TICKS.i()) == 0) cat.setWisps(cat.getWisps() + 1);
        if (cat.isIncapacitated()) return;
        LivingEntity t = cat.getTarget();
        if (t == null || !t.isAlive() || cat.getWisps() <= 0 || !CatTargeting.canHarm(cat, t)) return;
        if (cat.distanceToSqr(t) > RANGE.get() * RANGE.get() || cat.tickCount % Math.max(1, FIRE_TICKS.i()) != 0) return;
        cat.setWisps(cat.getWisps() - 1);
        t.hurt(CatDamage.ability(cat, DamageTypes.MAGIC), DAMAGE.f());
        t.setSecondsOnFire(2);
        Vec3 from = cat.position().add(0, 0.6, 0), to = t.position().add(0, t.getBbHeight() * 0.5, 0);
        for (int i = 0; i <= 10; i++) {
            Vec3 p = from.lerp(to, i / 10.0);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        sl.playSound(null, cat.getX(), cat.getY(), cat.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.NEUTRAL, 1.0F, 1.4F);
    }

    /** Client: the wisps circling the cat. */
    public static void clientTick(TalentedCat cat) {
        int n = cat.getWisps();
        if (n <= 0 || cat.isInvisible() || cat.tickCount % 2 != 0) return;
        double base = cat.tickCount * 0.12;
        for (int i = 0; i < n; i++) {
            double a = base + i * Math.PI * 2 / n;
            double x = cat.getX() + Math.cos(a) * 0.8, z = cat.getZ() + Math.sin(a) * 0.8;
            double y = cat.getY() + 0.7 + Math.sin(base * 2 + i) * 0.1;
            cat.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.005, 0);
        }
    }

    /** Spirit ward around a Habitat with a Nekomata: hostiles nearby are pushed out (call every 20 ticks). */
    public static void ward(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return;
        double r = WARD_RADIUS.get();
        Vec3 c = Vec3.atCenterOf(pos);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(r), e -> e instanceof Enemy && e.isAlive())) {
            Vec3 d = e.position().subtract(c);
            Vec3 push = d.horizontalDistanceSqr() < 1.0E-4 ? new Vec3(0.5, 0, 0) : new Vec3(d.x, 0, d.z).normalize().scale(0.8);
            e.push(push.x, 0.25, push.z);
            e.hurtMarked = true;
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, e.getX(), e.getY() + 0.5, e.getZ(), 4, 0.2, 0.3, 0.2, 0.01);
        }
        if (sl.random.nextInt(3) == 0) {
            double a = sl.random.nextDouble() * Math.PI * 2;
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x + Math.cos(a) * r * 0.6, c.y, c.z + Math.sin(a) * r * 0.6, 2, 0.1, 0.2, 0.1, 0.0);
        }
    }
}
