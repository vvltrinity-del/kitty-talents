package com.kittytalents.network;

import com.kittytalents.core.perm.CatAction;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** Client -> server: refund all of a cat's talents for one Catnip Respec Elixir (owner only). */
public class PacketRespecTalents extends PacketBase {
    private final int entityId;

    public PacketRespecTalents(int entityId) {
        this.entityId = entityId;
    }

    public PacketRespecTalents(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender, entityId);
    }

    public static boolean apply(ServerPlayer sender, int entityId) {
        TalentedCat cat = authorize(sender, entityId, CatAction.RESPEC);
        if (cat == null) return false;

        boolean creative = sender.getAbilities().instabuild;
        int elixirSlot = -1;
        if (!creative) {
            for (int i = 0; i < sender.getInventory().getContainerSize(); i++) {
                ItemStack stack = sender.getInventory().getItem(i);
                if (stack.is(ModItems.CATNIP_RESPEC_ELIXIR.get())) {
                    elixirSlot = i;
                    break;
                }
            }
            if (elixirSlot < 0) {
                sender.sendSystemMessage(Component.translatable("message.kittytalents.need_respec_elixir"));
                sender.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
                return false;
            }
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
        return true;
    }
}
