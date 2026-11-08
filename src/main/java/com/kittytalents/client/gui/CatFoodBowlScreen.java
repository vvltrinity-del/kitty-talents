package com.kittytalents.client.gui;

import com.kittytalents.client.gui.theme.KTContainerScreen;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.inventory.CatFoodBowlMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Cat Food Bowl (Phase 6 theme): food slots with how much is left, cats nearby and the feeding state. */
public class CatFoodBowlScreen extends KTContainerScreen<CatFoodBowlMenu> {
    public CatFoodBowlScreen(CatFoodBowlMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 184, 180);
    }

    @Override
    protected void drawPanels(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = leftPos + 8, y = topPos + 26, w = 168;
        KTTheme.inset(g, x, y, w, 50);
        int food = menu.getTotalFoodCount(), cats = menu.getNearbyCatsCount(), cd = menu.getFeedCooldown();
        g.drawString(font, KTText.s("bowl.food", "Food: %s", food), x + 6, y + 5, 0xFFFCD34D, false);
        String nearby = KTText.s("bowl.cats", "Cats nearby: %s", cats);
        g.drawString(font, nearby, x + w - 6 - font.width(nearby), y + 5, 0xFF7DD3FC, false);
        String state;
        int color;
        if (food <= 0) {
            state = KTText.s("bowl.empty", "Empty - add food");
            color = 0xFFF87171;
        } else if (cd > 0) {
            state = KTText.s("bowl.cooldown", "Refilling (%ss)", cd / 20);
            color = 0xFFFCD34D;
        } else {
            state = KTText.s("bowl.ready", "Ready to feed");
            color = 0xFF6EE7A0;
        }
        g.drawCenteredString(font, state, x + w / 2, y + 39, color);
    }
}
