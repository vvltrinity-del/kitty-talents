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
 * Soul Flame Barrage Ultimate Ability (R4, Nekomata / Arcane).
 * Unleashes swirling twin-soul spirit flames targeting all enemies within 12 blocks,
 * dealing immense Arcane damage and inflicting Marked and Vulnerable.
 */
public class SoulFlameBarrageUltimate extends CatAbility {
    private static final com.kittytalents.core.tuning.Tuning.Key DAMAGE =
            com.kittytalents.core.tuning.Tuning.ability("soul_flame_barrage", "damage_mult", 2.8, "Damage x attack");
    private static final com.kittytalents.core.tuning.Tuning.Key RADIUS =
            com.kittytalents.core.tuning.Tuning.ability("soul_flame_barrage", "radius", 12.0, "Radius");

    public SoulFlameBarrageUltimate() {
        super("soul_flame_barrage", "Soul Flame Barrage", "Unleashes swirling twin soul-flames at all surrounding enemies, inflicting Marked and Vulnerable.",
                90, 500, FelineElement.ARCANE, Category.ULTIMATE);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        AABB aoe = cat.getBoundingBox().inflate(10.0D);
        int enemies = cat.level().getEntitiesOfClass(LivingEntity.class, aoe,
                e -> com.kittytalents.ai.CatTargeting.canHarm(cat, e)).size();
        return enemies >= 2 ? 3.5f : 0.4f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        cat.playSound(SoundEvents.SOUL_ESCAPE, 1.4F, 0.8F);
        cat.playSound(SoundEvents.WITHER_SHOOT, 1.0F, 1.5F);

        AABB aoe = cat.getBoundingBox().inflate(RADIUS.get());
        List<LivingEntity> enemies = cat.level().getEntitiesOfClass(LivingEntity.class, aoe,
                e -> com.kittytalents.ai.CatTargeting.canHarm(cat, e));

        float dmg = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE.f();

        for (LivingEntity target : enemies) {
            target.hurt(com.kittytalents.combat.CatDamage.ability(cat, net.minecraft.world.damagesource.DamageTypes.MAGIC), dmg);
            FelineStatusEffects.applyStatus(target, FelineStatusType.MARKED, 1, 140);
            FelineStatusEffects.applyStatus(target, FelineStatusType.VULNERABLE, 2, 120);

            if (cat.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 0.5D, target.getZ(), 16, 0.3D, 0.3D, 0.3D, 0.05D);
            }
        }

        if (cat.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL, cat.getX(), cat.getY() + 0.8D, cat.getZ(), 25, 1.2D, 0.5D, 1.2D, 0.08D);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cat.getX(), cat.getY() + 0.5D, cat.getZ(), 40, 1.5D, 0.5D, 1.5D, 0.1D);
        }

        return true;
    }
}
