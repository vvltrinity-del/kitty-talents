package com.kittytalents.network;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talent;
import com.kittytalents.talent.Talents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketTrainTalent {
    private final int entityId;
    private final String talentId;

    public PacketTrainTalent(int entityId, String talentId) {
        this.entityId = entityId;
        this.talentId = talentId;
    }

    public PacketTrainTalent(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.talentId = buf.readUtf(64);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(talentId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                Entity entity = sender.level().getEntity(entityId);
                if (entity instanceof TalentedCat cat && cat.isOwnedBy(sender) && cat.distanceToSqr(sender) <= 64.0D * 64.0D) {
                    Talent talent = Talents.get(talentId);
                    if (talent != null && cat.canTrainTalent(talent)) {
                        if (talent.requiresDireTreat()) {
                            // Check for Dire Treat
                            boolean hasDireTreat = sender.getAbilities().instabuild;
                            int slotWithTreat = -1;
                            if (!hasDireTreat) {
                                for (int i = 0; i < sender.getInventory().getContainerSize(); i++) {
                                    ItemStack s = sender.getInventory().getItem(i);
                                    if (s.is(ModItems.DIRE_TREAT.get())) {
                                        hasDireTreat = true;
                                        slotWithTreat = i;
                                        break;
                                    }
                                }
                            }

                            if (!hasDireTreat) {
                                sender.sendSystemMessage(Component.translatable("message.kittytalents.need_dire_treat"));
                                sender.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
                                return;
                            }

                            // Consume Dire Treat
                            if (!sender.getAbilities().instabuild && slotWithTreat >= 0) {
                                sender.getInventory().removeItem(slotWithTreat, 1);
                            }
                            sender.playSound(SoundEvents.TOTEM_USE, 0.5F, 1.6F);
                        }

                        cat.trainTalent(talent);
                        cat.syncTalents(sender);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
