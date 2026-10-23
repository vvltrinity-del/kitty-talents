package com.kittytalents.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EntityType;

public enum FelineElement {
    PHYSICAL,
    FIRE,
    WATER,
    FROST,
    POISON,
    ELECTRIC,
    SHADOW,
    ARCANE,
    NATURE;

    public float getDamageMultiplier(FelineElement target) {
        if (this == PHYSICAL) return 1.0f;
        if (this == FIRE) {
            if (target == NATURE || target == FROST || target == POISON) return 2.0f;
            if (target == FIRE || target == WATER) return 0.5f;
        }
        if (this == WATER) {
            if (target == FIRE) return 2.0f;
            if (target == WATER || target == NATURE) return 0.5f;
        }
        if (this == FROST) {
            if (target == WATER || target == NATURE) return 2.0f;
            if (target == FIRE || target == FROST) return 0.5f;
        }
        if (this == ELECTRIC) {
            if (target == WATER) return 2.0f;
            if (target == NATURE || target == ELECTRIC) return 0.5f;
        }
        if (this == NATURE) {
            if (target == WATER || target == ELECTRIC) return 2.0f;
            if (target == FIRE || target == POISON || target == NATURE) return 0.5f;
        }
        if (this == POISON) {
            if (target == NATURE) return 2.0f;
            if (target == POISON || target == SHADOW) return 0.5f;
        }
        if (this == SHADOW) {
            if (target == ARCANE || target == PHYSICAL) return 2.0f;
            if (target == SHADOW) return 0.5f;
        }
        if (this == ARCANE) {
            if (target == SHADOW || target == PHYSICAL) return 2.0f;
            if (target == ARCANE) return 0.5f;
        }
        return 1.0f;
    }

    public static FelineElement fromDamageSource(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) return FIRE;
        if (source.is(DamageTypeTags.IS_FREEZING)) return FROST;
        if (source.is(DamageTypeTags.IS_LIGHTNING)) return ELECTRIC;
        String msgId = source.getMsgId();
        if (msgId != null) {
            if (msgId.contains("magic")) return ARCANE;
            if (msgId.contains("wither") || msgId.contains("shadow")) return SHADOW;
            if (msgId.contains("poison")) return POISON;
            if (msgId.contains("thorns") || msgId.contains("cactus") || msgId.contains("sweetBerry")) return NATURE;
            if (msgId.contains("drown")) return WATER;
        }
        return PHYSICAL;
    }

    public static FelineElement fromEntityType(EntityType<?> type) {
        if (type == EntityType.BLAZE || type == EntityType.MAGMA_CUBE || type == EntityType.GHAST) return FIRE;
        if (type == EntityType.STRAY || type == EntityType.SNOW_GOLEM || type == EntityType.POLAR_BEAR) return FROST;
        if (type == EntityType.DROWNED || type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN || type == EntityType.DOLPHIN || type == EntityType.SQUID || type == EntityType.GLOW_SQUID) return WATER;
        if (type == EntityType.CREEPER) return ELECTRIC;
        if (type == EntityType.CAVE_SPIDER || type == EntityType.PUFFERFISH) return POISON;
        if (type == EntityType.SPIDER || type == EntityType.SLIME || type == EntityType.SILVERFISH || type == EntityType.BEE) return NATURE;
        if (type == EntityType.ENDERMAN || type == EntityType.ENDERMITE || type == EntityType.ENDER_DRAGON || type == EntityType.WITHER || type == EntityType.WITHER_SKELETON || type == EntityType.WARDEN) return SHADOW;
        if (type == EntityType.WITCH || type == EntityType.ILLUSIONER || type == EntityType.EVOKER) return ARCANE;
        return PHYSICAL;
    }
}

