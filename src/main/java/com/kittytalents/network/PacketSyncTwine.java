package com.kittytalents.network;

import com.kittytalents.client.network.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncTwine {
    private final int catEntityId;
    private final boolean twined;

    public PacketSyncTwine(int catEntityId, boolean twined) {
        this.catEntityId = catEntityId;
        this.twined = twined;
    }

    public PacketSyncTwine(FriendlyByteBuf buf) {
        this.catEntityId = buf.readInt();
        this.twined = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(catEntityId);
        buf.writeBoolean(twined);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientPacketHandlers.handleSyncTwine(catEntityId, twined);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
