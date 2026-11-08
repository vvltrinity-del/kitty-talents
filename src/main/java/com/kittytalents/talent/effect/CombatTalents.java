package com.kittytalents.talent.effect;

import com.kittytalents.ai.CatTargeting;
import com.kittytalents.compat.tacz.TacZCompat;
import com.kittytalents.entity.FelineHissProjectileEntity;
import com.kittytalents.talent.Talents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Combat talents. Numbers match the pre-Phase-4 behavior; masteries are new. */
final class CombatTalents {
    private CombatTalents() {}

    static void register() {
        TalentEngine.register(Talents.CRITICAL_CLAWS, new CriticalClaws());
        TalentEngine.register(Talents.PEST_HUNTER, new PestHunter());
        TalentEngine.register(Talents.HUNTER_CAT, new HunterCat());
        TalentEngine.register(Talents.FELINE_BERSERKER, new FelineBerserker());
        TalentEngine.register(Talents.INFERNO_CAT, new InfernoCat());
        TalentEngine.register(Talents.VENOM_CLAWS, new VenomClaws());
        TalentEngine.register(Talents.CAT_ARMORY, new CatArmory());
        TalentEngine.register(Talents.FELINE_HISS, new FelineHiss());
        TalentEngine.register(Talents.GALE_PURR, new GalePurr());
        TalentEngine.register(Talents.QUICK_RELOAD, new QuickReload());
        TalentEngine.register(Talents.AMMO_SCAVENGER, new AmmoScavenger());
    }

    /** Crit chance/damage live in FelineCombatEngine. Mastery "Executioner": +50% damage to foes below 30% health. */
    static final class CriticalClaws implements TalentEffect {
        @Override public float modifyOutgoingDamage(TalentContext ctx, LivingEntity t, float dmg) {
            return ctx.mastered() && t.getHealth() < t.getMaxHealth() * 0.3F ? dmg * 1.5F : dmg;
        }
    }

    /** x(1.5 + 0.4/rank) damage to pests. Mastery "Exterminator": pests can't hurt the cat much (half damage, no poison). */
    static final class PestHunter implements TalentEffect {
        @Override public boolean worksWhileResting() { return true; }
        @Override public float modifyOutgoingDamage(TalentContext ctx, LivingEntity t, float dmg) {
            boolean pest = CatTargeting.isPest(t) || t instanceof net.minecraft.world.entity.ambient.Bat
                    || t instanceof net.minecraft.world.entity.monster.Phantom;
            return pest ? dmg * (1.5F + ctx.rank * 0.4F) : dmg;
        }
        @Override public float modifyIncomingDamage(TalentContext ctx, DamageSource src, float amt) {
            if (!ctx.mastered()) return amt;
            if (src.getEntity() instanceof LivingEntity e && CatTargeting.isPest(e)) return amt * 0.5F;
            return amt;
        }
        @Override public int tickInterval() { return 20; }
        @Override public void onTick(TalentContext ctx) {
            if (ctx.mastered() && ctx.cat.hasEffect(MobEffects.POISON)) ctx.cat.removeEffect(MobEffects.POISON);
        }
    }

    /** +2.5/rank damage to animals; each rank gives 10% chance to double an animal's drops (see HunterCat in TalentEngine). Mastery: always double. */
    static final class HunterCat implements TalentEffect {
        @Override public float modifyOutgoingDamage(TalentContext ctx, LivingEntity t, float dmg) {
            return t instanceof Animal ? dmg + ctx.rank * 2.5F : dmg;
        }
    }

    /** +3+2/rank damage when below 40% health or in Frenzy. Mastery "Unbreakable": takes 30% less damage below 40% health. */
    static final class FelineBerserker implements TalentEffect {
        private static boolean enraged(TalentContext ctx) {
            return ctx.cat.getHealth() < ctx.cat.getMaxHealth() * 0.4F || ctx.cat.isFrenzied();
        }
        @Override public float modifyOutgoingDamage(TalentContext ctx, LivingEntity t, float dmg) {
            if (!enraged(ctx)) return dmg;
            ctx.cat.playSound(SoundEvents.RAVAGER_ATTACK, 0.6F, 1.6F);
            return dmg + 3.0F + ctx.rank * 2.0F;
        }
        @Override public float modifyIncomingDamage(TalentContext ctx, DamageSource src, float amt) {
            return ctx.mastered() && ctx.cat.getHealth() < ctx.cat.getMaxHealth() * 0.4F ? amt * 0.7F : amt;
        }
    }

    /** Sets targets on fire for 3s/rank. Mastery "Phoenix": immune to fire, +30% damage to burning foes. */
    static final class InfernoCat implements TalentEffect {
        @Override public void onHitDealt(TalentContext ctx, LivingEntity t, float dmg) {
            t.setSecondsOnFire(ctx.rank * 3);
        }
        @Override public float modifyOutgoingDamage(TalentContext ctx, LivingEntity t, float dmg) {
            return ctx.mastered() && t.isOnFire() ? dmg * 1.3F : dmg;
        }
        @Override public float modifyIncomingDamage(TalentContext ctx, DamageSource src, float amt) {
            if (ctx.mastered() && src.is(DamageTypeTags.IS_FIRE)) {
                ctx.cat.clearFire();
                return 0.0F;
            }
            return amt;
        }
    }

