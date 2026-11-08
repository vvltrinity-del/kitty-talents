package com.kittytalents.ability;

import com.kittytalents.combat.FelineElement;
import com.kittytalents.combat.FelineStatusEffects;
import com.kittytalents.combat.FelineStatusType;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Celestial Purr Ultimate Ability (R4 Healer / Support).
 * Emits a divine resonant hymn that fully cures, cleanses, and super-regenerates
 * all nearby companions and the owner.
 */
public class CelestialPurrUltimate extends CatAbility {

    public CelestialPurrUltimate() {
        super("celestial_purr", "Celestial Purr", "Resonates with celestial warmth, cleansing all debuffs and restoring massive health and absorption to allies.",
                80, 550, FelineElement.NATURE, Category.ULTIMATE);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        if (cat.getHealth() < cat.getMaxHealth() * 0.4f) return 3.5f;
        if (cat.getOwner() instanceof Player p && p.getHealth() < p.getMaxHealth() * 0.4f && cat.distanceToSqr(p) < 144.0) return 3.5f;
        return 0.2f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        cat.playSound(SoundEvents.BEACON_ACTIVATE, 1.2F, 1.4F);
        cat.playSound(SoundEvents.TOTEM_USE, 0.8F, 1.5F);

        AABB aoe = cat.getBoundingBox().inflate(12.0D);
        List<LivingEntity> allies = cat.level().getEntitiesOfClass(LivingEntity.class, aoe,
                e -> com.kittytalents.ai.CatTargeting.isFriendly(cat, e));

        for (LivingEntity ally : allies) {
            ally.heal(ally.getMaxHealth() * 0.60F);
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 2));

            // Cleanse all harmful status effects
            FelineStatusEffects.removeStatus(ally, FelineStatusType.BURN);
            FelineStatusEffects.removeStatus(ally, FelineStatusType.POISON);
            FelineStatusEffects.removeStatus(ally, FelineStatusType.BLEED);
            FelineStatusEffects.removeStatus(ally, FelineStatusType.FREEZE);
            FelineStatusEffects.removeStatus(ally, FelineStatusType.SHOCK);
            FelineStatusEffects.removeStatus(ally, FelineStatusType.WEAKNESS);
            FelineStatusEffects.removeStatus(ally, FelineStatusType.VULNERABLE);
            FelineStatusEffects.removeStatus(ally, FelineStatusType.MARKED);

            // Grant beneficial status effects
            FelineStatusEffects.applyStatus(ally, FelineStatusType.REGENERATION, 3, 200);
            FelineStatusEffects.applyStatus(ally, FelineStatusType.FORTIFIED, 2, 200);

            if (cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + 0.8, ally.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
            }
        }

        if (cat.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, cat.getX(), cat.getY() + 0.5, cat.getZ(), 30, 0.8, 0.5, 0.8, 0.15);
        }

        return true;
    }
}
