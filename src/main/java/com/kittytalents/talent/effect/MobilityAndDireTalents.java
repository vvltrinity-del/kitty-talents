package com.kittytalents.talent.effect;

import com.kittytalents.combat.FelineEnergyEngine;
import com.kittytalents.combat.FelineStatusEffects;
import com.kittytalents.combat.FelineStatusType;
import com.kittytalents.talent.Talents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Mobility talents and the Dire (endgame) talents. */
final class MobilityAndDireTalents {
    private MobilityAndDireTalents() {}

    static void register() {
        TalentEngine.register(Talents.FELINE_AGILITY, new FelineAgility());
        TalentEngine.register(Talents.CAT_MOUNT, new CatMount());
        TalentEngine.register(Talents.SKY_FELINE, new SkyFeline());
        TalentEngine.register(Talents.CUSHION_PAWS, new CushionPaws());
        TalentEngine.register(Talents.MOLE_CAT, new MoleCat());
        TalentEngine.register(Talents.FRENZY_FOCUS, new FrenzyFocus());
        TalentEngine.register(Talents.ALCHEMY_KITTY, new AlchemyKitty());
        TalentEngine.register(Talents.CATNIP_CATASTROPHE, new CatnipCatastrophe());
    }

    /** +0.03 speed/rank; 10%/rank chance to dodge projectiles. Mastery "Ghost Step": also 15% chance to dodge melee. */
    static final class FelineAgility implements TalentEffect {
        @Override public void applyAttributes(TalentContext ctx, AttributeTotals t) {
            t.movementSpeed += ctx.rank * 0.03D;
        }
        @Override public float modifyIncomingDamage(TalentContext ctx, DamageSource src, float amt) {
            float chance = src.isIndirect() ? ctx.rank * 0.10F : (ctx.mastered() ? 0.15F : 0.0F);
            if (chance > 0 && ctx.cat.getRandom().nextFloat() < chance) {
                ctx.cat.playSound(SoundEvents.CAT_PURREOW, 0.7F, 1.5F);
                return 0.0F;
            }
            return amt;
        }
    }

    /** Rideable and bigger per rank (TalentedCat). Mastery "War Steed": the rider gets Resistance. */
    static final class CatMount implements TalentEffect {
        @Override public int tickInterval() { return 20; }
        @Override public void onTick(TalentContext ctx) {
            if (ctx.mastered() && ctx.cat.getControllingPassenger() instanceof Player rider) {
                TalentHelpers.aura(rider, MobEffects.DAMAGE_RESISTANCE, 60, 0);
            }
        }
    }

