package com.kittytalents.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real-time status effect and elemental affliction engine for Kitty Talents 3.0 (R7).
 * Handles Burn, Poison, Bleed, Freeze, Shock, Mark, Vulnerable, Weakness,
 * Haste, Regeneration, and Fortified with full anti-stacking safety and immunities.
 */
@Mod.EventBusSubscriber(modid = "kittytalents")
public class FelineStatusEffects {
    private static final Set<UUID> TRACKED_ENTITIES = ConcurrentHashMap.newKeySet();
    private static final String NBT_STATUSES = "FelineStatuses";

    /**
     * Applies a structured FelineStatusType with duration and intensity caps and immunity checks.
     */
    public static void applyStatus(LivingEntity target, FelineStatusType status, int power, int durationTicks) {
        if (target == null || !target.isAlive() || target.level().isClientSide || status == null) return;

        // Immunity checks
        if (status == FelineStatusType.BURN) {
            if (target.fireImmune() || target.isInWaterRainOrBubble()) return;
        } else if (status == FelineStatusType.POISON || status == FelineStatusType.BLEED) {
            if (target.getMobType() == MobType.UNDEAD) return; // Undead do not bleed or suffer organic poison
        } else if (status == FelineStatusType.FREEZE) {
            if (target.canFreeze() == false) return;
        }

        CompoundTag data = target.getPersistentData();
        CompoundTag statuses = data.contains(NBT_STATUSES) ? data.getCompound(NBT_STATUSES) : new CompoundTag();

        int cappedDuration = Math.min(status.getMaxDurationTicks(), Math.max(20, durationTicks));
        int cappedPower = Math.min(status.getMaxPower(), Math.max(1, power));

        String key = status.name();
        CompoundTag effectData = new CompoundTag();
        effectData.putInt("Ticks", cappedDuration);
        effectData.putInt("Power", cappedPower);

        statuses.put(key, effectData);
        data.put(NBT_STATUSES, statuses);
        TRACKED_ENTITIES.add(target.getUUID());
    }

    /**
     * Backward-compatible bridge for elemental status applications.
     */
    public static void applyElementStatus(LivingEntity target, FelineElement element, int power, int durationTicks) {
        if (target == null || element == null || element == FelineElement.PHYSICAL) return;
        switch (element) {
            case FIRE -> applyStatus(target, FelineStatusType.BURN, power, durationTicks);
            case FROST -> applyStatus(target, FelineStatusType.FREEZE, power, durationTicks);
            case POISON -> applyStatus(target, FelineStatusType.POISON, power, durationTicks);
            case ELECTRIC -> applyStatus(target, FelineStatusType.SHOCK, power, durationTicks);
            case SHADOW -> applyStatus(target, FelineStatusType.VULNERABLE, power, durationTicks);
            case ARCANE -> applyStatus(target, FelineStatusType.MARKED, power, durationTicks);
            case NATURE -> applyStatus(target, FelineStatusType.WEAKNESS, power, durationTicks);
            case WATER -> {
                if (target.isOnFire()) target.clearFire();
                applyStatus(target, FelineStatusType.FREEZE, power, durationTicks / 2);
            }
            default -> {}
        }
    }

