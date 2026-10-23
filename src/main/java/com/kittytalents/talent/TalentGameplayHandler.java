package com.kittytalents.talent;

import com.kittytalents.combat.FelineEnergyEngine;
import com.kittytalents.combat.FelineStatusEffects;
import com.kittytalents.combat.FelineStatusType;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Event-driven gameplay engine wiring all remaining feline talents into active Minecraft mechanics (R8).
 * Guarantees zero "facade" or decorative talents by applying real stat changes,
 * potion procs, gliding physics, and status effects.
 */
@Mod.EventBusSubscriber(modid = "kittytalents")
public class TalentGameplayHandler {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof TalentedCat cat && !cat.level().isClientSide && cat.isAlive()) {

            // 1. SKY_FELINE: Aerial Gliding and Slow Falling in Mid-Air
            int skyLvl = cat.getTalentLevel(Talents.SKY_FELINE);
            if (skyLvl > 0 && !cat.onGround() && !cat.isInWater()) {
                Vec3 mov = cat.getDeltaMovement();
                if (mov.y < -0.15) {
                    // Glide physics: cap downward speed and boost forward glide
                    double glideFall = Math.max(-0.06 - (skyLvl * 0.01), mov.y * 0.6);
                    Vec3 look = cat.getLookAngle();
                    double horizBoost = 1.05 + (skyLvl * 0.04);
                    cat.setDeltaMovement(mov.x * horizBoost, glideFall, mov.z * horizBoost);
                    cat.hasImpulse = true;

                    if (cat.tickCount % 5 == 0 && cat.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.CLOUD, cat.getX(), cat.getY() + 0.1, cat.getZ(), 2, 0.1, 0.05, 0.1, 0.01);
                    }
                }
            }

            // 2. AQUA_KITTY: Water Breathing and Swift Swimming
            int aquaLvl = cat.getTalentLevel(Talents.AQUA_KITTY);
            if (aquaLvl > 0 && cat.isInWater()) {
                cat.setAirSupply(cat.getMaxAirSupply());
                Vec3 mov = cat.getDeltaMovement();
                if (mov.lengthSqr() > 0.001) {
                    double swimSpeedMult = 1.15 + (aquaLvl * 0.06);
                    cat.setDeltaMovement(mov.x * swimSpeedMult, mov.y * 1.05, mov.z * swimSpeedMult);
                }
                if (cat.isOnFire()) cat.clearFire();
            }

            // 3. CUSHION_PAWS: Stealth Prowl Speed
            int cushionLvl = cat.getTalentLevel(Talents.CUSHION_PAWS);
            if (cushionLvl > 0 && cat.isCrouching()) {
                cat.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 1, false, false, false));
            }

            // 4. FRENZY_FOCUS: Emergency Combat Surge
            int frenzyLvl = cat.getTalentLevel(Talents.FRENZY_FOCUS);
            if (frenzyLvl > 0 && cat.getHealth() <= cat.getMaxHealth() * 0.5F && cat.getTarget() != null) {
                if (cat.tickCount % 40 == 0) {
                    cat.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 1, false, false, true));
                    FelineStatusEffects.applyStatus(cat, FelineStatusType.HASTE, 1, 50);
                    FelineEnergyEngine.restoreEnergy(cat, 5 + frenzyLvl * 2);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity attackerEntity = event.getSource().getEntity();
        LivingEntity victim = event.getEntity();

        if (attackerEntity instanceof TalentedCat cat && !cat.level().isClientSide) {
            // 5. ALCHEMY_KITTY: Debilitating potion brew procs on hit
            int alchemyLvl = cat.getTalentLevel(Talents.ALCHEMY_KITTY);
            if (alchemyLvl > 0) {
                float procChance = 0.15F + (alchemyLvl * 0.07F); // up to 50%
                if (cat.getRandom().nextFloat() < procChance) {
                    int roll = cat.getRandom().nextInt(3);
                    switch (roll) {
                        case 0 -> {
                            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                            FelineStatusEffects.applyStatus(victim, FelineStatusType.WEAKNESS, 2, 120);
                        }
                        case 1 -> {
                            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                            FelineStatusEffects.applyStatus(victim, FelineStatusType.FREEZE, 2, 80);
                        }
                        case 2 -> {
                            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                            FelineStatusEffects.applyStatus(victim, FelineStatusType.POISON, 2, 100);
                        }
                    }
                    cat.playSound(SoundEvents.BREWING_STAND_BREW, 0.8F, 1.6F);
                    if (cat.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.WITCH, victim.getX(), victim.getY() + 0.5, victim.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
                    }
                }
            }

            // 6. CATNIP_CATASTROPHE: Catnip Explosive Burst in battle
            int catnipLvl = cat.getTalentLevel(Talents.CATNIP_CATASTROPHE);
            if (catnipLvl > 0 && (cat.getHealth() < cat.getMaxHealth() * 0.4F || cat.getRandom().nextFloat() < 0.20F)) {
                AABB burstAoe = cat.getBoundingBox().inflate(3.5D + catnipLvl * 0.5D);
                List<Monster> monsters = cat.level().getEntitiesOfClass(Monster.class, burstAoe,
                        m -> m != victim && m.isAlive());

                float aoeDmg = 3.0F + (catnipLvl * 2.0F);
                for (Monster m : monsters) {
                    m.hurt(cat.damageSources().explosion(cat, cat), aoeDmg);
                    m.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 1));
                    m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                }

                cat.playSound(SoundEvents.GENERIC_EXPLODE, 0.6F, 1.8F);
                if (cat.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.EXPLOSION, cat.getX(), cat.getY() + 0.3, cat.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, cat.getX(), cat.getY() + 0.5, cat.getZ(), 10, 0.4, 0.2, 0.4, 0.05);
                }
            }
        }
    }
}
