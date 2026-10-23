package com.kittytalents.block;

import com.kittytalents.recipe.TreatBakingRecipe;
import com.kittytalents.registry.ModBlockEntities;
import com.kittytalents.registry.ModItems;
import com.kittytalents.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
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

import java.util.Optional;

/**
 * Automated baking machine for cat treats and meals.
 * Slots 0..2: Ingredient inputs
 * Slot 3: Fuel input
 * Slot 4: Output
 * Optional FE energy storage (10,000 FE): boosts baking speed up to +100%.
 */
public class TreatOvenBlockEntity extends BlockEntity {
    public static final int SLOT_ING_0 = 0;
    public static final int SLOT_ING_1 = 1;
    public static final int SLOT_ING_2 = 2;
    public static final int SLOT_FUEL = 3;
    public static final int SLOT_OUTPUT = 4;
    public static final int TOTAL_SLOTS = 5;

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
            if (slot == SLOT_OUTPUT) return false;
            if (slot == SLOT_FUEL) return ForgeHooks.getBurnTime(stack, null) > 0;
            return true;
        }
    };

    private final EnergyStorage energyStorage = new EnergyStorage(10000, 200, 200);
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energyStorage);
    private final LazyOptional<IItemHandler> fullItemCap = LazyOptional.of(() -> inventory);
    private final LazyOptional<IItemHandler> topInputCap = LazyOptional.of(() -> new RangedWrapper(inventory, 0, 3));
    private final LazyOptional<IItemHandler> sideFuelCap = LazyOptional.of(() -> new RangedWrapper(inventory, 0, 4));
    private final LazyOptional<IItemHandler> bottomOutputCap = LazyOptional.of(() -> new RangedWrapper(inventory, SLOT_OUTPUT, SLOT_OUTPUT + 1) {
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }
    });

    private int burnTime = 0;
    private int maxBurnTime = 0;
    private int cookProgress = 0;
    private int maxCookProgress = 120;

    public TreatOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TREAT_OVEN.get(), pos, state);
    }

    public ItemStackHandler getInventory() { return inventory; }
    public EnergyStorage getEnergyStorage() { return energyStorage; }
    public int getBurnTime() { return burnTime; }
    public int getMaxBurnTime() { return maxBurnTime; }
    public int getCookProgress() { return cookProgress; }
    public int getMaxCookProgress() { return maxCookProgress; }

    public static void tick(Level level, BlockPos pos, BlockState state, TreatOvenBlockEntity oven) {
        if (level.isClientSide) return;

        boolean dirty = false;
        if (oven.burnTime > 0) {
            oven.burnTime--;
            dirty = true;
        }

        SimpleContainer recipeContainer = new SimpleContainer(3);
        for (int i = 0; i < 3; i++) {
            recipeContainer.setItem(i, oven.inventory.getStackInSlot(i));
        }

        ItemStack craftResult = ItemStack.EMPTY;
        int recipeCookTime = 120;

        Optional<TreatBakingRecipe> recipeOpt = level.getRecipeManager()
                .getRecipeFor(ModRecipes.TREAT_BAKING_TYPE.get(), recipeContainer, level);
        if (recipeOpt.isPresent()) {
            craftResult = recipeOpt.get().getResult().copy();
            recipeCookTime = recipeOpt.get().getCookTime();
        } else {
            craftResult = getFallbackBakingResult(recipeContainer);
        }

        if (!craftResult.isEmpty() && oven.canOutput(craftResult)) {
            oven.maxCookProgress = recipeCookTime;

            // Consume fuel if not burning
            if (oven.burnTime <= 0) {
                ItemStack fuel = oven.inventory.getStackInSlot(SLOT_FUEL);
                int fuelBurn = ForgeHooks.getBurnTime(fuel, null);
                if (fuelBurn > 0) {
                    oven.burnTime = fuelBurn;
                    oven.maxBurnTime = fuelBurn;
                    ItemStack remainder = fuel.getCraftingRemainingItem();
                    fuel.shrink(1);
                    if (fuel.isEmpty()) {
                        oven.inventory.setStackInSlot(SLOT_FUEL, remainder);
                    }
                    dirty = true;
                }
            }

            if (oven.burnTime > 0) {
                int step = 1;
                // Optional FE speed boost: +100% speed when 20 FE/tick is available
                if (oven.energyStorage.getEnergyStored() >= 20) {
                    oven.energyStorage.extractEnergy(20, false);
                    step = 2;
                }
                oven.cookProgress += step;
                if (oven.cookProgress >= oven.maxCookProgress) {
                    oven.cookProgress = 0;
                    for (int i = 0; i < 3; i++) {
                        ItemStack s = oven.inventory.getStackInSlot(i);
                        if (!s.isEmpty()) {
                            s.shrink(1);
                            if (s.isEmpty()) oven.inventory.setStackInSlot(i, ItemStack.EMPTY);
                        }
                    }
                    ItemStack currentOut = oven.inventory.getStackInSlot(SLOT_OUTPUT);
                    if (currentOut.isEmpty()) {
                        oven.inventory.setStackInSlot(SLOT_OUTPUT, craftResult.copy());
                    } else {
                        currentOut.grow(craftResult.getCount());
                    }
                    level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 1.3F);
                }
                dirty = true;
            } else if (oven.cookProgress > 0) {
                oven.cookProgress = Math.max(0, oven.cookProgress - 2);
                dirty = true;
            }
        } else if (oven.cookProgress > 0) {
            oven.cookProgress = 0;
            dirty = true;
        }

        if (dirty) {
            oven.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private boolean canOutput(ItemStack result) {
        if (result.isEmpty()) return false;
        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(out, result)) return false;
        return out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    public static ItemStack getFallbackBakingResult(SimpleContainer container) {
        boolean hasCatnip = false;
        boolean hasWheat = false;
        boolean hasGold = false;
        boolean hasDiamond = false;
        boolean hasSalmon = false;
        boolean hasRice = false;
        int count = 0;
        for (int i = 0; i < 3; i++) {
            ItemStack s = container.getItem(i);
            if (s.isEmpty()) continue;
            count++;
            if (s.is(ModItems.CATNIP.get())) hasCatnip = true;
            else if (s.is(Items.WHEAT)) hasWheat = true;
            else if (s.is(Items.GOLD_INGOT)) hasGold = true;
            else if (s.is(Items.DIAMOND)) hasDiamond = true;
            else if (s.is(Items.SALMON) || s.is(Items.COOKED_SALMON)) hasSalmon = true;
            else if (s.is(ModItems.RICE_GRAIN.get()) || s.is(ModItems.RICE_BOWL.get())) hasRice = true;
        }
        if (count == 2 && hasCatnip && hasWheat) {
            return new ItemStack(ModItems.CATNIP_TREAT.get(), 2);
        }
        if (count == 2 && hasCatnip && hasGold) {
            return new ItemStack(ModItems.SUPER_CATNIP_TREAT.get(), 1);
        }
        if (count == 2 && hasCatnip && hasDiamond) {
            return new ItemStack(ModItems.MASTER_CATNIP_TREAT.get(), 1);
        }
        if (count == 2 && hasSalmon && hasRice) {
            return new ItemStack(ModItems.SALMON_SUSHI.get(), 2);
        }
        return ItemStack.EMPTY;
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
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == Direction.DOWN) return bottomOutputCap.cast();
            if (side == Direction.UP) return topInputCap.cast();
            if (side != null) return sideFuelCap.cast();
            return fullItemCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
        fullItemCap.invalidate();
        topInputCap.invalidate();
        sideFuelCap.invalidate();
        bottomOutputCap.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("BurnTime", burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
        tag.putInt("CookProgress", cookProgress);
        tag.putInt("MaxCookProgress", maxCookProgress);
        tag.putInt("Energy", energyStorage.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) inventory.deserializeNBT(tag.getCompound("Inventory"));
        burnTime = tag.getInt("BurnTime");
        maxBurnTime = tag.getInt("MaxBurnTime");
        cookProgress = tag.getInt("CookProgress");
        maxCookProgress = Math.max(1, tag.getInt("MaxCookProgress"));
        if (tag.contains("Energy")) {
            energyStorage.receiveEnergy(tag.getInt("Energy"), false);
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
