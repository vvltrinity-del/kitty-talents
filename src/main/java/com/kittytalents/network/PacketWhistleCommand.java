package com.kittytalents.network;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.WhistleCommand;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class PacketWhistleCommand {
    private final int commandId;

    public PacketWhistleCommand(WhistleCommand cmd) {
        this.commandId = cmd.ordinal();
    }

    public PacketWhistleCommand(int commandId) {
        this.commandId = commandId;
    }

    public PacketWhistleCommand(FriendlyByteBuf buf) {
        this.commandId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(commandId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            WhistleCommand[] commands = WhistleCommand.values();
            if (commandId < 0 || commandId >= commands.length) return;
            WhistleCommand cmd = commands[commandId];

            ServerLevel level = player.serverLevel();
            AABB area = player.getBoundingBox().inflate(64.0D);
            List<TalentedCat> cats = level.getEntitiesOfClass(TalentedCat.class, area,
                    c -> c.isOwnedBy(player) || c.isTrusted(player));

            if (cats.isEmpty()) {
                player.sendSystemMessage(Component.literal("No nearby cats heard your whistle.").withStyle(net.minecraft.ChatFormatting.GRAY));
                return;
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_FLUTE.get(), SoundSource.PLAYERS, 1.2F, 1.8F);

            for (TalentedCat cat : cats) {
                switch (cmd) {
                    case COME, FOLLOW -> {
                        cat.setOrderedToSit(false);
                        cat.setCatMode(CatMode.FOLLOW);
                        cat.getNavigation().moveTo(player, 1.4D);
                        level.sendParticles(ParticleTypes.HEART, cat.getX(), cat.getY() + 0.5D, cat.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
                    }
                    case STAY, SIT -> {
                        cat.setOrderedToSit(true);
                        cat.getNavigation().stop();
                        cat.setTarget(null);
                        cat.setCatMode(CatMode.DOCILE);
                    }
                    case STAND -> {
                        cat.setOrderedToSit(false);
                    }
                    case HEEL -> {
                        cat.setOrderedToSit(false);
                        cat.setCatMode(CatMode.FOLLOW);
                        cat.teleportTo(player.getX() + (player.getRandom().nextDouble() - 0.5D) * 2.0D, player.getY(), player.getZ() + (player.getRandom().nextDouble() - 0.5D) * 2.0D);
                    }
                    case DISENGAGE, STOP_ATTACK -> {
                        cat.setTarget(null);
                        cat.getNavigation().stop();
                        cat.setCatMode(CatMode.DOCILE);
                    }
                    case DEFEND, GUARD -> {
                        cat.setOrderedToSit(false);
                        cat.setCatMode(CatMode.GUARD);
                    }
                    case PATROL -> {
                        cat.setOrderedToSit(false);
                        cat.setCatMode(CatMode.PATROL);
                    }
                    case HUNT -> {
                        cat.setOrderedToSit(false);
                        cat.setCatMode(CatMode.HUNTING);
                    }
                    case AGGRESSIVE -> {
                        cat.setOrderedToSit(false);
                        cat.setCatMode(CatMode.AGGRESSIVE);
                    }
                    case SCATTER -> {
                        cat.setOrderedToSit(false);
                        cat.setCatMode(CatMode.WANDER);
                    }
                    case REST -> {
                        cat.setOrderedToSit(false);
                        if (cat.getHomeBedPos() != null) {
                            cat.getNavigation().moveTo(cat.getHomeBedPos().getX(), cat.getHomeBedPos().getY(), cat.getHomeBedPos().getZ(), 1.2D);
                        }
                    }
                    case EAT -> {
                        cat.setOrderedToSit(false);
                    }
                    case PURR_BUFF -> {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 160, 1));
                        cat.playSound(SoundEvents.CAT_PURREOW, 1.0F, 1.0F);
                    }
                    case ROAR_HISS -> {
                        cat.playSound(SoundEvents.CAT_HISS, 1.2F, 0.8F);
                        List<net.minecraft.world.entity.monster.Monster> monsters = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, cat.getBoundingBox().inflate(10.0D));
                        for (net.minecraft.world.entity.monster.Monster m : monsters) {
                            m.setTarget(null);
                            Vec3 diff = m.position().subtract(cat.position());
                            Vec3 push = diff.lengthSqr() > 1.0E-4D ? diff.normalize().scale(1.2D) : new Vec3(0, 0.5D, 0);
                            m.push(push.x, 0.3D, push.z);
                        }
                    }
                    case FRENZY -> {
                        cat.triggerFrenzy(1200);
                    }
                    case RETURN_HOME -> {
                        cat.setOrderedToSit(false);
                        if (cat.getHomeBedPos() != null) {
                            cat.teleportTo(cat.getHomeBedPos().getX() + 0.5D, cat.getHomeBedPos().getY() + 1.0D, cat.getHomeBedPos().getZ() + 0.5D);
                        }
                    }
                    case LOCATE -> {
                        cat.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 300, 0));
                    }
                    case PLAY -> {
                        cat.setOrderedToSit(false);
                        cat.setCatMode(CatMode.WANDER);
                    }
                    case FISH -> {
                        cat.setOrderedToSit(false);
                    }
                    case MINE -> {
                        cat.setOrderedToSit(false);
                    }
                    case FLY -> {
                        if (cat.getTalentLevel(Talents.SKY_FELINE) > 0) {
                            cat.startRiding(player);
                        }
                    }
                    case QUIET -> {
                        cat.setQuietUntil(cat.tickCount + 6000);
                    }
                    case INVENTORY -> {
                        cat.openPackScreen(player);
                        return; // Only open for first cat
                    }
                    default -> {}
                }
            }

            player.displayClientMessage(Component.literal("Whistled command: " + cmd.getDisplayName() + " (" + cats.size() + " cats responded)"), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
