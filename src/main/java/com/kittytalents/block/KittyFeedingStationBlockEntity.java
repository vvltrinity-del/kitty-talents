package com.kittytalents.block;

import com.kittytalents.registry.ModBlockEntities;
import com.kittytalents.util.CatFoodHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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

public class KittyFeedingStationBlockEntity extends BlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return CatFoodHelper.isEdibleByCat(stack, null);
        }
    };

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    public KittyFeedingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FEEDING_STATION.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, KittyFeedingStationBlockEntity station) {
        if (level.isClientSide || level.getGameTime() % 40 != 0) return;
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(pos).inflate(10.0D);
        java.util.List<com.kittytalents.entity.TalentedCat> cats = level.getEntitiesOfClass(
                com.kittytalents.entity.TalentedCat.class, box, c -> c.isAlive() && c.getHunger() < 80);
        for (com.kittytalents.entity.TalentedCat cat : cats) {
            for (int i = 0; i < station.inventory.getSlots(); i++) {
                ItemStack s = station.inventory.getStackInSlot(i);
                if (!s.isEmpty() && CatFoodHelper.isEdibleByCat(s, cat)) {
                    station.inventory.extractItem(i, 1, false);
                    cat.increaseHunger(25);
                    cat.heal(4.0F);
                    break;
                }
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
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) inventory.deserializeNBT(tag.getCompound("Inventory"));
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
