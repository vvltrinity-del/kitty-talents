package com.kittytalents.entity.ai;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CatCreeperWardenGoal extends Goal {
    private final TalentedCat cat;
    private int hissTimer = 0;

    public CatCreeperWardenGoal(TalentedCat cat) {
        this.cat = cat;
    }

    @Override
    public boolean canUse() {
        return !cat.isIncapacitated() && !cat.isOrderedToSit() && !cat.isSleeping() && !cat.isTwined() && cat.getTalentLevel(Talents.NIGHT_PROWLER) > 0;
    }

    @Override
    public boolean canContinueToUse() {
        return !cat.isIncapacitated() && !cat.isOrderedToSit() && !cat.isSleeping() && !cat.isTwined() && cat.getTalentLevel(Talents.NIGHT_PROWLER) > 0;
    }

    @Override
    public void tick() {
        if (hissTimer > 0) {
            hissTimer--;
        }

        if (cat.tickCount % 10 != 0) {
            return;
        }

        int level = cat.getTalentLevel(Talents.NIGHT_PROWLER);
        double radius = 6.0D + (level * 2.0D); // Up to 16 blocks
        AABB box = cat.getBoundingBox().inflate(radius);

        // Repel Creepers
        List<Creeper> creepers = cat.level().getEntitiesOfClass(Creeper.class, box);
        for (Creeper creeper : creepers) {
            Vec3 diff = creeper.position().subtract(cat.position());
            Vec3 fleeDir = diff.lengthSqr() > 1.0E-4D ? diff.normalize() : new Vec3(1, 0, 0);
            creeper.setDeltaMovement(fleeDir.x * 0.4D, 0.1D, fleeDir.z * 0.4D);
        }

        // Repel Phantoms
        List<Phantom> phantoms = cat.level().getEntitiesOfClass(Phantom.class, box);
        for (Phantom phantom : phantoms) {
            Vec3 diff = phantom.position().subtract(cat.position());
            Vec3 fleeDir = diff.lengthSqr() > 1.0E-4D ? diff.normalize() : new Vec3(1, 0, 0);
            phantom.setDeltaMovement(fleeDir.x * 0.6D, 0.4D, fleeDir.z * 0.6D);
        }

        if ((!creepers.isEmpty() || !phantoms.isEmpty()) && hissTimer <= 0) {
            cat.playSound(SoundEvents.CAT_HISS, 1.0F, 1.0F);
            hissTimer = 60;
            if (!creepers.isEmpty()) {
                for (int i = 0; i < creepers.size(); i++) {
                    cat.incrementCreepersScared();
                }
                cat.syncStats();
            }
        }

        // Night vision to owner at night (400 ticks: under 200 it blinks, which made Night Prowler flicker)
        if (cat.level().isNight() && cat.isTame() && cat.getOwner() instanceof Player player) {
            if (cat.distanceToSqr(player) < (radius * radius)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, true));
            }
        }
    }
}
