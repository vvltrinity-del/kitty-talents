package com.kittytalents.client.gui;

import com.kittytalents.block.KittyWorkStationBlockEntity;
import com.kittytalents.client.gui.theme.KTContainerScreen;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.inventory.KittyWorkStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Kitty Work Station (Phase 6 theme): bound workers, the output buffer they deposit into, supplies and upgrades. */
public class KittyWorkStationScreen extends KTContainerScreen<KittyWorkStationMenu> {
    private final KittyWorkStationBlockEntity station;

    public KittyWorkStationScreen(KittyWorkStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 196, 234);
        this.station = menu.getStation();
    }

    @Override
    protected Component bannerTitle() {
        return KTText.t("station.title", "KITTY WORK STATION");
    }

    @Override
    protected void drawPanels(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = leftPos + 8, y = topPos + 26, w = 180;
        KTTheme.inset(g, x, y, w, 100);
        List<String> names = station.getBoundWorkerNames();
        String roster = names.isEmpty() ? KTText.s("station.no_workers", "No workers - bind cats with a Work Clipboard")
                : KTText.s("station.workers", "Workers: %s", String.join(", ", names));
        g.drawString(font, font.plainSubstrByWidth(roster, w - 12), x + 6, y + 5, names.isEmpty() ? KTTheme.TEXT_DIM : 0xFF7DD3FC, false);
        KTTheme.header(g, font, x + 6, y + 16, w - 12, KTText.t("station.buffer", "Output Buffer"));
        KTTheme.header(g, font, x + 6, y + 68, 66, KTText.t("station.supplies", "Supplies"));
        KTTheme.header(g, font, x + 124, y + 68, w - 130, KTText.t("station.upgrades", "Upgrades"));
    }
}
