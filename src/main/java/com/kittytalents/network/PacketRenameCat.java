package com.kittytalents.network;

import com.kittytalents.core.perm.CatAction;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Client -> server: rename a cat (owner only). Names are cleaned: no formatting codes, at most 32 characters. */
public class PacketRenameCat extends PacketBase {
    /** Matches the Home tab's name field. */
    public static final int MAX_NAME = 32;

    private final int entityId;
    private final String newName;

    public PacketRenameCat(int entityId, String newName) {
        this.entityId = entityId;
        this.newName = newName == null ? "" : newName;
    }

    public PacketRenameCat(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.newName = buf.readUtf(64);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(newName, 64);
    }

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender, entityId, newName);
    }

    public static boolean apply(ServerPlayer sender, int entityId, String rawName) {
        String name = cleanText(rawName, MAX_NAME);
        if (name.isEmpty()) return false;
        TalentedCat cat = authorize(sender, entityId, CatAction.RENAME);
        if (cat == null) return false;
        cat.setCustomName(Component.literal(name));
        cat.setCustomNameVisible(true);
        return true;
    }
}
