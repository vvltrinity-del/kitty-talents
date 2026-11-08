package com.kittytalents.network;

import com.kittytalents.client.network.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** Server -> client: a Twine session with {@code catEntityId} started or ended ({@code -1} = whatever session is active). */
public class PacketSyncTwine extends PacketBase {
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

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(catEntityId);
        buf.writeBoolean(twined);
    }

    @Override
    protected void handleClient() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleSyncTwine(catEntityId, twined));
    }
}
