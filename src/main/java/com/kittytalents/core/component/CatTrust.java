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
    private final Map<UUID, com.kittytalents.core.perm.TrustRole> roles = new java.util.HashMap<>();

    /** The co-owner's role; FULL for co-owners saved before 4.0 (what they could do then). */
    public com.kittytalents.core.perm.TrustRole role(UUID id) { return roles.getOrDefault(id, com.kittytalents.core.perm.TrustRole.FULL); }
    public void setRole(UUID id, com.kittytalents.core.perm.TrustRole role) { if (trusted.containsKey(id) && role != null) roles.put(id, role); }
    public Map<UUID, com.kittytalents.core.perm.TrustRole> roles() {
        Map<UUID, com.kittytalents.core.perm.TrustRole> out = new LinkedHashMap<>();
        trusted.keySet().forEach(id -> out.put(id, role(id)));
        return out;
    }

    /** Read-only view; mutate through {@link #add}/{@link #remove}/{@link #replaceAll}. */
    public Map<UUID, String> view() { return Collections.unmodifiableMap(trusted); }
    public boolean contains(UUID id) { return id != null && trusted.containsKey(id); }
    public void add(UUID id, String name) { if (id != null) trusted.put(id, name == null ? "" : name); }
    public void remove(UUID id) { trusted.remove(id); roles.remove(id); }
    public void replaceAll(Map<UUID, String> players) {
        trusted.clear();
        if (players != null) players.forEach(this::add);
        roles.keySet().retainAll(trusted.keySet());
    }

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        trusted.forEach((id, name) -> {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", id);
            t.putString("Name", name);
            t.putString("Role", role(id).name());
            list.add(t);
        });
        tag.put("TrustedPlayers", list);
    }

    public void load(CompoundTag tag) {
        trusted.clear();
        roles.clear();
        ListTag list = tag.getList("TrustedPlayers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            if (t.hasUUID("Id")) {
                trusted.put(t.getUUID("Id"), t.getString("Name"));
                if (t.contains("Role")) roles.put(t.getUUID("Id"), com.kittytalents.core.perm.TrustRole.byName(t.getString("Role")));
            }
        }
    }
}
