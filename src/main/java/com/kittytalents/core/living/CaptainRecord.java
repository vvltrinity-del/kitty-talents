package com.kittytalents.core.living;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Which cat is each owner's Captain (5.0), saved with the world so "one per owner" holds for unloaded cats too. */
public final class CaptainRecord extends SavedData {
    private static final String NAME = "kittytalents_captains";
    private final Map<UUID, UUID> captainOf = new HashMap<>();

    public static CaptainRecord get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(CaptainRecord::load, CaptainRecord::new, NAME);
    }

    private static CaptainRecord load(CompoundTag tag) {
        CaptainRecord r = new CaptainRecord();
        for (String k : tag.getAllKeys()) {
            try { r.captainOf.put(UUID.fromString(k), tag.getUUID(k)); } catch (IllegalArgumentException ignored) {}
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        captainOf.forEach((o, c) -> tag.putUUID(o.toString(), c));
        return tag;
    }

    public UUID captainOf(UUID owner) { return captainOf.get(owner); }

    public void set(UUID owner, UUID cat) {
        if (cat == null) captainOf.remove(owner); else captainOf.put(owner, cat);
        setDirty();
    }
}
