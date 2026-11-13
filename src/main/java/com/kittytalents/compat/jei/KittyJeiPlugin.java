package com.kittytalents.compat.jei;

import com.kittytalents.KittyTalents;
import com.kittytalents.registry.ModBlocks;
import com.kittytalents.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class KittyJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = new ResourceLocation(KittyTalents.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(mezz.jei.api.registration.IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new KittyJeiCategories.HabitatCategory(gui), new KittyJeiCategories.BreedingCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 5.0: Habitat output per tier (and data-driven Habitat recipes), and every breeding outcome.
        registration.addRecipes(KittyJeiCategories.HABITAT, KittyJeiCategories.habitatEntries());
        registration.addRecipes(KittyJeiCategories.BREEDING, KittyJeiCategories.breedEntries());
        registration.addIngredientInfo(
                ModItems.KITTY_WORK_CLIPBOARD.get(),
                Component.translatable("jei.kittytalents.kitty_work_clipboard.info")
        );
        registration.addIngredientInfo(
                ModItems.KITTY_HABITAT.get(),
                Component.translatable("jei.kittytalents.kitty_habitat.info")
        );
        registration.addIngredientInfo(
                ModItems.KITTY_WORK_STATION.get(),
                Component.translatable("jei.kittytalents.kitty_work_station.info")
        );
        registration.addIngredientInfo(
                ModItems.TREAT_OVEN.get(),
                Component.translatable("jei.kittytalents.treat_oven.info")
        );
        registration.addIngredientInfo(
                ModItems.KITTY_WHISTLE.get(),
                Component.translatable("jei.kittytalents.kitty_whistle.info")
        );
        registration.addIngredientInfo(
                ModItems.CATNIP_TREAT.get(),
                Component.translatable("jei.kittytalents.treats.info")
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.TREAT_OVEN.get()), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(new ItemStack(ModItems.KITTY_HABITAT.get()), KittyJeiCategories.HABITAT);
        registration.addRecipeCatalyst(new ItemStack(ModItems.BREEDING_CATNIP.get()), KittyJeiCategories.BREEDING);
    }
}
