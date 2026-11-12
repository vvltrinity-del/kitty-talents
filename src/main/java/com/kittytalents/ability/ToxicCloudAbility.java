package com.kittytalents.ability;

import com.kittytalents.combat.FelineElement;
import com.kittytalents.combat.FelineStatusEffects;
import com.kittytalents.combat.FelineStatusType;
import com.kittytalents.core.tuning.Tuning;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Toxic Cloud (4.0, Poison skill): a burst of toxic spores around the target; Poisons and Weakens every foe in it. */
public class ToxicCloudAbility extends CatAbility {
    static final Tuning.Key DAMAGE = Tuning.ability("toxic_cloud", "damage_mult", 0.6, "Damage x attack");
    static final Tuning.Key RADIUS = Tuning.ability("toxic_cloud", "radius", 4.5, "Radius");
    static final Tuning.Key POISON = Tuning.ability("toxic_cloud", "poison_ticks", 140, "Poison ticks");
    private static final double CAST_RANGE = 8.0D;

    public ToxicCloudAbility() {
        super("toxic_cloud", "Toxic Cloud", "Bursts a cloud of toxic spores on a foe, poisoning and weakening everything around it.",
                30, 100, FelineElement.POISON, Category.SKILL);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        LivingEntity t = cat.getTarget();
        if (t == null || !t.isAlive() || cat.distanceTo(t) > CAST_RANGE) return 0.3f;
        long living = AbilityTargets.foesAround(cat, t, RADIUS.get()).stream().filter(e -> e.getMobType() != MobType.UNDEAD).count();
        if (living == 0) return 0.2f; // undead shrug off poison
        return living >= 2 ? 1.9f : 1.1f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        LivingEntity centre = AbilityTargets.target(cat, CAST_RANGE);
        if (centre == null) return false;
        cat.playSound(SoundEvents.PUFFER_FISH_BLOW_UP, 1.0F, 0.8F);
        float dmg = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE.f();
        for (LivingEntity t : AbilityTargets.foesAround(cat, centre, RADIUS.get())) {
            t.hurt(com.kittytalents.combat.CatDamage.ability(cat, net.minecraft.world.damagesource.DamageTypes.MAGIC), dmg);
            FelineStatusEffects.applyStatus(t, FelineStatusType.POISON, 2, POISON.i());
            FelineStatusEffects.applyStatus(t, FelineStatusType.WEAKNESS, 1, 100);
            t.addEffect(new MobEffectInstance(MobEffects.POISON, POISON.i(), 0));
        }
        if (cat.level() instanceof ServerLevel sl) {
            double r = RADIUS.get();
            sl.sendParticles(ParticleTypes.ITEM_SLIME, centre.getX(), centre.getY() + 0.6, centre.getZ(), 30, r * 0.4, 0.4, r * 0.4, 0.05);
            sl.sendParticles(ParticleTypes.SNEEZE, centre.getX(), centre.getY() + 0.8, centre.getZ(), 40, r * 0.4, 0.4, r * 0.4, 0.02);
        }
        return true;
    }
}
