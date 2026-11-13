package com.kittytalents.client.gui;

import com.kittytalents.client.ClientBaseAreas;
import com.kittytalents.client.gui.theme.KTButton;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.network.PacketBaseAreaAction;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.network.PacketSyncBaseAreas;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The Base Pin menu (5.0): your saved base areas with their size and guard count; rename, resize (the next two
 * corner clicks), merge with touching areas, or delete.
 */
public class BasePinScreen extends Screen {
    private static final int ROW_H = 22;
    private int px, py, pw, ph, scroll;
    private EditBox renameBox;
    private int renaming = -1;

    public BasePinScreen() {
        super(KTText.t("base_pin.title", "BASE PIN"));
    }

    public void refresh() {
        rebuildWidgets();
    }

    private int visibleRows() { return Math.max(1, (ph - 64) / ROW_H); }

    @Override
    protected void init() {
        pw = Math.min(360, width - 20);
        ph = Math.min(230, height - 20);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        List<PacketSyncBaseAreas.Entry> list = ClientBaseAreas.entries();
        scroll = Math.max(0, Math.min(scroll, list.size() - visibleRows()));
        int y = py + 40;
        for (int i = scroll; i < Math.min(list.size(), scroll + visibleRows()); i++) {
            var e = list.get(i);
            int id = e.area().id();
            int bx = px + pw - 12 - 4 * 52;
            if (renaming == id) {
                renameBox = new EditBox(font, px + 14, y + 3, bx - px - 22, 14, KTText.t("base_pin.name", "Name"));
                renameBox.setMaxLength(24);
                renameBox.setValue(e.area().name());
                addRenderableWidget(renameBox);
                setInitialFocus(renameBox);
                addRenderableWidget(new KTButton(bx, y + 2, 50, 16, KTText.t("base_pin.save", "Save"), b -> {
                    PacketHandler.sendToServer(new PacketBaseAreaAction(PacketBaseAreaAction.RENAME, id, renameBox.getValue()));
                    renaming = -1;
                    rebuildWidgets();
                }).style(KTButton.Style.GOLD));
            } else {
                addRenderableWidget(new KTButton(bx, y + 2, 50, 16, KTText.t("base_pin.rename", "Rename"), b -> {
                    renaming = id;
                    rebuildWidgets();
                }));
            }
            addRenderableWidget(new KTButton(bx + 52, y + 2, 50, 16, KTText.t("base_pin.resize", "Resize"), b -> {
                PacketHandler.sendToServer(new PacketBaseAreaAction(PacketBaseAreaAction.RESIZE, id, ""));
                onClose();
            }).tip(KTText.t("base_pin.resize.tip", "Right-click two new corners with the Base Pin")));
            addRenderableWidget(new KTButton(bx + 104, y + 2, 50, 16, KTText.t("base_pin.merge", "Merge"),
                    b -> PacketHandler.sendToServer(new PacketBaseAreaAction(PacketBaseAreaAction.MERGE, id, "")))
                    .tip(KTText.t("base_pin.merge.tip", "Join every area that overlaps or touches this one")));
            addRenderableWidget(new KTButton(bx + 156, y + 2, 50, 16, KTText.t("base_pin.delete", "Delete"),
                    b -> PacketHandler.sendToServer(new PacketBaseAreaAction(PacketBaseAreaAction.DELETE, id, "")))
                    .style(KTButton.Style.CHIP).color(0xFFFCA5A5));
            y += ROW_H;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        KTTheme.frame(g, px, py, pw, ph);
        KTTheme.banner(g, font, px + 6, py + 6, pw - 12, 16, title);
        g.drawCenteredString(font, KTText.t("base_pin.subtitle", "Right-click two corners to add a base. Sneak-click a cat, then a block, for a guard post."),
                px + pw / 2, py + 25, KTTheme.TEXT_DIM);
        KTTheme.inset(g, px + 8, py + 36, pw - 16, ph - 44);
        List<PacketSyncBaseAreas.Entry> list = ClientBaseAreas.entries();
        if (list.isEmpty()) {
            g.drawCenteredString(font, KTText.t("base_pin.none", "No bases yet."), px + pw / 2, py + 52, KTTheme.TEXT_DIM);
        }
        int y = py + 40;
        int textW = pw - 12 - 4 * 52 - 22;
        for (int i = scroll; i < Math.min(list.size(), scroll + visibleRows()); i++) {
            var e = list.get(i);
            if (renaming != e.area().id()) {
                g.drawString(font, font.plainSubstrByWidth(e.area().name(), textW), px + 14, y + 3, KTTheme.TEXT_GOLD, false);
            }
            String info = e.area().sizeX() + "x" + e.area().sizeZ() + " · " + KTText.s("base_pin.guards", "%s guards", e.guards());
            g.drawString(font, font.plainSubstrByWidth(info, textW), px + 14, y + 12, KTTheme.TEXT_DIM, false);
            y += ROW_H;
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int max = Math.max(0, ClientBaseAreas.entries().size() - visibleRows());
        int next = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    static Component dim(String s) { return Component.literal(s).withStyle(ChatFormatting.GRAY); }
}
