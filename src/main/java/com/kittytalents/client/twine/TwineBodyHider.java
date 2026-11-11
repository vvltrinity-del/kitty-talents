package com.kittytalents.client.twine;

import com.kittytalents.network.PacketSyncTwineBodies;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** A Twined player's dormant body is not drawn at all: no model, armor, held items, cape or name tag. */
@Mod.EventBusSubscriber(modid = "kittytalents", value = Dist.CLIENT)
public final class TwineBodyHider {
    private TwineBodyHider() {}

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (!PacketSyncTwineBodies.CLIENT_HIDDEN.contains(event.getEntity().getUUID())) return;
        event.setCanceled(true);
        // Other players get a small marker where the body rests, so they know someone is "in" a cat here.
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (TwineMarkers.enabled() && event.getEntity() != mc.player && mc.player != null && event.getEntity().distanceToSqr(mc.player) < 32 * 32) {
            TwineMarkers.label(event.getPoseStack(), event.getMultiBufferSource(),
                    net.minecraft.network.chat.Component.translatable("hud.kittytalents.twine_body", event.getEntity().getGameProfile().getName()),
                    1.0F, event.getPackedLight());
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        PacketSyncTwineBodies.CLIENT_HIDDEN.clear();
        PacketSyncTwineBodies.CLIENT_CAT_TO_PLAYER.clear();
    }
}
