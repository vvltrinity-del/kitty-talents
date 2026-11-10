package com.kittytalents.network;

import com.kittytalents.client.network.ClientPacketHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketSyncConductList extends PacketBase {
    public static class ConductCatDto {
        public final UUID catUUID;
        public final String name;
        public final String dimension;
        public final BlockPos pos;
        public final BlockPos bedPos;
        public final String bedDimension;

        public ConductCatDto(UUID catUUID, String name, String dimension, BlockPos pos, BlockPos bedPos, String bedDimension) {
            this.catUUID = catUUID;
            this.name = name != null ? name : "Cat";
            this.dimension = dimension != null ? dimension : "";
            this.pos = pos;
            this.bedPos = bedPos;
            this.bedDimension = bedDimension != null ? bedDimension : "";
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(catUUID);
            buf.writeUtf(name, 64);
            buf.writeUtf(dimension, 64);
            buf.writeBoolean(pos != null);
            if (pos != null) buf.writeBlockPos(pos);
            buf.writeBoolean(bedPos != null);
            if (bedPos != null) buf.writeBlockPos(bedPos);
            buf.writeUtf(bedDimension, 64);
        }

        public static ConductCatDto read(FriendlyByteBuf buf) {
            UUID id = buf.readUUID();
            String name = buf.readUtf(64);
            String dim = buf.readUtf(64);
            BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
            BlockPos bedPos = buf.readBoolean() ? buf.readBlockPos() : null;
            String bedDim = buf.readUtf(64);
            return new ConductCatDto(id, name, dim, pos, bedPos, bedDim);
        }
    }

    private final List<ConductCatDto> cats;
    private final boolean defaultToBed;

    public PacketSyncConductList(List<ConductCatDto> cats, boolean defaultToBed) {
        this.cats = cats;
        this.defaultToBed = defaultToBed;
    }

    public PacketSyncConductList(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        this.cats = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            cats.add(ConductCatDto.read(buf));
        }
        this.defaultToBed = buf.readBoolean();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(cats.size());
        for (ConductCatDto c : cats) {
            c.write(buf);
        }
        buf.writeBoolean(defaultToBed);
    }

    @Override
    protected void handleClient() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleOpenProducerStickScreen(cats, defaultToBed));
    }
}
