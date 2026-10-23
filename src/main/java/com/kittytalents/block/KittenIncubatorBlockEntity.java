package com.kittytalents.block;

import com.kittytalents.registry.ModBlockEntities;
import com.kittytalents.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KittenIncubatorBlockEntity extends BlockEntity {
    // 0 = Parent A / Input, 1 = Parent B / Catalyst, 2 = Output Kitten/Essence
    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);
    private int progress = 0;
    private static final int MAX_PROGRESS = 200;

    public KittenIncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KITTEN_INCUBATOR.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public int getProgress() {
        return progress;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, KittenIncubatorBlockEntity incubator) {
        if (level.isClientSide) return;

        ItemStack slot0 = incubator.inventory.getStackInSlot(0);
        ItemStack slot1 = incubator.inventory.getStackInSlot(1);
        ItemStack slot2 = incubator.inventory.getStackInSlot(2);

        if (!slot0.isEmpty() && !slot1.isEmpty() && slot2.isEmpty()) {
            incubator.progress++;
            if (incubator.progress >= MAX_PROGRESS) {
                incubator.progress = 0;
                slot0.shrink(1);
                slot1.shrink(1);

                ItemStack result = new ItemStack(ModItems.TALENTED_CAT_SPAWN_EGG.get());
                incubator.inventory.setStackInSlot(2, result);

                level.playSound(null, pos, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 1.0F, 1.2F);
                incubator.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            }
        } else {
            if (incubator.progress > 0) {
                incubator.progress = 0;
                incubator.setChanged();
            }
        }
    }

    public void dropContents() {
        if (level != null) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                }
            }
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Progress", progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) inventory.deserializeNBT(tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
