package com.kittytalents.client.gui;

import com.kittytalents.block.KittyWorkStationBlockEntity;
import com.kittytalents.inventory.KittyWorkStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class KittyWorkStationScreen extends AbstractContainerScreen<KittyWorkStationMenu> {
    private final KittyWorkStationBlockEntity station;

    public KittyWorkStationScreen(KittyWorkStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.station = menu.getStation();
        this.imageWidth = 194;
        this.imageHeight = 212;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xDD15151C);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 18, 0xFF2A2A38);

        graphics.drawString(this.font, "Kitty Work Station", x + 10, y + 5, 0xFFFACC15, false);

        List<String> names = station.getBoundWorkerNames();
        String rosterText = names.isEmpty()
                ? "Bound Workers: None (Use Work Clipboard)"
                : "Workers (" + names.size() + "): " + String.join(", ", names);
        if (rosterText.length() > 38) rosterText = rosterText.substring(0, 37) + "…";
        graphics.drawString(this.font, rosterText, x + 10, y + 23, 0xFF55FFFF, false);

        graphics.drawString(this.font, "Output Buffer (Worker Deposits)", x + 16, y + 40, 0xFF9CA3AF, false);
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = x + 15 + col * 18;
                int sy = y + 51 + row * 18;
                graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF2D3748);
            }
        }

        graphics.drawString(this.font, "Supplies", x + 16, y + 88, 0xFF9CA3AF, false);
        for (int i = 0; i < 4; i++) {
            int sx = x + 15 + i * 18;
            int sy = y + 97;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF1E3A5F);
        }

        graphics.drawString(this.font, "Upgrades", x + 134, y + 88, 0xFF9CA3AF, false);
        for (int i = 0; i < 2; i++) {
            int sx = x + 141 + i * 18;
            int sy = y + 97;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF3B2854);
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
