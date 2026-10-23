package com.kittytalents.entity;

import com.kittytalents.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class FelineHissProjectileEntity extends ThrowableItemProjectile {
    private float damage = 6.0F;

    public FelineHissProjectileEntity(EntityType<? extends FelineHissProjectileEntity> type, Level level) {
        super(type, level);
    }

    public FelineHissProjectileEntity(Level level, LivingEntity shooter, float damage) {
        super(ModEntities.FELINE_HISS_PROJECTILE.get(), shooter, level);
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.SONIC_BOOM, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getRandomX(0.3D), this.getY(), this.getRandomZ(0.3D), 0, 0.02D, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity target) {
            DamageSource source = this.damageSources().indirectMagic(this, this.getOwner());
            target.hurt(source, damage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            target.playSound(SoundEvents.CAT_HISS, 1.2F, 1.0F);
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }
}
