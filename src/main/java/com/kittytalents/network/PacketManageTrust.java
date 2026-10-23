package com.kittytalents.network;

import com.kittytalents.entity.TalentedCat;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class PacketManageTrust {
    private final int entityId;
    private final int action; // 0 = ADD by username, 1 = REMOVE by UUID string
    private final String payload;

    public PacketManageTrust(int entityId, int action, String payload) {
        this.entityId = entityId;
        this.action = action;
        this.payload = payload;
    }

    public PacketManageTrust(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.action = buf.readInt();
        this.payload = buf.readUtf(128);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(action);
        buf.writeUtf(payload);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                Entity entity = sender.level().getEntity(entityId);
                if (entity instanceof TalentedCat cat && cat.distanceToSqr(sender) <= 64.0D * 64.0D) {
                    boolean isOwner = cat.isPrimaryOwner(sender);

                    if (action == 0) {
                        // ADD by username (Only owner can add trusted players)
                        if (!isOwner) {
                            sender.sendSystemMessage(Component.literal("Only the owner can grant trust for this cat!").withStyle(ChatFormatting.RED));
                            return;
                        }

                        String targetName = payload.trim();
                        if (targetName.isEmpty()) return;

                        if (targetName.equalsIgnoreCase(sender.getGameProfile().getName())) {
                            sender.sendSystemMessage(Component.translatable("message.kittytalents.trust_cannot_self").withStyle(ChatFormatting.YELLOW));
                            return;
                        }

                        ServerPlayer targetPlayer = sender.server.getPlayerList().getPlayerByName(targetName);
                        UUID targetUuid = null;
                        String actualName = targetName;

                        if (targetPlayer != null) {
                            targetUuid = targetPlayer.getUUID();
                            actualName = targetPlayer.getGameProfile().getName();
                        } else {
                            Optional<GameProfile> cached = sender.server.getProfileCache().get(targetName);
                            if (cached.isPresent()) {
                                targetUuid = cached.get().getId();
                                actualName = cached.get().getName();
                            }
                        }

                        if (targetUuid == null) {
                            sender.sendSystemMessage(Component.translatable("message.kittytalents.trust_player_not_found", targetName).withStyle(ChatFormatting.RED));
                            sender.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
                            return;
                        }

                        if (cat.isTrusted(targetUuid)) {
                            sender.sendSystemMessage(Component.translatable("message.kittytalents.trust_already_trusted", actualName).withStyle(ChatFormatting.YELLOW));
                            return;
                        }

                        cat.addTrustedPlayer(targetUuid, actualName);
                        sender.playSound(SoundEvents.PLAYER_LEVELUP, 0.7F, 1.8F);
                        sender.sendSystemMessage(Component.translatable("message.kittytalents.trust_added", actualName, cat.getName().getString()).withStyle(ChatFormatting.GREEN));

                        if (targetPlayer != null) {
                            targetPlayer.sendSystemMessage(Component.translatable("message.kittytalents.trust_received", cat.getName().getString(), sender.getName().getString()).withStyle(ChatFormatting.GOLD));
                            targetPlayer.playSound(SoundEvents.CAT_PURREOW, 1.0F, 1.0F);
                        }

                        cat.syncTrust();

                    } else if (action == 1) {
                        // REMOVE by UUID
                        try {
                            UUID targetUuid = UUID.fromString(payload.trim());
                            boolean isSelfRevoke = sender.getUUID().equals(targetUuid);

                            if (!isOwner && !isSelfRevoke) {
                                sender.sendSystemMessage(Component.literal("Only the owner can revoke trust!").withStyle(ChatFormatting.RED));
                                return;
                            }

                            String removedName = cat.getTrustedPlayers().getOrDefault(targetUuid, "Player");
                            cat.removeTrustedPlayer(targetUuid);
                            sender.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 0.8F);
                            sender.sendSystemMessage(Component.translatable("message.kittytalents.trust_removed", removedName).withStyle(ChatFormatting.YELLOW));

                            cat.syncTrust();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
