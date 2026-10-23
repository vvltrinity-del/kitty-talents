package com.kittytalents.combat;

import com.kittytalents.entity.ProductionTier;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Visual particle effects (VFX), SFX audio triggers, and combat scaling engine
 * for Kitty Talents 3.0 (R32 Polish Pass).
 * Dynamically scales elemental particle bursts and status afflictions by CatRole and ProductionTier.
 */
public class CatAttackEffects {

    /**
     * Primary attack effects pipeline invoked upon successful feline melee or combo hit.
     * Computes role bonuses, tier scaling, applies elemental status debuffs, and emits custom VFX.
     */
    public static void applyAttackEffects(TalentedCat attacker, LivingEntity target, float baseDamage) {
        if (attacker == null || target == null || attacker.level().isClientSide) return;
        Level level = attacker.level();
        ServerLevel sl = level instanceof ServerLevel serverLevel ? serverLevel : null;
        if (sl == null) return;

        CatRole role = attacker.getCatRole();
        ProductionTier tier = attacker.getProductionTier();
        FelineElement catElem = attacker.getPrimaryElement();

        // 1. Role-Specific Combat Support & Debuffs
        applyRoleCombatEffects(attacker, target, role, baseDamage, sl);

        // 2. Production Tier Scaling
        int tierOrdinal = tier != null ? tier.ordinal() : 0;
        int statusPower = Math.max(1, 1 + (tierOrdinal / 3));
        int statusDuration = 60 + (tierOrdinal * 15);

        // 3. Elemental Affinity Affliction & VFX Burst
        if (catElem != null && catElem != FelineElement.PHYSICAL) {
            FelineStatusEffects.applyElementStatus(target, catElem, statusPower, statusDuration);
            spawnElementalBurst(sl, target.position().add(0, target.getBbHeight() * 0.5, 0), catElem, tierOrdinal);

            // Play elemental audio feedback
            attacker.playSound(ModSounds.ABILITY_ELEMENTAL.get(), 0.75F, 1.1F + (tierOrdinal * 0.05F));
        } else {
            // Physical impact sound & particles
            attacker.playSound(ModSounds.CAT_CLAW_HIT.get(), 0.85F, 1.2F);
            spawnPhysicalHitParticles(sl, target.position().add(0, target.getBbHeight() * 0.5, 0), tierOrdinal);
        }

        // 4. Secondary Bleed chance based on tier
        if (tierOrdinal >= ProductionTier.IRON.ordinal()) {
            if (attacker.getRandom().nextFloat() < (0.15F + tierOrdinal * 0.03F)) {
                FelineStatusEffects.applyStatus(target, FelineStatusType.BLEED, 1, 60 + tierOrdinal * 10);
                spawnBleedParticles(sl, target.position().add(0, target.getBbHeight() * 0.5, 0), tierOrdinal);
            }
        }
    }

