package com.kittytalents.core.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Players the owner has trusted with this cat (UUID -> last known name). NBT key unchanged from 2.0. */
public final class CatTrust {
    private final Map<UUID, String> trusted = new LinkedHashMap<>();

    /** Read-only view; mutate through {@link #add}/{@link #remove}/{@link #replaceAll}. */
    public Map<UUID, String> view() { return Collections.unmodifiableMap(trusted); }
    public boolean contains(UUID id) { return id != null && trusted.containsKey(id); }
    public void add(UUID id, String name) { if (id != null) trusted.put(id, name == null ? "" : name); }
    public void remove(UUID id) { trusted.remove(id); }
    public void replaceAll(Map<UUID, String> players) {
        trusted.clear();
        if (players != null) players.forEach(this::add);
    }

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        trusted.forEach((id, name) -> {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", id);
            t.putString("Name", name);
            list.add(t);
        });
        tag.put("TrustedPlayers", list);
    }

    public void load(CompoundTag tag) {
        trusted.clear();
        ListTag list = tag.getList("TrustedPlayers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            if (t.hasUUID("Id")) trusted.put(t.getUUID("Id"), t.getString("Name"));
        }
    }
}