    /** Glides when falling (flight while mounted lives in TalentedCat#travel). Mastery "Stormrider": the owner nearby falls slowly. */
    static final class SkyFeline implements TalentEffect {
        @Override public int tickInterval() { return 1; }
        @Override public void onTick(TalentContext ctx) {
            // AI glide on real drops only; Twined flight lives in SkyFlight via TalentedCat#travel.
            // (This used to multiply horizontal speed by up to 1.25 every falling tick, flinging cats around.)
            com.kittytalents.entity.SkyFlight.aiGlide(ctx.cat);
            if (!ctx.cat.onGround() && ctx.cat.fallDistance >= 3.0F && ctx.cat.tickCount % 5 == 0 && ctx.cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD, ctx.cat.getX(), ctx.cat.getY() + 0.1, ctx.cat.getZ(), 2, 0.1, 0.05, 0.1, 0.01);
            }
            if (ctx.mastered() && ctx.cat.tickCount % 10 == 0) {
                Player o = ctx.ownerWithin(16.0D);
                if (o != null && !o.onGround() && o.getDeltaMovement().y < -0.5D) TalentHelpers.aura(o, MobEffects.SLOW_FALLING, 40, 0);
            }
        }
    }

    /** Faster while crouch-stalking; safer drops (CatCliffSafetyHelper). Mastery "Featherfall": the owner nearby takes no fall damage. */
    static final class CushionPaws implements TalentEffect {
        @Override public int tickInterval() { return 10; }
        @Override public void onTick(TalentContext ctx) {
            if (ctx.cat.isCrouching()) TalentHelpers.aura(ctx.cat, MobEffects.MOVEMENT_SPEED, 20, 1);
        }
        @Override public float onOwnerFall(TalentContext ctx, Player owner, float distance) {
            return ctx.mastered() ? 0.0F : 1.0F;
        }
    }

    /** Mining (Miner job, habitat yields). Mastery "Prospector": underground, the owner nearby gets Haste II. */
    static final class MoleCat implements TalentEffect {
        @Override public boolean worksWhileResting() { return true; }
        @Override public int tickInterval() { return 100; }
        @Override public void onTick(TalentContext ctx) {
            if (!ctx.mastered()) return;
            Player o = ctx.ownerWithin(16.0D);
            if (o != null && o.getY() < o.level().getHeight(Heightmap.Types.WORLD_SURFACE, o.getBlockX(), o.getBlockZ()) - 8) {
                TalentHelpers.aura(o, MobEffects.DIG_SPEED, 200, 1);
            }
        }
    }

    /** Below half health in a fight: Speed, Haste and Energy every 2 s. Mastery "Bloodlust": heals 20% of melee damage dealt. */
    static final class FrenzyFocus implements TalentEffect {
        @Override public int tickInterval() { return 40; }
        @Override public void onTick(TalentContext ctx) {
            if (ctx.cat.getHealth() <= ctx.cat.getMaxHealth() * 0.5F && ctx.cat.getTarget() != null) {
                ctx.cat.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 1, false, false, true));
                FelineStatusEffects.applyStatus(ctx.cat, FelineStatusType.HASTE, 1, 50);
                FelineEnergyEngine.restoreEnergy(ctx.cat, 5 + ctx.rank * 2);
            }
        }
        @Override public void onHitDealt(TalentContext ctx, LivingEntity t, float dmg) {
            if (ctx.mastered()) ctx.cat.heal(dmg * 0.2F);
        }
    }

    /**
     * Brews a random buff for the owner every 80 s; hits may afflict Weakness, Freeze or Poison.
     * Mastery "Splash Brew": hit afflictions splash onto foes within 3 blocks, and every hit draws the owner's harmful
     * effects (poison, wither, slowness, ...) out of them and onto the enemy that was hit.
     * (Fixed: procs used to fire on any damage the cat caused, including its own AoE, now melee hits only.)
     */
    static final class AlchemyKitty implements TalentEffect {
        @Override public boolean worksWhileResting() { return true; }
        @Override public int tickInterval() { return 1600; }
        @Override public void onTick(TalentContext ctx) {
            Player o = ctx.ownerWithin(8.0D);
            if (o == null) return;
            MobEffectInstance[] options = {
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1), new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0),
                    new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0), new MobEffectInstance(MobEffects.DIG_SPEED, 400, 1)};
            o.addEffect(options[ctx.cat.getRandom().nextInt(options.length)]);
            ctx.cat.playSound(SoundEvents.BREWING_STAND_BREW, 0.8F, 1.2F);
        }
        @Override public void onHitDealt(TalentContext ctx, LivingEntity victim, float dmg) {
            if (ctx.mastered()) transferOwnerDebuffs(ctx, victim);
            if (ctx.cat.getRandom().nextFloat() >= 0.15F + ctx.rank * 0.07F) return;
            int roll = ctx.cat.getRandom().nextInt(3);
            afflict(victim, roll);
            if (ctx.mastered()) TalentHelpers.foesAround(ctx.cat, victim, 3.0D).forEach(e -> afflict(e, roll));
            ctx.cat.playSound(SoundEvents.BREWING_STAND_BREW, 0.8F, 1.6F);
            if (ctx.cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.WITCH, victim.getX(), victim.getY() + 0.5, victim.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
            }
        }
        /** Mastery: move every harmful effect from the nearby owner onto {@code victim} (same strength, remaining time). */
        static void transferOwnerDebuffs(TalentContext ctx, LivingEntity victim) {
            Player o = ctx.ownerWithin(16.0D);
            if (o == null || !victim.isAlive()) return;
            var harmful = o.getActiveEffects().stream()
                    .filter(e -> e.getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL)
                    .map(MobEffectInstance::new).toList();
            if (harmful.isEmpty()) return;
            for (MobEffectInstance e : harmful) {
                o.removeEffect(e.getEffect());
                victim.addEffect(new MobEffectInstance(e.getEffect(), e.getDuration(), e.getAmplifier()));
            }
            ctx.cat.playSound(SoundEvents.BREWING_STAND_BREW, 1.0F, 0.8F);
            if (ctx.cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.WITCH, o.getX(), o.getY() + 1.0, o.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
                sl.sendParticles(ParticleTypes.WITCH, victim.getX(), victim.getY() + 0.8, victim.getZ(), 16, 0.3, 0.4, 0.3, 0.08);
            }
        }

        private static void afflict(LivingEntity e, int roll) {
            switch (roll) {
                case 0 -> { e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1)); FelineStatusEffects.applyStatus(e, FelineStatusType.WEAKNESS, 2, 120); }
                case 1 -> { e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2)); FelineStatusEffects.applyStatus(e, FelineStatusType.FREEZE, 2, 80); }
                default -> { e.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1)); FelineStatusEffects.applyStatus(e, FelineStatusType.POISON, 2, 100); }
            }
        }
    }

    /**
     * Emergency shockwave below 25% health (20 s cooldown); melee hits may burst catnip on nearby foes.
     * Mastery "Second Wind": the emergency shockwave also heals the cat by 30%.
     * (Fixed: the burst used explosion damage credited to the cat, which re-triggered itself on every monster it hit.)
     */
    static final class CatnipCatastrophe implements TalentEffect {
        @Override public int tickInterval() { return 5; }
        @Override public void onTick(TalentContext ctx) {
            if (ctx.cat.getHealth() >= ctx.cat.getMaxHealth() * 0.25F || !ctx.ready("shockwave")) return;
            ctx.cooldown("shockwave", 400);
            ctx.cat.playSound(SoundEvents.GENERIC_EXPLODE, 1.0F, 1.5F);
            for (LivingEntity m : TalentHelpers.foesAround(ctx.cat, ctx.cat, 6.0D)) {
                Vec3 diff = m.position().subtract(ctx.cat.position());
                Vec3 blast = diff.lengthSqr() > 1.0E-4D ? diff.normalize().scale(1.8D) : new Vec3(0, 0.5D, 0);
                m.push(blast.x, 0.5D, blast.z);
                m.hurt(ctx.cat.damageSources().mobAttack(ctx.cat), 8.0F + ctx.rank * 3.0F);
            }
            if (ctx.mastered()) ctx.cat.heal(ctx.cat.getMaxHealth() * 0.3F);
            if (ctx.cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, ctx.cat.getX(), ctx.cat.getY() + 0.5D, ctx.cat.getZ(), 1, 0, 0, 0, 0);
            }
        }
        @Override public void onHitDealt(TalentContext ctx, LivingEntity victim, float dmg) {
            if (!(ctx.cat.getHealth() < ctx.cat.getMaxHealth() * 0.4F || ctx.cat.getRandom().nextFloat() < 0.20F)) return;
            float aoe = 3.0F + ctx.rank * 2.0F;
            for (LivingEntity m : TalentHelpers.foesAround(ctx.cat, victim, 3.5D + ctx.rank * 0.5D)) {
                m.hurt(ctx.cat.damageSources().magic(), aoe); // not credited as a cat melee hit: cannot re-trigger itself
                m.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 1));
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            }
            ctx.cat.playSound(SoundEvents.GENERIC_EXPLODE, 0.6F, 1.8F);
            if (ctx.cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.EXPLOSION, ctx.cat.getX(), ctx.cat.getY() + 0.3, ctx.cat.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
            }
        }
    }
}
