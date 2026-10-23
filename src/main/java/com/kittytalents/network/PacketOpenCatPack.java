package com.kittytalents.network;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketOpenCatPack {
    private final int entityId;

    public PacketOpenCatPack(int entityId) {
        this.entityId = entityId;
    }

    public PacketOpenCatPack(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                Entity entity = sender.level().getEntity(entityId);
                if (entity instanceof TalentedCat cat && (cat.isOwnedBy(sender) || cat.isTrusted(sender))) {
                    if (cat.distanceToSqr(sender) <= 64.0D * 64.0D) {
                        cat.openPackScreen(sender);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
