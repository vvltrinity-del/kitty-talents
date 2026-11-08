package com.kittytalents.talent.effect;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Everything a talent hook needs: the cat, the talent, its rank, mastery, and per-talent cooldowns. */
public final class TalentContext {
    public final TalentedCat cat;
    public final Talent talent;
    public final int rank;

    TalentContext(TalentedCat cat, Talent talent, int rank) {
        this.cat = cat;
        this.talent = talent;
        this.rank = rank;
    }

    /** Max rank reached: the talent's mastery perk is active. */
    public boolean mastered() {
        return TalentEngine.isMastered(cat, talent);
    }

    /** The owner, if online in the same level. */
    @Nullable
    public Player owner() {
        LivingEntity o = cat.getOwner();
        return o instanceof Player p && p.level() == cat.level() ? p : null;
    }

    /** The owner if within {@code blocks} of the cat. */
    @Nullable
    public Player ownerWithin(double blocks) {
        Player p = owner();
        return p != null && cat.distanceToSqr(p) <= blocks * blocks ? p : null;
    }

    /** True once the named cooldown of this talent has elapsed. */
    public boolean ready(String key) {
        return cat.level().getGameTime() >= cat.talentCooldowns().getOrDefault(talent.getId() + ":" + key, 0L);
    }

    public void cooldown(String key, int ticks) {
        cat.talentCooldowns().put(talent.getId() + ":" + key, cat.level().getGameTime() + ticks);
    }
}
