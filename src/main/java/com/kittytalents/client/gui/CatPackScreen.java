package com.kittytalents.client.gui;

import com.kittytalents.client.gui.theme.KTContainerScreen;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.inventory.CatPackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The cat's Kitty Pack (Phase 6 theme). */
public class CatPackScreen extends KTContainerScreen<CatPackMenu> {
    public CatPackScreen(CatPackMenu menu, Inventory playerInventory, Component title) {
        // Matches CatPackMenu: pack rows from y 31, player inventory 26 below them.
        super(menu, playerInventory, title, 184, 31 + menu.getNumRows() * 18 + 26 + 58 + 16 + 13);
    }

    @Override
    protected void drawPanels(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        KTTheme.inset(g, leftPos + 5, topPos + 26, 174, menu.getNumRows() * 18 + 10);
    }
}
