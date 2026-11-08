package com.kittytalents.client.gui;

import com.kittytalents.client.gui.theme.KTButton;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.entity.WhistleCommand;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.network.PacketWhistleCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Kitty Whistle (Phase 6 theme): searchable, scrolling grid of commands sent to all your cats nearby. */
public class KittyWhistleScreen extends Screen {
    private static final int COLS = 3, ROW_H = 18, GAP = 3;

    private EditBox searchBox;
    private final List<WhistleCommand> filtered = new ArrayList<>();
    private int scroll;
    private int px, py, pw, ph;

    public KittyWhistleScreen() {
        super(KTText.t("whistle.title", "KITTY WHISTLE"));
    }

    private int gridY() { return py + 54; }
    private int gridH() { return ph - 54 - 30; }
    private int visibleRows() { return Math.max(1, (gridH() - 8) / (ROW_H + GAP)); }
    private int rows() { return (filtered.size() + COLS - 1) / COLS; }
    private int maxScroll() { return Math.max(0, rows() - visibleRows()); }

    @Override
    protected void init() {
        pw = Math.min(400, width - 20);
        ph = Math.min(244, height - 20);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        String query = searchBox != null ? searchBox.getValue() : "";
        searchBox = new EditBox(font, px + 15, py + 33, pw - 30, 14, KTText.t("whistle.search", "Search"));
        searchBox.setHint(KTText.t("whistle.search.hint", "Search commands...").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setValue(query);
        searchBox.setResponder(q -> { scroll = 0; rebuild(); });
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        addRenderableWidget(searchBox);
        filtered.clear();
        String q = searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        for (WhistleCommand c : WhistleCommand.values()) {
            if (q.isEmpty() || c.getDisplayName().toLowerCase(Locale.ROOT).contains(q)) filtered.add(c);
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        int gx = px + 14, gw = pw - 28 - 8, bw = (gw - (COLS - 1) * GAP) / COLS;
        int first = scroll * COLS, last = Math.min(filtered.size(), first + visibleRows() * COLS);
        for (int i = first; i < last; i++) {
            WhistleCommand cmd = filtered.get(i);
            int col = (i - first) % COLS, row = (i - first) / COLS;
            addRenderableWidget(new KTButton(gx + col * (bw + GAP), gridY() + 5 + row * (ROW_H + GAP), bw, ROW_H,
                    KTText.t("whistle.cmd." + cmd.name().toLowerCase(Locale.ROOT), cmd.getDisplayName()), b -> {
                PacketHandler.sendToServer(new PacketWhistleCommand(cmd));
                onClose();
            }).tip(Component.literal(cmd.getDescription())));
        }
        addRenderableWidget(new KTButton(px + pw / 2 - 35, py + ph - 26, 70, 18, KTText.t("whistle.close", "Close"), b -> onClose())
                .style(KTButton.Style.RED));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int next = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta)));
        if (next != scroll) {
            scroll = next;
            rebuild();
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        KTTheme.frame(g, px, py, pw, ph);
        KTTheme.banner(g, font, px + 6, py + 6, pw - 12, 16, title);
        g.drawCenteredString(font, KTText.t("whistle.subtitle", "Every one of your cats nearby hears it."), px + pw / 2, py + 23 + 1, KTTheme.TEXT_DIM);
        int gx = px + 8, gy = gridY(), gw = pw - 16, gh = gridH();
        KTTheme.inset(g, gx, gy, gw, gh);
        if (maxScroll() > 0) {
            int trackX = gx + gw - 8, trackY = gy + 5, trackH = gh - 10;
            g.fill(trackX, trackY, trackX + 3, trackY + trackH, 0x66000000);
            int thumbH = Math.max(12, trackH * visibleRows() / Math.max(1, rows()));
            int thumbY = trackY + (trackH - thumbH) * scroll / maxScroll();
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, KTTheme.GOLD_EDGE);
        }
        if (filtered.isEmpty()) {
            g.drawCenteredString(font, KTText.t("whistle.none", "No matching commands"), gx + gw / 2, gy + 14, KTTheme.TEXT_DIM);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            if (keyCode == 256) {
                searchBox.setFocused(false);
                return true;
            }
            searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true; // typing never closes the screen
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
