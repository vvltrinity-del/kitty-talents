package com.kittytalents.network;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketChangeCatSkin {
    private final int entityId;
    private final int targetSkin; // -1 = prev, -2 = next, >= 0 = explicit skin

    public PacketChangeCatSkin(int entityId, boolean next) {
        this.entityId = entityId;
        this.targetSkin = next ? -2 : -1;
    }

    public PacketChangeCatSkin(int entityId, int skinId) {
        this.entityId = entityId;
        this.targetSkin = skinId;
    }

    public PacketChangeCatSkin(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.targetSkin = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(targetSkin);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                Entity entity = sender.level().getEntity(entityId);
                if (entity instanceof TalentedCat cat && (cat.isOwnedBy(sender) || cat.isTrusted(sender)) && cat.distanceToSqr(sender) <= 64.0D * 64.0D) {
                    int currentSkin = cat.getSkinId();
                    int totalSkins = 17;
                    int newSkin;
                    if (targetSkin == -2) {
                        newSkin = (currentSkin + 1) % totalSkins;
                    } else if (targetSkin == -1) {
                        newSkin = (currentSkin - 1 + totalSkins) % totalSkins;
                    } else {
                        newSkin = Math.max(0, Math.min(targetSkin, totalSkins - 1));
                    }
                    cat.setSkinId(newSkin);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
