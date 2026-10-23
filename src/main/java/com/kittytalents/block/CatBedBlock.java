package com.kittytalents.block;

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
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 6.0D, 15.0D);

    public CatBedBlock(Properties properties) {
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
