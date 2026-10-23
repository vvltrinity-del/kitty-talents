package com.kittytalents.entity;

import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownToyEntity extends ThrowableItemProjectile {
    private int groundTicks = 0;

    public ThrownToyEntity(EntityType<? extends ThrownToyEntity> type, Level level) {
        super(type, level);
    }

    public ThrownToyEntity(Level level, LivingEntity shooter) {
        super(ModEntities.THROWN_TOY.get(), shooter, level);
    }

    public ThrownToyEntity(Level level, double x, double y, double z) {
        super(ModEntities.THROWN_TOY.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.YARN_BALL.get();
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.2D, 0.1D, 0.2D));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.onGround()) {
            groundTicks++;
            // If untouched for 30 seconds (600 ticks), drop as an item entity
            if (groundTicks > 600 && !this.level().isClientSide) {
                this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), this.getItem()));
                this.discard();
            }
        }
    }
}
