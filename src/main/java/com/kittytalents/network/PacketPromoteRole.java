package com.kittytalents.network;

import com.kittytalents.combat.RolePromotions;
import com.kittytalents.core.perm.CatAction;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Client -> server (5.0): promote a max-level cat in its current role (owner only). */
public class PacketPromoteRole extends PacketBase {
    private final int entityId;

    public PacketPromoteRole(int entityId) { this.entityId = entityId; }

    public PacketPromoteRole(FriendlyByteBuf buf) { this.entityId = buf.readInt(); }

    @Override
    public void toBytes(FriendlyByteBuf buf) { buf.writeInt(entityId); }

    @Override
    protected void handleServer(ServerPlayer sender) { apply(sender, entityId); }

    public static boolean apply(ServerPlayer sender, int entityId) {
        TalentedCat cat = authorize(sender, entityId, CatAction.TRAIN);
        if (cat == null || !cat.promoteRole()) return false;
        sender.displayClientMessage(Component.translatable("message.kittytalents.promoted", cat.getName(),
                Component.translatable(RolePromotions.key(cat.getCatRole()))).withStyle(ChatFormatting.GOLD), true);
        return true;
    }
}
