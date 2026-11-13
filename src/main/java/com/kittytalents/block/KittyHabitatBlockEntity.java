package com.kittytalents.block;

import com.kittytalents.entity.CatVariant;
import com.kittytalents.entity.ProductionTier;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.recipe.HabitatProductionRecipe;
import com.kittytalents.registry.ModBlockEntities;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModItems;
import com.kittytalents.registry.ModRecipes;
import com.kittytalents.talent.Talents;
import com.kittytalents.util.CatFoodHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Habitat 2.0: Houses a worker cat inside as NBT (or links by UUID), supports optional FE boost,
 * food & catalyst inputs, data-driven HabitatProductionRecipe lookup, redstone pause, and comparator output.
 * Slots:
 * 0..3 = Upgrades
 * 4..7 = Outputs
 * 8 = Food Input
 * 9 = Catalyst Input
 */
public class KittyHabitatBlockEntity extends BlockEntity {
    public static final int SLOT_FOOD_IN = 8;
    public static final int SLOT_CATALYST_IN = 9;
    public static final int TOTAL_SLOTS = 10;

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
            if (slot >= 0 && slot <= 3) return isUpgradeItem(stack);
            if (slot >= 4 && slot <= 7) return false;
            if (slot == SLOT_FOOD_IN) return CatFoodHelper.isEdibleByCat(stack, null);
            return true;
        }
    };

    private final EnergyStorage energyStorage = new EnergyStorage(20000, 400, 400);
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energyStorage);
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);
    private final LazyOptional<IItemHandler> bottomHandler = LazyOptional.of(() -> new RangedWrapper(inventory, 4, 8) {
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }
    });
    private final LazyOptional<IItemHandler> topSideHandler = LazyOptional.of(() -> new IItemHandler() {
        @Override
        public int getSlots() {
            return TOTAL_SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot >= 0 && slot <= 3 && isUpgradeItem(stack)) {
                return inventory.insertItem(slot, stack, simulate);
            }
            if (slot == SLOT_FOOD_IN && CatFoodHelper.isEdibleByCat(stack, null)) {
                return inventory.insertItem(slot, stack, simulate);
            }
            if (slot == SLOT_CATALYST_IN) {
                return inventory.insertItem(slot, stack, simulate);
            }
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot >= 4 && slot <= 7) {
                return inventory.extractItem(slot, amount, simulate);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return inventory.isItemValid(slot, stack);
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
    private CompoundTag storedCat = new CompoundTag();
    private int housedHunger = 100;

    private int progress = 0;
    private int maxProgress = 160;
    private int totalCycles = 0;
    private int totalResources = 0;
    /** What the habitat is doing. Synced to open screens through {@link #dataAccess} (the text used to be server-only). */
    public enum Status { IDLE, WORKING, PAUSED, PAUSED_REDSTONE, WORKER_DOWN, WORKER_STARVING, OUTPUT_FULL }
    private Status status = Status.IDLE;

    /** Live values for an open screen: progress, max progress, total cycles, paused flag, status ordinal. */
    public final net.minecraft.world.inventory.ContainerData dataAccess = new net.minecraft.world.inventory.ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> totalCycles;
                case 3 -> isPaused ? 1 : 0;
                case 4 -> status.ordinal();
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 5; }
    };

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
    public EnergyStorage getEnergyStorage() { return energyStorage; }
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
    public Status getStatus() { return status; }
    /** English status text (kept for Jade and older callers). */
    public String getStatusMessage() { return statusText(status, assignedCatResourceTier); }
    public static String statusText(Status st, ProductionTier tier) {
        return switch (st) {
            case IDLE -> "Idle - No Worker Assigned";
            case WORKING -> "Working (" + tier.getDisplayName() + ")";
            case PAUSED -> "Paused";
            case PAUSED_REDSTONE -> "Paused (Redstone)";
            case WORKER_DOWN -> "Worker Incapacitated";
            case WORKER_STARVING -> "Worker Starving (<20 Hunger)";
            case OUTPUT_FULL -> "Output Full";
        };
    }

    /** Screen "Dismiss Worker": a housed cat is released beside the habitat; a linked worker is just unassigned. */
    public void dismissWorker() {
        if (hasStoredCat()) releaseHousedCat(worldPosition.above());
        else removeWorker();
    }
    public boolean hasStoredCat() { return storedCat != null && !storedCat.isEmpty(); }
    public CompoundTag getStoredCatTag() { return storedCat; }

    /**
     * Houses a physical TalentedCat inside the Habitat as NBT (like Productive Bees),
     * releasing any previously housed cat first.
     */
    public boolean houseWorker(TalentedCat cat) {
        if (cat == null || level == null || level.isClientSide) return false;
        if (hasStoredCat()) {
            releaseHousedCat(worldPosition.above());
        }
        if (cat.isTwined()) {
            com.kittytalents.entity.TwineManager.endForCat(cat, "Cat entered Production Habitat.");
        }
        cat.stopRiding();
        cat.ejectPassengers();

        CompoundTag catNbt = new CompoundTag();
        cat.saveWithoutId(catNbt);
        this.storedCat = catNbt;
        this.housedHunger = cat.getHunger();

        assignWorker(
                cat.getUUID(),
                cat.getName().getString(),
                cat.getCatVariant(),
                cat.getProductionTier(),
                cat.getCatLevel(),
                cat.isFemale(),
                cat.getCatVariant().getTraitDescription(),
                cat.getMutation().name()
        );
        cat.discard();
        return true;
    }

    /**
     * Releases the housed cat back into the world as a live TalentedCat entity.
     */
    @Nullable
    public TalentedCat releaseHousedCat(@Nullable BlockPos spawnPos) {
        if (!hasStoredCat() || !(level instanceof ServerLevel sl)) {
            removeWorker();
            return null;
        }
        TalentedCat cat = ModEntities.TALENTED_CAT.get().create(sl);
        if (cat != null) {
            cat.load(storedCat);
            cat.setHunger(Math.max(20, housedHunger));
            BlockPos target = spawnPos != null ? spawnPos : worldPosition.above();
            cat.moveTo(target.getX() + 0.5D, target.getY() + 0.1D, target.getZ() + 0.5D, 0.0F, 0.0F);
            cat.setOrderedToSit(false);
            sl.addFreshEntity(cat);
            sl.playSound(null, target, SoundEvents.CAT_PURREOW, SoundSource.NEUTRAL, 1.0F, 1.1F);
        }
        this.storedCat = new CompoundTag();
        removeWorker();
        return cat;
    }

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
        this.status = Status.WORKING;
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
            if (level != null && !level.isClientSide) {
                houseWorker(cat);
            } else {
                assignWorker(cat.getUUID(), cat.getName().getString(), cat.getCatVariant(), cat.getProductionTier(), cat.getCatLevel(), cat.isFemale(), cat.getCatVariant().getTraitDescription(), cat.getMutation().name());
            }
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
        this.status = Status.IDLE;
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, KittyHabitatBlockEntity habitat) {
        if (level.isClientSide) return;
        Status before = habitat.status;
        tickInner(level, pos, habitat);
        if (habitat.status != before) { // keep the client copy (Jade, screens) in step with the status
            habitat.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private static void tickInner(Level level, BlockPos pos, KittyHabitatBlockEntity habitat) {

        if (habitat.assignedCatUUID == null && !habitat.hasStoredCat()) {
            habitat.status = Status.IDLE;
            habitat.workerValid = false;
            habitat.cachedWorkerCat = null;
            if (habitat.progress > 0) {
                habitat.progress = 0;
                habitat.setChanged();
            }
            return;
        }

        if (habitat.isPaused || level.hasNeighborSignal(pos)) {
            habitat.status = level.hasNeighborSignal(pos) ? Status.PAUSED_REDSTONE : Status.PAUSED;
            return;
        }

        // Auto-feed housed cat from slot 8 if hungry
        if (habitat.housedHunger < 50) {
            ItemStack foodIn = habitat.inventory.getStackInSlot(SLOT_FOOD_IN);
            if (!foodIn.isEmpty() && CatFoodHelper.isEdibleByCat(foodIn, null)) {
                habitat.inventory.extractItem(SLOT_FOOD_IN, 1, false);
                habitat.housedHunger = Math.min(120, habitat.housedHunger + 30);
            }
        }

        if (--habitat.simulationCooldown <= 0 || !habitat.workerValid) {
            habitat.simulationCooldown = 20;

            TalentedCat activeCat = null;
            if (!habitat.hasStoredCat() && level instanceof ServerLevel serverLevel && habitat.assignedCatUUID != null) {
                Entity e = serverLevel.getEntity(habitat.assignedCatUUID);
                if (e instanceof TalentedCat cat) {
                    activeCat = cat;
                    if (cat.isIncapacitated()) {
                        habitat.status = Status.WORKER_DOWN;
                        habitat.workerValid = false;
                        return;
                    }
                    if (cat.getHunger() <= 20) {
                        habitat.status = Status.WORKER_STARVING;
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

            habitat.status = Status.WORKING;
            habitat.cachedCycleMultiplier = ("NEKOMATA".equalsIgnoreCase(habitat.assignedCatMutation) ||
                    (activeCat != null && activeCat.getMutation() == com.kittytalents.entity.CatBreedingManager.MutationType.NEKOMATA)) ? 2 : 1;

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

            // Catalyst slot 9 bonus
            ItemStack catalyst = habitat.inventory.getStackInSlot(SLOT_CATALYST_IN);
            if (!catalyst.isEmpty() && (catalyst.is(ModItems.RESOURCE_CATALYST.get()) || catalyst.is(ModItems.CATNIP_CATALYST.get()))) {
                yieldMultiplier += 1;
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

            // Check data-driven HabitatProductionRecipe first, then fallback to ProductionTier
            ItemStack baseResource = ItemStack.EMPTY;
            int baseDuration = habitat.assignedCatResourceTier.getBaseIntervalTicks();
            List<HabitatProductionRecipe> recipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.HABITAT_PRODUCTION_TYPE.get());
            for (HabitatProductionRecipe recipe : recipes) {
                if (recipe.matchesWorker(habitat.assignedCatResourceTier, habitat.assignedCatVariant, catalyst)) {
                    if (!recipe.getOutputs().isEmpty()) {
                        baseResource = recipe.getOutputs().get(0).stack().copy();
                        baseDuration = recipe.getDuration();
                        break;
                    }
                }
            }

            if (baseResource.isEmpty()) {
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
            }

            habitat.maxProgress = Math.max(20, baseDuration - speedBoost);
            ItemStack produced = baseResource.copy();
            produced.setCount(Math.min(produced.getMaxStackSize(), baseResource.getCount() * habitat.cachedYieldMultiplier * habitat.cachedCycleMultiplier));
            habitat.cachedProducedItem = produced;
            habitat.workerValid = true;
        }

        if (!habitat.workerValid) return;

        if (habitat.cachedProducedItem.isEmpty() || !habitat.canInsertOutput(habitat.cachedProducedItem)) {
            habitat.status = Status.OUTPUT_FULL;
            return;
        }

        // Optional FE speed boost: +100% speed when 40 FE/tick is available
        int step = 1;
        if (habitat.energyStorage.getEnergyStored() >= 40) {
            habitat.energyStorage.extractEnergy(40, false);
            step = 2;
        }

        habitat.progress += step;
        if (habitat.progress >= habitat.maxProgress) {
            habitat.progress = 0;
            habitat.insertOutput(habitat.cachedProducedItem.copy());
            habitat.totalCycles += habitat.cachedCycleMultiplier;
            habitat.totalResources += habitat.cachedProducedItem.getCount();

            if (habitat.totalCycles % 5 == 0) {
                if (habitat.cachedWorkerCat != null) {
                    int hungerLoss = habitat.cachedWorkerCat.getTalentLevel(Talents.HAPPY_EATER) >= 3 ? 0 : 1;
                    if (hungerLoss > 0) habitat.cachedWorkerCat.setHunger(Math.max(0, habitat.cachedWorkerCat.getHunger() - hungerLoss));
                } else if (habitat.hasStoredCat()) {
                    habitat.housedHunger = Math.max(20, habitat.housedHunger - 1);
                }
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

    public int getComparatorOutput() {
        int filled = 0;
        float ratio = 0.0F;
        for (int i = 4; i <= 7; i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (!s.isEmpty()) {
                filled++;
                ratio += (float) s.getCount() / (float) Math.max(1, s.getMaxStackSize());
            }
        }
        if (filled == 0) return 0;
        return Math.min(15, 1 + (int) Math.floor((ratio / 4.0F) * 14.0F));
    }

    public void dropContents() {
        if (level != null) {
            if (hasStoredCat()) {
                releaseHousedCat(worldPosition);
            }
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
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
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
        energyCap.invalidate();
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
        tag.putInt("Status", status.ordinal());
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putInt("HousedHunger", housedHunger);
        if (storedCat != null && !storedCat.isEmpty()) {
            tag.put("StoredCat", storedCat);
        }
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
        if (tag.contains("Inventory")) {
            CompoundTag invTag = tag.getCompound("Inventory");
            if (invTag.getInt("Size") < TOTAL_SLOTS) {
                invTag = invTag.copy();
                invTag.putInt("Size", TOTAL_SLOTS);
            }
            inventory.deserializeNBT(invTag);
        }
        progress = tag.getInt("Progress");
        totalCycles = tag.getInt("TotalCycles");
        totalResources = tag.getInt("TotalResources");
        isPaused = tag.getBoolean("IsPaused");
        if (tag.contains("Status")) status = Status.values()[Math.max(0, Math.min(Status.values().length - 1, tag.getInt("Status")))];
        if (tag.contains("MaxProgress")) maxProgress = Math.max(1, tag.getInt("MaxProgress"));
        housedHunger = tag.contains("HousedHunger") ? tag.getInt("HousedHunger") : 100;
        if (tag.contains("Energy")) {
            // Set, not add: the client reloads this tag on every status update (5.0 fix; it used to keep growing).
            energyStorage.deserializeNBT(net.minecraft.nbt.IntTag.valueOf(tag.getInt("Energy")));
        }
        storedCat = tag.contains("StoredCat") ? tag.getCompound("StoredCat") : new CompoundTag();
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
