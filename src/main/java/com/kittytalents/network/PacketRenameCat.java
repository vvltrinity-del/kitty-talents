package com.kittytalents.network;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketRenameCat {
    private final int entityId;
    private final String newName;

    public PacketRenameCat(int entityId, String newName) {
        this.entityId = entityId;
        this.newName = newName;
    }

    public PacketRenameCat(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.newName = buf.readUtf(64);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(newName);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                Entity entity = sender.level().getEntity(entityId);
                if (entity instanceof TalentedCat cat && cat.isPrimaryOwner(sender) && cat.distanceToSqr(sender) <= 64.0D * 64.0D) {
                    if (newName != null && !newName.trim().isEmpty()) {
                        cat.setCustomName(Component.literal(newName.trim()));
                        cat.setCustomNameVisible(true);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
