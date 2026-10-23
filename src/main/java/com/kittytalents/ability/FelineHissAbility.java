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
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/**
 * Feline Hiss active ability (R4).
 * Emits a ferocious feline hiss that inflicts Weakness and repels Creepers.
 */
public class FelineHissAbility extends CatAbility {

    public FelineHissAbility() {
        super("feline_hiss", "Feline Hiss", "Terrifies surrounding enemies, inflicting Weakness and repelling Creepers.",
                25, 80, FelineElement.PHYSICAL, Category.SKILL);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        AABB aoe = cat.getBoundingBox().inflate(6.0D);
        List<LivingEntity> enemies = cat.level().getEntitiesOfClass(LivingEntity.class, aoe,
                e -> e != cat && e != cat.getOwner() && !cat.isAlliedTo(e));
        return enemies.size() >= 2 ? 2.5f : 0.8f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        cat.playSound(com.kittytalents.registry.ModSounds.ABILITY_HISS.get(), 1.2F, 1.0F);
        AABB aoe = cat.getBoundingBox().inflate(6.5D);

        for (LivingEntity le : cat.level().getEntitiesOfClass(LivingEntity.class, aoe,
                e -> e != cat && e != player && e != cat.getOwner() && !cat.isAlliedTo(e))) {
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
            FelineStatusEffects.applyStatus(le, FelineStatusType.WEAKNESS, 1, 140);

            if (le instanceof Creeper creeper) {
                Vec3 repel = creeper.position().subtract(cat.position()).normalize();
                creeper.setDeltaMovement(repel.x * 0.95D, 0.3D, repel.z * 0.95D);
            }
        }

        if (cat.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, cat.getX(), cat.getY() + 0.5D, cat.getZ(), 8, 0.5D, 0.3D, 0.5D, 0.05D);
        }

        return true;
    }
}
