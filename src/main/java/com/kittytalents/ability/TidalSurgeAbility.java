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
 * Tidal Surge active skill (R4).
 * Summons a surging water wave that pushes enemies back and cleanses allies.
 */
public class TidalSurgeAbility extends CatAbility {

    public TidalSurgeAbility() {
        super("tidal_surge", "Tidal Surge", "Summons a cascading wave that knocks enemies back, douses fire, and cleanses allies.",
                30, 80, FelineElement.WATER, Category.SKILL);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        if (cat.isOnFire()) return 2.8f;
        if (cat.getOwner() instanceof Player p && p.isOnFire() && cat.distanceToSqr(p) < 64.0) return 2.5f;
        return 0.7f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        cat.playSound(SoundEvents.GENERIC_SPLASH, 1.2F, 1.0F);
        cat.playSound(SoundEvents.WATER_AMBIENT, 1.0F, 1.2F);

        AABB aoe = cat.getBoundingBox().inflate(6.0D);
        float dmg = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.1F;

        for (LivingEntity target : cat.level().getEntitiesOfClass(LivingEntity.class, aoe,
                e -> com.kittytalents.ai.CatTargeting.canHarm(cat, e))) {
            target.hurt(cat.damageSources().drown(), dmg);
            target.clearFire();
            Vec3 push = target.position().subtract(cat.position()).normalize().scale(1.2);
            target.push(push.x, 0.35, push.z);
        }

        // Cleanse allies of Burn and Weakness
        for (LivingEntity ally : cat.level().getEntitiesOfClass(LivingEntity.class, aoe,
                e -> com.kittytalents.ai.CatTargeting.isFriendly(cat, e))) {
            ally.clearFire();
            FelineStatusEffects.removeStatus(ally, FelineStatusType.BURN);
            FelineStatusEffects.removeStatus(ally, FelineStatusType.WEAKNESS);
        }

        if (cat.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPLASH, cat.getX(), cat.getY() + 0.4D, cat.getZ(), 25, 1.5D, 0.4D, 1.5D, 0.1D);
            sl.sendParticles(ParticleTypes.DRIPPING_WATER, cat.getX(), cat.getY() + 0.8D, cat.getZ(), 15, 1.0D, 0.4D, 1.0D, 0.05D);
        }

        return true;
    }
}
