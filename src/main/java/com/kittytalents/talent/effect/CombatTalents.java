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
        private static final com.kittytalents.core.tuning.Tuning.Key EXEC_HP =
                com.kittytalents.core.tuning.Tuning.talent("critical_claws", "execute_below_health", 0.3, "Mastery: execute below health");
        private static final com.kittytalents.core.tuning.Tuning.Key EXEC_MULT =
                com.kittytalents.core.tuning.Tuning.talent("critical_claws", "execute_damage_mult", 1.5, "Mastery: execute damage x");
        @Override public float modifyOutgoingDamage(TalentContext ctx, LivingEntity t, float dmg) {
            return ctx.mastered() && t.getHealth() < t.getMaxHealth() * EXEC_HP.f() ? dmg * EXEC_MULT.f() : dmg;
        }
    }

    /** x(1.5 + 0.4/rank) damage to pests. Mastery "Exterminator": pests can't hurt the cat much (half damage, no poison). */
    static final class PestHunter implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key BASE =
                com.kittytalents.core.tuning.Tuning.talent("pest_hunter", "damage_mult", 1.5, "Damage x vs pests");
        private static final com.kittytalents.core.tuning.Tuning.Key PER =
                com.kittytalents.core.tuning.Tuning.talent("pest_hunter", "damage_mult_per_rank", 0.4, "Extra x per rank");
        private static final com.kittytalents.core.tuning.Tuning.Key TAKEN =
                com.kittytalents.core.tuning.Tuning.talent("pest_hunter", "mastery_damage_taken", 0.5, "Mastery: damage taken from pests x");
        @Override public boolean worksWhileResting() { return true; }
        @Override public float modifyOutgoingDamage(TalentContext ctx, LivingEntity t, float dmg) {
            boolean pest = CatTargeting.isPest(t) || t instanceof net.minecraft.world.entity.ambient.Bat
                    || t instanceof net.minecraft.world.entity.monster.Phantom;
            return pest ? dmg * (BASE.f() + ctx.rank * PER.f()) : dmg;
        }
        @Override public float modifyIncomingDamage(TalentContext ctx, DamageSource src, float amt) {
            if (!ctx.mastered()) return amt;
            if (src.getEntity() instanceof LivingEntity e && CatTargeting.isPest(e)) return amt * TAKEN.f();
            return amt;
        }
        @Override public int tickInterval() { return 20; }
        @Override public void onTick(TalentContext ctx) {
            if (ctx.mastered() && ctx.cat.hasEffect(MobEffects.POISON)) ctx.cat.removeEffect(MobEffects.POISON);
        }
    }

    /** +2.5/rank damage to animals; each rank gives 10% chance to double an animal's drops (see HunterCat in TalentEngine). Mastery: always double. */
    static final class HunterCat implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key PER =
                com.kittytalents.core.tuning.Tuning.talent("hunter_cat", "damage_per_rank", 2.5, "Damage vs animals per rank");
        @Override public float modifyOutgoingDamage(TalentContext ctx, LivingEntity t, float dmg) {
            return t instanceof Animal ? dmg + ctx.rank * PER.f() : dmg;
        }
    }

    /** +3+2/rank damage when below 40% health or in Frenzy. Mastery "Unbreakable": takes 30% less damage below 40% health. */
    static final class FelineBerserker implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key HP =
                com.kittytalents.core.tuning.Tuning.talent("feline_berserker", "enrage_below_health", 0.4, "Enrage below health");
        private static final com.kittytalents.core.tuning.Tuning.Key BASE =
                com.kittytalents.core.tuning.Tuning.talent("feline_berserker", "damage", 3.0, "Enraged damage");
        private static final com.kittytalents.core.tuning.Tuning.Key PER =
                com.kittytalents.core.tuning.Tuning.talent("feline_berserker", "damage_per_rank", 2.0, "Enraged damage per rank");
        private static final com.kittytalents.core.tuning.Tuning.Key TAKEN =
                com.kittytalents.core.tuning.Tuning.talent("feline_berserker", "mastery_damage_taken", 0.7, "Mastery: damage taken x");
        private static boolean enraged(TalentContext ctx) {
            return ctx.cat.getHealth() < ctx.cat.getMaxHealth() * HP.f() || ctx.cat.isFrenzied();
        }
        @Override public float modifyOutgoingDamage(TalentContext ctx, LivingEntity t, float dmg) {
            if (!enraged(ctx)) return dmg;
            ctx.cat.playSound(SoundEvents.RAVAGER_ATTACK, 0.6F, 1.6F);
            return dmg + BASE.f() + ctx.rank * PER.f();
        }
        @Override public float modifyIncomingDamage(TalentContext ctx, DamageSource src, float amt) {
            return ctx.mastered() && ctx.cat.getHealth() < ctx.cat.getMaxHealth() * HP.f() ? amt * TAKEN.f() : amt;
        }
    }

    /** Sets targets on fire for 3s/rank. Mastery "Phoenix": immune to fire, +30% damage to burning foes. */
    static final class InfernoCat implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key FIRE =
                com.kittytalents.core.tuning.Tuning.talent("inferno_cat", "burn_seconds_per_rank", 3, "Burn seconds per rank");
        private static final com.kittytalents.core.tuning.Tuning.Key MULT =
                com.kittytalents.core.tuning.Tuning.talent("inferno_cat", "mastery_burning_damage_mult", 1.3, "Mastery: damage x vs burning");
        @Override public void onHitDealt(TalentContext ctx, LivingEntity t, float dmg) {
            t.setSecondsOnFire(ctx.rank * FIRE.i());
        }
        @Override public float modifyOutgoingDamage(TalentContext ctx, LivingEntity t, float dmg) {
            return ctx.mastered() && t.isOnFire() ? dmg * MULT.f() : dmg;
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
        private static final com.kittytalents.core.tuning.Tuning.Key BASE =
                com.kittytalents.core.tuning.Tuning.talent("venom_claws", "poison_ticks", 60, "Poison ticks");
        private static final com.kittytalents.core.tuning.Tuning.Key PER =
                com.kittytalents.core.tuning.Tuning.talent("venom_claws", "poison_ticks_per_rank", 20, "Poison ticks per rank");
        private static final com.kittytalents.core.tuning.Tuning.Key WITHER =
                com.kittytalents.core.tuning.Tuning.talent("venom_claws", "wither_ticks", 100, "Wither ticks (rank 5)");
        private static final com.kittytalents.core.tuning.Tuning.Key SPREAD =
                com.kittytalents.core.tuning.Tuning.talent("venom_claws", "mastery_spread_radius", 3.0, "Mastery: spread radius");
        @Override public void onHitDealt(TalentContext ctx, LivingEntity t, float dmg) {
            if (ctx.rank >= 5) t.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER.i(), 1));
            else t.addEffect(new MobEffectInstance(MobEffects.POISON, BASE.i() + ctx.rank * PER.i(), Math.max(0, ctx.rank - 1)));
        }
        @Override public void onKill(TalentContext ctx, LivingEntity victim) {
            if (!ctx.mastered()) return;
            for (LivingEntity e : TalentHelpers.foesAround(ctx.cat, victim, SPREAD.get())) {
                e.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            }
            if (ctx.cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ITEM_SLIME, victim.getX(), victim.getY() + 0.5, victim.getZ(), 20, 1.2, 0.4, 1.2, 0.05);
            }
        }
    }

    /** +2.5 attack damage/rank; rank 3+ ignites targets. Mastery "Blademaster": the equipped weapon repairs itself. */
    static final class CatArmory implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key PER =
                com.kittytalents.core.tuning.Tuning.talent("cat_armory", "attack_damage_per_rank", 2.5, "Attack damage per rank");
        private static final com.kittytalents.core.tuning.Tuning.Key FIRE =
                com.kittytalents.core.tuning.Tuning.talent("cat_armory", "burn_seconds", 4, "Burn seconds (rank 3+)");
        @Override public boolean worksWhileResting() { return true; }
        @Override public void applyAttributes(TalentContext ctx, AttributeTotals t) {
            t.attackDamage += ctx.rank * PER.get();
        }
        @Override public void onHitDealt(TalentContext ctx, LivingEntity t, float dmg) {
            if (ctx.rank >= 3) t.setSecondsOnFire(FIRE.i() * (ctx.rank - 2));
        }
        @Override public int tickInterval() { return 40; }
        @Override public void onTick(TalentContext ctx) {
            if (ctx.mastered()) TalentHelpers.repair(ctx.cat.getEquippedWeapon(), 1);
        }
    }

    /** Spits a hiss projectile at targets 4-16 blocks away. Mastery "Banshee Hiss": two projectiles per volley. */
    static final class FelineHiss implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key CD =
                com.kittytalents.core.tuning.Tuning.talent("feline_hiss", "cooldown_ticks", 80, "Cooldown ticks");
        private static final com.kittytalents.core.tuning.Tuning.Key CD_PER =
                com.kittytalents.core.tuning.Tuning.talent("feline_hiss", "cooldown_ticks_per_rank", 10, "Cooldown cut per rank");
        private static final com.kittytalents.core.tuning.Tuning.Key DMG =
                com.kittytalents.core.tuning.Tuning.talent("feline_hiss", "damage", 4.0, "Projectile damage");
        private static final com.kittytalents.core.tuning.Tuning.Key DMG_PER =
                com.kittytalents.core.tuning.Tuning.talent("feline_hiss", "damage_per_rank", 2.5, "Damage per rank");
        @Override public int tickInterval() { return 5; }
        @Override public void onTick(TalentContext ctx) {
            LivingEntity target = ctx.cat.getTarget();
            if (target == null || !target.isAlive() || !ctx.ready("shot")) return;
            double d = ctx.cat.distanceToSqr(target);
            if (d < 16.0D || d > 256.0D) return;
            ctx.cooldown("shot", Math.max(30, CD.i() - ctx.rank * CD_PER.i()));
            ctx.cat.playSound(SoundEvents.CAT_HISS, 1.0F, 1.0F);
            int shots = ctx.mastered() ? 2 : 1;
            for (int i = 0; i < shots; i++) {
                FelineHissProjectileEntity p = new FelineHissProjectileEntity(ctx.cat.level(), ctx.cat, DMG.f() + ctx.rank * DMG_PER.f());
                p.shoot(target.getX() - ctx.cat.getX(), target.getY(0.5D) - ctx.cat.getY(0.5D), target.getZ() - ctx.cat.getZ(),
                        1.3F, i == 0 ? 1.0F : 6.0F);
                ctx.cat.level().addFreshEntity(p);
            }
        }
    }

    /** Blasts back 3+ surrounding foes. Mastery "Hurricane": triggers against a single foe. */
    static final class GalePurr implements TalentEffect {
        private static final com.kittytalents.core.tuning.Tuning.Key R =
                com.kittytalents.core.tuning.Tuning.talent("gale_purr", "radius", 3.5, "Radius");
        private static final com.kittytalents.core.tuning.Tuning.Key R_PER =
                com.kittytalents.core.tuning.Tuning.talent("gale_purr", "radius_per_rank", 0.5, "Radius per rank");
        private static final com.kittytalents.core.tuning.Tuning.Key CD =
                com.kittytalents.core.tuning.Tuning.talent("gale_purr", "cooldown_ticks", 200, "Cooldown ticks");
        private static final com.kittytalents.core.tuning.Tuning.Key CD_PER =
                com.kittytalents.core.tuning.Tuning.talent("gale_purr", "cooldown_ticks_per_rank", 20, "Cooldown cut per rank");
        private static final com.kittytalents.core.tuning.Tuning.Key DMG =
                com.kittytalents.core.tuning.Tuning.talent("gale_purr", "damage", 2.0, "Damage");
        private static final com.kittytalents.core.tuning.Tuning.Key DMG_PER =
                com.kittytalents.core.tuning.Tuning.talent("gale_purr", "damage_per_rank", 1.0, "Damage per rank");
        @Override public int tickInterval() { return 20; }
        @Override public void onTick(TalentContext ctx) {
            if (!ctx.ready("gust")) return;
            var foes = TalentHelpers.foesAround(ctx.cat, ctx.cat, R.get() + ctx.rank * R_PER.get());
            if (foes.size() < (ctx.mastered() ? 1 : 3)) return;
            ctx.cooldown("gust", Math.max(20, CD.i() - ctx.rank * CD_PER.i()));
            ctx.cat.playSound(SoundEvents.PHANTOM_FLAP, 1.2F, 1.4F);
            for (LivingEntity m : foes) {
                Vec3 diff = m.position().subtract(ctx.cat.position());
                Vec3 push = diff.lengthSqr() > 1.0E-4D ? diff.normalize().scale(1.2D + ctx.rank * 0.2D) : new Vec3(0, 0.5D, 0);
                m.push(push.x, 0.35D, push.z);
                m.hurt(ctx.cat.damageSources().mobAttack(ctx.cat), DMG.f() + ctx.rank * DMG_PER.f());
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
        private static final com.kittytalents.core.tuning.Tuning.Key ROUNDS =
                com.kittytalents.core.tuning.Tuning.talent("ammo_scavenger", "mastery_rounds_per_kill", 2, "Mastery: rounds per kill");
        @Override public void onKill(TalentContext ctx, LivingEntity victim) {
            if (!ctx.mastered() || !TacZCompat.isLoaded()) return;
            ItemStack gun = ctx.cat.getEquippedWeapon();
            if (TacZCompat.isGun(gun)) {
                TacZCompat.setAmmoCount(gun, Math.min(TacZCompat.getMaxAmmoCount(gun), TacZCompat.getAmmoCount(gun) + ROUNDS.i()));
            }
        }
    }
}
