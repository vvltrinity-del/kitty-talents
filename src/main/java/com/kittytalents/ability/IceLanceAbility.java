package com.kittytalents.ability;

import com.kittytalents.combat.FelineElement;
import com.kittytalents.combat.FelineStatusEffects;
import com.kittytalents.combat.FelineStatusType;
import com.kittytalents.core.tuning.Tuning;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Ice Lance (4.0, Frost skill): a shard of ice thrown at a foe up to 10 blocks away; damages, Freezes and slows. */
public class IceLanceAbility extends CatAbility {
    static final Tuning.Key DAMAGE = Tuning.ability("ice_lance", "damage_mult", 1.6, "Damage x attack");
    static final Tuning.Key RANGE = Tuning.ability("ice_lance", "range", 10.0, "Range");
    static final Tuning.Key FREEZE = Tuning.ability("ice_lance", "freeze_ticks", 80, "Freeze ticks");

    public IceLanceAbility() {
        super("ice_lance", "Ice Lance", "Hurls a lance of ice at a distant foe, freezing it in place.",
                30, 70, FelineElement.FROST, Category.SKILL);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        LivingEntity t = cat.getTarget();
        if (t == null || !t.isAlive() || !cat.hasLineOfSight(t)) return 0.3f;
        double d = cat.distanceTo(t);
        if (d > RANGE.get()) return 0.3f;
        return d >= 3.0D ? 1.6f : 1.05f; // best at range: freezes a foe before it closes in
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        LivingEntity t = AbilityTargets.target(cat, RANGE.get());
        if (t == null || !cat.hasLineOfSight(t)) return false;
        cat.playSound(SoundEvents.GLASS_BREAK, 0.9F, 1.9F);
        cat.playSound(SoundEvents.PLAYER_HURT_FREEZE, 1.0F, 1.2F);
        t.hurt(cat.damageSources().freeze(), (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE.f());
        FelineStatusEffects.applyStatus(t, FelineStatusType.FREEZE, 1, FREEZE.i());
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE.i(), 2));
        if (t.canFreeze()) t.setTicksFrozen(t.getTicksRequiredToFreeze() + 40);
        AbilityTargets.trail(cat, t, ParticleTypes.SNOWFLAKE);
        return true;
    }
}
