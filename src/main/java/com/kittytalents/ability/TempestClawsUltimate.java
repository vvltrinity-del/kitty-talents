package com.kittytalents.ability;

import com.kittytalents.combat.FelineElement;
import com.kittytalents.combat.FelineStatusEffects;
import com.kittytalents.combat.FelineStatusType;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Tempest Claws Ultimate Ability (R4 Electric).
 * Summons chain lightning strikes across all enemies within 10 blocks,
 * dealing thunder damage and applying Shock.
 */
public class TempestClawsUltimate extends CatAbility {

    public TempestClawsUltimate() {
        super("tempest_claws", "Tempest Claws", "Summons a storm of lightning upon all nearby enemies, electrocuting and shocking them.",
                90, 500, FelineElement.ELECTRIC, Category.ULTIMATE);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        AABB aoe = cat.getBoundingBox().inflate(8.0D);
        int enemies = cat.level().getEntitiesOfClass(LivingEntity.class, aoe,
                e -> e != cat && e != cat.getOwner() && !cat.isAlliedTo(e)).size();
        return enemies >= 2 ? 3.3f : 0.4f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        cat.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0F, 1.2F);

        AABB aoe = cat.getBoundingBox().inflate(10.0D);
        List<LivingEntity> enemies = cat.level().getEntitiesOfClass(LivingEntity.class, aoe,
                e -> e != cat && e != player && e != cat.getOwner() && !cat.isAlliedTo(e));

        if (enemies.isEmpty()) return false;

        float dmg = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.2F;

        for (LivingEntity target : enemies) {
            target.hurt(cat.damageSources().lightningBolt(), dmg);
            FelineStatusEffects.applyStatus(target, FelineStatusType.SHOCK, 2, 100);

            if (cat.level() instanceof ServerLevel sl) {
                LightningBolt visualBolt = EntityType.LIGHTNING_BOLT.create(sl);
                if (visualBolt != null) {
                    visualBolt.moveTo(target.position());
                    visualBolt.setVisualOnly(true);
                    sl.addFreshEntity(visualBolt);
                }
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.5D, target.getZ(), 20, 0.4D, 0.4D, 0.4D, 0.15D);
            }
        }

        return true;
    }
}
