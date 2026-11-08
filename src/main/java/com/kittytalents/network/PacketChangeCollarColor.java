package com.kittytalents.network;

import com.kittytalents.core.perm.CatAction;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;

/** Client -> server: set the collar dye colour (0..15). */
public class PacketChangeCollarColor extends PacketBase {
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

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(colorId);
    }

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender, entityId, colorId);
    }

    public static boolean apply(ServerPlayer sender, int entityId, int colorId) {
        DyeColor[] colors = DyeColor.values();
        if (colorId < 0 || colorId >= colors.length) return false;
        TalentedCat cat = authorize(sender, entityId, CatAction.STYLE);
        if (cat == null) return false;
        cat.setCollarColor(colors[colorId]);
        return true;
    }
}
