package com.kittytalents.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kittytalents.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Data-driven recipe for the Treat Oven (type: "kittytalents:treat_baking").
 */
public class TreatBakingRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final int cookTime;
    private final float experience;

    public TreatBakingRecipe(ResourceLocation id, NonNullList<Ingredient> ingredients, ItemStack result, int cookTime, float experience) {
        this.id = id;
        this.ingredients = ingredients;
        this.result = result;
        this.cookTime = Math.max(20, cookTime);
        this.experience = experience;
    }

    public int getCookTime() { return cookTime; }
    public float getExperience() { return experience; }
    public ItemStack getResult() { return result; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    /**
     * Checks if inputs in slots 0..2 of the container satisfy all recipe ingredients (shapeless).
     */
    @Override
    public boolean matches(Container container, Level level) {
        List<ItemStack> inputs = new ArrayList<>();
        int slotCount = Math.min(3, container.getContainerSize());
        for (int i = 0; i < slotCount; i++) {
            ItemStack s = container.getItem(i);
            if (!s.isEmpty()) inputs.add(s);
        }
        if (inputs.size() != ingredients.size()) return false;

        boolean[] matched = new boolean[inputs.size()];
        for (Ingredient ing : ingredients) {
            boolean found = false;
            for (int i = 0; i < inputs.size(); i++) {
                if (!matched[i] && ing.test(inputs.get(i))) {
                    matched[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return result;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TREAT_BAKING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.TREAT_BAKING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<TreatBakingRecipe> {
        @Override
        public TreatBakingRecipe fromJson(ResourceLocation id, JsonObject json) {
            JsonArray ingArr = GsonHelper.getAsJsonArray(json, "ingredients");
            NonNullList<Ingredient> ingredients = NonNullList.create();
            for (JsonElement el : ingArr) {
                Ingredient ing = Ingredient.fromJson(el);
                if (!ing.isEmpty()) ingredients.add(ing);
            }
            ItemStack result = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true, true);
            int cookTime = GsonHelper.getAsInt(json, "cook_time", 160);
            float exp = GsonHelper.getAsFloat(json, "experience", 0.35F);
            return new TreatBakingRecipe(id, ingredients, result, cookTime, exp);
        }

        @Override
        public @Nullable TreatBakingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            for (int i = 0; i < size; i++) {
                ingredients.set(i, Ingredient.fromNetwork(buf));
            }
            ItemStack result = buf.readItem();
            int cookTime = buf.readVarInt();
            float exp = buf.readFloat();
            return new TreatBakingRecipe(id, ingredients, result, cookTime, exp);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, TreatBakingRecipe recipe) {
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ing : recipe.ingredients) {
                ing.toNetwork(buf);
            }
            buf.writeItem(recipe.result);
            buf.writeVarInt(recipe.cookTime);
            buf.writeFloat(recipe.experience);
        }
    }
}
