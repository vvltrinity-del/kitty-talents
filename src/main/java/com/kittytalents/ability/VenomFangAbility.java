package com.kittytalents.ability;

import com.kittytalents.combat.FelineElement;
import com.kittytalents.combat.FelineStatusEffects;
import com.kittytalents.combat.FelineStatusType;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Venom Fang active skill (R4).
 * Vicious bite infusing toxic venom that causes severe Poison and Bleed.
 */
public class VenomFangAbility extends CatAbility {

    public VenomFangAbility() {
        super("venom_fang", "Venom Fang", "Bites deeply into a foe, applying lethal Poison and Bleed.",
                25, 60, FelineElement.POISON, Category.SKILL);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        // Must match execute()'s bite range (< 4 blocks), otherwise the AI keeps picking a cast that always fails.
        // Undead are immune to Poison and Bleed: don't waste Energy on them.
        if (cat.getTarget() != null && cat.getTarget().getMobType() == net.minecraft.world.entity.MobType.UNDEAD) return 0.2f;
        if (cat.getTarget() != null && cat.distanceToSqr(cat.getTarget()) < 16.0D) {
            return 1.7f;
        }
        return 0.5f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        LivingEntity target = cat.getTarget();
        if (target == null && player != null) {
            AABB reach = cat.getBoundingBox().inflate(3.5D);
            for (LivingEntity e : cat.level().getEntitiesOfClass(LivingEntity.class, reach,
                    e -> com.kittytalents.ai.CatTargeting.canHarm(cat, e))) {
                target = e;
                break;
            }
        }

        if (target != null && target.isAlive() && cat.distanceToSqr(target) < 16.0D) {
            cat.playSound(SoundEvents.FOX_BITE, 1.0F, 1.1F);
            cat.playSound(SoundEvents.SPIDER_HURT, 1.0F, 1.4F);

            float dmg = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5F;
            target.hurt(cat.damageSources().mobAttack(cat), dmg);

            FelineStatusEffects.applyStatus(target, FelineStatusType.POISON, 2, 100);
            FelineStatusEffects.applyStatus(target, FelineStatusType.BLEED, 1, 80);

            if (cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY() + 0.4D, target.getZ(), 8, 0.2D, 0.2D, 0.2D, 0.05D);
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 0.4D, target.getZ(), 4, 0.2D, 0.2D, 0.2D, 0.1D);
            }
            return true;
        }

        return false;
    }
}
