package com.kittytalents.network;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketChangeCatMode {
    private final int entityId;
    private final int modeOrdinal;

    public PacketChangeCatMode(int entityId) {
        this.entityId = entityId;
        this.modeOrdinal = -1;
    }

    public PacketChangeCatMode(int entityId, int modeOrdinal) {
        this.entityId = entityId;
        this.modeOrdinal = modeOrdinal;
    }

    public PacketChangeCatMode(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.modeOrdinal = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(modeOrdinal);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                Entity entity = sender.level().getEntity(entityId);
                if (entity instanceof TalentedCat cat && (cat.isOwnedBy(sender) || cat.isTrusted(sender))) {
                    if (modeOrdinal >= 0 && modeOrdinal < CatMode.values().length) {
                        cat.setCatMode(CatMode.byId(modeOrdinal));
                    } else {
                        cat.setCatMode(cat.getCatMode().next());
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
