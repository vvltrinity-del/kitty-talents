package com.kittytalents.registry;

import com.kittytalents.KittyTalents;
import com.kittytalents.recipe.HabitatProductionRecipe;
import com.kittytalents.recipe.TreatBakingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, KittyTalents.MODID);

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, KittyTalents.MODID);

    public static final RegistryObject<RecipeType<HabitatProductionRecipe>> HABITAT_PRODUCTION_TYPE =
            TYPES.register("habitat_production", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return KittyTalents.MODID + ":habitat_production";
                }
            });

    public static final RegistryObject<RecipeSerializer<HabitatProductionRecipe>> HABITAT_PRODUCTION_SERIALIZER =
            SERIALIZERS.register("habitat_production", HabitatProductionRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<TreatBakingRecipe>> TREAT_BAKING_TYPE =
            TYPES.register("treat_baking", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return KittyTalents.MODID + ":treat_baking";
                }
            });

    public static final RegistryObject<RecipeSerializer<TreatBakingRecipe>> TREAT_BAKING_SERIALIZER =
            SERIALIZERS.register("treat_baking", TreatBakingRecipe.Serializer::new);
}
