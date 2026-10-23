package com.kittytalents.network;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.TwineManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Handles client-to-server discrete Twine actions:
 * Start/Stop session, 4-way Dodge, Team Synergy triggering, and Traversal actions.
 */
public class PacketTwineAction {

    public enum ActionType {
        TWINE_START(0),
        TWINE_STOP(1),
        DODGE(2),
        TEAM_SYNERGY(3),
        TRAVERSAL_ACTION(4),
        INTERACT(5);

        private final int id;

        ActionType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    private final int catEntityId;
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

    /**
     * Backward-compatible constructor for existing codebase callers.
     */
    public PacketTwineAction(int catEntityId, boolean twine) {
        this(catEntityId, twine ? ActionType.TWINE_START : ActionType.TWINE_STOP, 0.0F);
    }

    public PacketTwineAction(FriendlyByteBuf buf) {
        this.catEntityId = buf.readInt();
        if (buf.readableBytes() == 1) {
            // Backward-compatible read for legacy 1-byte boolean
            boolean legacyTwine = buf.readBoolean();
            this.actionType = legacyTwine ? ActionType.TWINE_START : ActionType.TWINE_STOP;
            this.actionData = 0.0F;
        } else {
            byte ord = buf.readByte();
            this.actionType = (ord >= 0 && ord < ActionType.values().length) ? ActionType.values()[ord] : ActionType.TWINE_STOP;
            this.actionData = buf.readableBytes() >= 4 ? buf.readFloat() : 0.0F;
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(catEntityId);
        buf.writeByte(actionType.ordinal());
        buf.writeFloat(actionData);
    }

    public int getCatEntityId() {
        return catEntityId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public float getActionData() {
        return actionData;
    }

    public boolean isTwine() {
        return actionType == ActionType.TWINE_START;
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            if (actionType == ActionType.TWINE_STOP) {
                // Untwine: safe untwine for player linked session
                TwineManager.endForPlayer(sender, "Untwined from your cat companion.");
                return;
            }

            Entity entity = sender.level().getEntity(catEntityId);
            if (!(entity instanceof TalentedCat cat)) return;

            switch (actionType) {
                case TWINE_START -> {
                    if (!cat.isOwnedBy(sender) && !cat.isTrusted(sender)) {
                        sender.displayClientMessage(Component.literal("You do not have permission to Twine with this cat!").withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    if (cat.isIncapacitated()) {
                        sender.displayClientMessage(Component.literal("Cannot Twine with an incapacitated cat!").withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    if (cat.distanceToSqr(sender) > 64.0D * 64.0D) {
                        sender.displayClientMessage(Component.literal("Cat is too far away to Twine!").withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    if (cat.isTwined() && !cat.isTwinedWith(sender)) {
                        sender.displayClientMessage(Component.literal("Another player is already controlling this cat!").withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    if (TwineManager.isPlayerTwined(sender.getUUID())) {
                        TwineManager.endForPlayer(sender, null);
                    }

                    cat.setTwinedPlayer(sender);
                    TwineManager.register(sender.getUUID(), cat.getUUID());
                    TwineManager.recordDormantBody(sender);
                    TwineManager.hideDormantBody(sender);
                    cat.setMaxUpStep(1.0F);

                    PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sender),
                            new PacketSyncTwine(catEntityId, true));
                    sender.playNotifySound(com.kittytalents.registry.ModSounds.TWINE_ENTER.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                    sender.displayClientMessage(Component.literal("✦ Twined with " + cat.getName().getString() + "! Controlling companion. ✦").withStyle(ChatFormatting.GREEN), true);
                }
                case DODGE -> {
                    if (cat.isTwinedWith(sender)) {
                        TwineManager.executeDodge(cat, sender, (int) actionData);
                    }
                }
                case TEAM_SYNERGY -> {
                    if (cat.isTwinedWith(sender)) {
                        TwineManager.triggerTeamSynergy(cat, sender);
                    }
                }
                case TRAVERSAL_ACTION -> {
                    if (cat.isTwinedWith(sender)) {
                        TwineManager.executeTraversalAction(cat, sender, actionData);
                    }
                }
                case INTERACT -> {
                    if (cat.isTwinedWith(sender)) {
                        cat.tryUseEquippedTool();
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
