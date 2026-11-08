package com.kittytalents.network;

import com.kittytalents.core.perm.CatAction;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Client -> server: previous / next / explicit skin. */
public class PacketChangeCatSkin extends PacketBase {
    public static final int PREV = -1;
    public static final int NEXT = -2;
    /** Skin ids 0..16 (TalentedCat#setSkinId clamps to the same range). */
    public static final int SKIN_COUNT = 17;

    private final int entityId;
    private final int targetSkin;

    public PacketChangeCatSkin(int entityId, boolean next) {
        this(entityId, next ? NEXT : PREV);
    }

    public PacketChangeCatSkin(int entityId, int skinId) {
        this.entityId = entityId;
        this.targetSkin = skinId;
    }

    public PacketChangeCatSkin(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.targetSkin = buf.readInt();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(targetSkin);
    }

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender, entityId, targetSkin);
    }

    /** @return true if the skin was changed. Out-of-range explicit ids are rejected (they used to be clamped). */
    public static boolean apply(ServerPlayer sender, int entityId, int targetSkin) {
        if (targetSkin != PREV && targetSkin != NEXT && (targetSkin < 0 || targetSkin >= SKIN_COUNT)) return false;
        TalentedCat cat = authorize(sender, entityId, CatAction.STYLE);
        if (cat == null) return false;
        int current = cat.getSkinId();
        int skin = switch (targetSkin) {
            case NEXT -> (current + 1) % SKIN_COUNT;
            case PREV -> (current - 1 + SKIN_COUNT) % SKIN_COUNT;
            default -> targetSkin;
        };
        cat.setSkinId(skin);
        return true;
    }
}
