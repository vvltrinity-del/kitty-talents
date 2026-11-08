package com.kittytalents.network;

import com.kittytalents.core.perm.CatAction;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Client -> server: set a cat's gender. Costs one Gender Ribbon outside creative. */
public class PacketChangeCatGender extends PacketBase {
    private final int entityId;
    private final boolean female;

    public PacketChangeCatGender(int entityId, boolean female) {
        this.entityId = entityId;
        this.female = female;
    }

    public PacketChangeCatGender(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.female = buf.readBoolean();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(female);
    }

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender, entityId, female);
    }

    /** @return true if the gender was changed. */
    public static boolean apply(ServerPlayer sender, int entityId, boolean female) {
        TalentedCat cat = authorize(sender, entityId, CatAction.STYLE);
        if (cat == null) return false;
        if (cat.isFemale() == female) return false; // nothing to do; don't eat a ribbon
        if (!sender.getAbilities().instabuild) {
            boolean consumed = false;
            for (int i = 0; i < sender.getInventory().getContainerSize(); i++) {
                ItemStack stack = sender.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.is(ModItems.GENDER_RIBBON.get())) {
                    stack.shrink(1);
                    consumed = true;
                    break;
                }
            }
            if (!consumed) {
                sender.displayClientMessage(Component.literal("Requires a Gender Ribbon / Treat item to change gender!").withStyle(ChatFormatting.RED), true);
                return false;
            }
            sender.displayClientMessage(Component.literal("Used Gender Ribbon to assign gender.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        cat.setFemale(female);
        return true;
    }
}
