package com.kittytalents.combat;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModSounds;
import com.kittytalents.talent.Talents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Core real-time combat engine for Kitty Talents 3.0 (R2, R3, R5, R6, R7, R8).
 * Manages combos, 4-way dodge, perfect dodge, Pounce 2.0 physics, elemental matchups,
 * and talent-driven combat mechanics.
 */
@Mod.EventBusSubscriber(modid = "kittytalents")
public class FelineCombatEngine {

    public enum DodgeDirection {
        FORWARD(0.0f),
        BACKWARD(180.0f),
        LEFT(90.0f),
        RIGHT(-90.0f);

        private final float yawOffset;
        DodgeDirection(float yawOffset) { this.yawOffset = yawOffset; }
        public float getYawOffset() { return yawOffset; }
    }


    /**
     * Executes a directional 4-way dodge consuming energy and granting brief invulnerability.
     */
    public static boolean performDodge(TalentedCat cat, DodgeDirection direction) {
        if (cat == null || cat.level().isClientSide || !cat.isAlive() || cat.isIncapacitated()) {
            return false;
        }

        int energyCost = 20;
        int agilityLvl = cat.getTalentLevel(Talents.FELINE_AGILITY);
        energyCost = Math.max(10, energyCost - (agilityLvl * 2));

        if (!FelineEnergyEngine.consumeEnergy(cat, energyCost)) {
            return false; // Not enough energy
        }

        long gameTime = cat.level().getGameTime();

        // 12 ticks of dodge invulnerability (0.6s)
        cat.combat().dodgeEnd = gameTime + 12;
        // First 6 ticks are the Perfect Dodge window (0.3s)
        // Feline Reflexes mastery "Perfect Reflexes": the perfect-dodge window is doubled.
        cat.combat().perfectDodgeEnd = gameTime + (com.kittytalents.talent.effect.TalentEngine.isMastered(cat, Talents.FELINE_REFLEXES) ? 12 : 6);

        // Directional impulse vector
        float targetYaw = cat.getYRot() + direction.getYawOffset();
        double rad = Math.toRadians(targetYaw);
        double mx = -Math.sin(rad) * 0.95;
        double mz = Math.cos(rad) * 0.95;
        double my = cat.onGround() ? 0.18 : 0.05;

        cat.setDeltaMovement(mx, my, mz);
        cat.hasImpulse = true;
        cat.playAnimation(TalentedCat.ANIM_DODGE);

        cat.level().playSound(null, cat.getX(), cat.getY(), cat.getZ(),
                ModSounds.CAT_DODGE.get(), SoundSource.NEUTRAL, 0.9F, 1.2F);

        if (cat.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, cat.getX(), cat.getY() + 0.2, cat.getZ(), 6, 0.2, 0.1, 0.2, 0.05);
        }

