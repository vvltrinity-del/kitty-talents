package com.kittytalents.network;

import com.kittytalents.core.perm.CatAction;
import com.kittytalents.core.perm.CatPermissions;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.TwineManager;
import com.kittytalents.talent.Talents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * Client -> server, every tick while twined: movement, look and combat input for the twined cat.
 * Needs {@link CatAction#TWINE_CONTROL}; non-finite values are dropped and the rest are clamped.
 */
public class PacketTwineInput extends PacketBase {
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

    @Override
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

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender);
    }

    /** @return true if the input was applied to the sender's twined cat. Public for GameTests. */
    public boolean apply(ServerPlayer sender) {
        if (!Float.isFinite(xxa) || !Float.isFinite(zza) || !Float.isFinite(yRot) || !Float.isFinite(xRot)) return false;
        CatPermissions.Access access = CatPermissions.check(sender, catEntityId, CatAction.TWINE_CONTROL);
        if (!access.allowed()) {
            if (access.denial() == CatPermissions.Denial.NOT_TWINED && !TwineManager.isPlayerTwined(sender.getUUID())) {
                // Stale client session: tell it to drop out of the cat's view.
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sender), new PacketSyncTwine(catEntityId, false));
            }
            return false;
        }
        TalentedCat cat = access.cat();
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

        // Flight (Sky Feline: climb/glide is applied every tick in TalentedCat#travel via SkyFlight) / jump / wall leap
        cat.setTwineJump(jump);
        if (jump && cat.getTalentLevel(Talents.SKY_FELINE) <= 0) {
            if (cat.getTalentLevel(Talents.FELINE_AGILITY) >= 3 && !cat.onGround() && cat.horizontalCollision) {
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
            long now = cat.level().getGameTime();
            if (cat.onGround() && now >= cat.getLastTwinePounceTick() + 20) {
                cat.setLastTwinePounceTick(now);
                Vec3 look = cat.getLookAngle();
                double pounceSpeed = 1.25D + cat.getTalentLevel(Talents.POUNCE) * 0.10D;
                cat.setDeltaMovement(look.x * pounceSpeed, 0.48D, look.z * pounceSpeed);
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
        return true;
    }
}
