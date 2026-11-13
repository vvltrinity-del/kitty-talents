package com.kittytalents.core.base;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base areas marked with the Base Pin (5.0), saved per owner with the world. An owner can keep several areas (a main
 * base, a farm, an outpost); overlapping or touching ones can be merged. Guard cats inside an area without a guard
 * post patrol it in small teams (BasePatrol).
 */
public final class BaseAreas extends SavedData {
    private static final String NAME = "kittytalents_bases";
    public static final int MAX_AREAS = 16, MAX_SIDE = 256;

    /** One area: inclusive block box in a dimension. */
    public record Area(int id, String name, String dimension, BlockPos min, BlockPos max) {
        public static Area of(int id, String name, String dim, BlockPos a, BlockPos b) {
            return new Area(id, name, dim, new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ())),
                    new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())));
        }
        public boolean contains(String dim, BlockPos p) {
            return dimension.equals(dim) && p.getX() >= min.getX() && p.getX() <= max.getX() && p.getZ() >= min.getZ() && p.getZ() <= max.getZ()
                    && p.getY() >= min.getY() - 8 && p.getY() <= max.getY() + 8;
        }
        /** Overlapping or touching (1 block apart) on the ground plan, same dimension. */
        public boolean touches(Area o) {
            return dimension.equals(o.dimension) && min.getX() <= o.max.getX() + 1 && o.min.getX() <= max.getX() + 1
                    && min.getZ() <= o.max.getZ() + 1 && o.min.getZ() <= max.getZ() + 1;
        }
        public int sizeX() { return max.getX() - min.getX() + 1; }
        public int sizeZ() { return max.getZ() - min.getZ() + 1; }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("Id", id);
            t.putString("Name", name);
            t.putString("Dim", dimension);
            t.put("Min", NbtUtils.writeBlockPos(min));
            t.put("Max", NbtUtils.writeBlockPos(max));
            return t;
        }
        static Area load(CompoundTag t) {
            return new Area(t.getInt("Id"), t.getString("Name"), t.getString("Dim"), NbtUtils.readBlockPos(t.getCompound("Min")), NbtUtils.readBlockPos(t.getCompound("Max")));
        }
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(id);
            buf.writeUtf(name, 32);
            buf.writeUtf(dimension, 128);
            buf.writeBlockPos(min);
            buf.writeBlockPos(max);
        }
        public static Area read(FriendlyByteBuf buf) {
            return new Area(buf.readVarInt(), buf.readUtf(32), buf.readUtf(128), buf.readBlockPos(), buf.readBlockPos());
        }
    }

    private final Map<UUID, List<Area>> areas = new HashMap<>();
    private int nextId = 1;

    public static BaseAreas get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BaseAreas::load, BaseAreas::new, NAME);
    }

    private static BaseAreas load(CompoundTag tag) {
        BaseAreas b = new BaseAreas();
        b.nextId = Math.max(1, tag.getInt("NextId"));
        CompoundTag owners = tag.getCompound("Owners");
        for (String k : owners.getAllKeys()) {
            List<Area> list = new ArrayList<>();
            for (Tag t : owners.getList(k, Tag.TAG_COMPOUND)) list.add(Area.load((CompoundTag) t));
            try { b.areas.put(UUID.fromString(k), list); } catch (IllegalArgumentException ignored) {}
        }
        return b;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("NextId", nextId);
        CompoundTag owners = new CompoundTag();
        areas.forEach((o, list) -> {
            ListTag l = new ListTag();
            list.forEach(a -> l.add(a.save()));
            owners.put(o.toString(), l);
        });
        tag.put("Owners", owners);
        return tag;
    }

    public List<Area> of(UUID owner) {
        return List.copyOf(areas.getOrDefault(owner, List.of()));
    }

    /** The owner's area containing {@code pos}, or null. */
    public Area at(UUID owner, String dim, BlockPos pos) {
        for (Area a : areas.getOrDefault(owner, List.of())) if (a.contains(dim, pos)) return a;
        return null;
    }

    /** Adds an area from two corners. @return the area, or null if the owner has too many or it is too big. */
    public Area add(UUID owner, String dim, BlockPos a, BlockPos b) {
        List<Area> list = areas.computeIfAbsent(owner, k -> new ArrayList<>());
        if (list.size() >= MAX_AREAS) return null;
        Area area = Area.of(nextId++, "Base " + (list.size() + 1), dim, a, b);
        if (area.sizeX() > MAX_SIDE || area.sizeZ() > MAX_SIDE) return null;
        list.add(area);
        setDirty();
        return area;
    }

    public boolean resize(UUID owner, int id, String dim, BlockPos a, BlockPos b) {
        List<Area> list = areas.get(owner);
        if (list == null) return false;
        for (int i = 0; i < list.size(); i++) {
            Area old = list.get(i);
            if (old.id() != id) continue;
            Area area = Area.of(id, old.name(), dim, a, b);
            if (area.sizeX() > MAX_SIDE || area.sizeZ() > MAX_SIDE) return false;
            list.set(i, area);
            setDirty();
            return true;
        }
        return false;
    }

    public boolean rename(UUID owner, int id, String name) {
        List<Area> list = areas.get(owner);
        if (list == null || name.isBlank()) return false;
        for (int i = 0; i < list.size(); i++) {
            Area a = list.get(i);
            if (a.id() == id) {
                list.set(i, new Area(id, name, a.dimension(), a.min(), a.max()));
                setDirty();
                return true;
            }
        }
        return false;
    }

    public boolean delete(UUID owner, int id) {
        List<Area> list = areas.get(owner);
        boolean removed = list != null && list.removeIf(a -> a.id() == id);
        if (removed) setDirty();
        return removed;
    }

    /** Merges area {@code id} with every touching area of the same owner into one box. @return how many were merged in. */
    public int merge(UUID owner, int id) {
        List<Area> list = areas.get(owner);
        if (list == null) return 0;
        Area base = list.stream().filter(a -> a.id() == id).findFirst().orElse(null);
        if (base == null) return 0;
        int merged = 0;
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Area o : List.copyOf(list)) {
                if (o.id() == base.id() || !base.touches(o)) continue;
                Area joined = Area.of(base.id(), base.name(), base.dimension(),
                        new BlockPos(Math.min(base.min().getX(), o.min().getX()), Math.min(base.min().getY(), o.min().getY()), Math.min(base.min().getZ(), o.min().getZ())),
                        new BlockPos(Math.max(base.max().getX(), o.max().getX()), Math.max(base.max().getY(), o.max().getY()), Math.max(base.max().getZ(), o.max().getZ())));
                if (joined.sizeX() > MAX_SIDE || joined.sizeZ() > MAX_SIDE) continue;
                list.remove(o);
                final int baseId = base.id();
                list.replaceAll(a -> a.id() == baseId ? joined : a);
                base = joined;
                merged++;
                grew = true;
            }
        }
        if (merged > 0) setDirty();
        return merged;
    }
}
