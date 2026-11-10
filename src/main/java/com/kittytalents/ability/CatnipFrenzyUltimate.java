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
import net.minecraft.world.entity.player.Player;

/**
 * Catnip Frenzy Ultimate Ability (R4).
 * Berserker ultimate entering an uncontrollable battle frenzy with enhanced speed,
 * regeneration, fortified armor, and fire trails.
 */
public class CatnipFrenzyUltimate extends CatAbility {
    private static final com.kittytalents.core.tuning.Tuning.Key TICKS =
            com.kittytalents.core.tuning.Tuning.ability("catnip_frenzy", "duration_ticks", 140, "Duration ticks");

    public CatnipFrenzyUltimate() {
        super("catnip_frenzy", "Catnip Frenzy", "Enters a legendary berserk rage, supercharging attack speed, granting Regeneration and Fortified.",
                80, 450, FelineElement.PHYSICAL, Category.ULTIMATE);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        if (cat.getTarget() != null && cat.getHealth() < cat.getMaxHealth() * 0.6f) {
            return 3.0f;
        }
        return 0.3f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        cat.playSound(SoundEvents.RAVAGER_ROAR, 1.2F, 1.5F);
        cat.playSound(SoundEvents.ENDER_DRAGON_GROWL, 0.8F, 1.8F);

        cat.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, TICKS.i(), 2));
        cat.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, TICKS.i(), 1));
        cat.addEffect(new MobEffectInstance(MobEffects.REGENERATION, TICKS.i(), 2));

        FelineStatusEffects.applyStatus(cat, FelineStatusType.HASTE, 2, TICKS.i());
        FelineStatusEffects.applyStatus(cat, FelineStatusType.FORTIFIED, 2, TICKS.i());
        FelineStatusEffects.applyStatus(cat, FelineStatusType.REGENERATION, 3, TICKS.i());

        if (cat.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, cat.getX(), cat.getY() + 0.4D, cat.getZ(), 30, 0.5D, 0.3D, 0.5D, 0.1D);
            sl.sendParticles(ParticleTypes.LAVA, cat.getX(), cat.getY() + 0.5D, cat.getZ(), 10, 0.4D, 0.2D, 0.4D, 0.05D);
        }

        return true;
    }
}
