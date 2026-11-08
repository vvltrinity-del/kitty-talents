package com.kittytalents.network;

import com.kittytalents.core.perm.CatAction;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Client -> server: set a cat's mode by ordinal, or {@link #NEXT} to cycle to the next mode. */
public class PacketChangeCatMode extends PacketBase {
    public static final int NEXT = -1;

    private final int entityId;
    private final int modeOrdinal;

    public PacketChangeCatMode(int entityId) {
        this(entityId, NEXT);
    }

    public PacketChangeCatMode(int entityId, int modeOrdinal) {
        this.entityId = entityId;
        this.modeOrdinal = modeOrdinal;
    }

    public PacketChangeCatMode(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.modeOrdinal = buf.readInt();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(modeOrdinal);
    }

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender, entityId, modeOrdinal);
    }

    /** @return true if the mode was set. Ordinals other than NEXT or a real mode are rejected. */
    public static boolean apply(ServerPlayer sender, int entityId, int modeOrdinal) {
        if (modeOrdinal != NEXT && (modeOrdinal < 0 || modeOrdinal >= CatMode.values().length)) return false;
        TalentedCat cat = authorize(sender, entityId, CatAction.COMMAND);
        if (cat == null) return false;
        cat.setCatMode(modeOrdinal == NEXT ? cat.getCatMode().next() : CatMode.values()[modeOrdinal]);
        return true;
    }
}
