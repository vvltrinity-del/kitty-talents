package com.kittytalents.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CatBedBlock extends BaseEntityBlock {
    /** Front of the model; rotated by the blockstate (4.0 Phase 2: 3D models). Blocks placed in 3.0 load as NORTH. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final java.util.Map<Direction, VoxelShape> SHAPES = KTShapes.byFacing(KTShapes.of(new double[]{1, 0, 1, 15, 6.6, 15}));

    public CatBedBlock(Properties properties) {
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
        return new CatBedBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CatBedBlockEntity bedEntity)) {
            return InteractionResult.PASS;
        }

        // Dye bed cushion
        if (held.getItem() instanceof DyeItem dye) {
            bedEntity.setColor(dye.getDyeColor());
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Link nearby owned cat to this bed
        if (!level.isClientSide) {
            List<TalentedCat> cats = level.getEntitiesOfClass(TalentedCat.class, new AABB(pos).inflate(8.0D), c -> c.isOwnedBy(player));
            if (!cats.isEmpty()) {
                TalentedCat cat = cats.get(0);
                bedEntity.setOwnerUUID(player.getUUID());
                bedEntity.setLinkedCatUUID(cat.getUUID());
                cat.setHomeBedPos(pos);
                cat.setHomeBedDimension(level.dimension().location().toString());
                player.sendSystemMessage(Component.translatable("message.kittytalents.bed_linked", cat.getName()));
                level.playSound(null, pos, SoundEvents.CAT_PURREOW, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            } else {
                player.sendSystemMessage(Component.translatable("message.kittytalents.no_cat_near_bed"));
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && !level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CatBedBlockEntity bedEntity) {
                bedEntity.setOwnerUUID(player.getUUID());
            }
        }
    }
}
