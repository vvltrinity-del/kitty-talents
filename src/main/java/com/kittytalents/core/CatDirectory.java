package com.kittytalents.core;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CatDirectory extends SavedData {
    public static class Entry {
        public UUID catUUID;
        public UUID ownerUUID;
        public String name;
        public String dimension;
        public BlockPos pos;
        public BlockPos bedPos;
        public String bedDimension;
        public long lastSeenTick;

        public Entry(UUID catUUID, UUID ownerUUID, String name, String dimension, BlockPos pos, BlockPos bedPos, String bedDimension) {
            this.catUUID = catUUID;
            this.ownerUUID = ownerUUID;
            this.name = name != null && !name.isBlank() ? name : "Cat";
            this.dimension = dimension != null ? dimension : "minecraft:overworld";
            this.pos = pos;
            this.bedPos = bedPos;
            this.bedDimension = bedDimension != null ? bedDimension : "";
            this.lastSeenTick = 0;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("CatUUID", catUUID);
            if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
            tag.putString("Name", name);
            tag.putString("Dim", dimension);
            if (pos != null) {
                tag.putInt("X", pos.getX());
                tag.putInt("Y", pos.getY());
                tag.putInt("Z", pos.getZ());
            }
            if (bedPos != null) {
                tag.putInt("BedX", bedPos.getX());
                tag.putInt("BedY", bedPos.getY());
                tag.putInt("BedZ", bedPos.getZ());
            }
            tag.putString("BedDim", bedDimension);
            tag.putLong("LastSeen", lastSeenTick);
            return tag;
        }

        public static Entry load(CompoundTag tag) {
            UUID catUUID = tag.getUUID("CatUUID");
            UUID ownerUUID = tag.hasUUID("OwnerUUID") ? tag.getUUID("OwnerUUID") : null;
            String name = tag.getString("Name");
            String dim = tag.getString("Dim");
            BlockPos pos = tag.contains("X") ? new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")) : null;
            BlockPos bedPos = tag.contains("BedX") ? new BlockPos(tag.getInt("BedX"), tag.getInt("BedY"), tag.getInt("BedZ")) : null;
            String bedDim = tag.getString("BedDim");
            Entry e = new Entry(catUUID, ownerUUID, name, dim, pos, bedPos, bedDim);
            e.lastSeenTick = tag.getLong("LastSeen");
            return e;
        }
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    public static CatDirectory get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                CatDirectory::load,
                CatDirectory::new,
                "kittytalents_cats"
        );
    }

    public static CatDirectory load(CompoundTag tag) {
        CatDirectory dir = new CatDirectory();
        ListTag list = tag.getList("Cats", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Entry e = Entry.load(list.getCompound(i));
            dir.entries.put(e.catUUID, e);
        }
        return dir;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            list.add(e.save());
        }
        tag.put("Cats", list);
        return tag;
    }

    public void updateCat(TalentedCat cat) {
        UUID owner = cat.getOwnerUUID();
        if (owner == null) return;
        String name = cat.getName().getString();
        String dim = cat.level().dimension().location().toString();
        BlockPos pos = cat.blockPosition();
        BlockPos bedPos = cat.getHomeBedPos();
        String bedDim = cat.getHomeBedDimension();
        Entry e = entries.computeIfAbsent(cat.getUUID(), id -> new Entry(id, owner, name, dim, pos, bedPos, bedDim));
        e.ownerUUID = owner;
        e.name = name;
        e.dimension = dim;
        e.pos = pos;
        e.bedPos = bedPos;
        e.bedDimension = bedDim;
        e.lastSeenTick = cat.level().getGameTime();
        setDirty();
    }

    /** Every known cat (5.0 safe removal reaches unloaded ones through this). */
    public List<Entry> all() {
        return List.copyOf(entries.values());
    }

    public List<Entry> getCatsForOwner(UUID ownerUUID) {
        List<Entry> result = new ArrayList<>();
        for (Entry e : entries.values()) {
            if (ownerUUID.equals(e.ownerUUID)) {
                result.add(e);
            }
        }
        return result;
    }

    @Nullable
    public Entry getEntry(UUID catUUID) {
        return entries.get(catUUID);
    }
}