        return true;
    }

    /**
     * Executes Pounce 2.0 with a realistic physics parabolic launch trajectory.
     */
    public static boolean launchPounce(TalentedCat cat, Vec3 targetPos) {
        return launchPounce(cat, targetPos, true);
    }

    public static boolean launchPounce(TalentedCat cat, Vec3 targetPos, boolean deductEnergy) {
        if (cat == null || cat.level().isClientSide || !cat.isAlive() || cat.isIncapacitated()) {
            return false;
        }

        if (deductEnergy) {
            int energyCost = 20;
            if (!FelineEnergyEngine.consumeEnergy(cat, energyCost)) {
                return false;
            }
        }

        int pounceTalent = Math.max(1, cat.getTalentLevel(Talents.POUNCE));
        Vec3 start = cat.position();
        Vec3 dir;

        if (targetPos != null) {
            dir = targetPos.subtract(start);
        } else {
            dir = cat.getLookAngle().scale(6.0);
        }

        double horizDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        double horizSpeed = Math.min(2.0, Math.max(0.8, horizDist * 0.22)) * (1.1 + pounceTalent * 0.06);
        double vertSpeed = 0.52 + (pounceTalent * 0.04);

        // Zero-length direction (target directly above/below) would normalize to NaN velocity: fall back to look.
        Vec3 flat = horizDist > 1.0E-3 ? new Vec3(dir.x, 0, dir.z) : new Vec3(cat.getLookAngle().x, 0, cat.getLookAngle().z);
        Vec3 horizNorm = flat.lengthSqr() > 1.0E-6 ? flat.normalize() : new Vec3(0, 0, 1);
        cat.setDeltaMovement(horizNorm.x * horizSpeed, vertSpeed, horizNorm.z * horizSpeed);
        cat.hasImpulse = true;

        cat.combat().pounceActive = true;
        cat.combat().pounceStart = cat.level().getGameTime();
        cat.playAnimation(TalentedCat.ANIM_POUNCE);

        cat.playSound(ModSounds.CAT_POUNCE_LAUNCH.get(), 1.0F, 1.2F);
        if (cat.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT, cat.getX(), cat.getY() + 0.2, cat.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
        }

        return true;
    }

    /**
     * Resolves landing impact of Pounce 2.0 physics arc.
     */
    private static void checkPounceLanding(TalentedCat cat) {
        if (!cat.combat().pounceActive) return;

        long elapsed = cat.level().getGameTime() - cat.combat().pounceStart;
        if (elapsed > 4 && (cat.onGround() || cat.verticalCollision || elapsed > 40)) {
            cat.combat().pounceActive = false;

            int pounceTalent = Math.max(1, cat.getTalentLevel(Talents.POUNCE));
            double radius = 2.5 + (pounceTalent * 0.35);
            AABB impactBox = cat.getBoundingBox().inflate(radius);

            float baseDamage = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5F + (pounceTalent * 2.0F);

            // Only foes: never villagers, other players' pets, passive bystanders or friends.
            List<LivingEntity> enemies = cat.level().getEntitiesOfClass(LivingEntity.class, impactBox,
                    e -> com.kittytalents.ai.CatTargeting.canHarm(cat, e));

            for (LivingEntity enemy : enemies) {
                enemy.hurt(cat.damageSources().mobAttack(cat), baseDamage);
                enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));

                // Apply cat's primary element
                FelineStatusEffects.applyElementStatus(enemy, cat.getPrimaryElement(), 1, 80);
                if (com.kittytalents.talent.effect.TalentEngine.isMastered(cat, Talents.POUNCE)) {
                    // Pounce Training mastery "Apex Pounce": landings stun.
                    enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4));
                    enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1));
                }

                // Small radial knockback
                Vec3 push = enemy.position().subtract(cat.position()).normalize().scale(0.65);
                enemy.push(push.x, 0.25, push.z);
            }

            cat.playSound(ModSounds.CAT_POUNCE_IMPACT.get(), 0.9F, 1.2F);
            if (cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.EXPLOSION, cat.getX(), cat.getY(), cat.getZ(), 2, 0.3, 0.1, 0.3, 0.0);
                sl.sendParticles(ParticleTypes.CRIT, cat.getX(), cat.getY() + 0.2, cat.getZ(), 16, 0.4, 0.2, 0.4, 0.15);
            }
        }
    }

    /**
     * Intercepts attacks on cats to evaluate Dodge and Perfect Dodge (R6).
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        // Void / kill damage can never be dodged (a dodging cat below the world would fall forever).
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (event.getEntity() instanceof TalentedCat cat) {
            long gameTime = cat.level().getGameTime();

            long dodgeEnd = cat.combat().dodgeEnd;
            if (gameTime <= dodgeEnd) {
                // Dodged!
                event.setCanceled(true);

                long perfectDodgeEnd = cat.combat().perfectDodgeEnd;
                if (gameTime <= perfectDodgeEnd) {
                    // PERFECT DODGE!
                    handlePerfectDodge(cat, event.getSource().getEntity());
                } else {
                    cat.playSound(ModSounds.CAT_DODGE.get(), 0.8F, 1.2F);
                }
                return;
            }

            // Passive dodge from Feline Reflexes talent (R8)
            int reflexesLvl = cat.getTalentLevel(Talents.FELINE_REFLEXES);
            if (reflexesLvl > 0) {
                float passiveDodgeChance = reflexesLvl * com.kittytalents.talent.TalentTuning.REFLEX_DODGE_PER_RANK.f(); // up to 35%
                if (cat.getRandom().nextFloat() < passiveDodgeChance) {
                    event.setCanceled(true);
                    cat.playSound(ModSounds.CAT_DODGE.get(), 0.7F, 1.4F);
                    if (cat.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.POOF, cat.getX(), cat.getY() + 0.3, cat.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
                    }
                }
            }
        }
    }

    private static void handlePerfectDodge(TalentedCat cat, Entity attacker) {
        // Slowdown and weaken attacker
        if (attacker instanceof LivingEntity livingAttacker) {
            livingAttacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
            livingAttacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
        }

        // Energy refund: +25 energy (net positive!)
        FelineEnergyEngine.restoreEnergy(cat, 25);

        // Sound & visual feedback
        cat.playSound(ModSounds.CAT_PERFECT_DODGE.get(), 1.0F, 1.2F);
        cat.playSound(SoundEvents.BELL_RESONATE, 0.6F, 2.0F);

        if (cat.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, cat.getX(), cat.getY() + 0.4, cat.getZ(), 12, 0.3, 0.3, 0.3, 0.1);
            sl.sendParticles(ParticleTypes.FLASH, cat.getX(), cat.getY() + 0.4, cat.getZ(), 1, 0, 0, 0, 0);
        }

        if (cat.getOwner() instanceof Player owner) {
            owner.displayClientMessage(Component.translatable("combat.kittytalents.perfect_dodge", cat.getName()), true);
        }
    }

    /**
     * Intercepts incoming damage for Cushion Paws (fall damage negation - R8).
     */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof TalentedCat cat) {
            int cushionLvl = cat.getTalentLevel(Talents.CUSHION_PAWS);
            if (cushionLvl > 0) {
                event.setCanceled(true);
                if (cushionLvl >= 3) {
                    cat.playSound(SoundEvents.SLIME_SQUISH, 0.6F, 1.5F);
                }
            }
        }
    }

    /**
     * Central damage calculation hook for combos, elements, status effects, and talents (R2, R6, R7, R8).
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        LivingEntity victim = event.getEntity();
        Entity attackerEntity = source.getEntity();

        // 1. Target Status Modifiers (Victim buffs/debuffs)
        if (FelineStatusEffects.hasStatus(victim, FelineStatusType.MARKED)) {
            event.setAmount(event.getAmount() * 1.30F);
        }
        if (FelineStatusEffects.hasStatus(victim, FelineStatusType.VULNERABLE)) {
            event.setAmount(event.getAmount() * 1.25F);
        }
        if (FelineStatusEffects.hasStatus(victim, FelineStatusType.FORTIFIED)) {
            event.setAmount(event.getAmount() * 0.70F);
        }

        // 2. Attacker Status Modifiers
        if (attackerEntity instanceof LivingEntity livingAttacker) {
            if (FelineStatusEffects.hasStatus(livingAttacker, FelineStatusType.WEAKNESS)) {
                event.setAmount(event.getAmount() * 0.70F);
            }
            if (FelineStatusEffects.hasStatus(livingAttacker, FelineStatusType.HASTE)) {
                event.setAmount(event.getAmount() * 1.15F);
            }
        }

        // 3. Cat Attacker Combat Logic
        if (attackerEntity instanceof TalentedCat cat && !cat.level().isClientSide) {
            float damage = event.getAmount();
            long gameTime = cat.level().getGameTime();

            // A) Combo System (R6)
            long lastAttack = cat.combat().lastAttackTime;
            long comboExpiry = cat.combat().comboExpiry;
            int step = cat.combat().comboStep;

            boolean inComboWindow = (gameTime <= comboExpiry && gameTime >= lastAttack + 4);
            if (inComboWindow) {
                step = (step % 4) + 1;
            } else {
                step = 1;
            }

            cat.combat().comboStep = step;
            cat.combat().lastAttackTime = gameTime;
            cat.combat().comboExpiry = gameTime + 35; // 1.75s combo window

            // Check for Perfect Attack Timing Window (between 8 and 18 ticks)
            boolean perfectTiming = inComboWindow && (gameTime - lastAttack >= 8 && gameTime - lastAttack <= 18);
            if (perfectTiming) {
                damage *= 1.25F;
                FelineEnergyEngine.restoreEnergy(cat, 10);
                cat.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 0.8F, 1.5F);
                if (cat.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight()/2, victim.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
                }
            }

            // Apply combo step damage and effects
            switch (step) {
                case 1 -> {
                    cat.playSound(SoundEvents.PLAYER_ATTACK_WEAK, 0.7F, 1.2F);
                    FelineEnergyEngine.restoreEnergy(cat, 3);
                }
                case 2 -> {
                    damage *= 1.15F;
                    cat.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 0.8F, 1.3F);
                    FelineEnergyEngine.restoreEnergy(cat, 5);
                }
                case 3 -> {
                    damage *= 1.30F;
                    cat.playSound(SoundEvents.FOX_BITE, 0.9F, 1.2F);
                    FelineStatusEffects.applyStatus(victim, FelineStatusType.BLEED, 1, 60);
                    FelineEnergyEngine.restoreEnergy(cat, 7);
                }
                case 4 -> {
                    // Heavy Claw Finisher
                    damage *= 1.60F;
                    cat.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.4F);
                    FelineEnergyEngine.restoreEnergy(cat, 15);
                    if (cat.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY() + 0.4, victim.getZ(), 1, 0, 0, 0, 0);
                    }
                    Vec3 knockback = victim.position().subtract(cat.position()).normalize().scale(0.8);
                    victim.push(knockback.x, 0.2, knockback.z);
                }
            }

            // B) Role Combat Modifiers (R3)
            CatRole role = cat.getCatRole();
            damage *= role.getDamageMultiplier();

            // C) Critical Claws Talent (R8)
            int critLvl = cat.getTalentLevel(Talents.CRITICAL_CLAWS);
            float critChance = role.getCritChanceBonus();
            if (critLvl > 0) {
                critChance += com.kittytalents.talent.TalentTuning.CRIT_CHANCE.f() + (critLvl * com.kittytalents.talent.TalentTuning.CRIT_CHANCE_PER_RANK.f()); // Up to 50% crit chance
            }
            if (cat.getRandom().nextFloat() < critChance) {
                float critMult = com.kittytalents.talent.TalentTuning.CRIT_MULT.f() + (critLvl * com.kittytalents.talent.TalentTuning.CRIT_MULT_PER_RANK.f()); // Up to 2.5x crit damage
                damage *= critMult;
                cat.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 0.9F, 1.4F);
                FelineEnergyEngine.restoreEnergy(cat, 5);
                if (cat.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 0.5, victim.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                }
            }

            // D) Elemental Matchup & Status Application (R2, R7)
            FelineElement catElem = cat.getPrimaryElement();
            FelineElement victimElem = FelineElement.fromEntityType(victim.getType());
            if (victim instanceof TalentedCat victimCat) {
                victimElem = victimCat.getPrimaryElement();
            }

            float elemMult = catElem.getDamageMultiplier(victimElem);
            damage *= elemMult;

            if (elemMult > 1.0F) {
                // Super effective!
                cat.playSound(SoundEvents.ARROW_HIT_PLAYER, 0.8F, 1.5F);
                FelineStatusEffects.applyElementStatus(victim, catElem, 2, 100);
            } else if (elemMult < 1.0F) {
                cat.playSound(SoundEvents.SHIELD_BLOCK, 0.6F, 1.8F);
            } else if (catElem != FelineElement.PHYSICAL) {
                FelineStatusEffects.applyElementStatus(victim, catElem, 1, 60);
            }

            // E) Feline Berserker Lifesteal (R8)
            int berserkLvl = cat.getTalentLevel(Talents.FELINE_BERSERKER);
            if (berserkLvl > 0 && cat.getHealth() < cat.getMaxHealth() * 0.4F) {
                float heal = damage * (0.15F + berserkLvl * 0.05F);
                cat.heal(heal);
                if (cat.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HEART, cat.getX(), cat.getY() + 0.6, cat.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
                }
            }

            // F) Neko Night Combat Scaling for Cat (Bonus Energy & Combat XP)
            if (isNekoNight(cat.level()) && victim instanceof net.minecraft.world.entity.monster.Monster) {
                // Hostile mobs have 25% extra resistance
                damage *= 0.80F;
                // Cats earn bonus Energy (XP gain is not implemented yet; see OVERHAUL_PLAN progression item)
                FelineEnergyEngine.restoreEnergy(cat, 10);
                if (cat.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SOUL, cat.getX(), cat.getY() + 0.6, cat.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
                }
            }

            event.setAmount(damage);
        } else if (attackerEntity instanceof net.minecraft.world.entity.monster.Monster && (victim instanceof TalentedCat || victim instanceof Player)) {
            // Neko Night Combat Scaling for Hostiles (+30% attack damage)
            if (isNekoNight(victim.level())) {
                event.setAmount(event.getAmount() * 1.30F);
                if (victim.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, victim.getX(), victim.getY() + 0.5, victim.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }
    }

    /**
     * Checks if Neko Night is active via Agent 3's NekoNightManager or celestial moon fallback.
     */
    public static boolean isNekoNight(net.minecraft.world.level.Level level) {
        if (level == null) return false;
        try {
            Class<?> clazz = Class.forName("com.kittytalents.entity.ai.NekoNightManager");
            java.lang.reflect.Method m = clazz.getMethod("isNekoNight", net.minecraft.world.level.Level.class);
            return (boolean) m.invoke(null, level);
        } catch (Throwable ignored) {}
        try {
            Class<?> clazz = Class.forName("com.kittytalents.event.NekoNightManager");
            java.lang.reflect.Method m = clazz.getMethod("isNekoNight", net.minecraft.world.level.Level.class);
            return (boolean) m.invoke(null, level);
        } catch (Throwable ignored) {}
        if (level.isNight()) {
            long day = level.getDayTime() / 24000L;
            if (day > 0 && day % 8 == 5) return true;
        }
        return false;
    }

    /**
     * Ticks energy regeneration and pounce landing per tick.
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof TalentedCat cat && !cat.level().isClientSide) {
            FelineEnergyEngine.tick(cat);
            checkPounceLanding(cat);
        }
    }
}
