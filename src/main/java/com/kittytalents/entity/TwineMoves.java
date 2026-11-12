package com.kittytalents.entity;

import com.kittytalents.registry.ModSounds;
import com.kittytalents.talent.Talents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a twined cat can do beyond walking: traversal capabilities (R9), team synergy attacks (R10) and the 4-way dodge (R1).
 * Session state and lifecycle live in {@link TwineManager}; these methods assume the caller already checked
 * {@code CatAction.TWINE_CONTROL}.
 */
public final class TwineMoves {
    private static final Map<UUID, Long> SYNERGY_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> DODGE_COOLDOWNS = new ConcurrentHashMap<>();

    private TwineMoves() {}

    /** Server stop: cooldowns are keyed by cat UUID and must not leak into the next world. */
    public static void clearAll() {
        SYNERGY_COOLDOWNS.clear();
        DODGE_COOLDOWNS.clear();
    }

    // -------------------------------------------------------------------------
    // R9: Feline Traversal / Exploration Capability System
    // -------------------------------------------------------------------------

    public enum FelineCapability {
        CAN_FLY("can_fly", "Aerial Gliding & Sky Flight"),
        CAN_DIG("can_dig", "Burrowing & Subterranean Excavation"),
        CAN_SWIM("can_swim", "Fluid Swimming & Aquatic Diving"),
        CAN_CLIMB("can_climb", "Vertical Wall Climbing"),
        CAN_RETRIEVE("can_retrieve", "Paw Item Retrieval"),
        CAN_LIGHT("can_light", "Torch Placement & Luminescence"),
        CAN_SNIFF("can_sniff", "Whisker Radar & Scent Tracking"),
        CAN_WALL_JUMP("can_wall_jump", "Airborne Wall Leaping"),
        CAN_HIGH_JUMP("can_high_jump", "Feline High Jumping"),
        CAN_PASSAGE_ACCESS("can_passage_access", "Cat-Sized Passage Access");

        private final String id;
        private final String description;

