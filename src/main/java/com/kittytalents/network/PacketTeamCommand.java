package com.kittytalents.network;

import com.kittytalents.ability.CatAbilities;
import com.kittytalents.ability.CatAbility;
import com.kittytalents.combat.FelineEnergyEngine;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.TwineManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

            List<TalentedCat> team = new ArrayList<>();
            UUID twinedCatUuid = TwineManager.getCatOf(sender.getUUID());
            if (twinedCatUuid != null && sender.level() instanceof ServerLevel sl) {
                Entity e = sl.getEntity(twinedCatUuid);
                if (e instanceof TalentedCat twinedCat && twinedCat.isAlive() && !twinedCat.isIncapacitated()) {
                    team.add(twinedCat);
                }
            }

            if (team.isEmpty()) {
                AABB reach = sender.getBoundingBox().inflate(64.0D);
                team.addAll(sender.level().getEntitiesOfClass(
                        TalentedCat.class,
                        reach,
                        c -> (c.isOwnedBy(sender) || c.isTrusted(sender)) && !c.isOrderedToSit() && !c.isIncapacitated()
                ));
            }

            boolean isTwinedCast = twinedCatUuid != null && !team.isEmpty();

            for (TalentedCat cat : team) {
                switch (command) {
                    case ATTACK_MY_TARGET -> {
                        LivingEntity pTarget = sender.getLastHurtMob();
                        if (pTarget != null && pTarget.isAlive()) {
                            cat.setTarget(pTarget);
                            if (cat.level() instanceof ServerLevel sl) {
                                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER, cat.getX(), cat.getY() + 1.0, cat.getZ(), 1, 0, 0, 0, 0);
                            }
                        }
                    }
                    case COME_TO_ME -> {
                        cat.setTarget(null);
                        cat.getNavigation().moveTo(sender, 1.5D);
                        if (cat.level() instanceof ServerLevel sl) {
                            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, cat.getX(), cat.getY() + 1.0, cat.getZ(), 1, 0, 0, 0, 0);
                        }
                    }
                    case CAST_ABILITY_1 -> tryCastSlot(cat, sender, cat.getAbilitySlot1(), isTwinedCast);
                    case CAST_ABILITY_2 -> tryCastSlot(cat, sender, cat.getAbilitySlot2(), isTwinedCast);
                    case CAST_ABILITY_3 -> tryCastSlot(cat, sender, cat.getAbilitySlot3(), isTwinedCast);
                    case CAST_ULTIMATE -> tryCastSlot(cat, sender, cat.getAbilityUltimate(), isTwinedCast);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void tryCastSlot(TalentedCat cat, ServerPlayer sender, String abilityId, boolean notifyFeedback) {
        if (abilityId == null || abilityId.isEmpty()) return;
        CatAbility ability = CatAbilities.get(abilityId);
        if (ability == null) return;

        long lastCastTime = cat.getAbilityCooldown(ability.getId());
        long currentTime = cat.level().getGameTime();
        if (currentTime < lastCastTime + ability.getCooldownTicks()) {
            if (notifyFeedback) {
                float rem = (lastCastTime + ability.getCooldownTicks() - currentTime) / 20.0F;
                sender.displayClientMessage(Component.literal(ability.getDisplayName() + " recharging (" + String.format("%.1fs", rem) + ")").withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        if (FelineEnergyEngine.getEnergy(cat) < ability.getEnergyCost()) {
            if (notifyFeedback) {
                sender.displayClientMessage(Component.literal("Not enough Energy for " + ability.getDisplayName() + "! (Need " + ability.getEnergyCost() + "⚡)").withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        boolean cast = ability.tryCast(cat, sender);
        if (cast && notifyFeedback) {
            sender.displayClientMessage(Component.literal("✦ Cast " + ability.getDisplayName() + "! ✦").withStyle(ChatFormatting.AQUA), true);
        }
    }
}
