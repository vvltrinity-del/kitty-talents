package com.kittytalents.client.gui;

import com.kittytalents.block.TreatOvenBlockEntity;
import com.kittytalents.client.gui.theme.KTContainerScreen;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.inventory.TreatOvenMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Kitty Treat Oven (Phase 6 theme): ingredients, fuel with its flame, baking progress arrow and the output. */
public class TreatOvenScreen extends KTContainerScreen<TreatOvenMenu> {
    private final TreatOvenBlockEntity oven;

    public TreatOvenScreen(TreatOvenMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 196, 216);
        this.oven = menu.getOven();
    }

    @Override
    protected Component bannerTitle() {
        return KTText.t("oven.title", "KITTY TREAT OVEN");
    }

    @Override
    protected void drawPanels(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = leftPos + 8, y = topPos + 26, w = 180;
        KTTheme.inset(g, x, y, w, 84);
        KTTheme.header(g, font, x + 6, y + 5, 70, KTText.t("oven.ingredients", "Ingredients"));
        g.drawString(font, KTText.s("oven.fuel", "Fuel"), x + 8, y + 63, KTTheme.TEXT_DIM, false);

        // Flame between the ingredients and the fuel slot
        int fx = leftPos + 49, fy = topPos + 67;
        g.fill(fx, fy, fx + 12, fy + 14, 0x66000000);
        if (oven.getBurnTime() > 0) {
            int h = Math.max(2, oven.getBurnTime() * 14 / Math.max(1, oven.getMaxBurnTime()));
            g.fill(fx + 2, fy + 14 - h, fx + 10, fy + 14, 0xFFF97316);
            g.fill(fx + 4, fy + 14 - h * 2 / 3, fx + 8, fy + 14, 0xFFFDE047);
        }

        // Progress arrow towards the output
        int ax = leftPos + 98, ay = topPos + 62, aw = 36;
        int filled = oven.getCookProgress() * aw / Math.max(1, oven.getMaxCookProgress());
        boolean done = filled >= aw;
        g.fill(ax, ay, ax + aw, ay + 6, 0xFF2E2C2A);
        if (filled > 0) g.fill(ax, ay, ax + filled, ay + 6, 0xFF22C55E);
        for (int i = 0; i < 5; i++) { // arrow head
            g.fill(ax + aw + i, ay - 2 + i, ax + aw + i + 1, ay + 8 - i, done ? 0xFF22C55E : 0xFF2E2C2A);
        }
        g.drawString(font, KTText.s("oven.baked", "Baked"), leftPos + 142, topPos + 44, KTTheme.TEXT_DIM, false);
        int fe = oven.getEnergyStorage().getEnergyStored();
        if (fe > 0) g.drawString(font, KTText.s("oven.fe", "FE boost: %s", fe), leftPos + 98, topPos + 92, 0xFF7DD3FC, false);
    }

    @Override
    protected void drawOverlay(GuiGraphics g, int mouseX, int mouseY) {
        // Gold edge on the output slot
        var out = menu.slots.get(TreatOvenBlockEntity.SLOT_OUTPUT);
        KTTheme.outline(g, leftPos + out.x - 2, topPos + out.y - 2, 20, 20, KTTheme.GOLD_EDGE);
    }
}
