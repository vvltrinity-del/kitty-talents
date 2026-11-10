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
        if (PacketSyncTwineBodies.CLIENT_HIDDEN.contains(event.getEntity().getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        PacketSyncTwineBodies.CLIENT_HIDDEN.clear();
    }
}
