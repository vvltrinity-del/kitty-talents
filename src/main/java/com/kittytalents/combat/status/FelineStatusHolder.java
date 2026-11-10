package com.kittytalents.combat.status;

import com.kittytalents.combat.FelineStatusType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class FelineStatusHolder implements IFelineStatusCapability {
    public static class StatusEntry {
        public int ticks;
        public int power;

        public StatusEntry(int ticks, int power) {
            this.ticks = ticks;
            this.power = power;
        }
    }

    private final Map<FelineStatusType, StatusEntry> active = new EnumMap<>(FelineStatusType.class);

    @Override
    public void apply(FelineStatusType status, int power, int durationTicks) {
        if (status == null) return;
        int cappedDuration = Math.min(status.getMaxDurationTicks(), Math.max(20, durationTicks));
        int cappedPower = Math.min(status.getMaxPower(), Math.max(1, power));
        active.put(status, new StatusEntry(cappedDuration, cappedPower));
    }

    @Override
    public boolean has(FelineStatusType status) {
        if (status == null) return false;
        StatusEntry entry = active.get(status);
        return entry != null && entry.ticks > 0;
    }

    @Override
    public int getPower(FelineStatusType status) {
        if (status == null) return 0;
        StatusEntry entry = active.get(status);
        return entry != null && entry.ticks > 0 ? entry.power : 0;
    }

    @Override
    public void remove(FelineStatusType status) {
        if (status != null) {
            active.remove(status);
        }
    }

    @Override
    public void clear() {
        active.clear();
    }

    @Override
    public boolean isEmpty() {
        return active.isEmpty();
    }

    @Override
    public CompoundTag getActiveStatuses() {
        if (active.isEmpty()) return new CompoundTag();
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<FelineStatusType, StatusEntry> e : active.entrySet()) {
            if (e.getValue().ticks > 0) {
                CompoundTag sub = new CompoundTag();
                sub.putInt("Ticks", e.getValue().ticks);
                sub.putInt("Power", e.getValue().power);
                tag.put(e.getKey().name(), sub);
            }
        }
        return tag;
    }

    @Override
    public void tick(LivingEntity entity, StatusTickHandler tickHandler) {
        if (active.isEmpty()) return;
        List<FelineStatusType> keys = new ArrayList<>(active.keySet());
        for (FelineStatusType status : keys) {
            StatusEntry entry = active.get(status);
            if (entry == null) continue;
            if (entry.ticks <= 0) {
                active.remove(status);
                continue;
            }
            if (tickHandler != null) {
                tickHandler.execute(entity, status, entry.power, entry.ticks);
            }
            entry.ticks--;
            if (entry.ticks <= 0) {
                active.remove(status);
            }
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        return getActiveStatuses();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        active.clear();
        if (nbt == null) return;
        for (String key : nbt.getAllKeys()) {
            FelineStatusType type = FelineStatusType.fromString(key);
            if (type != null) {
                CompoundTag sub = nbt.getCompound(key);
                int ticks = sub.getInt("Ticks");
                int power = Math.max(1, sub.getInt("Power"));
                if (ticks > 0) {
                    active.put(type, new StatusEntry(ticks, power));
                }
            }
        }
    }
}
