package com.kittytalents.network;

import com.kittytalents.core.perm.CatAction;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Client -> server (5.0): promote a cat to Captain (owner only, bond 10), or make it step down. */
public class PacketCaptain extends PacketBase {
    private final int entityId;
    private final boolean promote;

    public PacketCaptain(int entityId, boolean promote) {
        this.entityId = entityId;
        this.promote = promote;
    }

    public PacketCaptain(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.promote = buf.readBoolean();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(promote);
    }

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender, entityId, promote);
    }

    public static boolean apply(ServerPlayer sender, int entityId, boolean promote) {
        TalentedCat cat = authorize(sender, entityId, CatAction.TRAIN);
        if (cat == null) return false;
        if (!promote) {
            cat.demoteCaptain();
            return true;
        }
        if (!cat.promoteToCaptain()) {
            sender.displayClientMessage(Component.translatable("message.kittytalents.captain_bond", cat.getName(), TalentedCat.CAPTAIN_BOND)
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        sender.displayClientMessage(Component.translatable("message.kittytalents.captain_promoted", cat.getName()).withStyle(ChatFormatting.GOLD), true);
        return true;
    }
}
