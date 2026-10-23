package com.kittytalents.client;

import com.kittytalents.KittyTalents;
import com.kittytalents.client.gui.CatPackScreen;
import com.kittytalents.client.gui.CatTalentScreen;
import com.kittytalents.client.renderer.TalentedCatRenderer;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = KittyTalents.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.CAT_TALENT_MENU.get(), CatTalentScreen::new);
            MenuScreens.register(ModMenus.CAT_PACK_MENU.get(), CatPackScreen::new);
            MenuScreens.register(ModMenus.CAT_FOOD_BOWL_MENU.get(), com.kittytalents.client.gui.CatFoodBowlScreen::new);
            MenuScreens.register(ModMenus.KITTY_HABITAT_MENU.get(), com.kittytalents.client.gui.KittyHabitatScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TALENTED_CAT.get(), TalentedCatRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_TOY.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.FELINE_HISS_PROJECTILE.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }

    public static void openWhistleScreen() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new com.kittytalents.client.gui.KittyWhistleScreen());
    }

    @SubscribeEvent
    public static void registerKeyMappings(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        ClientTwineHandler.registerKeys(event);
    }
}
