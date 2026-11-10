package com.kittytalents.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import com.kittytalents.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class KittyHabitatBlock extends BaseEntityBlock {
    /** Front of the model; rotated by the blockstate (4.0 Phase 2: 3D models). Blocks placed in 3.0 load as NORTH. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final java.util.Map<Direction, VoxelShape> SHAPES = KTShapes.byFacing(KTShapes.of(new double[]{1, 0, 1, 15, 10, 15}, new double[]{0, 10, 0, 16, 12, 16}, new double[]{3, 12, 0, 13, 14, 16}, new double[]{4.6, 13, 0.4, 11.4, 15, 1.6}));

    public KittyHabitatBlock(Properties properties) {
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
        return new KittyHabitatBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.KITTY_HABITAT.get(), KittyHabitatBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (level.getBlockEntity(pos) instanceof KittyHabitatBlockEntity habitat) {
            // Check if holding KittyWorkClipboardItem
            if (held.getItem() instanceof com.kittytalents.item.KittyWorkClipboardItem) {
                return InteractionResult.PASS; // Handled by KittyWorkClipboardItem#useOn
            }

            // Sneak right click extracts outputs
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide) {
                    boolean extracted = false;
                    for (int i = 4; i <= 7; i++) {
                        ItemStack output = habitat.getInventory().getStackInSlot(i);
                        if (!output.isEmpty()) {
                            player.addItem(output.copy());
                            habitat.getInventory().setStackInSlot(i, ItemStack.EMPTY);
                            extracted = true;
                        }
                    }
                    if (extracted) {
                        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.7F, 1.3F);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // Open Habitat Menu GUI
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.SimpleMenuProvider(
                        (id, inv, p) -> new com.kittytalents.inventory.KittyHabitatMenu(id, inv, habitat),
                        Component.literal("Kitty Production Habitat")
                ), pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof KittyHabitatBlockEntity habitat) {
            return habitat.getComparatorOutput();
        }
        return 0;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof KittyHabitatBlockEntity habitat) {
                habitat.dropContents();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
