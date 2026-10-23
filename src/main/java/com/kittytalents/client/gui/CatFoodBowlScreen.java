package com.kittytalents.client.gui;

import com.kittytalents.inventory.CatFoodBowlMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CatFoodBowlScreen extends AbstractContainerScreen<CatFoodBowlMenu> {
    private static final ResourceLocation CONTAINER_BACKGROUND =
            new ResourceLocation("textures/gui/container/hopper.png");

    public CatFoodBowlScreen(CatFoodBowlMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Status Panel above the slots
        int x = this.leftPos + 8;
        int y = this.topPos + 18;

        int foodCount = this.menu.getTotalFoodCount();
        int nearbyCats = this.menu.getNearbyCatsCount();
        int cooldown = this.menu.getFeedCooldown();

        String statusStr = foodCount <= 0 ? (ChatFormatting.RED + "Empty (Add Food)") :
                (cooldown > 0 ? (ChatFormatting.YELLOW + "Cooling Down (" + (cooldown / 20) + "s)") :
                        (ChatFormatting.GREEN + "Ready to Feed"));

        guiGraphics.drawString(this.font, "Food: " + ChatFormatting.GOLD + foodCount + " items", x, y, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "Cats Nearby: " + ChatFormatting.AQUA + nearbyCats, x + 82, y, 0xFFFFFF, false);

        // Draw dish slot outline frames
        int startX = this.leftPos + 43;
        int slotY = this.topPos + 35;
        guiGraphics.renderOutline(startX - 2, slotY - 2, 94, 22, 0xFFC69C6D);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Clean container background
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF2A2A38);
        guiGraphics.renderOutline(x, y, this.imageWidth, this.imageHeight, 0xFF555577);

        // Header banner
        guiGraphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + 16, 0xFF1C1C26);

        // Food Slots Background Area
        int slotsX = x + 40;
        int slotsY = y + 32;
        guiGraphics.fill(slotsX, slotsY, slotsX + 96, slotsY + 24, 0xFF1A1A22);

        for (int i = 0; i < 5; i++) {
            int slotX = x + 44 + i * 18;
            int sY = y + 36;
            guiGraphics.fill(slotX, sY, slotX + 16, sY + 16, 0xFF353545);
            guiGraphics.renderOutline(slotX - 1, sY - 1, 18, 18, 0xFF656580);
        }

        // Player Inventory Slots Background
        int invX = x + 7;
        int invY = y + 83;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = invX + col * 18;
                int sy = invY + row * 18;
                guiGraphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF22222E);
                guiGraphics.renderOutline(sx, sy, 18, 18, 0xFF44445A);
            }
        }

        // Hotbar Slots Background
        int hotbarY = y + 141;
        for (int col = 0; col < 9; col++) {
            int sx = invX + col * 18;
            guiGraphics.fill(sx + 1, hotbarY + 1, sx + 17, hotbarY + 17, 0xFF22222E);
            guiGraphics.renderOutline(sx, hotbarY, 18, 18, 0xFF44445A);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0xFFDD88, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xAAAAAA, false);
    }
}
