package com.kittytalents.network;

import com.kittytalents.client.network.ClientPacketHandlers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class PacketSyncCatStats extends PacketBase {
    private final int entityId;
    private final CompoundTag statsTag;

    public PacketSyncCatStats(int entityId, CompoundTag statsTag) {
        this.entityId = entityId;
        this.statsTag = statsTag != null ? statsTag : new CompoundTag();
    }

    public PacketSyncCatStats(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.statsTag = buf.readNbt();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeNbt(statsTag);
    }

    @Override
    protected void handleClient() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleSyncCatStats(entityId, statsTag));
    }
}
