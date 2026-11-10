package com.kittytalents.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import com.kittytalents.registry.ModBlockEntities;
import com.kittytalents.util.CatFoodHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class KittyFeedingStationBlock extends BaseEntityBlock {
    /** Front of the model; rotated by the blockstate (4.0 Phase 2: 3D models). Blocks placed in 3.0 load as NORTH. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final java.util.Map<Direction, VoxelShape> SHAPES = KTShapes.byFacing(KTShapes.of(new double[]{1, 0, 2, 15, 1.5, 14}, new double[]{3.5, 1.5, 7.5, 12.5, 14.6, 13.5}, new double[]{4, 1.5, 2.5, 12, 3.5, 6.5}));

    public KittyFeedingStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KittyFeedingStationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FEEDING_STATION.get(), KittyFeedingStationBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (level.getBlockEntity(pos) instanceof KittyFeedingStationBlockEntity station) {
            if (held.isEmpty()) {
                // Extract from last non-empty slot
                if (!level.isClientSide) {
                    for (int i = 3; i >= 0; i--) {
                        ItemStack inSlot = station.getInventory().getStackInSlot(i);
                        if (!inSlot.isEmpty()) {
                            player.addItem(inSlot.copy());
                            station.getInventory().setStackInSlot(i, ItemStack.EMPTY);
                            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
                            break;
                        }
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            } else if (CatFoodHelper.isEdibleByCat(held, null)) {
                // Insert into first available slot
                if (!level.isClientSide) {
                    for (int i = 0; i < 4; i++) {
                        ItemStack inSlot = station.getInventory().getStackInSlot(i);
                        if (inSlot.isEmpty() || (ItemStack.isSameItemSameTags(inSlot, held) && inSlot.getCount() < inSlot.getMaxStackSize())) {
                            int toAdd = Math.min(held.getCount(), inSlot.isEmpty() ? 64 : inSlot.getMaxStackSize() - inSlot.getCount());
                            if (inSlot.isEmpty()) {
                                ItemStack inserted = held.copy();
                                inserted.setCount(toAdd);
                                station.getInventory().setStackInSlot(i, inserted);
                            } else {
                                inSlot.grow(toAdd);
                            }
                            if (!player.getAbilities().instabuild) held.shrink(toAdd);
                            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 0.9F);
                            break;
                        }
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof KittyFeedingStationBlockEntity station) {
                station.dropContents();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