    /** Poison on hit (Wither at rank 5). Mastery "Toxic Bloom": a poisoned kill spreads poison to foes within 3 blocks. */
    static final class VenomClaws implements TalentEffect {
        @Override public void onHitDealt(TalentContext ctx, LivingEntity t, float dmg) {
            if (ctx.rank >= 5) t.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            else t.addEffect(new MobEffectInstance(MobEffects.POISON, 60 + ctx.rank * 20, Math.max(0, ctx.rank - 1)));
        }
        @Override public void onKill(TalentContext ctx, LivingEntity victim) {
            if (!ctx.mastered()) return;
            for (LivingEntity e : TalentHelpers.foesAround(ctx.cat, victim, 3.0D)) {
                e.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            }
            if (ctx.cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ITEM_SLIME, victim.getX(), victim.getY() + 0.5, victim.getZ(), 20, 1.2, 0.4, 1.2, 0.05);
            }
        }
    }

    /** +2.5 attack damage/rank; rank 3+ ignites targets. Mastery "Blademaster": the equipped weapon repairs itself. */
    static final class CatArmory implements TalentEffect {
        @Override public boolean worksWhileResting() { return true; }
        @Override public void applyAttributes(TalentContext ctx, AttributeTotals t) {
            t.attackDamage += ctx.rank * 2.5D;
        }
        @Override public void onHitDealt(TalentContext ctx, LivingEntity t, float dmg) {
            if (ctx.rank >= 3) t.setSecondsOnFire(4 * (ctx.rank - 2));
        }
        @Override public int tickInterval() { return 40; }
        @Override public void onTick(TalentContext ctx) {
            if (ctx.mastered()) TalentHelpers.repair(ctx.cat.getEquippedWeapon(), 1);
        }
    }

    /** Spits a hiss projectile at targets 4-16 blocks away. Mastery "Banshee Hiss": two projectiles per volley. */
    static final class FelineHiss implements TalentEffect {
        @Override public int tickInterval() { return 5; }
        @Override public void onTick(TalentContext ctx) {
            LivingEntity target = ctx.cat.getTarget();
            if (target == null || !target.isAlive() || !ctx.ready("shot")) return;
            double d = ctx.cat.distanceToSqr(target);
            if (d < 16.0D || d > 256.0D) return;
            ctx.cooldown("shot", Math.max(30, 80 - ctx.rank * 10));
            ctx.cat.playSound(SoundEvents.CAT_HISS, 1.0F, 1.0F);
            int shots = ctx.mastered() ? 2 : 1;
            for (int i = 0; i < shots; i++) {
                FelineHissProjectileEntity p = new FelineHissProjectileEntity(ctx.cat.level(), ctx.cat, 4.0F + ctx.rank * 2.5F);
                p.shoot(target.getX() - ctx.cat.getX(), target.getY(0.5D) - ctx.cat.getY(0.5D), target.getZ() - ctx.cat.getZ(),
                        1.3F, i == 0 ? 1.0F : 6.0F);
                ctx.cat.level().addFreshEntity(p);
            }
        }
    }

    /** Blasts back 3+ surrounding foes. Mastery "Hurricane": triggers against a single foe. */
    static final class GalePurr implements TalentEffect {
        @Override public int tickInterval() { return 20; }
        @Override public void onTick(TalentContext ctx) {
            if (!ctx.ready("gust")) return;
            var foes = TalentHelpers.foesAround(ctx.cat, ctx.cat, 3.5D + ctx.rank * 0.5D);
            if (foes.size() < (ctx.mastered() ? 1 : 3)) return;
            ctx.cooldown("gust", 200 - ctx.rank * 20);
            ctx.cat.playSound(SoundEvents.PHANTOM_FLAP, 1.2F, 1.4F);
            for (LivingEntity m : foes) {
                Vec3 diff = m.position().subtract(ctx.cat.position());
                Vec3 push = diff.lengthSqr() > 1.0E-4D ? diff.normalize().scale(1.2D + ctx.rank * 0.2D) : new Vec3(0, 0.5D, 0);
                m.push(push.x, 0.35D, push.z);
                m.hurt(ctx.cat.damageSources().mobAttack(ctx.cat), 2.0F + ctx.rank);
            }
        }
    }

    /** Reload behavior lives in TacZImpl. Mastery "Speed Loader": tops up the magazine from the pack every 3 s. */
    static final class QuickReload implements TalentEffect {
        @Override public int tickInterval() { return 60; }
        @Override public void onTick(TalentContext ctx) {
            if (!ctx.mastered() || !TacZCompat.isLoaded()) return;
            ItemStack gun = ctx.cat.getEquippedWeapon();
            if (TacZCompat.isGun(gun) && TacZCompat.getAmmoCount(gun) < TacZCompat.getMaxAmmoCount(gun)) {
                TacZCompat.tryReloadFromPack(ctx.cat, gun);
            }
        }
    }

    /** Ammo saving lives in TacZEvents. Mastery "Scrounger": every kill with a gun puts 2 rounds back in the magazine. */
    static final class AmmoScavenger implements TalentEffect {
        @Override public void onKill(TalentContext ctx, LivingEntity victim) {
            if (!ctx.mastered() || !TacZCompat.isLoaded()) return;
            ItemStack gun = ctx.cat.getEquippedWeapon();
            if (TacZCompat.isGun(gun)) {
                TacZCompat.setAmmoCount(gun, Math.min(TacZCompat.getMaxAmmoCount(gun), TacZCompat.getAmmoCount(gun) + 2));
            }
        }
    }
}
