package com.kittytalents.combat.status;

import com.kittytalents.combat.FelineStatusType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.util.INBTSerializable;

@AutoRegisterCapability
public interface IFelineStatusCapability extends INBTSerializable<CompoundTag> {
    void apply(FelineStatusType status, int power, int durationTicks);
    boolean has(FelineStatusType status);
    int getPower(FelineStatusType status);
    void remove(FelineStatusType status);
    void clear();
    boolean isEmpty();
    CompoundTag getActiveStatuses();
    void tick(LivingEntity entity, StatusTickHandler tickHandler);

    @FunctionalInterface
    interface StatusTickHandler {
        void execute(LivingEntity entity, FelineStatusType status, int power, int ticksRemaining);
    }
}
