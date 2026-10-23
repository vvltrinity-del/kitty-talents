package com.kittytalents.block;

import com.kittytalents.config.KittyTalentsConfig;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModBlockEntities;
import com.kittytalents.util.CatFoodHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Physical automation hub for free-roaming worker cats.
 * Slots 0..17: Output Buffer (18 slots)
 * Slots 18..21: Supply Input Slots (4 slots: seeds, saplings, torches, tools, food, ammo)
 * Slots 22..23: Upgrade Slots (2 slots)
 */
public class KittyWorkStationBlockEntity extends BlockEntity {
    public static final int OUTPUT_SLOTS = 18;
    public static final int SUPPLY_START = 18;
    public static final int SUPPLY_END = 22; // exclusive
    public static final int UPGRADE_START = 22;
    public static final int TOTAL_SLOTS = 24;

    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot >= UPGRADE_START) {
                return KittyHabitatBlockEntity.isUpgradeItem(stack);
            }
            return true;
        }
    };

    private final LazyOptional<IItemHandler> fullHandler = LazyOptional.of(() -> inventory);
    private final LazyOptional<IItemHandler> bottomOutputOnlyHandler = LazyOptional.of(() -> new RangedWrapper(inventory, 0, OUTPUT_SLOTS) {
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }
    });
    private final LazyOptional<IItemHandler> sideSupplyAndOutputHandler = LazyOptional.of(() -> new IItemHandler() {
        @Override
        public int getSlots() {
            return UPGRADE_START; // 0..21
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            // External pipes on top/sides insert into supply slots 18..21
            if (slot < SUPPLY_START || slot >= SUPPLY_END) return stack;
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // External pipes extract from output buffer 0..17
            if (slot >= OUTPUT_SLOTS) return ItemStack.EMPTY;
            return inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot >= SUPPLY_START && slot < SUPPLY_END;
        }
    });

    private final List<UUID> boundWorkers = new ArrayList<>();
    private final List<String> boundWorkerNames = new ArrayList<>();

    public KittyWorkStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KITTY_WORK_STATION.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public List<UUID> getBoundWorkers() {
        return boundWorkers;
    }

    public List<String> getBoundWorkerNames() {
        return boundWorkerNames;
    }

    public boolean bindWorker(TalentedCat cat) {
        if (cat == null) return false;
        int maxWorkers = KittyTalentsConfig.COMMON.maxWorkersPerStation.get();
        UUID id = cat.getUUID();
        if (!boundWorkers.contains(id)) {
            if (boundWorkers.size() >= maxWorkers) {
                boundWorkers.remove(0);
                if (!boundWorkerNames.isEmpty()) boundWorkerNames.remove(0);
            }
            boundWorkers.add(id);
            boundWorkerNames.add(cat.getName().getString());
        }
        cat.getJobData().setStationPos(this.worldPosition);
        cat.setCatMode(CatMode.WORK);
        cat.setClockedIn(true);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    public void unbindAllWorkers() {
        if (level instanceof ServerLevel sl) {
            for (UUID id : boundWorkers) {
                Entity e = sl.getEntity(id);
                if (e instanceof TalentedCat cat && worldPosition.equals(cat.getJobData().getStationPos())) {
                    cat.getJobData().setStationPos(null);
                }
            }
        }
        boundWorkers.clear();
        boundWorkerNames.clear();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, KittyWorkStationBlockEntity station) {
        if (level.isClientSide || !(level instanceof ServerLevel sl)) return;
        if (level.getGameTime() % 40 != 0) return;

        boolean redstonePowered = level.hasNeighborSignal(pos);
        station.boundWorkerNames.clear();

        for (UUID id : new ArrayList<>(station.boundWorkers)) {
            Entity e = sl.getEntity(id);
            if (!(e instanceof TalentedCat cat) || !cat.isAlive()) continue;

            station.boundWorkerNames.add(cat.getName().getString() + " [" + cat.getCatJob().getDisplayName() + "]");

            // Redstone signal pauses/resumes bound workers
            cat.setClockedIn(!redstonePowered);

            // Auto-feed hungry bound workers from supply slots (18..21) or adjacent feeding station
            if (cat.getHunger() < 75) {
                station.tryFeedWorker(cat);
            }

            // Auto-equip replacement tool if worker has no tool
            if (cat.getEquippedTool().isEmpty()) {
                station.tryEquipToolFromSupplies(cat);
            }
        }
    }

    private void tryFeedWorker(TalentedCat cat) {
        for (int i = SUPPLY_START; i < SUPPLY_END; i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (!s.isEmpty() && CatFoodHelper.isEdibleByCat(s, cat)) {
                ItemStack one = inventory.extractItem(i, 1, false);
                if (!one.isEmpty()) {
                    cat.increaseHunger(25);
                    cat.heal(4.0F);
                    return;
                }
            }
        }
        // Check adjacent Feeding Station or Food Bowl
        if (level == null) return;
        for (Direction dir : Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(dir));
            if (be instanceof KittyFeedingStationBlockEntity feeder) {
                for (int i = 0; i < feeder.getInventory().getSlots(); i++) {
                    ItemStack s = feeder.getInventory().getStackInSlot(i);
                    if (!s.isEmpty() && CatFoodHelper.isEdibleByCat(s, cat)) {
                        feeder.getInventory().extractItem(i, 1, false);
                        cat.increaseHunger(25);
                        cat.heal(4.0F);
                        return;
                    }
                }
            } else if (be instanceof CatFoodBowlBlockEntity bowl) {
                ItemStack consumed = bowl.consumeOneFood(cat);
                if (!consumed.isEmpty()) {
                    cat.increaseHunger(25);
                    cat.heal(4.0F);
                    return;
                }
            }
        }
    }

    private void tryEquipToolFromSupplies(TalentedCat cat) {
        for (int i = SUPPLY_START; i < SUPPLY_END; i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (!s.isEmpty() && (s.getItem() instanceof DiggerItem ||
                    s.getItem() instanceof ShearsItem ||
                    s.getItem() instanceof FishingRodItem)) {
                ItemStack tool = inventory.extractItem(i, 1, false);
                if (!tool.isEmpty()) {
                    cat.setEquippedTool(tool);
                    return;
                }
            }
        }
    }

    public int getComparatorOutput() {
        int filledSlots = 0;
        float totalRatio = 0.0F;
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (!s.isEmpty()) {
                filledSlots++;
                totalRatio += (float) s.getCount() / (float) Math.max(1, s.getMaxStackSize());
            }
        }
        if (filledSlots == 0) return 0;
        return Math.min(15, 1 + (int) Math.floor((totalRatio / OUTPUT_SLOTS) * 14.0F));
    }

    public void dropContents() {
        if (level != null) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack s = inventory.getStackInSlot(i);
                if (!s.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), s);
                }
            }
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == Direction.DOWN) return bottomOutputOnlyHandler.cast();
            if (side != null) return sideSupplyAndOutputHandler.cast();
            return fullHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fullHandler.invalidate();
        bottomOutputOnlyHandler.invalidate();
        sideSupplyAndOutputHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        ListTag workersTag = new ListTag();
        for (UUID id : boundWorkers) {
            CompoundTag w = new CompoundTag();
            w.putUUID("Id", id);
            workersTag.add(w);
        }
        tag.put("BoundWorkers", workersTag);

        ListTag namesTag = new ListTag();
        for (String n : boundWorkerNames) {
            CompoundTag nt = new CompoundTag();
            nt.putString("Name", n);
            namesTag.add(nt);
        }
        tag.put("BoundWorkerNames", namesTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        boundWorkers.clear();
        if (tag.contains("BoundWorkers", Tag.TAG_LIST)) {
            ListTag list = tag.getList("BoundWorkers", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag w = list.getCompound(i);
                if (w.hasUUID("Id")) boundWorkers.add(w.getUUID("Id"));
            }
        }
        boundWorkerNames.clear();
        if (tag.contains("BoundWorkerNames", Tag.TAG_LIST)) {
            ListTag list = tag.getList("BoundWorkerNames", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                boundWorkerNames.add(list.getCompound(i).getString("Name"));
            }
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
