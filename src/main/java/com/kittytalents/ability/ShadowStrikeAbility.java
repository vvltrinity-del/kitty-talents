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
 * Shadow Strike active skill (R4).
 * Ambush teleport behind an enemy dealing burst shadow damage and inflicting Vulnerable.
 */
public class ShadowStrikeAbility extends CatAbility {

    public ShadowStrikeAbility() {
        super("shadow_strike", "Shadow Strike", "Steps through the shadows to strike an enemy from behind, dealing burst damage and inflicting Vulnerable.",
                35, 100, FelineElement.SHADOW, Category.SKILL);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        if (cat.getTarget() != null && cat.distanceToSqr(cat.getTarget()) < 64.0D) return 1.8f;
        return 0.0f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        LivingEntity target = cat.getTarget();
        if (target == null && player != null) {
            AABB reach = cat.getBoundingBox().inflate(6.0D);
            for (LivingEntity e : cat.level().getEntitiesOfClass(LivingEntity.class, reach,
                    e -> com.kittytalents.ai.CatTargeting.canHarm(cat, e))) {
                target = e;
                break;
            }
        }

        if (target != null && target.isAlive()) {
            double behindX = target.getX() - Math.sin(Math.toRadians(target.getYRot())) * 1.2;
            double behindZ = target.getZ() + Math.cos(Math.toRadians(target.getYRot())) * 1.2;
            cat.teleportTo(behindX, target.getY(), behindZ);
            cat.setYRot(target.getYRot());

            cat.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.2F);
            cat.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.4F);

            float dmg = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.2F;
            target.hurt(cat.damageSources().magic(), dmg);

            FelineStatusEffects.applyStatus(target, FelineStatusType.VULNERABLE, 2, 120);
            FelineStatusEffects.applyElementStatus(target, FelineElement.SHADOW, 1, 100);

            if (cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + 0.5D, target.getZ(), 12, 0.3D, 0.3D, 0.3D, 0.05D);
            }
            return true;
        }
        return false;
    }
}
