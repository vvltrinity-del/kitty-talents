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

/**
 * Healing Purr active skill (R4).
 * Emits soothing vibrational purrs that heal and apply Regeneration to self and nearby allies.
 */
public class HealingPurrAbility extends CatAbility {
    private static final com.kittytalents.core.tuning.Tuning.Key SELF =
            com.kittytalents.core.tuning.Tuning.ability("healing_purr", "self_heal", 0.3, "Self heal fraction");
    private static final com.kittytalents.core.tuning.Tuning.Key ALLY =
            com.kittytalents.core.tuning.Tuning.ability("healing_purr", "ally_heal", 0.2, "Ally heal fraction");
    private static final com.kittytalents.core.tuning.Tuning.Key RADIUS =
            com.kittytalents.core.tuning.Tuning.ability("healing_purr", "radius", 8.0, "Radius");

    public HealingPurrAbility() {
        super("healing_purr", "Healing Purr", "Soothes the cat and nearby allies, restoring health and granting Regeneration.",
                40, 200, FelineElement.NATURE, Category.SKILL);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        if (cat.getHealth() < cat.getMaxHealth() * 0.5f) return 2.5f;
        if (cat.getOwner() instanceof Player p && p.getHealth() < p.getMaxHealth() * 0.5f && cat.distanceToSqr(p) < 64.0) return 2.2f;
        return 0.1f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        cat.playSound(SoundEvents.CAT_PURREOW, 1.0F, 1.0F);
        cat.heal(cat.getMaxHealth() * SELF.f());
        cat.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
        FelineStatusEffects.applyStatus(cat, FelineStatusType.REGENERATION, 2, 120);

        AABB reach = cat.getBoundingBox().inflate(RADIUS.get());
        for (LivingEntity ally : cat.level().getEntitiesOfClass(LivingEntity.class, reach,
                e -> com.kittytalents.ai.CatTargeting.isFriendly(cat, e))) {
            ally.heal(ally.getMaxHealth() * ALLY.f());
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0));
            FelineStatusEffects.applyStatus(ally, FelineStatusType.REGENERATION, 1, 120);
        }

        if (cat.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, cat.getX(), cat.getY() + 0.8D, cat.getZ(), 10, 0.5D, 0.3D, 0.5D, 0.05D);
        }

        return true;
    }
}