    private static void applyRoleCombatEffects(TalentedCat attacker, LivingEntity target, CatRole role, float damage, ServerLevel sl) {
        if (role == null) return;
        Vec3 pos = attacker.position();

        switch (role) {
            case DPS, ASSASSIN -> {
                // Critical strike spark burst
                sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 8, 0.25, 0.25, 0.25, 0.15);
            }
            case TANK, BRUISER -> {
                // Fortifying shield pulse around cat & nearby owner
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y + 0.4, pos.z, 6, 0.3, 0.2, 0.3, 0.05);
                attacker.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, false));
            }
            case SCOUT, MOBILITY -> {
                // Evasive dust whirl & agility buff
                sl.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.2, pos.z, 5, 0.2, 0.1, 0.2, 0.03);
                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, false));
            }
            case HEALER, SUPPORT -> {
                // Restoration burst healing owner and allied cats within 6m
                AABB radius = attacker.getBoundingBox().inflate(6.0);
                List<LivingEntity> allies = attacker.level().getEntitiesOfClass(LivingEntity.class, radius,
                        e -> e == attacker.getOwner() || (e instanceof TalentedCat tc && tc.isOwnedBy(attacker.getOwner())));
                float healAmount = Math.max(1.0F, damage * 0.12F);
                for (LivingEntity ally : allies) {
                    ally.heal(healAmount);
                    sl.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + ally.getBbHeight() * 0.6, ally.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
                }
            }
            case MAGE -> {
                // Arcane mark applying Vulnerable debuff to target
                FelineStatusEffects.applyStatus(target, FelineStatusType.MARKED, 1, 80);
                sl.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 8, 0.3, 0.3, 0.3, 0.08);
            }
            case CONTROLLER, BREAKER -> {
                // Disruptive shockwave slowing the foe
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true, true));
                sl.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 0.3, target.getZ(), 10, 0.3, 0.2, 0.3, 0.1);
            }
            default -> {}
        }
    }

    // ========================================================
    // DEDICATED ELEMENTAL PARTICLE BURSTS
    // ========================================================

    public static void spawnElementalBurst(ServerLevel level, Vec3 pos, FelineElement element, int tier) {
        if (level == null || pos == null || element == null) return;
        int count = 6 + tier * 2;

        switch (element) {
            case FIRE -> spawnBurnParticles(level, pos, tier);
            case FROST -> spawnFrostParticles(level, pos, tier);
            case ELECTRIC -> spawnShockParticles(level, pos, tier);
            case POISON -> spawnPoisonParticles(level, pos, tier);
            case NATURE -> {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, count, 0.3, 0.3, 0.3, 0.05);
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.x, pos.y, pos.z, count * 2, 0.4, 0.4, 0.4, 0.02);
            }
            case WATER -> {
                level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y, pos.z, count * 3, 0.35, 0.25, 0.35, 0.15);
                level.sendParticles(ParticleTypes.BUBBLE_POP, pos.x, pos.y, pos.z, count, 0.3, 0.3, 0.3, 0.05);
            }
            case SHADOW -> {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, count, 0.3, 0.3, 0.3, 0.05);
                level.sendParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, count / 2 + 1, 0.2, 0.2, 0.2, 0.02);
            }
            case ARCANE -> {
                level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, count * 2, 0.4, 0.4, 0.4, 0.2);
                level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y, pos.z, count, 0.25, 0.25, 0.25, 0.05);
            }
            default -> spawnPhysicalHitParticles(level, pos, tier);
        }
    }

    public static void spawnBurnParticles(ServerLevel level, Vec3 pos, int tier) {
        if (level == null || pos == null) return;
        int count = 8 + tier * 2;
        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, count, 0.3, 0.3, 0.3, 0.05);
        level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, Math.max(1, tier / 2), 0.2, 0.2, 0.2, 0.0);
        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.2, pos.z, 4 + tier, 0.2, 0.2, 0.2, 0.02);
    }

    public static void spawnFrostParticles(ServerLevel level, Vec3 pos, int tier) {
        if (level == null || pos == null) return;
        int count = 10 + tier * 3;
        level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, count, 0.35, 0.35, 0.35, 0.04);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, pos.x, pos.y, pos.z, 4 + tier, 0.25, 0.25, 0.25, 0.08);
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 3 + tier / 2, 0.2, 0.2, 0.2, 0.02);
    }

    public static void spawnShockParticles(ServerLevel level, Vec3 pos, int tier) {
        if (level == null || pos == null) return;
        int count = 12 + tier * 3;
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, count, 0.35, 0.35, 0.35, 0.12);
        if (tier >= ProductionTier.IRON.ordinal()) {
            level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public static void spawnPoisonParticles(ServerLevel level, Vec3 pos, int tier) {
        if (level == null || pos == null) return;
        int count = 10 + tier * 2;
        level.sendParticles(ParticleTypes.SNEEZE, pos.x, pos.y, pos.z, count, 0.3, 0.3, 0.3, 0.05);
        level.sendParticles(ParticleTypes.ENTITY_EFFECT, pos.x, pos.y, pos.z, count / 2 + 1, 0.2, 0.2, 0.2, 0.0);
    }

    public static void spawnBleedParticles(ServerLevel level, Vec3 pos, int tier) {
        if (level == null || pos == null) return;
        int count = 6 + tier * 2;
        // Deep red blood droplet dust (RGB: 0.85, 0.05, 0.05)
        DustParticleOptions bloodDust = new DustParticleOptions(new Vector3f(0.85F, 0.05F, 0.05F), 1.2F);
        level.sendParticles(bloodDust, pos.x, pos.y, pos.z, count * 2, 0.25, 0.25, 0.25, 0.05);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, pos.x, pos.y, pos.z, 2 + tier / 3, 0.2, 0.2, 0.2, 0.05);
    }

    public static void spawnPhysicalHitParticles(ServerLevel level, Vec3 pos, int tier) {
        if (level == null || pos == null) return;
        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 6 + tier, 0.2, 0.2, 0.2, 0.1);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    public static void spawnPounceImpactParticles(ServerLevel level, Vec3 pos, int tier) {
        if (level == null || pos == null) return;
        int count = 16 + tier * 4;
        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 2, 0.3, 0.1, 0.3, 0.0);
        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 0.2, pos.z, count, 0.45, 0.25, 0.45, 0.15);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 6 + tier, 0.3, 0.1, 0.3, 0.03);
    }

    public static void spawnDodgeParticles(ServerLevel level, Vec3 pos, boolean perfect) {
        if (level == null || pos == null) return;
        if (perfect) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.3, pos.z, 14, 0.3, 0.3, 0.3, 0.12);
            level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 0.3, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        } else {
            level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.15, pos.z, 6, 0.2, 0.1, 0.2, 0.05);
            level.sendParticles(ParticleTypes.POOF, pos.x, pos.y + 0.2, pos.z, 3, 0.15, 0.1, 0.15, 0.02);
        }
    }
}
