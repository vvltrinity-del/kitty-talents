package com.kittytalents.registry;

import com.kittytalents.KittyTalents;
import com.kittytalents.inventory.CatPackMenu;
import com.kittytalents.inventory.CatTalentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, KittyTalents.MODID);

    public static final RegistryObject<MenuType<CatTalentMenu>> CAT_TALENT_MENU =
            MENUS.register("cat_talent_menu", () -> IForgeMenuType.create(CatTalentMenu::new));

    public static final RegistryObject<MenuType<CatPackMenu>> CAT_PACK_MENU =
            MENUS.register("cat_pack_menu", () -> IForgeMenuType.create(CatPackMenu::new));

    public static final RegistryObject<MenuType<com.kittytalents.inventory.CatFoodBowlMenu>> CAT_FOOD_BOWL_MENU =
            MENUS.register("cat_food_bowl_menu", () -> IForgeMenuType.create(com.kittytalents.inventory.CatFoodBowlMenu::new));

    public static final RegistryObject<MenuType<com.kittytalents.inventory.KittyHabitatMenu>> KITTY_HABITAT_MENU =
            MENUS.register("kitty_habitat_menu", () -> IForgeMenuType.create(com.kittytalents.inventory.KittyHabitatMenu::new));
}
