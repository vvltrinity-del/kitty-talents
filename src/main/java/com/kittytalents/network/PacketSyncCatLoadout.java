package com.kittytalents.network;

import com.kittytalents.client.network.ClientPacketHandlers;
import com.kittytalents.core.component.CatLoadout;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;


/** Server -> client: the authoritative combat loadout of a cat (for the Combat tab and the Twine HUD). */
public class PacketSyncCatLoadout extends PacketBase {
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

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        loadout.write(buf);
    }

    @Override
    protected void handleClient() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleSyncLoadout(entityId, loadout));
    }
}
