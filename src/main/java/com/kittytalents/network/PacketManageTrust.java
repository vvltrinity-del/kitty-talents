package com.kittytalents.network;

import com.kittytalents.core.perm.CatAction;
import com.kittytalents.core.perm.CatPermissions;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

/**
 * Client -> server: add a co-owner by name, or remove one by UUID.
 * Only the owner can add or remove co-owners; a co-owner may remove themselves. New co-owners must be real players
 * who are online right now (decided 2026-11-07).
 */
public class PacketManageTrust extends PacketBase {
    public static final int ADD = 0;
    public static final int REMOVE = 1;

    private final int entityId;
    private final int action;
    private final String payload;

    public PacketManageTrust(int entityId, int action, String payload) {
        this.entityId = entityId;
        this.action = action;
        this.payload = payload == null ? "" : payload;
    }

    public PacketManageTrust(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.action = buf.readInt();
        this.payload = buf.readUtf(128);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(action);
        buf.writeUtf(payload, 128);
    }

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender, entityId, action, payload);
    }

    /** @return true if the trust list changed. */
    public static boolean apply(ServerPlayer sender, int entityId, int action, String payload) {
        if (action != ADD && action != REMOVE) return false;
        if (action == ADD) return add(sender, entityId, payload);
        UUID target;
        try {
            target = UUID.fromString(payload.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        return remove(sender, entityId, target);
    }

    private static boolean add(ServerPlayer sender, int entityId, String rawName) {
        String targetName = cleanText(rawName, 16);
        if (targetName.isEmpty() || !targetName.matches("\\S{1,16}")) return false;
        CatPermissions.Access access = CatPermissions.check(sender, entityId, CatAction.MANAGE_TRUST);
        if (!access.allowed()) {
            if (access.denial() == CatPermissions.Denial.NOT_ALLOWED && access.cat() != null) {
                sender.sendSystemMessage(Component.literal("Only the owner can grant trust for this cat!").withStyle(ChatFormatting.RED));
                access.cat().syncTrust(sender);
            } else {
                explain(sender, access, CatAction.MANAGE_TRUST);
            }
            return false;
        }
        TalentedCat cat = access.cat();

        if (targetName.equalsIgnoreCase(sender.getGameProfile().getName())) {
            sender.sendSystemMessage(Component.translatable("message.kittytalents.trust_cannot_self").withStyle(ChatFormatting.YELLOW));
            return false;
        }

        // Only a real player who is online right now can be made a co-owner: no offline names from the
        // profile cache, and no FakePlayers (machines/automation mods acting as players).
        ServerPlayer targetPlayer = sender.server.getPlayerList().getPlayerByName(targetName);
        if (targetPlayer == null || targetPlayer instanceof FakePlayer) {
            sender.sendSystemMessage(Component.translatable("message.kittytalents.trust_player_not_found", targetName).withStyle(ChatFormatting.RED));
            sender.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
            return false;
        }
        UUID targetUuid = targetPlayer.getUUID();
        String actualName = targetPlayer.getGameProfile().getName();
        if (targetUuid.equals(cat.getOwnerUUID())) return false;
        if (cat.isTrusted(targetUuid)) {
            sender.sendSystemMessage(Component.translatable("message.kittytalents.trust_already_trusted", actualName).withStyle(ChatFormatting.YELLOW));
            return false;
        }

        cat.addTrustedPlayer(targetUuid, actualName);
        sender.playSound(SoundEvents.PLAYER_LEVELUP, 0.7F, 1.8F);
        sender.sendSystemMessage(Component.translatable("message.kittytalents.trust_added", actualName, cat.getName().getString()).withStyle(ChatFormatting.GREEN));
        targetPlayer.sendSystemMessage(Component.translatable("message.kittytalents.trust_received", cat.getName().getString(), sender.getName().getString()).withStyle(ChatFormatting.GOLD));
        targetPlayer.playSound(SoundEvents.CAT_PURREOW, 1.0F, 1.0F);
        cat.syncTrust();
        return true;
    }

    private static boolean remove(ServerPlayer sender, int entityId, UUID targetUuid) {
        boolean selfRevoke = sender.getUUID().equals(targetUuid);
        CatPermissions.Access access = CatPermissions.check(sender, entityId, CatAction.MANAGE_TRUST);
        TalentedCat cat = access.cat();
        // A co-owner leaving on their own is the one exception to "owner only".
        boolean allowed = access.allowed()
                || (selfRevoke && access.denial() == CatPermissions.Denial.NOT_ALLOWED && cat != null && cat.isTrusted(sender));
        if (!allowed) {
            if (access.denial() == CatPermissions.Denial.NOT_ALLOWED && cat != null) {
                sender.sendSystemMessage(Component.literal("Only the owner can revoke trust!").withStyle(ChatFormatting.RED));
                cat.syncTrust(sender);
            } else {
                explain(sender, access, CatAction.MANAGE_TRUST);
            }
            return false;
        }
        if (!cat.isTrusted(targetUuid)) {
            cat.syncTrust(sender);
            return false;
        }

        String removedName = cat.getTrustedPlayers().getOrDefault(targetUuid, "Player");
        cat.removeTrustedPlayer(targetUuid);
        sender.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 0.8F);
        sender.sendSystemMessage(Component.translatable("message.kittytalents.trust_removed", removedName).withStyle(ChatFormatting.YELLOW));
        ServerPlayer removed = sender.server.getPlayerList().getPlayer(targetUuid);
        if (removed != null && !selfRevoke) {
            removed.sendSystemMessage(Component.translatable("message.kittytalents.trust_revoked", cat.getName().getString()).withStyle(ChatFormatting.YELLOW));
        }
        // A removed co-owner who is twined with this cat loses control right away.
        if (cat.getTwinedPlayerUUID() != null && cat.getTwinedPlayerUUID().equals(targetUuid)) {
            com.kittytalents.entity.TwineManager.endForCat(cat, "You are no longer a co-owner of this cat.");
        }
        cat.syncTrust();
        return true;
    }
}
