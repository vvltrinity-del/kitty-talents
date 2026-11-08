package com.kittytalents.ability;

import com.kittytalents.combat.FelineElement;
import com.kittytalents.combat.FelineStatusEffects;
import com.kittytalents.combat.FelineStatusType;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Fortress Guard active skill (R3 Tank, R4).
 * Stomps the ground, drawing enemy aggro and granting Fortified (30% damage reduction).
 */
public class FortressGuardAbility extends CatAbility {

    public FortressGuardAbility() {
        super("fortress_guard", "Fortress Guard", "Hardens defenses, taunting nearby enemies and granting Fortified to self and owner.",
                30, 140, FelineElement.PHYSICAL, Category.SKILL);
    }

    @Override
    public float getAiPriority(TalentedCat cat) {
        if (cat.getHealth() < cat.getMaxHealth() * 0.7f) return 2.0f;
        return 0.5f;
    }

    @Override
    protected boolean execute(TalentedCat cat, Player player) {
        cat.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 1.0F);
        cat.playSound(SoundEvents.ANVIL_LAND, 0.5F, 1.6F);

        FelineStatusEffects.applyStatus(cat, FelineStatusType.FORTIFIED, 2, 140);
        LivingEntity owner = cat.getOwner();
        if (owner != null) {
            FelineStatusEffects.applyStatus(owner, FelineStatusType.FORTIFIED, 1, 140);
        }

        AABB tauntArea = cat.getBoundingBox().inflate(8.0D);
        for (Mob mob : cat.level().getEntitiesOfClass(Mob.class, tauntArea, e -> e.getTarget() != cat && com.kittytalents.ai.CatTargeting.canHarm(cat, e))) {
            mob.setTarget(cat);
        }

        if (cat.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ENCHANT, cat.getX(), cat.getY() + 0.5D, cat.getZ(), 18, 0.4D, 0.4D, 0.4D, 0.1D);
        }

        return true;
    }
}
