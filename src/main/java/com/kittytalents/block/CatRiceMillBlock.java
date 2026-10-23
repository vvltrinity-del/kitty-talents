package com.kittytalents.block;

import com.kittytalents.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CatRiceMillBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D);

    public CatRiceMillBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CatRiceMillBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CAT_RICE_MILL.get(), CatRiceMillBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (level.getBlockEntity(pos) instanceof CatRiceMillBlockEntity mill) {
            // Water refill
            if (held.is(Items.WATER_BUCKET)) {
                if (mill.getWaterLevel() < 4) {
                    if (!level.isClientSide) {
                        mill.setWaterLevel(4);
                        if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }

            // Extract output if ready
            ItemStack output = mill.getInventory().getStackInSlot(1);
            if (!output.isEmpty() && held.isEmpty()) {
                if (!level.isClientSide) {
                    player.addItem(output.copy());
                    mill.getInventory().setStackInSlot(1, ItemStack.EMPTY);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // Insert millable input
            if (!held.isEmpty() && CatRiceMillBlockEntity.isMillable(held)) {
                ItemStack input = mill.getInventory().getStackInSlot(0);
                if (input.isEmpty() || (ItemStack.isSameItemSameTags(input, held) && input.getCount() < input.getMaxStackSize())) {
                    if (!level.isClientSide) {
                        int toAdd = Math.min(held.getCount(), 64 - input.getCount());
                        if (input.isEmpty()) {
                            ItemStack inserted = held.copy();
                            inserted.setCount(toAdd);
                            mill.getInventory().setStackInSlot(0, inserted);
                        } else {
                            input.grow(toAdd);
                        }
                        if (!player.getAbilities().instabuild) held.shrink(toAdd);
                        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 0.9F);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof CatRiceMillBlockEntity mill) {
                mill.dropContents();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
