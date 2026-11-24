package com.kittytalents.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kittytalents.entity.CatVariant;
import com.kittytalents.entity.ProductionTier;
import com.kittytalents.registry.ModRecipes;
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
 * Data-driven recipe for the Kitty Production Habitat (type: "kittytalents:habitat_production").
 */
public class HabitatProductionRecipe implements Recipe<Container> {
    public record WeightedOutput(ItemStack stack, float chance) {}

    private final ResourceLocation id;
    private final String tierId;
    private final String variantId;
    private final Ingredient catalyst;
    private final int duration;
    private final List<WeightedOutput> outputs;

    public HabitatProductionRecipe(ResourceLocation id, String tierId, String variantId,
                                   Ingredient catalyst, int duration, List<WeightedOutput> outputs) {
        this.id = id;
        this.tierId = tierId != null ? tierId : "";
        this.variantId = variantId != null ? variantId : "";
        this.catalyst = catalyst != null ? catalyst : Ingredient.EMPTY;
        this.duration = Math.max(20, duration);
        this.outputs = outputs;
    }

    public String getTierId() { return tierId; }
    public String getVariantId() { return variantId; }
    public Ingredient getCatalyst() { return catalyst; }
    public int getDuration() { return duration; }
    public List<WeightedOutput> getOutputs() { return outputs; }

    public boolean matchesWorker(ProductionTier tier, CatVariant variant, ItemStack catalystStack) {
        if (!tierId.isEmpty() && (tier == null || !tier.getId().equalsIgnoreCase(tierId))) {
            return false;
        }
        if (!variantId.isEmpty() && (variant == null || !variant.getId().equalsIgnoreCase(variantId))) {
            return false;
        }
        if (!catalyst.isEmpty() && !catalyst.test(catalystStack)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return true;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return getResultItem(registryAccess).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).stack();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.HABITAT_PRODUCTION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.HABITAT_PRODUCTION_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<HabitatProductionRecipe> {
        @Override
        public HabitatProductionRecipe fromJson(ResourceLocation id, JsonObject json) {
            String tier = GsonHelper.getAsString(json, "tier", "");
            String variant = GsonHelper.getAsString(json, "variant", "");
            Ingredient catalyst = json.has("catalyst") ? Ingredient.fromJson(json.get("catalyst")) : Ingredient.EMPTY;
            // 5.1.1: "ticks" works too. The built-in catnip recipe wrote its time as "ticks", which nothing read, so it
            // always ran at the 160-tick default instead of its 200.
            int duration = GsonHelper.getAsInt(json, "duration", GsonHelper.getAsInt(json, "ticks", 160));

            List<WeightedOutput> outputs = new ArrayList<>();
            if (json.has("outputs")) {
                JsonArray arr = GsonHelper.getAsJsonArray(json, "outputs");
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    ItemStack stack = CraftingHelper.getItemStack(obj, true, true);
                    float chance = GsonHelper.getAsFloat(obj, "chance", 1.0F);
                    outputs.add(new WeightedOutput(stack, chance));
                }
            }
            return new HabitatProductionRecipe(id, tier, variant, catalyst, duration, outputs);
        }

        @Override
        public @Nullable HabitatProductionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            String tier = buf.readUtf();
            String variant = buf.readUtf();
            Ingredient catalyst = Ingredient.fromNetwork(buf);
            int duration = buf.readVarInt();
            int size = buf.readVarInt();
            List<WeightedOutput> outputs = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                ItemStack stack = buf.readItem();
                float chance = buf.readFloat();
                outputs.add(new WeightedOutput(stack, chance));
            }
            return new HabitatProductionRecipe(id, tier, variant, catalyst, duration, outputs);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, HabitatProductionRecipe recipe) {
            buf.writeUtf(recipe.tierId);
            buf.writeUtf(recipe.variantId);
            recipe.catalyst.toNetwork(buf);
            buf.writeVarInt(recipe.duration);
            buf.writeVarInt(recipe.outputs.size());
            for (WeightedOutput out : recipe.outputs) {
                buf.writeItem(out.stack());
                buf.writeFloat(out.chance());
            }
        }
    }
}
