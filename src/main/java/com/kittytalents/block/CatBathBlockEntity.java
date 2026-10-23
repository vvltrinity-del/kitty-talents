package com.kittytalents.block;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModBlockEntities;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CatBathBlockEntity extends BlockEntity {
    private int waterLevel = 3;
    private int cleanCooldown = 0;
    private int fishCooldown = 0;

    public CatBathBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAT_BATH.get(), pos, state);
    }

    public int getWaterLevel() {
        return waterLevel;
    }

    public void setWaterLevel(int level) {
        this.waterLevel = Math.max(0, Math.min(3, level));
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CatBathBlockEntity bath) {
        if (bath.waterLevel <= 0) return;

        // Visual water bubbles
        if (level.isClientSide) {
            if (level.random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.BUBBLE,
                        pos.getX() + 0.3D + level.random.nextDouble() * 0.4D,
                        pos.getY() + 0.4D,
                        pos.getZ() + 0.3D + level.random.nextDouble() * 0.4D,
                        0.0D, 0.04D, 0.0D);
            }
            return;
        }

        if (bath.cleanCooldown > 0) bath.cleanCooldown--;
        if (bath.fishCooldown > 0) bath.fishCooldown--;

        if ((bath.cleanCooldown <= 0 || bath.fishCooldown <= 0) && level.getGameTime() % 10 == 0) {
            // Check for cats inside or within 5 blocks on uneven terrain
            List<TalentedCat> cats = level.getEntitiesOfClass(
                    TalentedCat.class,
                    new AABB(pos).inflate(5.0D, 3.0D, 5.0D),
                    c -> !c.isIncapacitated()
            );

            if (!cats.isEmpty()) {
                TalentedCat cat = cats.get(0);

                if (bath.cleanCooldown <= 0) {
                    bath.cleanCooldown = 80;

                    // Cleanse harmful effects
                    cat.removeEffect(MobEffects.POISON);
                    cat.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    cat.removeEffect(MobEffects.WEAKNESS);
                    cat.removeEffect(MobEffects.WITHER);

                    // Add happiness buff: Speed & Regeneration
                    cat.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2400, 1, false, false, true));
                    cat.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, false, true));

                    cat.playSound(SoundEvents.GENERIC_SWIM, 0.7F, 1.2F);
                    cat.playSound(SoundEvents.CAT_PURREOW, 0.8F, 1.3F);

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SPLASH,
                                pos.getX() + 0.5D, pos.getY() + 0.45D, pos.getZ() + 0.5D,
                                12, 0.2D, 0.1D, 0.2D, 0.1D);
                        serverLevel.sendParticles(ParticleTypes.HEART,
                                cat.getX(), cat.getY() + 0.6D, cat.getZ(),
                                2, 0.2D, 0.2D, 0.2D, 0.02D);
                    }
                }

                // Fisher Cat interaction: catch bonus fish
                if (cat.getTalentLevel(Talents.FISHER_CAT) > 0 && bath.fishCooldown <= 0) {
                    bath.fishCooldown = 300; // Every 15 seconds
                    ItemStack prize = switch (level.random.nextInt(6)) {
                        case 0 -> new ItemStack(Items.COD);
                        case 1 -> new ItemStack(Items.SALMON);
                        case 2 -> new ItemStack(Items.TROPICAL_FISH);
                        case 3 -> new ItemStack(Items.PRISMARINE_SHARD);
                        case 4 -> new ItemStack(Items.NAUTILUS_SHELL);
                        default -> new ItemStack(Items.RAW_COPPER);
                    };

                    ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D, prize);
                    level.addFreshEntity(itemEntity);
                    cat.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.8F, 1.2F);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("WaterLevel", waterLevel);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        waterLevel = tag.getInt("WaterLevel");
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
