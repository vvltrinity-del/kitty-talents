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

public class CatBathBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D);

    public CatBathBlock(Properties properties) {
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
        return new CatBathBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CAT_BATH.get(), CatBathBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (level.getBlockEntity(pos) instanceof CatBathBlockEntity bath) {
            if (held.is(Items.WATER_BUCKET)) {
                if (bath.getWaterLevel() < 3) {
                    if (!level.isClientSide) {
                        bath.setWaterLevel(3);
                        if (!player.getAbilities().instabuild) {
                            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                        }
                        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            } else if (held.is(Items.BUCKET)) {
                if (bath.getWaterLevel() >= 3) {
                    if (!level.isClientSide) {
                        bath.setWaterLevel(0);
                        if (!player.getAbilities().instabuild) {
                            held.shrink(1);
                            player.addItem(new ItemStack(Items.WATER_BUCKET));
                        }
                        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            } else if (bath.getWaterLevel() > 0) {
                // Wash nearby cats within 5 blocks on any terrain
                java.util.List<com.kittytalents.entity.TalentedCat> cats = level.getEntitiesOfClass(
                        com.kittytalents.entity.TalentedCat.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(5.0D, 3.0D, 5.0D),
                        c -> !c.isIncapacitated() && (c.isOwnedBy(player) || c.isTrusted(player) || !c.isTame())
                );
                if (!cats.isEmpty()) {
                    if (!level.isClientSide) {
                        for (com.kittytalents.entity.TalentedCat cat : cats) {
                            cat.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
                            cat.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
                            cat.removeEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
                            cat.removeEffect(net.minecraft.world.effect.MobEffects.WITHER);

                            cat.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 2400, 1, false, false, true));
                            cat.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 200, 1, false, false, true));

                            cat.playSound(SoundEvents.GENERIC_SWIM, 0.7F, 1.2F);
                            cat.playSound(SoundEvents.CAT_PURREOW, 0.8F, 1.3F);

                            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,
                                        cat.getX(), cat.getY() + 0.3D, cat.getZ(),
                                        12, 0.2D, 0.1D, 0.2D, 0.1D);
                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                                        cat.getX(), cat.getY() + 0.6D, cat.getZ(),
                                        3, 0.2D, 0.2D, 0.2D, 0.02D);
                            }
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal(cat.getName().getString() + " has been washed and groomed!").withStyle(net.minecraft.ChatFormatting.AQUA), true);
                        }
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return InteractionResult.PASS;
    }
}
