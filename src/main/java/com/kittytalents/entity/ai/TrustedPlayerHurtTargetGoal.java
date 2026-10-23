package com.kittytalents.entity.ai;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

public class TrustedPlayerHurtTargetGoal extends TargetGoal {
    private final TalentedCat cat;
    private LivingEntity target;
    private int timestamp;

    public TrustedPlayerHurtTargetGoal(TalentedCat cat) {
        super(cat, false);
        this.cat = cat;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (cat.isIncapacitated() || cat.isOrderedToSit() || cat.isSleeping() || cat.isTwined() || cat.getCatMode() == CatMode.DOCILE || cat.getCatMode() == CatMode.WANDER) {
            return false;
        }
        if (cat.getTrustedPlayers().isEmpty()) {
            return false;
        }

        List<Player> nearby = cat.level().getEntitiesOfClass(
                Player.class,
                cat.getBoundingBox().inflate(16.0D),
                p -> cat.isTrusted(p.getUUID()) && p != cat.getOwner()
        );

        for (Player trusted : nearby) {
            LivingEntity lastHurt = trusted.getLastHurtMob();
            int lastHurtTimestamp = trusted.getLastHurtMobTimestamp();
            if (lastHurt != null && lastHurtTimestamp != this.timestamp && this.canAttack(lastHurt, TargetingConditions.DEFAULT)) {
                if (lastHurt instanceof Player p && cat.isTrusted(p)) {
                    continue;
                }
                this.target = lastHurt;
                this.timestamp = lastHurtTimestamp;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.target);
        super.start();
    }
}
