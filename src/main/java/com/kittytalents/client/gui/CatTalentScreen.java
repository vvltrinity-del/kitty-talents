package com.kittytalents.client.gui;

import com.kittytalents.client.gui.tab.*;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.core.component.CatLoadout;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.inventory.CatTalentMenu;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.network.PacketManageTrust;
import com.kittytalents.network.PacketRenameCat;
import com.kittytalents.network.PacketSetCatLoadout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The companion screen (Phase 6 redesign): a dark-oak frame with folder tabs on top, a spruce title strip, and
 * stitched-leather panels. This class is only the shell - geometry, tabs, the title strip, edit-box keyboard focus and
 * the refresh hooks the network layer calls. Each page lives in {@code client.gui.tab}.
 */
public class CatTalentScreen extends AbstractContainerScreen<CatTalentMenu> {
    public static final int HOME = 0, TALENTS = 1, GEAR = 2, STYLE = 3, STATS = 4, COMBAT = 5, TWINE = 6;

    private static final int TAB_H = 15;

    private final TalentedCat cat;
    private final CatTab[] tabs;
    private final String[] tabKeys = { "home", "talents", "gear", "style", "stats", "combat", "twine" };
    private final String[] tabNames = { "Home", "Talents", "Gear", "Style", "Stats", "Combat", "Twine" };
    private final int[] tabColors = { 0xFFF2EEE6, 0xFFF0C75E, 0xFFF59E42, 0xFFC084FC, 0xFF6EE7A0, 0xFFF87171, 0xFF67E8F9 };
    private int currentTab = HOME;

    // Panel geometry (recomputed in init)
    private int px, py, pw, ph;

    // Focusable text fields registered by the active tab (keyboard capture while typing)
    private EditBox nameEditBox;
    private EditBox trustEditBox;

    public CatTalentScreen(CatTalentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.cat = menu.getCat();
        this.tabs = new CatTab[] { new HomeTab(), new TalentsTab(), new GearTab(), new StyleTab(), new StatsTab(), new CombatTab(), new TwineTab() };
    }

    // ------------------------------------------------------------------ geometry & accessors for tabs

    public TalentedCat cat() { return cat; }
    public Font font() { return font; }
    public Minecraft mc() { return minecraft; }
    /** Content area inside the frame, below the title strip. */
    public int cx() { return px + 8; }
    public int cy() { return py + 28; }
    public int cw() { return pw - 16; }
    public int ch() { return ph - 36; }
    /** Title strip rectangle (tabs may draw on its right end, e.g. skill points). */
    public int titleX() { return px + 6; }
    public int titleY() { return py + 6; }
    public int titleW() { return pw - 12; }

    public <T extends GuiEventListener & Renderable & NarratableEntry> T add(T widget) {
        return addRenderableWidget(widget);
    }

    public void setNameBox(EditBox box) { this.nameEditBox = box; addRenderableWidget(box); }
    public void setTrustBox(EditBox box) { this.trustEditBox = box; addRenderableWidget(box); }

    /** Rebuild the active tab's widgets (after a local change or a server sync). */
    public void rebuild() {
        clearWidgets();
        nameEditBox = null;
        trustEditBox = null;
        menu.setSlotsShown(currentTab == GEAR);
        if (cat == null) return;
        tabs[currentTab].init(this);
    }

    /** The menu slot under the mouse (only Gear-tab slots are active). */
    @javax.annotation.Nullable
    public net.minecraft.world.inventory.Slot hoveredSlot() { return hoveredSlot; }

    public void showTab(int index) {
        if (index == currentTab || index < 0 || index >= tabs.length) return;
        currentTab = index;
        rebuild();
    }

    /** The Combat tab edits the local copy for instant feedback; the server validates, applies and syncs back. */
    public void sendLoadout() {
        PacketHandler.sendToServer(new PacketSetCatLoadout(cat.getId(), CatLoadout.of(cat)));
    }

