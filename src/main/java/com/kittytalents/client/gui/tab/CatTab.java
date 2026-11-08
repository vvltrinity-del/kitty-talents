package com.kittytalents.client.gui.tab;

import com.kittytalents.client.gui.CatTalentScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * One page of the companion screen (Phase 6). Tab objects live as long as the screen, so their state (selected
 * talent, scroll position, sub-page) survives the widget rebuilds that follow every server sync.
 */
public interface CatTab {
    /** Text after "TALENTED CAT — " in the title strip. */
    Component title(CatTalentScreen s);

    /** Add this tab's widgets. Called on every rebuild. */
    void init(CatTalentScreen s);

    /** Panels, text and models drawn behind the widgets. */
    void render(CatTalentScreen s, GuiGraphics g, int mouseX, int mouseY, float partialTick);

    /** Drawn after the widgets (pips on top of buttons, tooltips). */
    default void renderOverlay(CatTalentScreen s, GuiGraphics g, int mouseX, int mouseY) {}

    default boolean mouseScrolled(CatTalentScreen s, double mouseX, double mouseY, double delta) { return false; }
}
