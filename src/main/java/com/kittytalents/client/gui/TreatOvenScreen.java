package com.kittytalents.client.gui;

import com.kittytalents.block.TreatOvenBlockEntity;
import com.kittytalents.inventory.TreatOvenMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TreatOvenScreen extends AbstractContainerScreen<TreatOvenMenu> {
    private final TreatOvenBlockEntity oven;

    public TreatOvenScreen(TreatOvenMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.oven = menu.getOven();
        this.imageWidth = 194;
        this.imageHeight = 192;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xDD15151C);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 18, 0xFF2A2A38);

        graphics.drawString(this.font, "Kitty Treat Oven", x + 10, y + 5, 0xFFFACC15, false);

        // Ingredient Slots (0..2)
        graphics.drawString(this.font, "Ingredients", x + 30, y + 21, 0xFF9CA3AF, false);
        for (int i = 0; i < 3; i++) {
            int sx = x + 29 + i * 20;
            int sy = y + 31;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF2D3748);
        }

        // Fuel Slot (3)
        graphics.drawString(this.font, "Fuel", x + 22, y + 76, 0xFF9CA3AF, false);
        graphics.fill(x + 49, y + 71, x + 67, y + 89, 0xFF3F2318);

        // Burn flame indicator
        if (oven.getBurnTime() > 0) {
            int maxBurn = Math.max(1, oven.getMaxBurnTime());
            int burnH = Math.max(2, (oven.getBurnTime() * 14) / maxBurn);
            graphics.fill(x + 53, y + 68 - burnH, x + 63, y + 68, 0xFFF97316);
        }

        // Cook progress bar
        int prog = oven.getCookProgress();
        int maxProg = Math.max(1, oven.getMaxCookProgress());
        int barW = 36;
        int filled = (prog * barW) / maxProg;
        graphics.fill(x + 92, y + 55, x + 92 + barW, y + 61, 0xFF374151);
        graphics.fill(x + 92, y + 55, x + 92 + filled, y + 61, 0xFF10B981);

        // Output Slot (4)
        graphics.drawString(this.font, "Baked", x + 128, y + 36, 0xFF9CA3AF, false);
        graphics.fill(x + 131, y + 47, x + 155, y + 71, 0xFF1E3A5F);

        // Optional FE indicator
        int fe = oven.getEnergyStorage().getEnergyStored();
        if (fe > 0) {
            graphics.drawString(this.font, "FE Boost: " + fe, x + 92, y + 76, 0xFF38BDF8, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Handled in renderBg
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
