package com.kittytalents.entity.ai;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Silverfish;

public class CatMouserGoal extends NearestAttackableTargetGoal<LivingEntity> {
    private final TalentedCat cat;

    public CatMouserGoal(TalentedCat cat) {
        super(cat, LivingEntity.class, 10, true, false, (entity) -> {
            if (!(entity instanceof Silverfish || entity instanceof Endermite || entity instanceof Bat || entity instanceof CaveSpider || entity instanceof Rabbit)) {
                return false;
            }
            return true;
        });
        this.cat = cat;
    }

    @Override
    public boolean canUse() {
        if (cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping() || cat.getCatMode() == CatMode.DOCILE) {
            return false;
        }
        if (cat.getTalentLevel(Talents.MOUSER) <= 0 && cat.getCatMode() != CatMode.HUNTING) {
            return false;
        }
        return super.canUse();
    }
}
