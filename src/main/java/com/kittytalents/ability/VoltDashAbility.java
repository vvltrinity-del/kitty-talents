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
import net.minecraft.world.phys.Vec3;

/**
 * Volt Dash active skill (R4).
 * Electrified forward rush piercing through foes and inflicting Shock.
 */
public class VoltDashAbility extends CatAbility {
    private static final com.kittytalents.core.tuning.Tuning.Key DAMAGE =
            com.kittytalents.core.tuning.Tuning.ability("volt_dash", "damage_mult", 1.35, "Damage x attack");

    public VoltDashAbility() {
        super("volt_dash", "Volt Dash", "Electrifies the cat and surges forward, striking and shocking all enemies in the path.",
                30, 80, FelineElement.ELECTRIC, Category.SKILL);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        if (cat.getTarget() != null && cat.distanceToSqr(cat.getTarget()) > 16.0D && cat.distanceToSqr(cat.getTarget()) < 100.0D) {
            return 1.6f;
        }
        return 0.2f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        Vec3 look = cat.getTarget() != null ? cat.getTarget().position().subtract(cat.position()).normalize() : cat.getLookAngle();
        cat.setDeltaMovement(look.x * 2.2D, 0.25D, look.z * 2.2D);
        cat.hasImpulse = true;

        cat.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6F, 2.0F);

        AABB reach = cat.getBoundingBox().inflate(2.5D);
        for (LivingEntity target : cat.level().getEntitiesOfClass(LivingEntity.class, reach,
                e -> com.kittytalents.ai.CatTargeting.canHarm(cat, e))) {
            float dmg = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE.f();
            target.hurt(cat.damageSources().lightningBolt(), dmg);
            FelineStatusEffects.applyStatus(target, FelineStatusType.SHOCK, 1, 60);
        }

        if (cat.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, cat.getX(), cat.getY() + 0.4D, cat.getZ(), 16, 0.4D, 0.4D, 0.4D, 0.1D);
        }

        return true;
    }
}
