package com.kittytalents.core.living;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Captain cats that are rallying: perched on a Cat Tree (5.0 "Captain cat"). While one rallies, the owner's cats
 * within 16 blocks deal +10% damage and working Habitats within 16 blocks run 5% faster (HabitatModifiers).
 */
public final class CaptainRally {
    private CaptainRally() {}

    public static final double RANGE = 16.0;
    public static final float DAMAGE_BONUS = 0.10F;

    private static final Map<Level, Set<TalentedCat>> RALLYING = new WeakHashMap<>();

    public static void set(TalentedCat captain, boolean rallying) {
        if (captain.level().isClientSide) return;
        Set<TalentedCat> set = RALLYING.computeIfAbsent(captain.level(), l -> Collections.newSetFromMap(new WeakHashMap<>()));
        if (rallying) set.add(captain); else set.remove(captain);
    }

    private static Set<TalentedCat> in(Level level) {
        Set<TalentedCat> set = RALLYING.get(level);
        if (set == null) return Set.of();
        set.removeIf(c -> !c.isAlive() || c.isRemoved() || !c.isCaptain());
        return set;
    }

    public static boolean boostsHabitat(Level level, BlockPos pos) {
        for (TalentedCat c : in(level)) {
            if (c.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= RANGE * RANGE) return true;
        }
        return false;
    }

    /** True if a rallying Captain of the same owner is within range of {@code cat} (the Captain counts itself). */
    public static boolean boostsCat(TalentedCat cat) {
        if (cat.getOwnerUUID() == null) return false;
        for (TalentedCat c : in(cat.level())) {
            if (cat.getOwnerUUID().equals(c.getOwnerUUID()) && c.distanceToSqr(cat) <= RANGE * RANGE) return true;
        }
        return false;
    }
}
