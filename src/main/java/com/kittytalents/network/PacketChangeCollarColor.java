package com.kittytalents.network;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketChangeCollarColor {
    private final int entityId;
    private final int colorId;

    public PacketChangeCollarColor(int entityId, int colorId) {
        this.entityId = entityId;
        this.colorId = colorId;
    }

    public PacketChangeCollarColor(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.colorId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(colorId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                Entity entity = sender.level().getEntity(entityId);
                if (entity instanceof TalentedCat cat && (cat.isOwnedBy(sender) || cat.isTrusted(sender)) && cat.distanceToSqr(sender) <= 64.0D * 64.0D) {
                    DyeColor[] colors = DyeColor.values();
                    if (colorId >= 0 && colorId < colors.length) {
                        cat.setCollarColor(colors[colorId]);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
