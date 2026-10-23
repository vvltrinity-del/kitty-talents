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
 * Inferno Spin active skill (R4).
 * Rotates rapidly with fiery claws, igniting all surrounding enemies and inflicting Burn.
 */
public class InfernoSpinAbility extends CatAbility {

    public InfernoSpinAbility() {
        super("inferno_spin", "Inferno Spin", "Whirls with blazing claws, damaging all surrounding enemies and setting them on fire.",
                35, 90, FelineElement.FIRE, Category.SKILL);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        AABB aoe = cat.getBoundingBox().inflate(4.0D);
        int enemies = cat.level().getEntitiesOfClass(LivingEntity.class, aoe,
                e -> e != cat && e != cat.getOwner() && !cat.isAlliedTo(e)).size();
        return enemies >= 2 ? 2.2f : 0.6f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        cat.playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 1.2F);
        cat.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.5F);

        AABB aoe = cat.getBoundingBox().inflate(4.5D);
        float dmg = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.4F;

        for (LivingEntity target : cat.level().getEntitiesOfClass(LivingEntity.class, aoe,
                e -> e != cat && e != player && e != cat.getOwner() && !cat.isAlliedTo(e))) {
            target.hurt(cat.damageSources().inFire(), dmg);
            target.setSecondsOnFire(5);
            FelineStatusEffects.applyStatus(target, FelineStatusType.BURN, 2, 100);
        }

        if (cat.level() instanceof ServerLevel sl) {
            for (int i = 0; i < 20; i++) {
                double angle = (2 * Math.PI * i) / 20;
                double px = cat.getX() + Math.cos(angle) * 2.2;
                double pz = cat.getZ() + Math.sin(angle) * 2.2;
                sl.sendParticles(ParticleTypes.FLAME, px, cat.getY() + 0.3D, pz, 1, 0, 0.05, 0, 0.02);
            }
        }

        return true;
    }
}
