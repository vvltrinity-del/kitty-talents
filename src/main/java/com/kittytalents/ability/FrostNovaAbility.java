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
 * Frost Nova active skill (R4).
 * Unleashes a chilling shockwave that freezes and slows all nearby foes.
 */
public class FrostNovaAbility extends CatAbility {
    private static final com.kittytalents.core.tuning.Tuning.Key DAMAGE =
            com.kittytalents.core.tuning.Tuning.ability("frost_nova", "damage_mult", 1.2, "Damage x attack");
    private static final com.kittytalents.core.tuning.Tuning.Key RADIUS =
            com.kittytalents.core.tuning.Tuning.ability("frost_nova", "radius", 5.0, "Radius");
    private static final com.kittytalents.core.tuning.Tuning.Key FREEZE =
            com.kittytalents.core.tuning.Tuning.ability("frost_nova", "freeze_ticks", 100, "Freeze ticks");

    public FrostNovaAbility() {
        super("frost_nova", "Frost Nova", "Unleashes an icy blast freezing surrounding enemies and drastically reducing their speed.",
                35, 90, FelineElement.FROST, Category.SKILL);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        AABB aoe = cat.getBoundingBox().inflate(RADIUS.get());
        int enemies = cat.level().getEntitiesOfClass(LivingEntity.class, aoe,
                e -> com.kittytalents.ai.CatTargeting.canHarm(cat, e)).size();
        return enemies >= 2 ? 2.0f : 0.7f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        cat.playSound(SoundEvents.PLAYER_HURT_FREEZE, 1.2F, 1.4F);
        cat.playSound(SoundEvents.GLASS_BREAK, 0.8F, 1.8F);

        AABB aoe = cat.getBoundingBox().inflate(RADIUS.get());
        float dmg = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE.f();

        for (LivingEntity target : cat.level().getEntitiesOfClass(LivingEntity.class, aoe,
                e -> com.kittytalents.ai.CatTargeting.canHarm(cat, e))) {
            target.hurt(cat.damageSources().freeze(), dmg);
            FelineStatusEffects.applyStatus(target, FelineStatusType.FREEZE, 2, FREEZE.i());
            if (target.canFreeze()) {
                target.setTicksFrozen(target.getTicksRequiredToFreeze() + 60);
            }
        }

        if (cat.level() instanceof ServerLevel sl) {
            for (int i = 0; i < 24; i++) {
                double angle = (2 * Math.PI * i) / 24;
                double px = cat.getX() + Math.cos(angle) * 3.0;
                double pz = cat.getZ() + Math.sin(angle) * 3.0;
                sl.sendParticles(ParticleTypes.SNOWFLAKE, px, cat.getY() + 0.3D, pz, 2, 0.1, 0.05, 0.1, 0.02);
            }
        }

        return true;
    }
}
