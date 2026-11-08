package com.kittytalents.client.gui.theme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Base for the block and pack screens in the wood-and-leather theme: dark-oak frame, centred title banner, a leather
 * "Player Inventory" panel, and slot frames drawn from the menu's own slot positions (so art and slots always line up).
 * Subclasses draw their panels in {@link #drawPanels} and anything on top of items in {@link #drawOverlay}.
 */
public abstract class KTContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    protected KTContainerScreen(T menu, Inventory playerInventory, Component title, int width, int height) {
        super(menu, playerInventory, title);
        this.imageWidth = width;
        this.imageHeight = height;
    }

    /** Panels, text and widgets' backdrops; coordinates are absolute (use leftPos / topPos). */
    protected abstract void drawPanels(GuiGraphics g, int mouseX, int mouseY, float partialTick);

    /** Drawn after slots and items (progress overlays, tooltips for custom areas). */
    protected void drawOverlay(GuiGraphics g, int mouseX, int mouseY) {}

    /** The title shown in the banner. */
    protected Component bannerTitle() { return title; }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        KTTheme.frame(g, x, y, imageWidth, imageHeight);
        KTTheme.banner(g, font, x + 6, y + 6, imageWidth - 12, 16, bannerTitle());
        drawInventoryPanel(g);
        drawPanels(g, mouseX, mouseY, partialTick);
        for (Slot slot : menu.slots) {
            KTTheme.slot(g, x + slot.x - 1, y + slot.y - 1, false);
        }
    }

    /** Leather panel and header around the player's inventory slots (found from the menu). */
    protected void drawInventoryPanel(GuiGraphics g) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof Inventory)) continue;
            minX = Math.min(minX, slot.x); minY = Math.min(minY, slot.y);
            maxX = Math.max(maxX, slot.x + 16); maxY = Math.max(maxY, slot.y + 16);
        }
        if (minX == Integer.MAX_VALUE) return;
        int px = leftPos + minX - 7, py = topPos + minY - 16, pw = maxX - minX + 14, ph = maxY - minY + 23;
        KTTheme.inset(g, px, py, pw, ph);
        KTTheme.header(g, font, px + 6, py + 4, pw - 12, KTText.t("container.player_inventory", "Player Inventory"));
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Title is in the banner; the inventory label is the panel header.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        drawOverlay(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }
}
