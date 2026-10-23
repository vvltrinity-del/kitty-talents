package com.kittytalents.network;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.talent.Talents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketTwineInput {
    private final int catEntityId;
    private final float xxa; // strafe (-1.0 to 1.0)
    private final float zza; // forward (-1.0 to 1.0)
    private final boolean jump;
    private final boolean sprint;
    private final boolean sneak;
    private final boolean attack;
    private final boolean pounce;
    private final boolean ability1;
    private final boolean ability2;
    private final float yRot;
    private final float xRot;

    public PacketTwineInput(int catEntityId, float xxa, float zza, boolean jump, boolean sprint, boolean sneak, boolean attack, boolean pounce, boolean ability1, boolean ability2, float yRot, float xRot) {
        this.catEntityId = catEntityId;
        this.xxa = xxa;
        this.zza = zza;
        this.jump = jump;
        this.sprint = sprint;
        this.sneak = sneak;
        this.attack = attack;
        this.pounce = pounce;
        this.ability1 = ability1;
        this.ability2 = ability2;
        this.yRot = yRot;
        this.xRot = xRot;
    }

    public PacketTwineInput(FriendlyByteBuf buf) {
        this.catEntityId = buf.readInt();
        this.xxa = buf.readFloat();
        this.zza = buf.readFloat();
        this.jump = buf.readBoolean();
        this.sprint = buf.readBoolean();
        this.sneak = buf.readBoolean();
        this.attack = buf.readBoolean();
        this.pounce = buf.readBoolean();
        this.ability1 = buf.readBoolean();
        this.ability2 = buf.readBoolean();
        this.yRot = buf.readFloat();
        this.xRot = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(catEntityId);
        buf.writeFloat(xxa);
        buf.writeFloat(zza);
        buf.writeBoolean(jump);
        buf.writeBoolean(sprint);
        buf.writeBoolean(sneak);
        buf.writeBoolean(attack);
        buf.writeBoolean(pounce);
        buf.writeBoolean(ability1);
        buf.writeBoolean(ability2);
        buf.writeFloat(yRot);
        buf.writeFloat(xRot);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            Entity entity = sender.level().getEntity(catEntityId);
            if (entity instanceof TalentedCat cat && cat.isTwinedWith(sender)) {
                if (!Float.isFinite(xxa) || !Float.isFinite(zza) || !Float.isFinite(yRot) || !Float.isFinite(xRot)) return;
                float yRot = net.minecraft.util.Mth.wrapDegrees(this.yRot);
                float xRot = net.minecraft.util.Mth.clamp(this.xRot, -90.0F, 90.0F);
                float xxa = net.minecraft.util.Mth.clamp(this.xxa, -1.0F, 1.0F);
                float zza = net.minecraft.util.Mth.clamp(this.zza, -1.0F, 1.0F);

                // The server is the sole authoritative gameplay transform writer.
                // Preserve the entity's previous rotation for normal interpolation;
                // setting yRotO/xRotO here caused the camera to snap every update.
                cat.setYRot(yRot);
                cat.setXRot(xRot);
                cat.setYHeadRot(yRot);
                cat.setYBodyRot(yRot);

                // Movement is stored and applied in TalentedCat#customServerAiStep and travel()
                cat.setTwineInput(xxa, zza);
                cat.setSprinting(sprint);
                cat.setShiftKeyDown(sneak);
                if (xxa != 0.0F || zza != 0.0F) {
                    cat.hasImpulse = true;
                }
                float speed = (float) cat.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                if (cat.isSprinting()) speed *= 1.35F;
                cat.setSpeed(speed);

                // Flight / Jump / Wall leap logic
                if (jump) {
                    if (cat.getTalentLevel(Talents.SKY_FELINE) > 0) {
                        Vec3 motion = cat.getDeltaMovement();
                        cat.setDeltaMovement(motion.x, Math.min(0.65D, motion.y + 0.18D), motion.z);
                        cat.hasImpulse = true;
                        cat.resetFallDistance();
                    } else if (cat.getTalentLevel(Talents.FELINE_AGILITY) >= 3 && !cat.onGround() && cat.horizontalCollision) {
                        Vec3 motion = cat.getDeltaMovement();
                        cat.setDeltaMovement(-motion.x * 0.5D, 0.45D, -motion.z * 0.5D);
                        cat.hasImpulse = true;
                        cat.playSound(net.minecraft.sounds.SoundEvents.PLAYER_SMALL_FALL, 0.8F, 1.2F);
                    } else if (cat.onGround()) {
                        cat.doJump();
                    }
                }

                // Attack (Left click)
                if (attack) {
                    if (com.kittytalents.compat.tacz.TacZCompat.isGun(cat.getEquippedWeapon())) {
                        com.kittytalents.compat.tacz.TacZCompat.shootGunTwined(cat, sender);
                    } else {
                        cat.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        net.minecraft.world.phys.AABB reach = cat.getBoundingBox().inflate(2.2D);
                        for (net.minecraft.world.entity.LivingEntity target : cat.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, reach, e -> e != cat && e != sender && !cat.isAlliedTo(e))) {
                            cat.doHurtTarget(target);
                            break;
                        }
                    }
                }

                // Pounce leap or Gun Aiming (Right click)
                if (com.kittytalents.compat.tacz.TacZCompat.isGun(cat.getEquippedWeapon())) {
                    com.kittytalents.compat.tacz.TacZCompat.setAiming(cat, pounce);
                } else if (pounce) {
                    if (cat.getTalentLevel(Talents.POUNCE) > 0 && cat.onGround()) {
                        Vec3 look = cat.getLookAngle();
                        double pounceSpeed = 1.35D + cat.getTalentLevel(Talents.POUNCE) * 0.08D;
                        cat.setDeltaMovement(look.x * pounceSpeed, 0.52D, look.z * pounceSpeed);
                        cat.hasImpulse = true;
                        cat.playSound(net.minecraft.sounds.SoundEvents.FOX_BITE, 1.0F, 1.2F);

                        net.minecraft.world.phys.AABB pounceReach = cat.getBoundingBox().inflate(2.5D);
                        for (net.minecraft.world.entity.LivingEntity target : cat.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, pounceReach, e -> e != cat && e != sender && !cat.isAlliedTo(e))) {
                            cat.doHurtTarget(target);
                            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                            if (cat.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, cat.getX(), cat.getY() + 0.2D, cat.getZ(), 10, 0.3D, 0.2D, 0.3D, 0.1D);
                            }
                            break;
                        }
                    }
                }

                // Ability 1 (V): Feline Hiss AoE
                if (ability1) {
                    cat.playSound(net.minecraft.sounds.SoundEvents.CAT_HISS, 1.2F, 1.1F);
                    net.minecraft.world.phys.AABB aoe = cat.getBoundingBox().inflate(6.0D);
                    for (net.minecraft.world.entity.LivingEntity le : cat.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, aoe, e -> e != cat && e != sender && !cat.isAlliedTo(e))) {
                        le.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 160, 1));
                        if (le instanceof net.minecraft.world.entity.monster.Creeper creeper) {
                            Vec3 diff = creeper.position().subtract(cat.position());
                            Vec3 repel = diff.lengthSqr() > 1.0E-4D ? diff.normalize() : new Vec3(0, 1.0D, 0);
                            creeper.setDeltaMovement(repel.x * 0.85D, 0.25D, repel.z * 0.85D);
                        }
                    }
                    if (cat.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER, cat.getX(), cat.getY() + 0.5D, cat.getZ(), 8, 0.5D, 0.3D, 0.5D, 0.05D);
                    }
                }

                // Ability 2 (G): Torch placement / Tool action
                if (ability2) {
                    if (cat.getTalentLevel(Talents.TORCH_KITTY) > 0) {
                        cat.tryPlaceTorchManual();
                    } else if (cat.getTalentLevel(Talents.KITTY_TOOLS) > 0) {
                        cat.tryUseEquippedTool();
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
