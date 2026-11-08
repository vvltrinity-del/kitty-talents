package com.kittytalents.network;

import com.kittytalents.KittyTalents;
import com.kittytalents.core.perm.CatAction;
import com.kittytalents.core.perm.CatPermissions;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Base for every Kitty Talents packet: decode (constructor) → validate → authorize ({@link CatPermissions}) → apply → sync.
 * <p>
 * Client-to-server packets implement {@link #handleServer}; it runs on the server thread and only for a real sender.
 * Client-bound packets implement {@link #handleClient}. Packets that arrive on the wrong side are dropped.
 * <p>
 * Server handlers should be written as a public static {@code apply(sender, ...)} that returns whether the request
 * was applied, so GameTests can feed it malicious values directly.
 */
public abstract class PacketBase {

    public abstract void toBytes(FriendlyByteBuf buf);

    /** Server side, main thread. {@code sender} is never null. */
    protected void handleServer(ServerPlayer sender) {
        KittyTalents.LOGGER.debug("[KT net] {} is client-bound; dropped a copy sent by {}", getClass().getSimpleName(), sender.getGameProfile().getName());
    }

    /** Client side, main thread. Implementations call into client-only classes through DistExecutor. */
    protected void handleClient() {
    }

    public final void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        boolean toServer = ctx.getDirection().getReceptionSide().isServer();
        ctx.enqueueWork(() -> {
            if (toServer) {
                ServerPlayer sender = ctx.getSender();
                if (sender != null) handleServer(sender);
            } else {
                handleClient();
            }
        });
        ctx.setPacketHandled(true);
    }

    // ------------------------------------------------------------------ shared validation helpers

    /**
     * Resolves and authorizes the cat named by a packet. Returns the cat when allowed, otherwise null after telling
     * the sender why (only for refusals a normal player can hit; tampered ids are dropped silently).
     */
    @Nullable
    public static TalentedCat authorize(ServerPlayer sender, int entityId, CatAction action) {
        CatPermissions.Access access = CatPermissions.check(sender, entityId, action);
        if (access.allowed()) return access.cat();
        explain(sender, access, action);
        return null;
    }

    static void explain(ServerPlayer sender, CatPermissions.Access access, CatAction action) {
        if (access.denial() == null) return;
        switch (access.denial()) {
            case NOT_ALLOWED -> {
                if (access.cat() != null && CatPermissions.roleOf(access.cat(), sender) == CatPermissions.Role.TRUSTED) {
                    sender.displayClientMessage(Component.translatable("message.kittytalents.perm.owner_only").withStyle(ChatFormatting.RED), true);
                }
            }
            case TOO_FAR -> sender.displayClientMessage(Component.translatable("message.kittytalents.perm.too_far").withStyle(ChatFormatting.RED), true);
            default -> { }
        }
        KittyTalents.LOGGER.debug("[KT net] refused {} from {}: {}", action, sender.getGameProfile().getName(), access.denial());
    }

    /** Server strings: trimmed, no formatting codes or control characters, at most {@code maxLen} characters. */
    public static String cleanText(@Nullable String raw, int maxLen) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder(Math.min(raw.length(), maxLen));
        for (int i = 0; i < raw.length() && sb.length() < maxLen; i++) {
            char c = raw.charAt(i);
            if (c == '§') { i++; continue; } // drop the formatting code and its letter ("§cBob" -> "Bob")
            if (Character.isISOControl(c)) continue;
            sb.append(c);
        }
        return sb.toString().trim();
    }
}