    /** The live cat model, following the mouse, sized to fill and clipped to the given box. */
    public void drawCat(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        // Scale is pixels per block; a cat is ~0.7 tall and ~1 long side-on.
        int scale = (int) Math.min(h * 1.1F, w * 0.8F);
        int footX = x + w / 2, footY = y + h - Math.max(6, h / 7);
        g.enableScissor(x, y, x + w, y + h);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, footX, footY, scale,
                (float) footX - mouseX, (float) (footY - scale) - mouseY, cat);
        g.disableScissor();
    }

    // ------------------------------------------------------------------ refresh hooks (ClientPacketHandlers)

    public void refreshTrustUi() { if (currentTab == HOME) rebuild(); }
    public void refreshEquipmentUi() { if (currentTab == GEAR) rebuild(); }
    public void refreshCombatUi() { if (currentTab == COMBAT) rebuild(); }
    public void refreshTalentUi() { if (currentTab == TALENTS) rebuild(); }

    // ------------------------------------------------------------------ synced-state watch

    private int lastState;
    private boolean statePending;

    /** Everything the tabs draw as widgets that the server syncs through entity data. */
    private int syncedState() {
        return java.util.Objects.hash(cat.getCatMode(), cat.getCatJob(), cat.isClockedIn(), cat.isOrderedToSit(),
                cat.getSkin(), cat.getCollarColor(), cat.isCosmeticsVisible(), cat.getName().getString(),
                cat.getSkillPoints(), cat.getCatLevel(), cat.isDireAwakened(), cat.getProductionTier(),
                cat.getTrustedPlayers().keySet());
    }

    /**
     * Tabs only send requests; when the synced state changes (accepted or not) the widgets are rebuilt from it.
     * While a text field is focused the rebuild waits, so typing is never interrupted.
     */
    @Override
    protected void containerTick() {
        super.containerTick();
        if (cat == null) return;
        int state = syncedState();
        if (state != lastState) { lastState = state; statePending = true; }
        boolean typing = (nameEditBox != null && nameEditBox.isFocused()) || (trustEditBox != null && trustEditBox.isFocused());
        if (statePending && !typing) { statePending = false; rebuild(); }
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void init() {
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        this.leftPos = 0;
        this.topPos = 0;
        super.init();
        pw = Math.min(420, width - 20);
        ph = Math.min(244, height - 36);
        px = (width - pw) / 2;
        py = Math.max(TAB_H + 4, (height - ph) / 2 + TAB_H / 2);
        if (cat != null) lastState = syncedState();
        rebuild();
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void renderBackground(GuiGraphics g) {
        // Keep the world visible behind the companion screen.
        g.fillGradient(0, 0, width, height, 0x6005050C, 0x900A0A14);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        if (cat == null) return;
        // Inactive tabs sit behind the frame; the active tab is drawn after it so it joins the frame.
        for (int i = 0; i < tabs.length; i++) {
            if (i != currentTab) drawTab(g, i, mouseX, mouseY);
        }
        KTTheme.frame(g, px, py, pw, ph);
        drawTab(g, currentTab, mouseX, mouseY);
        Component title = KTText.t("title", "TALENTED CAT").append(" — ").append(tabs[currentTab].title(this));
        KTTheme.titleBar(g, font, titleX(), titleY(), titleW(), 16, title);
        tabs[currentTab].render(this, g, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // The frame's title strip replaces the container's default "title / Inventory" labels.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        if (cat != null) tabs[currentTab].renderOverlay(this, g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    private int tabW() { return Math.min(58, (pw - 16) / tabs.length - 2); }
    private int tabX(int i) { return px + 8 + i * (tabW() + 2); }

    private void drawTab(GuiGraphics g, int i, int mouseX, int mouseY) {
        int x = tabX(i), w = tabW();
        boolean active = i == currentTab;
        int y = active ? py - TAB_H : py - TAB_H + 2;
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= py - TAB_H && mouseY < py;
        KTTheme.tab(g, font, x, y, w, active ? TAB_H + 1 : TAB_H - 2, KTText.t("tab." + tabKeys[i], tabNames[i]), tabColors[i], active, hover);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseY >= py - TAB_H && mouseY < py) {
            for (int i = 0; i < tabs.length; i++) {
                int x = tabX(i);
                if (mouseX >= x && mouseX < x + tabW()) {
                    if (i != currentTab) {
                        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0F));
                        showTab(i);
                    }
                    return true;
                }
            }
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (nameEditBox != null && !nameEditBox.isMouseOver(mouseX, mouseY)) nameEditBox.setFocused(false);
        if (trustEditBox != null && !trustEditBox.isMouseOver(mouseX, mouseY)) trustEditBox.setFocused(false);
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (cat != null && tabs[currentTab].mouseScrolled(this, mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameEditBox != null && nameEditBox.isFocused()) {
            if (keyCode == 257 || keyCode == 335) submitName();
            if (keyCode == 256 || keyCode == 257 || keyCode == 335) { nameEditBox.setFocused(false); return true; }
            nameEditBox.keyPressed(keyCode, scanCode, modifiers);
            return true; // swallow everything else (don't close the screen or swap hotbar slots while typing)
        }
        if (trustEditBox != null && trustEditBox.isFocused()) {
            if (keyCode == 257 || keyCode == 335) submitTrust();
            if (keyCode == 256 || keyCode == 257 || keyCode == 335) { trustEditBox.setFocused(false); return true; }
            trustEditBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        // Keyboard tab switching: Ctrl+Tab / Ctrl+Shift+Tab, or 1-7.
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB && hasControlDown()) {
            showTab((currentTab + (hasShiftDown() ? tabs.length - 1 : 1)) % tabs.length);
            return true;
        }
        if (keyCode >= org.lwjgl.glfw.GLFW.GLFW_KEY_1 && keyCode < org.lwjgl.glfw.GLFW.GLFW_KEY_1 + tabs.length && modifiers == 0) {
            showTab(keyCode - org.lwjgl.glfw.GLFW.GLFW_KEY_1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (nameEditBox != null && nameEditBox.isFocused()) { nameEditBox.charTyped(codePoint, modifiers); return true; }
        if (trustEditBox != null && trustEditBox.isFocused()) { trustEditBox.charTyped(codePoint, modifiers); return true; }
        return super.charTyped(codePoint, modifiers);
    }

    public void submitName() {
        if (nameEditBox == null) return;
        String newName = nameEditBox.getValue().trim();
        if (!newName.isEmpty() && newName.length() <= 32) {
            PacketHandler.sendToServer(new PacketRenameCat(cat.getId(), newName));
        }
    }

    public void submitTrust() {
        if (trustEditBox == null) return;
        String target = trustEditBox.getValue().trim();
        if (!target.isEmpty()) {
            PacketHandler.sendToServer(new PacketManageTrust(cat.getId(), 0, target));
            trustEditBox.setValue("");
        }
    }
}
