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

import java.util.List;

/**
 * Apex Predator Ultimate Ability (R4 Assassin).
 * Becomes an untargetable phantom, sequentially striking up to 4 nearby enemies
 * with lethal critical damage, inflicting Bleed and Weakness.
 */
public class ApexPredatorUltimate extends CatAbility {
    private static final com.kittytalents.core.tuning.Tuning.Key DAMAGE =
            com.kittytalents.core.tuning.Tuning.ability("apex_predator", "damage_mult", 2.5, "Damage x attack");
    private static final com.kittytalents.core.tuning.Tuning.Key RADIUS =
            com.kittytalents.core.tuning.Tuning.ability("apex_predator", "radius", 10.0, "Radius");

    public ApexPredatorUltimate() {
        super("apex_predator", "Apex Predator", "Teleports in rapid sequence across up to 4 nearby foes, dealing lethal critical strikes.",
                85, 450, FelineElement.SHADOW, Category.ULTIMATE);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        AABB aoe = cat.getBoundingBox().inflate(8.0D);
        int enemies = cat.level().getEntitiesOfClass(LivingEntity.class, aoe,
                e -> com.kittytalents.ai.CatTargeting.canHarm(cat, e)).size();
        return enemies >= 2 ? 3.2f : 0.5f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        AABB aoe = cat.getBoundingBox().inflate(RADIUS.get());
        List<LivingEntity> enemies = cat.level().getEntitiesOfClass(LivingEntity.class, aoe,
                e -> com.kittytalents.ai.CatTargeting.canHarm(cat, e));

        if (enemies.isEmpty()) return false;

        cat.playSound(SoundEvents.PHANTOM_BITE, 1.2F, 1.6F);
        cat.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 1.2F);

        float dmg = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE.f();
        enemies.sort(java.util.Comparator.comparingDouble(cat::distanceToSqr)); // the four nearest, not any four
        int count = 0;

        for (LivingEntity target : enemies) {
            if (count >= 4) break;
            target.hurt(cat.damageSources().mobAttack(cat), dmg);
            FelineStatusEffects.applyStatus(target, FelineStatusType.BLEED, 2, 100);
            FelineStatusEffects.applyStatus(target, FelineStatusType.WEAKNESS, 2, 100);

            if (cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 0.5, target.getZ(), 2, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.5, target.getZ(), 12, 0.3, 0.3, 0.3, 0.1);
            }
            count++;
        }

        return true;
    }
}
