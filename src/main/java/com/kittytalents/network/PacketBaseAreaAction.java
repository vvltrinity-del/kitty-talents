package com.kittytalents.network;

import com.kittytalents.core.base.BaseAreas;
import com.kittytalents.item.BasePinItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Client -> server (5.0): Base Pin menu actions on one of the sender's own areas. */
public class PacketBaseAreaAction extends PacketBase {
    public static final int RENAME = 0, DELETE = 1, MERGE = 2, RESIZE = 3;

    private final int op, id;
    private final String text;

    public PacketBaseAreaAction(int op, int id, String text) {
        this.op = op;
        this.id = id;
        this.text = text == null ? "" : text;
    }

    public PacketBaseAreaAction(FriendlyByteBuf buf) {
        this.op = buf.readVarInt();
        this.id = buf.readVarInt();
        this.text = buf.readUtf(32);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(op);
        buf.writeVarInt(id);
        buf.writeUtf(text, 32);
    }

    @Override
    protected void handleServer(ServerPlayer sender) {
        apply(sender, op, id, text);
    }

    /** Acts only on the sender's own areas (the id is looked up in their list). */
    public static boolean apply(ServerPlayer sender, int op, int id, String text) {
        BaseAreas bases = BaseAreas.get(sender.server);
        if (bases.of(sender.getUUID()).stream().noneMatch(a -> a.id() == id)) return false;
        boolean ok = switch (op) {
            case RENAME -> bases.rename(sender.getUUID(), id, cleanText(text, 24));
            case DELETE -> bases.delete(sender.getUUID(), id);
            case MERGE -> {
                int n = bases.merge(sender.getUUID(), id);
                sender.displayClientMessage(Component.translatable(n > 0 ? "message.kittytalents.base_pin.merged" : "message.kittytalents.base_pin.nothing_to_merge", n)
                        .withStyle(n > 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
                yield n > 0;
            }
            case RESIZE -> {
                ItemStack pin = pinIn(sender);
                if (pin.isEmpty()) yield false;
                pin.getOrCreateTag().putInt("ResizeId", id);
                pin.getOrCreateTag().remove("CornerA");
                sender.displayClientMessage(Component.translatable("message.kittytalents.base_pin.resize_hint").withStyle(ChatFormatting.AQUA), true);
                yield true;
            }
            default -> false;
        };
        PacketSyncBaseAreas.send(sender, false);
        return ok;
    }

    private static ItemStack pinIn(ServerPlayer p) {
        for (InteractionHand h : InteractionHand.values()) {
            if (p.getItemInHand(h).getItem() instanceof BasePinItem) return p.getItemInHand(h);
        }
        return ItemStack.EMPTY;
    }
}
