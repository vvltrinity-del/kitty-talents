package com.kittytalents.ability;

import com.kittytalents.combat.FelineElement;
import com.kittytalents.combat.FelineStatusEffects;
import com.kittytalents.combat.FelineStatusType;
import com.kittytalents.core.tuning.Tuning;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.List;

/** Starfall (4.0, Arcane ultimate): stars crash onto up to six foes, dealing heavy magic damage and leaving them Vulnerable. */
public class StarfallUltimate extends CatAbility {
    static final Tuning.Key DAMAGE = Tuning.ability("starfall", "damage_mult", 2.4, "Damage x attack");
    static final Tuning.Key RADIUS = Tuning.ability("starfall", "radius", 12.0, "Radius");
    static final Tuning.Key TARGETS = Tuning.ability("starfall", "max_targets", 6, "Max targets");

    public StarfallUltimate() {
        super("starfall", "Starfall", "Calls down a rain of falling stars on up to six foes, leaving them Vulnerable.",
                90, 520, FelineElement.ARCANE, Category.ULTIMATE);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        return AbilityTargets.ultimatePriority(cat, AbilityTargets.foes(cat, RADIUS.get()).size());
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        List<LivingEntity> foes = AbilityTargets.foes(cat, RADIUS.get());
        if (foes.isEmpty()) return false;
        foes.sort(Comparator.comparingDouble(cat::distanceToSqr));
        cat.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 0.7F);
        cat.playSound(SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.2F, 0.9F);
        float dmg = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE.f();
        int n = Math.min(foes.size(), Math.max(1, TARGETS.i()));
        for (int i = 0; i < n; i++) {
            LivingEntity t = foes.get(i);
            t.hurt(cat.damageSources().indirectMagic(cat, cat), dmg);
            FelineStatusEffects.applyStatus(t, FelineStatusType.VULNERABLE, 2, 120);
            if (cat.level() instanceof ServerLevel sl) {
                for (int k = 0; k < 10; k++) {
                    sl.sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY() + 6.0 - k * 0.6, t.getZ(), 2, 0.05, 0.05, 0.05, 0.0);
                }
                sl.sendParticles(ParticleTypes.FLASH, t.getX(), t.getY() + 0.5, t.getZ(), 1, 0, 0, 0, 0);
            }
        }
        return true;
    }
}
