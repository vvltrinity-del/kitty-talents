package com.kittytalents.network;

import com.kittytalents.client.network.ClientPacketHandlers;
import com.kittytalents.core.component.CatLoadout;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: the authoritative combat loadout of a cat (for the Combat tab and the Twine HUD). */
public class PacketSyncCatLoadout {
    private final int entityId;
    private final CatLoadout loadout;

    public PacketSyncCatLoadout(int entityId, CatLoadout loadout) {
        this.entityId = entityId;
        this.loadout = loadout;
    }

    public PacketSyncCatLoadout(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.loadout = CatLoadout.read(buf);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        loadout.write(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.handleSyncLoadout(entityId, loadout)));
        ctx.get().setPacketHandled(true);
    }
}
