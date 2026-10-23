package com.kittytalents.network;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketChangeCatGender {
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

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(female);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                Entity entity = sender.level().getEntity(entityId);
                if (entity instanceof TalentedCat cat && (cat.isOwnedBy(sender) || cat.isTrusted(sender)) && cat.distanceToSqr(sender) <= 64.0D * 64.0D) {
                    if (sender.getAbilities().instabuild) {
                        cat.setFemale(female);
                    } else {
                        boolean consumed = false;
                        for (int i = 0; i < sender.getInventory().getContainerSize(); i++) {
                            net.minecraft.world.item.ItemStack stack = sender.getInventory().getItem(i);
                            if (!stack.isEmpty() && stack.is(com.kittytalents.registry.ModItems.GENDER_RIBBON.get())) {
                                stack.shrink(1);
                                consumed = true;
                                break;
                            }
                        }
                        if (consumed) {
                            cat.setFemale(female);
                            sender.displayClientMessage(net.minecraft.network.chat.Component.literal("Used Gender Ribbon to assign gender.").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
                        } else {
                            sender.displayClientMessage(net.minecraft.network.chat.Component.literal("Requires a Gender Ribbon / Treat item to change gender!").withStyle(net.minecraft.ChatFormatting.RED), true);
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
