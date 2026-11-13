package com.kittytalents.compat.jei;

import com.kittytalents.KittyTalents;
import com.kittytalents.entity.CatBreedingManager;
import com.kittytalents.entity.CatVariant;
import com.kittytalents.entity.ProductionTier;
import com.kittytalents.recipe.HabitatProductionRecipe;
import com.kittytalents.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JEI categories (5.0): what each Habitat tier (and data-driven Habitat recipe) produces, and every breeding
 * outcome (same-tier parents give the next tier; breed crosses give a new breed).
 */
public final class KittyJeiCategories {
    private KittyJeiCategories() {}

    // ---------------------------------------------------------------- Habitat production

    /** One Habitat line: who works it, the optional catalyst, the outputs with chances, and the cycle time. */
    public record HabitatEntry(String worker, Ingredient catalyst, List<HabitatProductionRecipe.WeightedOutput> outputs, int ticks) {}

    public static final RecipeType<HabitatEntry> HABITAT = RecipeType.create(KittyTalents.MODID, "habitat_production", HabitatEntry.class);

    public static List<HabitatEntry> habitatEntries() {
        List<HabitatEntry> out = new ArrayList<>();
        for (ProductionTier t : ProductionTier.values()) {
            out.add(new HabitatEntry(t.getDisplayName() + " cat", Ingredient.EMPTY,
                    List.of(new HabitatProductionRecipe.WeightedOutput(t.getProductionResult(), 1.0F)), t.getBaseIntervalTicks()));
        }
        var level = Minecraft.getInstance().level;
        if (level != null) {
            for (HabitatProductionRecipe r : level.getRecipeManager().getAllRecipesFor(com.kittytalents.registry.ModRecipes.HABITAT_PRODUCTION_TYPE.get())) {
                String who = !r.getTierId().isEmpty() ? r.getTierId() + " cat" : !r.getVariantId().isEmpty() ? r.getVariantId() + " cat" : "Any cat";
                out.add(new HabitatEntry(who.substring(0, 1).toUpperCase() + who.substring(1), r.getCatalyst(), r.getOutputs(), r.getDuration()));
            }
        }
        return out;
    }

    public static final class HabitatCategory implements IRecipeCategory<HabitatEntry> {
        private final IDrawable background, icon;

        public HabitatCategory(IGuiHelper gui) {
            background = gui.createBlankDrawable(150, 42);
            icon = gui.createDrawableItemStack(new ItemStack(ModItems.KITTY_HABITAT.get()));
        }

        @Override public RecipeType<HabitatEntry> getRecipeType() { return HABITAT; }
        @Override public Component getTitle() { return Component.translatable("jei.kittytalents.category.habitat"); }
        @SuppressWarnings("removal") @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder b, HabitatEntry e, IFocusGroup focuses) {
            b.addSlot(RecipeIngredientRole.CATALYST, 2, 22).addItemStack(new ItemStack(ModItems.KITTY_HABITAT.get()));
            if (!e.catalyst().isEmpty()) b.addSlot(RecipeIngredientRole.INPUT, 22, 22).addIngredients(e.catalyst());
            int x = 70;
            for (var o : e.outputs()) {
                if (x > 132) break;
                b.addSlot(RecipeIngredientRole.OUTPUT, x, 22).addItemStack(o.stack());
                x += 20;
            }
        }

