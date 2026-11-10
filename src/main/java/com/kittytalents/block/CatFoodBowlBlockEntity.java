package com.kittytalents.block;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.inventory.CatFoodBowlMenu;
import com.kittytalents.registry.ModBlockEntities;
import com.kittytalents.util.CatFoodHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class CatFoodBowlBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_COUNT = 5;

    public static class BowlItemStackHandler extends ItemStackHandler {
        private final CatFoodBowlBlockEntity bowl;

        public BowlItemStackHandler(int size, CatFoodBowlBlockEntity bowl) {
            super(size);
            this.bowl = bowl;
        }

        @Override
        protected void onContentsChanged(int slot) {
            bowl.setChanged();
            bowl.updateFillState();
            if (bowl.level != null && !bowl.level.isClientSide) {
                bowl.level.sendBlockUpdated(bowl.worldPosition, bowl.getBlockState(), bowl.getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return CatFoodHelper.isEdibleByCat(stack, null);
        }
    }

    public static class BowlContainerData implements ContainerData {
        private final CatFoodBowlBlockEntity bowl;

        public BowlContainerData(CatFoodBowlBlockEntity bowl) {
            this.bowl = bowl;
        }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> bowl.nearbyCatsCount;
                case 1 -> bowl.getTotalFoodCount();
                case 2 -> bowl.feedCooldown;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> bowl.nearbyCatsCount = value;
                case 2 -> bowl.feedCooldown = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

    private final BowlItemStackHandler inventory = new BowlItemStackHandler(SLOT_COUNT, this);
    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> inventory);
    private int feedCooldown = 0;
    private int nearbyCatsCount = 0;
    private UUID ownerUUID = null;
    private String ownerName = "Public Bowl";
    protected final BowlContainerData dataAccess = new BowlContainerData(this);

    public CatFoodBowlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAT_FOOD_BOWL.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public void setOwner(Player player) {
        if (player != null) {
            this.ownerUUID = player.getUUID();
            this.ownerName = player.getName().getString();
            setChanged();
        }
    }

    public String getOwnerName() {
        return ownerName;
    }

    /** Shows the food level on the 3D model (blockstate FOOD 0..3). Server side; harmless if nothing changed. */
    public void updateFillState() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(CatFoodBowlBlock.FOOD)) return;
        int total = getTotalFoodCount();
        int fill = total <= 0 ? 0 : total <= 8 ? 1 : total <= 32 ? 2 : 3;
        if (state.getValue(CatFoodBowlBlock.FOOD) != fill) {
            level.setBlock(worldPosition, state.setValue(CatFoodBowlBlock.FOOD, fill), 3);
        }
    }

    public int getTotalFoodCount() {
        int count = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            count += inventory.getStackInSlot(i).getCount();
        }
        return count;
    }

    public boolean hasFood() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public ItemStack getFirstEdibleFood(TalentedCat cat) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && CatFoodHelper.isEdibleByCat(stack, cat)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack consumeOneFood(TalentedCat cat) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && CatFoodHelper.isEdibleByCat(stack, cat)) {
                ItemStack taken = stack.split(1);
                inventory.setStackInSlot(i, stack);
                setChanged();
                if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CatFoodBowlBlockEntity bowl) {
        // Bowls placed before 4.0 (no FOOD state yet) catch up within two seconds.
        if (!level.isClientSide && (level.getGameTime() + pos.asLong()) % 40 == 0) bowl.updateFillState();
        if (level.isClientSide) return;

        if (bowl.feedCooldown > 0) {
            bowl.feedCooldown--;
        }

        // Periodically scan nearby cats (every 20 ticks)
        if (level.getGameTime() % 20 == 0) {
            List<TalentedCat> nearby = level.getEntitiesOfClass(
                    TalentedCat.class,
                    new AABB(pos).inflate(16.0D),
                    c -> !c.isIncapacitated()
            );
            bowl.nearbyCatsCount = nearby.size();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.kittytalents.cat_food_bowl");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CatFoodBowlMenu(containerId, playerInventory, this, this.dataAccess);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Cooldown", feedCooldown);
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
            tag.putString("OwnerName", ownerName);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        feedCooldown = tag.getInt("Cooldown");
        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
            ownerName = tag.getString("OwnerName");
        }
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
