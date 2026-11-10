package com.kittytalents.client.debug;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Dev-only screen (ScreenShots harness): accessory and block item icons, drawn large. */
public class ItemGallery extends Screen {
    private static final String[] IDS = {
            "top_hat", "witch_hat", "crown", "halo", "feline_goggles", "phantom_mask", "bowtie", "cat_scarf", "hero_cape",
            "angel_wings", "cat_food_bowl", "cat_bed", "cat_scratcher", "cat_bath", "cat_rice_mill", "kitty_habitat",
            "feeding_station", "kitten_incubator", "kitty_work_station", "treat_oven", "cat_tree"};

    public ItemGallery() {
        super(Component.literal("Item gallery"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF6F7F8F);
        int cols = 7, cellW = width / cols, cellH = height / 3;
        for (int i = 0; i < IDS.length; i++) {
            ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("kittytalents", IDS[i])));
            int cx = (i % cols) * cellW + cellW / 2, cy = (i / cols) * cellH + cellH / 2;
            g.pose().pushPose();
            g.pose().translate(cx - 24, cy - 24, 0);
            g.pose().scale(3.0F, 3.0F, 1.0F);
            g.renderItem(stack, 0, 0);
            g.pose().popPose();
            g.drawCenteredString(font, IDS[i], cx, (i / cols) * cellH + 6, 0xFFFFFFFF);
        }
    }
}