    public static boolean hasStatus(LivingEntity entity, FelineStatusType status) {
        if (entity == null || status == null) return false;
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_STATUSES)) return false;
        CompoundTag statuses = data.getCompound(NBT_STATUSES);
        return statuses.contains(status.name()) && statuses.getCompound(status.name()).getInt("Ticks") > 0;
    }

    public static int getStatusPower(LivingEntity entity, FelineStatusType status) {
        if (!hasStatus(entity, status)) return 0;
        return entity.getPersistentData().getCompound(NBT_STATUSES).getCompound(status.name()).getInt("Power");
    }

    public static void removeStatus(LivingEntity entity, FelineStatusType status) {
        if (entity == null || status == null) return;
        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_STATUSES)) {
            CompoundTag statuses = data.getCompound(NBT_STATUSES);
            statuses.remove(status.name());
            if (statuses.isEmpty()) {
                data.remove(NBT_STATUSES);
                TRACKED_ENTITIES.remove(entity.getUUID());
            } else {
                data.put(NBT_STATUSES, statuses);
            }
        }
    }

    public static void clearAllStatuses(LivingEntity entity) {
        if (entity == null) return;
        entity.getPersistentData().remove(NBT_STATUSES);
        TRACKED_ENTITIES.remove(entity.getUUID());
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !entity.isAlive() || entity.isRemoved()) {
            if (entity.isRemoved()) {
                TRACKED_ENTITIES.remove(entity.getUUID());
            }
            return;
        }

        if (TRACKED_ENTITIES.isEmpty() || !TRACKED_ENTITIES.contains(entity.getUUID())) {
            return;
        }

        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_STATUSES)) {
            TRACKED_ENTITIES.remove(entity.getUUID());
            return;
        }

        CompoundTag statuses = data.getCompound(NBT_STATUSES);
        boolean changed = false;

        for (String key : statuses.getAllKeys()) {
            CompoundTag effectData = statuses.getCompound(key);
            int ticks = effectData.getInt("Ticks");
            int power = Math.max(1, effectData.getInt("Power"));

            if (ticks <= 0) {
                statuses.remove(key);
                changed = true;
                continue;
            }

            FelineStatusType status = FelineStatusType.fromString(key);
            if (status != null) {
                executeStatusTick(entity, status, power, ticks);
            }

            effectData.putInt("Ticks", ticks - 1);
            changed = true;
        }

        if (statuses.isEmpty()) {
            data.remove(NBT_STATUSES);
            TRACKED_ENTITIES.remove(entity.getUUID());
        } else if (changed) {
            data.put(NBT_STATUSES, statuses);
        }
    }

    private static void executeStatusTick(LivingEntity entity, FelineStatusType status, int power, int ticksRemaining) {
        ServerLevel sl = entity.level() instanceof ServerLevel level ? level : null;
        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() / 2.0;
        double z = entity.getZ();

        switch (status) {
            case BURN -> {
                if (entity.isInWaterRainOrBubble()) {
                    removeStatus(entity, FelineStatusType.BURN);
                    if (sl != null) sl.sendParticles(ParticleTypes.SMOKE, x, y, z, 5, 0.2, 0.2, 0.2, 0.05);
                    return;
                }
                if (ticksRemaining % 20 == 0) {
                    entity.hurt(entity.damageSources().onFire(), 1.0F + power * 0.75F);
                    if (sl != null) sl.sendParticles(ParticleTypes.FLAME, x, y, z, 4, 0.2, 0.2, 0.2, 0.05);
                }
            }
            case POISON -> {
                if (ticksRemaining % 25 == 0) {
                    entity.hurt(entity.damageSources().magic(), 1.0F + power * 0.5F);
                    if (sl != null) sl.sendParticles(ParticleTypes.SNEEZE, x, y, z, 3, 0.2, 0.2, 0.2, 0.05);
                }
            }
            case BLEED -> {
                if (ticksRemaining % 20 == 0) {
                    Vec3 mov = entity.getDeltaMovement();
                    boolean isMoving = (mov.x * mov.x + mov.z * mov.z) > 0.005;
                    float dmg = (1.0F + power * 0.6F) * (isMoving ? 2.0F : 1.0F);
                    entity.hurt(entity.damageSources().generic(), dmg);
                    if (sl != null) {
                        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, isMoving ? 6 : 2, 0.2, 0.2, 0.2, 0.1);
                    }
                }
            }
            case FREEZE -> {
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.65, 1.0, 0.65));
                if (entity.canFreeze()) {
                    entity.setTicksFrozen(Math.min(entity.getTicksRequiredToFreeze() + 40, entity.getTicksFrozen() + 5));
                }
                if (ticksRemaining % 10 == 0 && sl != null) {
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 3, 0.3, 0.3, 0.3, 0.02);
                }
            }
            case SHOCK -> {
                if (ticksRemaining % 30 == 0) {
                    entity.hurt(entity.damageSources().lightningBolt(), 2.0F + power);
                    entity.setDeltaMovement(0, entity.getDeltaMovement().y > 0 ? 0 : entity.getDeltaMovement().y, 0); // micro-stun
                    if (sl != null) sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 6, 0.25, 0.25, 0.25, 0.1);
                }
            }
            case MARKED -> {
                if (ticksRemaining % 20 == 0 && sl != null) {
                    sl.sendParticles(ParticleTypes.CRIT, x, entity.getY() + entity.getBbHeight() + 0.3, z, 2, 0.1, 0.1, 0.1, 0.02);
                }
            }
            case VULNERABLE -> {
                if (ticksRemaining % 25 == 0 && sl != null) {
                    sl.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 2, 0.2, 0.2, 0.2, 0.02);
                }
            }
            case WEAKNESS -> {
                if (ticksRemaining % 20 == 0 && sl != null) {
                    sl.sendParticles(ParticleTypes.SMOKE, x, y, z, 2, 0.2, 0.2, 0.2, 0.02);
                }
            }
            case HASTE -> {
                if (ticksRemaining % 10 == 0 && sl != null && entity.getDeltaMovement().lengthSqr() > 0.01) {
                    sl.sendParticles(ParticleTypes.CLOUD, x, entity.getY(), z, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
            case REGENERATION -> {
                if (ticksRemaining % 25 == 0) {
                    entity.heal(1.0F + power * 0.5F);
                    if (sl != null) sl.sendParticles(ParticleTypes.HEART, x, y + 0.3, z, 2, 0.2, 0.2, 0.2, 0.02);
                }
            }
            case FORTIFIED -> {
                if (ticksRemaining % 20 == 0 && sl != null) {
                    sl.sendParticles(ParticleTypes.ENCHANT, x, y, z, 3, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            CompoundTag data = living.getPersistentData();
            if (data.contains(NBT_STATUSES)) {
                TRACKED_ENTITIES.add(living.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        TRACKED_ENTITIES.remove(event.getEntity().getUUID());
    }
}
