package com.kittytalents.compat.apotheosis;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public class ApotheosisCompat {
    private static Boolean loaded = null;

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded("apotheosis") || ModList.get().isLoaded("attributeslib");
        }
        return loaded;
    }

    private static final String[] ATTRIBUTES = new String[] {
            "crit_chance",
            "crit_damage",
            "cold_damage",
            "fire_damage",
            "life_steal",
            "armor_pierce",
            "armor_shred",
            "dodge_chance",
            "overheal",
            "healing_received"
    };

    public static void registerAttributes(AttributeSupplier.Builder builder) {
        if (!isLoaded()) return;
        for (String name : ATTRIBUTES) {
            Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib", name));
            if (attr != null) {
                double base = "crit_damage".equals(name) ? 1.5D : 0.0D;
                builder.add(attr, base);
            }
        }
    }

    public static double getAttributeValue(TalentedCat cat, String attributeName) {
        if (!isLoaded()) return 0.0D;
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib", attributeName));
        if (attr != null && cat.getAttributes().hasAttribute(attr)) {
            return cat.getAttributeValue(attr);
        }
        return 0.0D;
    }

    public static float applyAttackModifiers(TalentedCat cat, Entity target, float damage) {
        if (!isLoaded() || !(target instanceof LivingEntity living)) return damage;

        // 1. Crit Chance & Crit Damage
        double critChance = getAttributeValue(cat, "crit_chance");
        if (critChance > 0.0D && cat.getRandom().nextFloat() < critChance) {
            double critMult = Math.max(1.5D, getAttributeValue(cat, "crit_damage"));
            damage *= (float) critMult;
            cat.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.2F);
            if (cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + 0.5D, target.getZ(), 12, 0.3, 0.3, 0.3, 0.1);
            }
        }

        // 2. Fire Damage
        double fireDamage = getAttributeValue(cat, "fire_damage");
        if (fireDamage > 0.0D) {
            living.setSecondsOnFire((int) Math.max(1, fireDamage));
            damage += (float) fireDamage;
        }

        // 3. Cold Damage
        double coldDamage = getAttributeValue(cat, "cold_damage");
        if (coldDamage > 0.0D) {
            living.setTicksFrozen((int) (coldDamage * 40));
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            damage += (float) coldDamage;
        }

        // 4. Life Steal
        double lifeSteal = getAttributeValue(cat, "life_steal");
        if (lifeSteal > 0.0D && damage > 0.0F) {
            float healed = (float) (damage * lifeSteal);
            cat.heal(healed);
            if (cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HEART, cat.getX(), cat.getY() + 0.6D, cat.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
            }
        }

        // 5. Armor Pierce
        double armorPierce = getAttributeValue(cat, "armor_pierce");
        if (armorPierce > 0.0D) {
            living.invulnerableTime = 0;
            damage += (float) (armorPierce * 2.0D);
        }

        return damage;
    }

    public static boolean checkDodge(TalentedCat cat, DamageSource source) {
        if (!isLoaded()) return false;
        double dodgeChance = getAttributeValue(cat, "dodge_chance");
        if (dodgeChance > 0.0D && cat.getRandom().nextFloat() < dodgeChance) {
            cat.playSound(SoundEvents.GENERIC_SWIM, 0.8F, 1.8F);
            if (cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.POOF, cat.getX(), cat.getY() + 0.3D, cat.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
            }
            return true;
        }
        return false;
    }
}
