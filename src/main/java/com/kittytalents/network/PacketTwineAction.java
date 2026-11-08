package com.kittytalents.network;

import com.kittytalents.core.perm.CatAction;
import com.kittytalents.core.perm.CatPermissions;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.TwineManager;
import com.kittytalents.entity.TwineMoves;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * Client -> server discrete Twine actions: start / stop a session, dodge, team synergy, traversal, tool use, gun reload
 * and aim. Starting needs {@link CatAction#TWINE_START}; everything else needs {@link CatAction#TWINE_CONTROL} (the
 * sender must be twined with exactly this cat). A control action for a cat the server doesn't consider twined tells
 * the client to end its session, so a desynced client can't stay stuck in the cat's view.
 */
public class PacketTwineAction extends PacketBase {

    public enum ActionType {
        TWINE_START,
        TWINE_STOP,
        DODGE,
        TEAM_SYNERGY,
        TRAVERSAL_ACTION,
        INTERACT,
        RELOAD_GUN,
        AIM_GUN
    }

    private final int catEntityId;
    /** Null when the ordinal on the wire was unknown (the packet is then ignored). */
    @Nullable
    private final ActionType actionType;
    private final float actionData;

    public PacketTwineAction(int catEntityId, ActionType actionType, float actionData) {
        this.catEntityId = catEntityId;
        this.actionType = actionType;
        this.actionData = actionData;
    }

    public PacketTwineAction(int catEntityId, ActionType actionType) {
        this(catEntityId, actionType, 0.0F);
    }

    public PacketTwineAction(int catEntityId, boolean twine) {
        this(catEntityId, twine ? ActionType.TWINE_START : ActionType.TWINE_STOP, 0.0F);
    }

    public PacketTwineAction(FriendlyByteBuf buf) {
        this.catEntityId = buf.readInt();
        byte ord = buf.readByte();
        this.actionType = (ord >= 0 && ord < ActionType.values().length) ? ActionType.values()[ord] : null;
        this.actionData = buf.readFloat();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(catEntityId);
        buf.writeByte(actionType == null ? -1 : actionType.ordinal());
        buf.writeFloat(actionData);
    }

    public int getCatEntityId() {
        return catEntityId;
    }

    @Nullable
    public ActionType getActionType() {
        return actionType;
    }

    public float getActionData() {
        return actionData;
    }

    @Override
    protected void handleServer(ServerPlayer sender) {
        if (actionType != null) apply(sender, catEntityId, actionType, actionData);
    }

    /** @return true if the action was accepted (not necessarily that e.g. a dodge had enough energy). */
    public static boolean apply(ServerPlayer sender, int catEntityId, ActionType actionType, float actionData) {
        if (actionType == null || !Float.isFinite(actionData)) return false;

        if (actionType == ActionType.TWINE_STOP) {
            // Always allowed: it only ever ends the sender's own session.
            TwineManager.endForPlayer(sender, "Untwined from your cat companion.");
            return true;
        }
        if (actionType == ActionType.TWINE_START) return start(sender, catEntityId);

        CatPermissions.Access access = CatPermissions.check(sender, catEntityId, CatAction.TWINE_CONTROL);
        if (!access.allowed()) {
            if (access.denial() == CatPermissions.Denial.NOT_TWINED && !TwineManager.isPlayerTwined(sender.getUUID())) {
                // The client thinks it's twined with this cat but the server doesn't: end the client's session.
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sender), new PacketSyncTwine(catEntityId, false));
            }
            return false;
        }
        TalentedCat cat = access.cat();
        int intData = (int) actionData;
        switch (actionType) {
            case DODGE -> {
                if (intData < 0 || intData > 3) return false;
                TwineMoves.executeDodge(cat, sender, intData);
            }
            case TEAM_SYNERGY -> TwineMoves.triggerTeamSynergy(cat, sender);
            case TRAVERSAL_ACTION -> {
                if (intData < 0 || intData > 4) return false;
                TwineMoves.executeTraversalAction(cat, sender, intData);
            }
            case INTERACT -> cat.tryUseEquippedTool();
            case RELOAD_GUN -> {
                if (cat.hasEquippedGun()) com.kittytalents.compat.tacz.TacZCompat.triggerReload(cat, cat.getEquippedWeapon());
            }
            case AIM_GUN -> {
                if (cat.hasEquippedGun()) com.kittytalents.compat.tacz.TacZCompat.setAiming(cat, actionData > 0.5F);
            }
            default -> { return false; }
        }
        return true;
    }

    private static boolean start(ServerPlayer sender, int catEntityId) {
        CatPermissions.Access access = CatPermissions.check(sender, catEntityId, CatAction.TWINE_START);
        TalentedCat cat = access.cat();
        if (!access.allowed()) {
            if (access.denial() == CatPermissions.Denial.NOT_ALLOWED) {
                sender.displayClientMessage(Component.literal("You do not have permission to Twine with this cat!").withStyle(ChatFormatting.RED), true);
            } else if (access.denial() == CatPermissions.Denial.TOO_FAR) {
                sender.displayClientMessage(Component.literal("Cat is too far away to Twine!").withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        if (cat.isIncapacitated()) {
            sender.displayClientMessage(Component.literal("Cannot Twine with an incapacitated cat!").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (cat.isTwined() && !cat.isTwinedWith(sender)) {
            sender.displayClientMessage(Component.literal("Another player is already controlling this cat!").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (cat.isVehicle() || cat.isPassenger()) {
            sender.displayClientMessage(Component.literal("Your cat can't be twined while riding or being ridden.").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!sender.isAlive() || sender.isSpectator() || sender.isSleeping()) return false;
        return TwineManager.start(sender, cat);
    }
}
