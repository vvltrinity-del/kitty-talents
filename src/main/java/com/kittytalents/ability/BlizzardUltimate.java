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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Blizzard (4.0, Frost ultimate): every foe in range is hurt and deeply Frozen; the cat and its owner are Fortified. */
public class BlizzardUltimate extends CatAbility {
    static final Tuning.Key DAMAGE = Tuning.ability("blizzard", "damage_mult", 2.0, "Damage x attack");
    static final Tuning.Key RADIUS = Tuning.ability("blizzard", "radius", 9.0, "Radius");
    static final Tuning.Key FREEZE = Tuning.ability("blizzard", "freeze_ticks", 160, "Freeze ticks");

    public BlizzardUltimate() {
        super("blizzard", "Blizzard", "Summons a howling blizzard that freezes every nearby foe and shields the cat and its owner.",
                85, 500, FelineElement.FROST, Category.ULTIMATE);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        return AbilityTargets.ultimatePriority(cat, AbilityTargets.foes(cat, RADIUS.get()).size());
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        List<LivingEntity> foes = AbilityTargets.foes(cat, RADIUS.get());
        if (foes.isEmpty()) return false;
        cat.playSound(SoundEvents.POWDER_SNOW_STEP, 1.5F, 0.6F);
        cat.playSound(SoundEvents.PLAYER_HURT_FREEZE, 1.2F, 0.8F);
        float dmg = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE.f();
        for (LivingEntity t : foes) {
            t.hurt(cat.damageSources().freeze(), dmg);
            FelineStatusEffects.applyStatus(t, FelineStatusType.FREEZE, 3, FREEZE.i());
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE.i(), 3));
            if (t.canFreeze()) t.setTicksFrozen(t.getTicksRequiredToFreeze() + 100);
        }
        FelineStatusEffects.applyStatus(cat, FelineStatusType.FORTIFIED, 1, 100);
        if (cat.getOwner() instanceof Player o && o.distanceToSqr(cat) < RADIUS.get() * RADIUS.get()) {
            FelineStatusEffects.applyStatus(o, FelineStatusType.FORTIFIED, 1, 100);
        }
        if (cat.level() instanceof ServerLevel sl) {
            double r = RADIUS.get();
            sl.sendParticles(ParticleTypes.SNOWFLAKE, cat.getX(), cat.getY() + 2.0, cat.getZ(), 160, r * 0.5, 1.5, r * 0.5, 0.05);
            sl.sendParticles(ParticleTypes.CLOUD, cat.getX(), cat.getY() + 3.0, cat.getZ(), 40, r * 0.4, 0.4, r * 0.4, 0.01);
        }
        return true;
    }
}
