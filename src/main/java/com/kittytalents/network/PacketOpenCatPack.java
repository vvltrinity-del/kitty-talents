package com.kittytalents.network;

import com.kittytalents.core.perm.CatAction;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Client -> server: open a cat's pack inventory. */
public class PacketOpenCatPack extends PacketBase {
    private final int entityId;

    public PacketOpenCatPack(int entityId) {
        this.entityId = entityId;
    }

    public PacketOpenCatPack(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender, entityId);
    }

    public static boolean apply(ServerPlayer sender, int entityId) {
        TalentedCat cat = authorize(sender, entityId, CatAction.OPEN_PACK);
        if (cat == null) return false;
        cat.openPackScreen(sender);
        return true;
    }
}
