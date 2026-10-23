package com.kittytalents.network;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketRespecTalents {
    private final int entityId;

    public PacketRespecTalents(int entityId) {
        this.entityId = entityId;
    }

    public PacketRespecTalents(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                Entity entity = sender.level().getEntity(entityId);
                if (entity instanceof TalentedCat cat && cat.isPrimaryOwner(sender) && cat.distanceToSqr(sender) <= 64.0D * 64.0D) {
                    boolean hasElixir = sender.getAbilities().instabuild;
                    int elixirSlot = -1;

                    if (!hasElixir) {
                        for (int i = 0; i < sender.getInventory().getContainerSize(); i++) {
                            ItemStack stack = sender.getInventory().getItem(i);
                            if (stack.is(ModItems.CATNIP_RESPEC_ELIXIR.get())) {
                                hasElixir = true;
                                elixirSlot = i;
                                break;
                            }
                        }
                    }

                    if (!hasElixir) {
                        sender.sendSystemMessage(Component.translatable("message.kittytalents.need_respec_elixir"));
                        sender.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
                        return;
                    }

                    if (!sender.getAbilities().instabuild && elixirSlot >= 0) {
                        sender.getInventory().removeItem(elixirSlot, 1);
                    }

                    int refunded = cat.respecTalents();
                    cat.syncTalents(sender);
                    sender.sendSystemMessage(Component.translatable("message.kittytalents.respec_success", cat.getName(), refunded));
                    sender.level().playSound(null, cat.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);

                    if (sender.level() instanceof ServerLevel serverLevel) {
                        for (int i = 0; i < 25; i++) {
                            serverLevel.sendParticles(ParticleTypes.ENCHANT, cat.getX(), cat.getY() + 0.5D, cat.getZ(), 1,
                                    (serverLevel.random.nextDouble() - 0.5D) * 0.8D, 0.5D, (serverLevel.random.nextDouble() - 0.5D) * 0.8D, 0.1D);
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