        FelineCapability(String id, String description) {
            this.id = id;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Returns whether the given cat currently possesses the requested exploration capability.
     */
    public static boolean hasCapability(TalentedCat cat, FelineCapability capability) {
        if (cat == null) return false;
        return switch (capability) {
            case CAN_FLY -> cat.getTalentLevel(Talents.SKY_FELINE) > 0;
            case CAN_DIG -> cat.getTalentLevel(Talents.MOLE_CAT) > 0 || cat.getTalentLevel(Talents.KITTY_TOOLS) > 0;
            case CAN_SWIM -> cat.getTalentLevel(Talents.AQUA_KITTY) > 0 || cat.getTalentLevel(Talents.WATER_PELT) > 0;
            case CAN_CLIMB -> cat.getTalentLevel(Talents.FELINE_AGILITY) >= 2;
            case CAN_WALL_JUMP -> cat.getTalentLevel(Talents.FELINE_AGILITY) >= 3;
            case CAN_HIGH_JUMP -> cat.getTalentLevel(Talents.FELINE_AGILITY) >= 1;
            case CAN_RETRIEVE -> cat.getTalentLevel(Talents.PAW_RETRIEVER) > 0;
            case CAN_LIGHT -> cat.getTalentLevel(Talents.TORCH_KITTY) > 0;
            case CAN_SNIFF -> cat.getTalentLevel(Talents.SNIFFER_WHISKERS) > 0 || cat.getTalentLevel(Talents.WHISKER_RADAR) > 0;
            case CAN_PASSAGE_ACCESS -> true; // Inherent feline physiological trait
        };
    }

    /**
     * Returns an unmodifiable set of all active capabilities for a cat.
     */
    public static Set<FelineCapability> getCapabilities(TalentedCat cat) {
        EnumSet<FelineCapability> caps = EnumSet.noneOf(FelineCapability.class);
        if (cat == null) return Collections.emptySet();
        for (FelineCapability cap : FelineCapability.values()) {
            if (hasCapability(cat, cap)) {
                caps.add(cap);
            }
        }
        return Collections.unmodifiableSet(caps);
    }

    /**
     * Executes real server-side traversal/exploration actions triggered by the twined player.
     */
    public static boolean executeTraversalAction(TalentedCat cat, ServerPlayer player, int actionType) {
        if (cat == null || !cat.isAlive() || cat.isIncapacitated()) return false;

        switch (actionType) {
            case 0 -> { // Wall Jump / Leap
                if (hasCapability(cat, FelineCapability.CAN_WALL_JUMP)) {
                    Vec3 look = cat.getLookAngle();
                    cat.setDeltaMovement(-look.x * 0.45D, 0.48D, -look.z * 0.45D);
                    cat.hasImpulse = true;
                    cat.playSound(SoundEvents.PLAYER_SMALL_FALL, 0.8F, 1.2F);
                    if (cat.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.POOF, cat.getX(), cat.getY() + 0.2D, cat.getZ(), 6, 0.2D, 0.1D, 0.2D, 0.05D);
                    }
                    return true;
                }
            }
            case 1 -> { // Dig / Burrow Ahead
                if (hasCapability(cat, FelineCapability.CAN_DIG)) {
                    BlockPos front = cat.blockPosition().relative(cat.getDirection());
                    if (!cat.level().isEmptyBlock(front)) {
                        cat.tryUseEquippedTool();
                        return true;
                    }
                }
            }
            case 2 -> { // Sniff / Whisker Radar Ping
                if (hasCapability(cat, FelineCapability.CAN_SNIFF)) {
                    AABB scan = cat.getBoundingBox().inflate(32.0D);
                    List<LivingEntity> enemies = cat.level().getEntitiesOfClass(LivingEntity.class, scan,
                            e -> e != cat && e != player && !cat.isAlliedTo(e) && e.isAlive());
                    player.displayClientMessage(Component.literal("✦ Sniff Scent: Detected " + enemies.size() + " creature(s) nearby. ✦").withStyle(ChatFormatting.AQUA), true);
                    player.playNotifySound(SoundEvents.SNIFFER_SNIFFING, SoundSource.NEUTRAL, 1.0F, 1.2F);
                    if (cat.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, cat.getX(), cat.getY() + 0.4D, cat.getZ(), 8, 0.4D, 0.2D, 0.4D, 0.05D);
                    }
                    return true;
                }
            }
            case 3 -> { // Item Retrieval
                if (hasCapability(cat, FelineCapability.CAN_RETRIEVE)) {
                    AABB itemBox = cat.getBoundingBox().inflate(6.0D);
                    List<ItemEntity> items = cat.level().getEntitiesOfClass(ItemEntity.class, itemBox, ItemEntity::isAlive);
                    for (ItemEntity item : items) {
                        item.teleportTo(cat.getX(), cat.getY(), cat.getZ());
                    }
                    if (!items.isEmpty()) {
                        cat.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 1.3F);
                        return true;
                    }
                }
            }
            case 4 -> { // Light / Torch Placement
                if (hasCapability(cat, FelineCapability.CAN_LIGHT)) {
                    return cat.tryPlaceTorchManual();
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // R10: Twine Team Synergy System (Coordinated Companion Feline Attacks)
    // -------------------------------------------------------------------------

    public enum SynergyType {
        INFERNO_HUNT("Inferno Hunt", "Fiery Rush & Blazing Barrage", ChatFormatting.GOLD),
        FROZEN_TIDE("Frozen Tide", "Sub-Zero Tidal Wave & Slowness", ChatFormatting.AQUA),
        TWIN_SOUL_ASSAULT("Twin Soul Assault", "Spectral Clones & Armor-Piercing Void Strike", ChatFormatting.DARK_PURPLE),
        PACK_POUNCE_SURGE("Pack Pounce Surge", "Coordinated Feline Pack Pounce", ChatFormatting.YELLOW);

        private final String title;
        private final String description;
        private final ChatFormatting formatting;

        SynergyType(String title, String description, ChatFormatting formatting) {
            this.title = title;
            this.description = description;
            this.formatting = formatting;
        }

        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public ChatFormatting getFormatting() { return formatting; }
    }

    /**
     * Triggers a coordinated team synergy attack between the twined lead cat and nearby companions.
     */
    public static boolean triggerTeamSynergy(TalentedCat leadCat, ServerPlayer player) {
        if (leadCat == null || !leadCat.isAlive() || leadCat.isIncapacitated()) return false;
        ServerLevel level = (ServerLevel) leadCat.level();

        // 1. Scan for nearby companion cats owned or trusted by player
        AABB scanRadius = leadCat.getBoundingBox().inflate(16.0D);
        List<TalentedCat> companions = level.getEntitiesOfClass(TalentedCat.class, scanRadius,
                c -> c != leadCat && c.isAlive() && !c.isIncapacitated() && (c.isOwnedBy(player) || c.isTrusted(player)));

        if (companions.isEmpty()) {
            player.displayClientMessage(Component.literal("No companion cats nearby for Team Synergy!").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }

        // 2. Cooldown check (5 seconds = 100 ticks)
        long currentTime = level.getGameTime();
        long lastUsed = SYNERGY_COOLDOWNS.getOrDefault(leadCat.getUUID(), 0L);
        if (currentTime < lastUsed + 100L) {
            long remaining = (lastUsed + 100L - currentTime) / 20L + 1;
            player.displayClientMessage(Component.literal("Team Synergy recharging (" + remaining + "s)...").withStyle(ChatFormatting.RED), true);
            return false;
        }

        // 3. Energy cost (25 Energy)
        if (leadCat.getEnergy() < 25) {
            player.displayClientMessage(Component.literal("Not enough Feline Energy for Team Synergy! (Need 25)").withStyle(ChatFormatting.RED), true);
            return false;
        }

        // 4. Select companion and determine synergy matchup
        companions.sort(java.util.Comparator.comparingDouble(leadCat::distanceToSqr));
        TalentedCat partner = companions.get(0);
        SynergyType synergy = determineSynergy(leadCat, partner);

        // Deduct energy & record cooldown
        leadCat.setEnergy(leadCat.getEnergy() - 25);
        SYNERGY_COOLDOWNS.put(leadCat.getUUID(), currentTime);

        // 5. Execute synergistic effects
        switch (synergy) {
            case INFERNO_HUNT -> {
                // Fiery rush: launch both cats forward, ignite area, heavy burst damage
                Vec3 look = leadCat.getLookAngle();
                leadCat.setDeltaMovement(look.x * 1.5D, 0.35D, look.z * 1.5D);
                leadCat.hasImpulse = true;
                partner.setDeltaMovement(look.x * 1.3D, 0.35D, look.z * 1.3D);
                partner.hasImpulse = true;

                level.playSound(null, leadCat.getX(), leadCat.getY(), leadCat.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2F, 0.9F);
                level.playSound(null, leadCat.getX(), leadCat.getY(), leadCat.getZ(), SoundEvents.CAT_HISS, SoundSource.PLAYERS, 1.2F, 0.8F);

                AABB aoe = leadCat.getBoundingBox().inflate(6.0D);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, aoe, e -> e != partner && e != player && com.kittytalents.ai.CatTargeting.canHarm(leadCat, e))) {
                    target.hurt(leadCat.damageSources().mobAttack(leadCat), 14.0F);
                    target.setSecondsOnFire(8);
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
                }
                level.sendParticles(ParticleTypes.FLAME, leadCat.getX(), leadCat.getY() + 0.3D, leadCat.getZ(), 40, 1.5D, 0.5D, 1.5D, 0.1D);
                level.sendParticles(ParticleTypes.LAVA, leadCat.getX(), leadCat.getY() + 0.3D, leadCat.getZ(), 15, 1.0D, 0.3D, 1.0D, 0.05D);
            }
            case FROZEN_TIDE -> {
                // Glacial tidal wave: severe slowness, damage, knockback
                level.playSound(null, leadCat.getX(), leadCat.getY(), leadCat.getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.2F, 1.2F);
                level.playSound(null, leadCat.getX(), leadCat.getY(), leadCat.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.4F);

                AABB wave = leadCat.getBoundingBox().inflate(8.0D);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, wave, e -> e != partner && e != player && com.kittytalents.ai.CatTargeting.canHarm(leadCat, e))) {
                    target.hurt(leadCat.damageSources().mobAttack(leadCat), 10.0F);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 3));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                    Vec3 push = target.position().subtract(leadCat.position()).normalize().scale(1.2D);
                    target.push(push.x, 0.3D, push.z);
                }
                level.sendParticles(ParticleTypes.SPLASH, leadCat.getX(), leadCat.getY() + 0.5D, leadCat.getZ(), 60, 2.0D, 0.8D, 2.0D, 0.2D);
                level.sendParticles(ParticleTypes.SNOWFLAKE, leadCat.getX(), leadCat.getY() + 0.5D, leadCat.getZ(), 40, 2.0D, 0.8D, 2.0D, 0.05D);
            }
            case TWIN_SOUL_ASSAULT -> {
                // Spectral assault: armor-piercing soul strike & wither
                level.playSound(null, leadCat.getX(), leadCat.getY(), leadCat.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0F, 1.3F);
                level.playSound(null, leadCat.getX(), leadCat.getY(), leadCat.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.2F, 1.1F);

                AABB soulAoe = leadCat.getBoundingBox().inflate(7.0D);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, soulAoe, e -> e != partner && e != player && com.kittytalents.ai.CatTargeting.canHarm(leadCat, e))) {
                    target.hurt(com.kittytalents.combat.CatDamage.ability(leadCat, net.minecraft.world.damagesource.DamageTypes.MAGIC), 18.0F);
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1));
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                }
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, leadCat.getX(), leadCat.getY() + 0.4D, leadCat.getZ(), 50, 1.8D, 0.6D, 1.8D, 0.08D);
                level.sendParticles(ParticleTypes.SCULK_SOUL, partner.getX(), partner.getY() + 0.4D, partner.getZ(), 25, 1.0D, 0.5D, 1.0D, 0.05D);
            }
            case PACK_POUNCE_SURGE -> {
                // Pack surge: coordinated leap of all nearby cats, buffing companion speed and attack
                leadCat.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                leadCat.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 1));
                partner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                partner.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 1));

                level.playSound(null, leadCat.getX(), leadCat.getY(), leadCat.getZ(), SoundEvents.FOX_BITE, SoundSource.PLAYERS, 1.2F, 1.1F);
                level.playSound(null, leadCat.getX(), leadCat.getY(), leadCat.getZ(), SoundEvents.CAT_PURREOW, SoundSource.PLAYERS, 1.0F, 1.2F);

                AABB packAoe = leadCat.getBoundingBox().inflate(5.5D);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, packAoe, e -> e != partner && e != player && com.kittytalents.ai.CatTargeting.canHarm(leadCat, e))) {
                    target.hurt(leadCat.damageSources().mobAttack(leadCat), 12.0F);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                }
                level.sendParticles(ParticleTypes.CRIT, leadCat.getX(), leadCat.getY() + 0.3D, leadCat.getZ(), 35, 1.2D, 0.4D, 1.2D, 0.15D);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, leadCat.getX(), leadCat.getY() + 0.3D, leadCat.getZ(), 8, 0.8D, 0.2D, 0.8D, 0.0D);
            }
        }

        player.displayClientMessage(Component.literal("✦ Team Synergy: " + synergy.getTitle() + " with " + partner.getName().getString() + "! ✦").withStyle(synergy.getFormatting()), true);
        return true;
    }

    private static SynergyType determineSynergy(TalentedCat leadCat, TalentedCat partner) {
        boolean leadHasFire = leadCat.getTalentLevel(Talents.INFERNO_CAT) > 0 || leadCat.getCatVariant() == CatVariant.PANTHER;
        boolean partnerHasFire = partner.getTalentLevel(Talents.INFERNO_CAT) > 0 || partner.getCatVariant() == CatVariant.PANTHER;
        if (leadHasFire || partnerHasFire) {
            return SynergyType.INFERNO_HUNT;
        }

        boolean leadHasAqua = leadCat.getTalentLevel(Talents.AQUA_KITTY) > 0 || leadCat.getTalentLevel(Talents.WATER_PELT) > 0;
        boolean partnerHasAqua = partner.getTalentLevel(Talents.AQUA_KITTY) > 0 || partner.getTalentLevel(Talents.WATER_PELT) > 0;
        boolean hasSnow = leadCat.getCatVariant() == CatVariant.SNOW_LEOPARD || partner.getCatVariant() == CatVariant.SNOW_LEOPARD;
        if ((leadHasAqua && partnerHasAqua) || (leadHasAqua && hasSnow) || (partnerHasAqua && hasSnow)) {
            return SynergyType.FROZEN_TIDE;
        }

        boolean leadHasShadow = leadCat.getTalentLevel(Talents.SHADOW_PELT) > 0 || leadCat.getMutation() == CatBreedingManager.MutationType.NEKOMATA;
        boolean partnerHasShadow = partner.getTalentLevel(Talents.SHADOW_PELT) > 0 || partner.getMutation() == CatBreedingManager.MutationType.NEKOMATA;
        if (leadHasShadow || partnerHasShadow) {
            return SynergyType.TWIN_SOUL_ASSAULT;
        }

        return SynergyType.PACK_POUNCE_SURGE;
    }

    // -------------------------------------------------------------------------
    // R1: 4-Way Dodge & Perfect Dodge Mechanics
    // -------------------------------------------------------------------------

    /**
     * Executes a 4-way feline dodge on the server.
     * Direction: 0 = Forward, 1 = Backward, 2 = Left, 3 = Right.
     */
    public static boolean executeDodge(TalentedCat cat, ServerPlayer player, int direction) {
        if (cat == null || !cat.isAlive() || cat.isIncapacitated()) return false;
        ServerLevel level = (ServerLevel) cat.level();

        // 1. Cooldown (15 ticks)
        long currentTime = level.getGameTime();
        long lastDodge = DODGE_COOLDOWNS.getOrDefault(cat.getUUID(), 0L);
        if (currentTime < lastDodge + 15L) return false;

        // 2. Energy check (15 Energy)
        if (cat.getEnergy() < 15) {
            player.displayClientMessage(Component.literal("Not enough Energy to dodge! (Need 15)").withStyle(ChatFormatting.RED), true);
            return false;
        }

        // Deduct Energy & set cooldown
        cat.setEnergy(cat.getEnergy() - 15);
        DODGE_COOLDOWNS.put(cat.getUUID(), currentTime);

        // 3. Compute directional dodge vector from yaw
        float yaw = cat.getYRot();
        float rad = (float) Math.toRadians(yaw);
        Vec3 forwardVec = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
        Vec3 strafeVec = new Vec3(Math.cos(rad), 0, Math.sin(rad));

        Vec3 dodgeDir = switch (direction) {
            case 0 -> forwardVec;
            case 1 -> forwardVec.scale(-1.0D);
            case 2 -> strafeVec;
            case 3 -> strafeVec.scale(-1.0D);
            default -> forwardVec.scale(-1.0D);
        };

        double dodgeSpeed = 1.15D + (cat.getTalentLevel(Talents.FELINE_AGILITY) * 0.05D);
        cat.setDeltaMovement(dodgeDir.x * dodgeSpeed, 0.22D, dodgeDir.z * dodgeSpeed);
        cat.hasImpulse = true;
        cat.invulnerableTime = 15; // Dodge i-frames

        level.playSound(null, cat.getX(), cat.getY(), cat.getZ(), ModSounds.CAT_DODGE.get(), SoundSource.PLAYERS, 0.9F, 1.2F);

        // 4. Perfect Dodge Check: Hostile entity within 3.5 blocks targeting cat or player
        AABB scan = cat.getBoundingBox().inflate(3.5D);
        List<LivingEntity> nearbyHostiles = level.getEntitiesOfClass(LivingEntity.class, scan,
                e -> e != player && com.kittytalents.ai.CatTargeting.canHarm(cat, e));

        boolean perfectDodge = false;
        for (LivingEntity hostile : nearbyHostiles) {
            // Apply counter-slowdown and weakness
            hostile.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
            hostile.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 1));
            perfectDodge = true;
        }

        if (perfectDodge) {
            // Refund energy on perfect timing!
            com.kittytalents.combat.FelineEnergyEngine.restoreEnergy(cat, 15);
            level.playSound(null, cat.getX(), cat.getY(), cat.getZ(), ModSounds.CAT_PERFECT_DODGE.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, cat.getX(), cat.getY() + 0.3D, cat.getZ(), 20, 0.6D, 0.3D, 0.6D, 0.1D);
            player.displayClientMessage(Component.literal("★ Perfect Dodge! Energy refunded! ★").withStyle(ChatFormatting.GOLD), true);
        } else {
            level.sendParticles(ParticleTypes.CLOUD, cat.getX(), cat.getY() + 0.1D, cat.getZ(), 8, 0.3D, 0.1D, 0.3D, 0.05D);
        }

        return true;
    }
}
