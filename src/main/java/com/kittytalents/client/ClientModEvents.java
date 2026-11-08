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
            MenuScreens.register(ModMenus.KITTY_WORK_STATION_MENU.get(), com.kittytalents.client.gui.KittyWorkStationScreen::new);
            MenuScreens.register(ModMenus.TREAT_OVEN_MENU.get(), com.kittytalents.client.gui.TreatOvenScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TALENTED_CAT.get(), TalentedCatRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_TOY.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.FELINE_HISS_PROJECTILE.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.kittytalents.client.model.KTCatModel.MAIN, com.kittytalents.client.model.KTCatModel::createCatLayer);
        event.registerLayerDefinition(com.kittytalents.client.model.KTCatModel.COLLAR, com.kittytalents.client.model.KTCatModel::createCollarLayer);
        event.registerLayerDefinition(com.kittytalents.client.model.KTCatModel.ARMOR_OUTER, () -> com.kittytalents.client.model.KTCatModel.createArmorLayer(0.3F));
        event.registerLayerDefinition(com.kittytalents.client.model.KTCatModel.ARMOR_INNER, () -> com.kittytalents.client.model.KTCatModel.createArmorLayer(0.2F));
    }

    @SubscribeEvent
    public static void registerReloadListeners(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(com.kittytalents.client.skin.SkinRegistry.INSTANCE);
    }

    public static void openWhistleScreen() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new com.kittytalents.client.gui.KittyWhistleScreen());
    }

    @SubscribeEvent
    public static void registerKeyMappings(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        ClientTwineHandler.registerKeys(event);
    }
}
