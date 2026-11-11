package com.kittytalents;

import com.kittytalents.network.PacketHandler;
import com.kittytalents.registry.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(KittyTalents.MODID)
public class KittyTalents {
    public static final String MODID = "kittytalents";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public KittyTalents() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register registries
        ModEntities.ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModRecipes.TYPES.register(modEventBus);

        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                com.kittytalents.config.KittyTalentsConfig.COMMON_SPEC
        );
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                com.kittytalents.config.KittyTalentsClientConfig.SPEC
        );

        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            PacketHandler.register();
            com.kittytalents.compat.tacz.TacZCompat.isLoaded();
            com.kittytalents.compat.curios.CuriosCompat.init();
            // GameTest server only: a stand-in addon that adds a mode and a job through the API.
            if (net.minecraftforge.gametest.ForgeGameTestHooks.isGametestServer()) com.kittytalents.test.TestAddon.register();
        });
        LOGGER.info("Kitty Talents initialized successfully!");
    }
}
