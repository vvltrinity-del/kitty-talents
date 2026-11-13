package com.kittytalents.talent;

import com.kittytalents.KittyTalents;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;

/**
 * Automatic skill-point refunds for reworked talents (5.0 save safety). Each reworked talent gets a revision number
 * here; a cat saves the revisions it has seen. When a cat loads with an older revision of a talent it has bought
 * ranks in, those ranks are refunded (back to the free baseline for standard talents, else 0) so the owner can
 * re-spend them on the new version. Dire ranks come from treats, not points, and are left alone.
 */
public final class TalentRevisions {
    private TalentRevisions() {}

    /** Bump a talent's number whenever its effect changes in a way that should give its points back. */
    public static final Map<String, Integer> CURRENT = Map.of(
            "feline_reflexes", 1, // 5.0: melee parry only (projectiles moved to Feline Agility)
            "feline_agility", 1   // 5.0: projectile dodge only; mastery Ghost Step is now a speed burst
    );

    public static final String TAG = "TalentRevisions";

    public static void save(CompoundTag tag) {
        CompoundTag t = new CompoundTag();
        CURRENT.forEach(t::putInt);
        tag.put(TAG, t);
    }

    /** Refunds outdated talents on {@code cat}. @return points refunded. */
    public static int apply(TalentedCat cat, CompoundTag tag) {
        CompoundTag seen = tag.getCompound(TAG);
        int refunded = 0;
        for (Map.Entry<String, Integer> e : CURRENT.entrySet()) {
            if (seen.getInt(e.getKey()) >= e.getValue()) continue;
            Talent talent = Talents.get(e.getKey());
            if (talent == null || talent.requiresDireTreat()) continue;
            int level = cat.getTalentLevel(talent);
            int floor = talent.isStandard() ? TalentedCat.STANDARD_TALENT_BASELINE : 0;
            if (level <= floor) continue;
            int points = 0;
            for (int r = floor + 1; r <= level; r++) points += TalentedCat.costOfRank(talent, r);
            cat.talents().set(talent.getId(), floor);
            refunded += points;
        }
        if (refunded > 0) {
            cat.setSkillPoints(cat.getSkillPoints() + refunded);
            KittyTalents.LOGGER.info("[Talents] refunded {} skill points to {} for reworked talents", refunded, cat.getName().getString());
        }
        return refunded;
    }
}
