package com.kittytalents.block;

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
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);

    public KittyHabitatBlock(Properties properties) {
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