        @Override
        public void draw(HabitatEntry e, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
            Font font = Minecraft.getInstance().font;
            g.drawString(font, e.worker(), 2, 2, 0xFF404040, false);
            g.drawString(font, "→", 52, 26, 0xFF404040, false);
            String time = String.format("%.1fs", e.ticks() / 20.0);
            g.drawString(font, time, 150 - font.width(time), 2, 0xFF606060, false);
            int x = 70;
            for (var o : e.outputs()) {
                if (x > 132) break;
                if (o.chance() < 1.0F) g.drawString(font, Math.round(o.chance() * 100) + "%", x, 12, 0xFF606060, false);
                x += 20;
            }
        }
    }

    // ---------------------------------------------------------------- Breeding

    /** Parents (tier or breed names) and the kitten, with an item standing for each. */
    public record BreedEntry(String parentA, String parentB, String child, ItemStack iconA, ItemStack iconB, ItemStack iconChild) {}

    public static final RecipeType<BreedEntry> BREEDING = RecipeType.create(KittyTalents.MODID, "breeding", BreedEntry.class);

    public static List<BreedEntry> breedEntries() {
        List<BreedEntry> out = new ArrayList<>();
        ProductionTier[] tiers = ProductionTier.values();
        for (ProductionTier t : tiers) {
            ProductionTier child = CatBreedingManager.getChildTier(t, t);
            if (child == t) continue;
            out.add(new BreedEntry(t.getDisplayName(), t.getDisplayName(), child.getDisplayName() + " kitten",
                    t.getProductionResult(), t.getProductionResult(), child.getProductionResult()));
        }
        for (Map.Entry<String, CatVariant> r : CatBreedingManager.getAllRecipes().entrySet()) {
            int plus = r.getKey().indexOf('+');
            if (plus <= 0) continue;
            String ida = r.getKey().substring(0, plus), idb = r.getKey().substring(plus + 1);
            CatVariant a = variant(ida), b = variant(idb);
            out.add(new BreedEntry(pretty(ida), pretty(idb), r.getValue().getDisplayName() + " kitten",
                    iconOf(a), iconOf(b), iconOf(r.getValue())));
        }
        return out;
    }

    private static CatVariant variant(String id) {
        for (CatVariant c : CatVariant.values()) if (c.getId().equalsIgnoreCase(id) || c.name().equalsIgnoreCase(id)) return c;
        return null;
    }

    private static ItemStack iconOf(CatVariant v) {
        ItemStack s = v == null ? ItemStack.EMPTY : v.getProductionResult();
        return s.isEmpty() ? new ItemStack(ModItems.CATNIP.get()) : s.copy();
    }

    private static String pretty(String id) {
        CatVariant v = null;
        for (CatVariant c : CatVariant.values()) if (c.getId().equalsIgnoreCase(id) || c.name().equalsIgnoreCase(id)) v = c;
        if (v != null) return v.getDisplayName();
        return id.isEmpty() ? id : id.substring(0, 1).toUpperCase() + id.substring(1).replace('_', ' ');
    }

    public static final class BreedingCategory implements IRecipeCategory<BreedEntry> {
        private final IDrawable background, icon;

        public BreedingCategory(IGuiHelper gui) {
            background = gui.createBlankDrawable(150, 40);
            icon = gui.createDrawableItemStack(new ItemStack(ModItems.BREEDING_CATNIP.get()));
        }

        @Override public RecipeType<BreedEntry> getRecipeType() { return BREEDING; }
        @Override public Component getTitle() { return Component.translatable("jei.kittytalents.category.breeding"); }
        @SuppressWarnings("removal") @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder b, BreedEntry e, IFocusGroup focuses) {
            b.addSlot(RecipeIngredientRole.INPUT, 2, 2).addItemStack(e.iconA());
            b.addSlot(RecipeIngredientRole.INPUT, 2, 22).addItemStack(e.iconB());
            b.addSlot(RecipeIngredientRole.OUTPUT, 130, 12).addItemStack(e.iconChild());
        }

        @Override
        public void draw(BreedEntry e, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
            Font font = Minecraft.getInstance().font;
            g.drawString(font, font.plainSubstrByWidth(e.parentA(), 56), 22, 6, 0xFF404040, false);
            g.drawString(font, font.plainSubstrByWidth(e.parentB(), 56), 22, 26, 0xFF404040, false);
            g.drawString(font, "→", 80, 16, 0xFF404040, false);
            String c = font.plainSubstrByWidth(e.child(), 70);
            g.drawString(font, c, 126 - font.width(c), 32, 0xFF2F6B2F, false);
        }
    }
}
