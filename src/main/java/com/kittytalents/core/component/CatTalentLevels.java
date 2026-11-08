package com.kittytalents.core.component;

import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Trained rank per talent id. Pure storage: training rules (cost, Dire gating) stay with the talent system.
 * NBT key unchanged from 2.0. Unknown ids are kept so a talent removed in one version is not lost if it returns.
 */
public final class CatTalentLevels {
    private final Map<String, Integer> levels = new HashMap<>();

    /** @return the stored rank, or {@code fallback} if this talent was never set (standard talents default above 0). */
    public int getOrDefault(String id, int fallback) { return id == null ? fallback : levels.getOrDefault(id, fallback); }

    /**
     * Stores the rank, including 0. A stored 0 is meaningful: "never set" and "set to 0" differ for standard talents,
     * which default to a free baseline rank when absent.
     */
    public void set(String id, int level) {
        if (id != null) levels.put(id, Math.max(0, level));
    }

    /** Read-only view of every trained talent. */
    public Map<String, Integer> view() { return Collections.unmodifiableMap(levels); }

    public void replaceAll(Map<String, Integer> newLevels) {
        levels.clear();
        if (newLevels != null) newLevels.forEach(this::set);
    }

    public void clear() { levels.clear(); }

    public void save(CompoundTag tag) {
        CompoundTag t = new CompoundTag();
        levels.forEach(t::putInt);
        tag.put("Talents", t);
    }

    public void load(CompoundTag tag) {
        if (!tag.contains("Talents", CompoundTag.TAG_COMPOUND)) return;
        levels.clear();
        CompoundTag t = tag.getCompound("Talents");
        for (String key : t.getAllKeys()) set(key, t.getInt(key));
    }
}
