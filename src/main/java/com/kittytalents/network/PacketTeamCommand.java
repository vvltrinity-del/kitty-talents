package com.kittytalents.network;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Supplier;

public class PacketTeamCommand {
    public enum CommandType {
        ATTACK_MY_TARGET,
        COME_TO_ME,
        CAST_ABILITY_1,
        CAST_ABILITY_2,
        CAST_ABILITY_3,
        CAST_ULTIMATE
    }

    private final CommandType command;

    public PacketTeamCommand(CommandType command) {
        this.command = command;
    }

    public PacketTeamCommand(FriendlyByteBuf buf) {
        int ord = buf.readInt();
        CommandType[] values = CommandType.values();
        this.command = (ord >= 0 && ord < values.length) ? values[ord] : CommandType.ATTACK_MY_TARGET;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(command.ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            AABB reach = sender.getBoundingBox().inflate(64.0D);
            List<TalentedCat> team = sender.level().getEntitiesOfClass(TalentedCat.class, reach, c -> (c.isOwnedBy(sender) || c.isTrusted(sender)) && !c.isOrderedToSit());

            for (TalentedCat cat : team) {
                switch (command) {
                    case ATTACK_MY_TARGET:
                        LivingEntity pTarget = sender.getLastHurtMob();
                        if (pTarget != null && pTarget.isAlive()) {
                            cat.setTarget(pTarget);
                            if (cat.level() instanceof net.minecraft.server.level.ServerLevel sl) sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER, cat.getX(), cat.getY() + 1.0, cat.getZ(), 1, 0, 0, 0, 0);
                        }
                        break;
                    case COME_TO_ME:
                        cat.setTarget(null);
                        cat.getNavigation().moveTo(sender, 1.5D);
                        if (cat.level() instanceof net.minecraft.server.level.ServerLevel sl) sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, cat.getX(), cat.getY() + 1.0, cat.getZ(), 1, 0, 0, 0, 0);
                        break;
                    case CAST_ABILITY_1:
                        if (!cat.getAbilitySlot1().isEmpty()) com.kittytalents.ability.CatAbilities.get(cat.getAbilitySlot1()).tryCast(cat, null);
                        break;
                    case CAST_ABILITY_2:
                        if (!cat.getAbilitySlot2().isEmpty()) com.kittytalents.ability.CatAbilities.get(cat.getAbilitySlot2()).tryCast(cat, null);
                        break;
                    case CAST_ABILITY_3:
                        if (!cat.getAbilitySlot3().isEmpty()) com.kittytalents.ability.CatAbilities.get(cat.getAbilitySlot3()).tryCast(cat, null);
                        break;
                    case CAST_ULTIMATE:
                        if (!cat.getAbilityUltimate().isEmpty()) com.kittytalents.ability.CatAbilities.get(cat.getAbilityUltimate()).tryCast(cat, null);
                        break;
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
