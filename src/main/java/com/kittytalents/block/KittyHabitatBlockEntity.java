package com.kittytalents.block;

import com.kittytalents.entity.CatVariant;
import com.kittytalents.entity.ProductionTier;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModBlockEntities;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
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

import java.util.UUID;

public class KittyHabitatBlockEntity extends BlockEntity {
    // 0..3 = Upgrades (Speed, Productivity, Efficiency, Storage), 4..7 = Output Slots
    private final ItemStackHandler inventory = new ItemStackHandler(8) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);
    private final LazyOptional<IItemHandler> bottomHandler = LazyOptional.of(() -> new net.minecraftforge.items.wrapper.RangedWrapper(inventory, 4, 8) {
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack; // Deny insertion into output slots from bottom
        }
    });
    private final LazyOptional<IItemHandler> topSideHandler = LazyOptional.of(() -> new net.minecraftforge.items.wrapper.RangedWrapper(inventory, 0, 4) {
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY; // Deny extracting upgrades from top/sides
        }
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!isUpgradeItem(stack)) return stack;
            return super.insertItem(slot, stack, simulate);
        }
    });

    public static boolean isUpgradeItem(ItemStack stack) {
        return stack.is(ModItems.UPGRADE_PRODUCTIVITY_1.get()) ||
               stack.is(ModItems.UPGRADE_PRODUCTIVITY_2.get()) ||
               stack.is(ModItems.UPGRADE_PRODUCTIVITY_3.get()) ||
               stack.is(ModItems.UPGRADE_PRODUCTIVITY_4.get()) ||
               stack.is(ModItems.UPGRADE_PRODUCTIVITY_5.get()) ||
               stack.is(ModItems.UPGRADE_PRODUCTIVITY_6.get());
    }

    private UUID assignedCatUUID = null;
    private String assignedCatName = "";
    private CatVariant assignedCatVariant = CatVariant.TABBY;
    private ProductionTier assignedCatResourceTier = ProductionTier.STANDARD;
    private String assignedCatMutation = "NONE";
    private int assignedCatLevel = 1;
    private boolean assignedCatFemale = false;
    private String assignedCatRole = "";
    private boolean isPaused = false;

    private int progress = 0;
    private int maxProgress = 160;
    private int totalCycles = 0;
    private int totalResources = 0;
    private String statusMessage = "Idle - No Worker Assigned";

    // Performance-optimized simulation caching (R29, R30)
    private int simulationCooldown = 0;
    private TalentedCat cachedWorkerCat = null;
    private int cachedYieldMultiplier = 1;
    private int cachedCycleMultiplier = 1;
    private ItemStack cachedProducedItem = ItemStack.EMPTY;
    private boolean workerValid = false;

    public KittyHabitatBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KITTY_HABITAT.get(), pos, state);
    }

    public ItemStackHandler getInventory() { return inventory; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public int getTotalCycles() { return totalCycles; }
    public int getTotalResources() { return totalResources; }
    public boolean isPaused() { return isPaused; }
    public void setPaused(boolean paused) { this.isPaused = paused; setChanged(); }

    public UUID getAssignedCatUUID() { return assignedCatUUID; }
    public String getAssignedCatName() { return assignedCatName; }
    public CatVariant getAssignedCatVariant() { return assignedCatVariant; }
    public ProductionTier getAssignedCatResourceTier() { return assignedCatResourceTier; }
    public String getAssignedCatMutation() { return assignedCatMutation; }
    public int getAssignedCatLevel() { return assignedCatLevel; }
    public boolean isAssignedCatFemale() { return assignedCatFemale; }
    public String getAssignedCatRole() { return assignedCatRole; }
    public String getStatusMessage() { return statusMessage; }

    public void assignWorker(UUID uuid, String name, CatVariant variant, ProductionTier tier, int level, boolean female, String role, String mutation) {
        this.assignedCatUUID = uuid;
        this.assignedCatName = name;
        this.assignedCatVariant = variant != null ? variant : CatVariant.TABBY;
        this.assignedCatResourceTier = tier != null ? tier : ProductionTier.STANDARD;
        this.assignedCatMutation = mutation != null ? mutation : "NONE";
        this.assignedCatLevel = Math.max(1, level);
        this.assignedCatFemale = female;
        this.assignedCatRole = role;
        this.progress = 0;
        this.statusMessage = "Working (" + this.assignedCatResourceTier.getDisplayName() + ")";
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void assignWorker(UUID uuid, String name, CatVariant variant, ProductionTier tier, int level, boolean female, String role) {
        assignWorker(uuid, name, variant, tier, level, female, role, "NONE");
    }

    public void setWorker(TalentedCat cat) {
        if (cat != null) {
            assignWorker(cat.getUUID(), cat.getName().getString(), cat.getCatVariant(), cat.getProductionTier(), cat.getCatLevel(), cat.isFemale(), cat.getCatVariant().getTraitDescription(), cat.getMutation().name());
        }
    }

    public void assignWorker(UUID uuid, String name, CatVariant variant, int level, boolean female, String role) {
        assignWorker(uuid, name, variant, ProductionTier.STANDARD, level, female, role, "NONE");
    }

    public void removeWorker() {
        this.assignedCatUUID = null;
        this.assignedCatName = "";
        this.assignedCatVariant = CatVariant.TABBY;
        this.assignedCatResourceTier = ProductionTier.STANDARD;
        this.assignedCatMutation = "NONE";
        this.assignedCatLevel = 1;
        this.assignedCatRole = "";
        this.progress = 0;
        this.statusMessage = "Idle - No Worker Assigned";
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, KittyHabitatBlockEntity habitat) {
        if (level.isClientSide) return;

        if (habitat.assignedCatUUID == null) {
            habitat.statusMessage = "Idle - No Worker Assigned";
            habitat.workerValid = false;
            habitat.cachedWorkerCat = null;
            if (habitat.progress > 0) {
                habitat.progress = 0;
                habitat.setChanged();
            }
            return;
        }

        if (habitat.isPaused) {
            habitat.statusMessage = "Paused";
            return;
        }

        // Periodically refresh cached worker cat and simulation metrics every 20 ticks (1s)
        if (--habitat.simulationCooldown <= 0 || !habitat.workerValid) {
            habitat.simulationCooldown = 20;

            TalentedCat activeCat = null;
            if (level instanceof ServerLevel serverLevel) {
                Entity e = serverLevel.getEntity(habitat.assignedCatUUID);
                if (e instanceof TalentedCat cat) {
                    activeCat = cat;
                    if (cat.isIncapacitated()) {
                        habitat.statusMessage = "Worker Incapacitated";
                        habitat.workerValid = false;
                        return;
                    }
                    if (cat.getHunger() <= 20) {
                        habitat.statusMessage = "Worker Starving (<20 Hunger)";
                        habitat.workerValid = false;
                        return;
                    }
                }
            }
            habitat.cachedWorkerCat = activeCat;

            if (activeCat != null) {
                habitat.assignedCatResourceTier = activeCat.getProductionTier();
                habitat.assignedCatMutation = activeCat.getMutation().name();
                habitat.assignedCatVariant = activeCat.getCatVariant();
                habitat.assignedCatLevel = activeCat.getCatLevel();
                habitat.assignedCatFemale = activeCat.isFemale();
            }

            habitat.statusMessage = "Working (" + habitat.assignedCatResourceTier.getDisplayName() + ")";
            habitat.cachedCycleMultiplier = ("NEKOMATA".equalsIgnoreCase(habitat.assignedCatMutation) || (activeCat != null && activeCat.getMutation() == com.kittytalents.entity.CatBreedingManager.MutationType.NEKOMATA)) ? 2 : 1;

            int speedBoost = 0;
            int yieldMultiplier = 1;
            for (int i = 0; i <= 3; i++) {
                ItemStack up = habitat.inventory.getStackInSlot(i);
                if (!up.isEmpty()) {
                    if (up.is(ModItems.UPGRADE_PRODUCTIVITY_1.get())) { speedBoost += 20 * up.getCount(); }
                    else if (up.is(ModItems.UPGRADE_PRODUCTIVITY_2.get())) { speedBoost += 35 * up.getCount(); yieldMultiplier += up.getCount(); }
                    else if (up.is(ModItems.UPGRADE_PRODUCTIVITY_3.get())) { speedBoost += 50 * up.getCount(); yieldMultiplier += 2 * up.getCount(); }
                    else if (up.is(ModItems.UPGRADE_PRODUCTIVITY_4.get())) { speedBoost += 65 * up.getCount(); yieldMultiplier += 3 * up.getCount(); }
                    else if (up.is(ModItems.UPGRADE_PRODUCTIVITY_5.get())) { speedBoost += 80 * up.getCount(); yieldMultiplier += 4 * up.getCount(); }
                    else if (up.is(ModItems.UPGRADE_PRODUCTIVITY_6.get())) { speedBoost += 100 * up.getCount(); yieldMultiplier += 5 * up.getCount(); }
                }
            }

            if (activeCat != null) {
                if (activeCat.getTalentLevel(Talents.MOLE_CAT) > 0 &&
                    (habitat.assignedCatResourceTier == ProductionTier.IRON ||
                     habitat.assignedCatResourceTier == ProductionTier.GOLD ||
                     habitat.assignedCatResourceTier == ProductionTier.DIAMOND ||
                     habitat.assignedCatResourceTier == ProductionTier.COPPER ||
                     habitat.assignedCatResourceTier == ProductionTier.NETHERITE)) {
                    yieldMultiplier += activeCat.getTalentLevel(Talents.MOLE_CAT);
                }
                if (activeCat.getTalentLevel(Talents.FISHER_CAT) > 0 && habitat.assignedCatVariant == CatVariant.FISH) {
                    yieldMultiplier += activeCat.getTalentLevel(Talents.FISHER_CAT);
                }
            }

            habitat.cachedYieldMultiplier = yieldMultiplier;
            habitat.maxProgress = Math.max(20, habitat.assignedCatResourceTier.getBaseIntervalTicks() - speedBoost);

            ItemStack baseResource = ItemStack.EMPTY;
            if (habitat.assignedCatResourceTier != null && habitat.assignedCatResourceTier != ProductionTier.STANDARD) {
                baseResource = habitat.assignedCatResourceTier.getProductionResult();
            } else if (habitat.assignedCatVariant != null && habitat.assignedCatVariant.isProductive()) {
                baseResource = habitat.assignedCatVariant.getProductionResult();
            } else if (habitat.assignedCatResourceTier != null) {
                baseResource = habitat.assignedCatResourceTier.getProductionResult();
            }
            if (baseResource.isEmpty()) {
                baseResource = new ItemStack(Items.STRING);
            }

            ItemStack produced = baseResource.copy();
            produced.setCount(Math.min(produced.getMaxStackSize(), baseResource.getCount() * habitat.cachedYieldMultiplier * habitat.cachedCycleMultiplier));
            habitat.cachedProducedItem = produced;
            habitat.workerValid = true;
        }

        if (!habitat.workerValid) return;

        // Check output slot space
        if (habitat.cachedProducedItem.isEmpty() || !habitat.canInsertOutput(habitat.cachedProducedItem)) {
            habitat.statusMessage = "Output Full";
            return;
        }

        habitat.progress++;
        if (habitat.progress >= habitat.maxProgress) {
            habitat.progress = 0;
            habitat.insertOutput(habitat.cachedProducedItem.copy());
            habitat.totalCycles += habitat.cachedCycleMultiplier;
            habitat.totalResources += habitat.cachedProducedItem.getCount();

            if (habitat.cachedWorkerCat != null && habitat.totalCycles % 5 == 0) {
                int hungerLoss = habitat.cachedWorkerCat.getTalentLevel(Talents.HAPPY_EATER) >= 3 ? 0 : 1;
                if (hungerLoss > 0) habitat.cachedWorkerCat.setHunger(Math.max(0, habitat.cachedWorkerCat.getHunger() - hungerLoss));
            }

            habitat.setChanged();
        }
    }

    private boolean canInsertOutput(ItemStack stack) {
        int needed = stack.getCount();
        for (int i = 4; i <= 7; i++) {
            ItemStack inSlot = inventory.getStackInSlot(i);
            if (inSlot.isEmpty()) {
                needed -= stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameTags(inSlot, stack)) {
                needed -= (inSlot.getMaxStackSize() - inSlot.getCount());
            }
            if (needed <= 0) return true;
        }
        return false;
    }

    private void insertOutput(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 4; i <= 7; i++) {
            ItemStack inSlot = inventory.getStackInSlot(i);
            if (inSlot.isEmpty()) {
                inventory.setStackInSlot(i, remaining);
                return;
            } else if (ItemStack.isSameItemSameTags(inSlot, remaining)) {
                int toAdd = Math.min(remaining.getCount(), inSlot.getMaxStackSize() - inSlot.getCount());
                inSlot.grow(toAdd);
                remaining.shrink(toAdd);
                if (remaining.isEmpty()) return;
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
            if (side == Direction.DOWN) {
                return bottomHandler.cast();
            } else if (side != null) {
                return topSideHandler.cast();
            }
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
        bottomHandler.invalidate();
        topSideHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Progress", progress);
        tag.putInt("TotalCycles", totalCycles);
        tag.putInt("TotalResources", totalResources);
        tag.putBoolean("IsPaused", isPaused);
        if (assignedCatUUID != null) {
            tag.putUUID("AssignedCatUUID", assignedCatUUID);
            tag.putString("AssignedCatName", assignedCatName);
            tag.putString("AssignedCatVariant", assignedCatVariant.getId());
            tag.putString("AssignedCatTier", assignedCatResourceTier.getId());
            tag.putInt("AssignedCatLevel", assignedCatLevel);
            tag.putBoolean("AssignedCatFemale", assignedCatFemale);
            tag.putString("AssignedCatRole", assignedCatRole);
            tag.putString("AssignedCatMutation", assignedCatMutation);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) inventory.deserializeNBT(tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
        totalCycles = tag.getInt("TotalCycles");
        totalResources = tag.getInt("TotalResources");
        isPaused = tag.getBoolean("IsPaused");
        assignedCatMutation = tag.contains("AssignedCatMutation") ? tag.getString("AssignedCatMutation") : "NONE";
        if (tag.hasUUID("AssignedCatUUID")) {
            assignedCatUUID = tag.getUUID("AssignedCatUUID");
            assignedCatName = tag.getString("AssignedCatName");
            assignedCatVariant = CatVariant.fromId(tag.getString("AssignedCatVariant"));
            assignedCatResourceTier = tag.contains("AssignedCatTier") ?
                    ProductionTier.fromId(tag.getString("AssignedCatTier")) : ProductionTier.STANDARD;
            assignedCatLevel = tag.getInt("AssignedCatLevel");
            assignedCatFemale = tag.getBoolean("AssignedCatFemale");
            assignedCatRole = tag.getString("AssignedCatRole");
        } else if (tag.contains("AssignedCatUUID")) {
            try {
                assignedCatUUID = UUID.fromString(tag.getString("AssignedCatUUID"));
                assignedCatName = tag.getString("AssignedCatName");
                assignedCatVariant = CatVariant.fromId(tag.getString("AssignedCatVariant"));
                assignedCatResourceTier = tag.contains("AssignedCatTier") ?
                        ProductionTier.fromId(tag.getString("AssignedCatTier")) : ProductionTier.STANDARD;
                assignedCatLevel = tag.getInt("AssignedCatLevel");
                assignedCatFemale = tag.getBoolean("AssignedCatFemale");
                assignedCatRole = tag.getString("AssignedCatRole");
            } catch (Exception ex) {
                assignedCatUUID = null;
            }
        } else {
            assignedCatUUID = null;
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
