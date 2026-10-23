package com.kittytalents.compat.tacz;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.work.WorkInventoryHelper;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles Gunsmith job recipe discovery and crafting for TacZ.
 * Strictly allows Ammo (Cat Level 10+) and Attachments (Cat Level 25+) only.
 * Firearm (IGun) recipes are strictly excluded.
 */
public class TacZGunsmith {

    public static boolean isAllowedGunsmithOutput(ItemStack output, int catLevel) {
        if (output == null || output.isEmpty()) return false;
        // Never allow crafting guns
        if (IGun.getIGunOrNull(output) != null) {
            return false;
        }
        // Ammo & AmmoBoxes allowed at Level 10+
        if (IAmmo.getIAmmoOrNull(output) != null || output.getItem() instanceof IAmmoBox) {
            return catLevel >= 10;
        }
        // Attachments allowed at Level 25+
        if (IAttachment.getIAttachmentOrNull(output) != null) {
            return catLevel >= 25;
        }
        return false;
    }

    public static List<ResourceLocation> getAvailableRecipes(int catLevel) {
        List<ResourceLocation> result = new ArrayList<>();
        Map<ResourceLocation, GunSmithTableRecipe> all = TimelessAPI.getAllRecipes();
        if (all == null) return result;
        for (Map.Entry<ResourceLocation, GunSmithTableRecipe> entry : all.entrySet()) {
            GunSmithTableRecipe recipe = entry.getValue();
            if (recipe != null && isAllowedGunsmithOutput(recipe.getOutput(), catLevel)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public static GunSmithTableRecipe resolveRecipeForCat(TalentedCat cat) {
        int level = cat.getCatLevel();
        String selectedId = cat.getJobData().getSelectedRecipeId();
        if (selectedId != null && !selectedId.isEmpty()) {
            ResourceLocation rl = ResourceLocation.tryParse(selectedId);
            if (rl != null) {
                Optional<GunSmithTableRecipe> opt = TimelessAPI.getRecipe(rl);
                if (opt.isPresent() && isAllowedGunsmithOutput(opt.get().getOutput(), level)) {
                    return opt.get();
                }
            }
        }
        // Auto-select: if the cat has an equipped gun, prefer crafting matching ammo for it
        ItemStack gun = cat.getEquippedWeapon();
        Map<ResourceLocation, GunSmithTableRecipe> all = TimelessAPI.getAllRecipes();
        if (all == null || all.isEmpty()) return null;

        if (!gun.isEmpty() && IGun.getIGunOrNull(gun) != null) {
            for (GunSmithTableRecipe recipe : all.values()) {
                ItemStack out = recipe.getOutput();
                if (isAllowedGunsmithOutput(out, level) && TacZCompat.isMatchingAmmo(gun, out)) {
                    if (hasIngredientsAvailable(cat, recipe)) {
                        return recipe;
                    }
                }
            }
        }

        // Otherwise pick the first allowed recipe whose ingredients are available
        for (GunSmithTableRecipe recipe : all.values()) {
            if (isAllowedGunsmithOutput(recipe.getOutput(), level) && hasIngredientsAvailable(cat, recipe)) {
                return recipe;
            }
        }
        return null;
    }

    public static boolean canCraftSelectedRecipe(TalentedCat cat) {
        GunSmithTableRecipe recipe = resolveRecipeForCat(cat);
        if (recipe == null) return false;

        // Check stock target
        int target = cat.getJobData().getStockTarget();
        if (target > 0 && countOutputInPack(cat, recipe.getOutput()) >= target) {
            return false;
        }
        return hasIngredientsAvailable(cat, recipe);
    }

    public static boolean craftSelectedRecipe(TalentedCat cat) {
        GunSmithTableRecipe recipe = resolveRecipeForCat(cat);
        if (recipe == null) return false;
        if (!isAllowedGunsmithOutput(recipe.getOutput(), cat.getCatLevel())) {
            return false;
        }

        List<GunSmithTableIngredient> inputs = recipe.getInputs();
        if (inputs == null || inputs.isEmpty()) return false;

        // Ensure all ingredients are in catPack (pulling from linked inputs / station if needed)
        for (GunSmithTableIngredient req : inputs) {
            Ingredient ing = req.getIngredient();
            int needed = req.getCount();
            int inPack = countMatchingInContainer(cat.getCatPack(), ing);
            if (inPack < needed) {
                int toPull = needed - inPack;
                WorkInventoryHelper.pullToCatPack(cat, stack -> ing.test(stack), toPull);
                inPack = countMatchingInContainer(cat.getCatPack(), ing);
                if (inPack < needed) {
                    return false;
                }
            }
        }

        // Consume ingredients from catPack
        for (GunSmithTableIngredient req : inputs) {
            Ingredient ing = req.getIngredient();
            int remaining = req.getCount();
            SimpleContainer pack = cat.getCatPack();
            for (int i = 0; i < pack.getContainerSize() && remaining > 0; i++) {
                ItemStack s = pack.getItem(i);
                if (!s.isEmpty() && ing.test(s)) {
                    int take = Math.min(remaining, s.getCount());
                    s.shrink(take);
                    if (s.isEmpty()) pack.setItem(i, ItemStack.EMPTY);
                    remaining -= take;
                }
            }
        }

        ItemStack result = recipe.getOutput().copy();
        // If output is ammo and ammoPouch has space, or deposit into pack
        ItemStack rem = cat.getCatPack().addItem(result);
        if (!rem.isEmpty()) {
            cat.spawnAtLocation(rem);
        }
        WorkInventoryHelper.depositPackToStationOrOutputs(cat, null);
        return true;
    }

    private static boolean hasIngredientsAvailable(TalentedCat cat, GunSmithTableRecipe recipe) {
        List<GunSmithTableIngredient> inputs = recipe.getInputs();
        if (inputs == null || inputs.isEmpty()) return false;
        for (GunSmithTableIngredient req : inputs) {
            Ingredient ing = req.getIngredient();
            int needed = req.getCount();
            int have = countMatchingInContainer(cat.getCatPack(), ing);
            if (have < needed) {
                // Try pulling from station / linked inputs into catPack
                WorkInventoryHelper.pullToCatPack(cat, stack -> ing.test(stack), needed - have);
                have = countMatchingInContainer(cat.getCatPack(), ing);
                if (have < needed) return false;
            }
        }
        return true;
    }

    private static int countMatchingInContainer(SimpleContainer container, Ingredient ingredient) {
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (!s.isEmpty() && ingredient.test(s)) {
                total += s.getCount();
            }
        }
        return total;
    }

    private static int countOutputInPack(TalentedCat cat, ItemStack target) {
        int total = 0;
        SimpleContainer pack = cat.getCatPack();
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack s = pack.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, target)) {
                total += s.getCount();
            }
        }
        return total;
    }
}
