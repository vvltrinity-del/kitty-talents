package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps the GameTest world deterministic: no natural mob spawns (a stray witch or zombie walking into an AI test made
 * target assertions flaky) and a fixed daytime. Only applies to the GameTest server, never to real worlds.
 */
@Mod.EventBusSubscriber(modid = KittyTalents.MODID)
public final class GameTestWorldSetup {
    private GameTestWorldSetup() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!(event.getServer() instanceof GameTestServer server)) return;
        GameRules rules = server.getGameRules();
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        server.overworld().setDayTime(6000L);
        KittyTalents.LOGGER.info("[GameTest] mob spawning off, time fixed at noon");
    }
}
