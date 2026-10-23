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
import net.minecraft.world.item.Items;
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

public class CatRiceMillBlockEntity extends BlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);
    private int waterLevel = 2;
    private int progress = 0;
    private static final int MAX_PROGRESS = 80;

    public CatRiceMillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAT_RICE_MILL.get(), pos, state);
    }

    public int getWaterLevel() {
        return waterLevel;
    }

    public void setWaterLevel(int level) {
        this.waterLevel = Math.max(0, Math.min(4, level));
        setChanged();
        if (this.level != null) this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CatRiceMillBlockEntity mill) {
        if (level.isClientSide) return;

        ItemStack input = mill.inventory.getStackInSlot(0);
        ItemStack output = mill.inventory.getStackInSlot(1);

        if (mill.waterLevel > 0 && !input.isEmpty() && isMillable(input)) {
            ItemStack result = getMillResult(input);
            if (output.isEmpty() || (ItemStack.isSameItemSameTags(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize())) {
                mill.progress++;
                if (mill.progress >= MAX_PROGRESS) {
                    mill.progress = 0;
                    input.shrink(1);
                    if (output.isEmpty()) {
                        mill.inventory.setStackInSlot(1, result.copy());
                    } else {
                        output.grow(result.getCount());
                    }

                    // Water usage every 4 items
                    if (level.random.nextInt(4) == 0) {
                        mill.waterLevel--;
                    }

                    level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
                    mill.setChanged();
                    level.sendBlockUpdated(pos, state, state, 3);
                }
                return;
            }
        }

        if (mill.progress > 0) {
            mill.progress = 0;
            mill.setChanged();
        }
    }

    public static boolean isMillable(ItemStack stack) {
        return stack.is(Items.WHEAT) || stack.is(Items.WHEAT_SEEDS) || stack.is(ModItems.RICE_GRAIN.get()) || stack.is(Items.CARROT) || stack.is(Items.BEETROOT);
    }

    public static ItemStack getMillResult(ItemStack input) {
        if (input.is(Items.WHEAT) || input.is(Items.WHEAT_SEEDS)) {
            return new ItemStack(ModItems.RICE_GRAIN.get(), 2);
        } else if (input.is(ModItems.RICE_GRAIN.get())) {
            return new ItemStack(ModItems.RICE_BOWL.get(), 1);
        } else {
            return new ItemStack(ModItems.CATNIP_COOKIE.get(), 1);
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
        tag.putInt("WaterLevel", waterLevel);
        tag.putInt("Progress", progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) inventory.deserializeNBT(tag.getCompound("Inventory"));
        waterLevel = tag.getInt("WaterLevel");
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
