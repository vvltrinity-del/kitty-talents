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

import java.util.List;

/** Plague Bloom (4.0, Poison ultimate): foes around the cat are badly Poisoned, Vulnerable and Weakened; the cat heals per foe. */
public class PlagueBloomUltimate extends CatAbility {
    static final Tuning.Key DAMAGE = Tuning.ability("plague_bloom", "damage_mult", 1.5, "Damage x attack");
    static final Tuning.Key RADIUS = Tuning.ability("plague_bloom", "radius", 8.0, "Radius");
    static final Tuning.Key POISON = Tuning.ability("plague_bloom", "poison_ticks", 200, "Poison ticks");
    static final Tuning.Key LEECH = Tuning.ability("plague_bloom", "heal_per_foe", 0.05, "Heal per foe hit");

    public PlagueBloomUltimate() {
        super("plague_bloom", "Plague Bloom", "A toxic bloom bursts around the cat, poisoning and weakening every foe; the cat heals for each one.",
                85, 500, FelineElement.POISON, Category.ULTIMATE);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        int living = (int) AbilityTargets.foes(cat, RADIUS.get()).stream().filter(e -> e.getMobType() != MobType.UNDEAD).count();
        return AbilityTargets.ultimatePriority(cat, living);
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        List<LivingEntity> foes = AbilityTargets.foes(cat, RADIUS.get());
        if (foes.isEmpty()) return false;
        cat.playSound(SoundEvents.SPORE_BLOSSOM_PLACE, 1.4F, 0.6F);
        cat.playSound(SoundEvents.PUFFER_FISH_BLOW_OUT, 1.2F, 0.6F);
        float dmg = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE.f();
        for (LivingEntity t : foes) {
            t.hurt(cat.damageSources().magic(), dmg);
            FelineStatusEffects.applyStatus(t, FelineStatusType.POISON, 3, POISON.i());
            FelineStatusEffects.applyStatus(t, FelineStatusType.VULNERABLE, 2, 160);
            FelineStatusEffects.applyStatus(t, FelineStatusType.WEAKNESS, 2, 160);
            t.addEffect(new MobEffectInstance(MobEffects.POISON, POISON.i(), 1));
        }
        cat.heal(cat.getMaxHealth() * LEECH.f() * foes.size());
        if (cat.level() instanceof ServerLevel sl) {
            double r = RADIUS.get();
            for (int i = 0; i < 32; i++) {
                double a = Math.PI * 2 * i / 32;
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, cat.getX() + Math.cos(a) * r * 0.7, cat.getY() + 0.4,
                        cat.getZ() + Math.sin(a) * r * 0.7, 3, 0.2, 0.3, 0.2, 0.0);
            }
            sl.sendParticles(ParticleTypes.ITEM_SLIME, cat.getX(), cat.getY() + 0.5, cat.getZ(), 60, r * 0.4, 0.5, r * 0.4, 0.08);
        }
        return true;
    }
}
