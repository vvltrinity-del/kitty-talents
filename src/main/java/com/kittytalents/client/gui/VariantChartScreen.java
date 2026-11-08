package com.kittytalents.client.gui;

import com.kittytalents.client.gui.theme.KTButton;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.entity.ProductionTier;
import com.kittytalents.registry.ModItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Feline Tier Evolution and Breeding Codex (Phase 6 theme): tier list on the left (tier colour and its produce as the
 * icon), details on the right: habitat harvest, the breeding recipe that produces the tier, and the Nekomata rule.
 */
public class VariantChartScreen extends Screen {
    private static final int LIST_W = 150, ROW_H = 20, ROW_GAP = 2;
    private static final ProductionTier[] TIERS = ProductionTier.values();

    private ProductionTier selected = ProductionTier.STANDARD;
    private int scroll;
    private int px, py, pw, ph;

    public VariantChartScreen() {
        super(KTText.t("codex.title", "FELINE TIER EVOLUTION & BREEDING CODEX"));
    }

    private int listY() { return py + 28; }
    private int listH() { return ph - 36; }
    private int visibleRows() { return Math.max(1, (listH() - 10) / (ROW_H + ROW_GAP)); }
    private int maxScroll() { return Math.max(0, TIERS.length - visibleRows()); }

    @Override
    protected void init() {
        pw = Math.min(420, width - 20);
        ph = Math.min(244, height - 20);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        int x = px + 13, w = LIST_W - 10 - 6, y = listY() + 5;
        int end = Math.min(TIERS.length, scroll + visibleRows());
        for (int i = scroll; i < end; i++) {
            ProductionTier t = TIERS[i];
            addRenderableWidget(new KTButton(x, y + (i - scroll) * (ROW_H + ROW_GAP), w, ROW_H,
                    KTText.t("codex.tier." + t.getId(), "%s Tier", t.getDisplayName()), b -> {
                selected = t;
                rebuild();
            }).icon(t.getProductionResult()).left().selected(t == selected).color(t.getColorValue() | 0xFF000000));
        }
        int dx = px + 8 + LIST_W + 6, dw = pw - LIST_W - 22;
        addRenderableWidget(new KTButton(dx + dw - 66, py + ph - 30, 60, 18, KTText.t("codex.close", "Close"), b -> onClose())
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
        Font font = this.font;
        KTTheme.frame(g, px, py, pw, ph);
        KTTheme.banner(g, font, px + 6, py + 6, pw - 12, 16, title);

        // Tier list
        int lx = px + 8, ly = listY(), lh = listH();
        KTTheme.inset(g, lx, ly, LIST_W, lh);
        if (maxScroll() > 0) {
            int trackX = lx + LIST_W - 8, trackY = ly + 5, trackH = lh - 10;
            g.fill(trackX, trackY, trackX + 3, trackY + trackH, 0x66000000);
            int thumbH = Math.max(12, trackH * visibleRows() / TIERS.length);
            int thumbY = trackY + (trackH - thumbH) * scroll / maxScroll();
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, KTTheme.GOLD_EDGE);
        }

        // Details
        int dx = lx + LIST_W + 6, dw = pw - LIST_W - 22, tx = dx + 8, tw = dw - 16;
        KTTheme.inset(g, dx, ly, dw, lh);
        int tierColor = selected.getColorValue() | 0xFF000000;
        int y = ly + 7;
        g.drawString(font, KTText.s("codex.tier_title", "%s TIER", selected.getDisplayName().toUpperCase()), tx, y, tierColor, true);
        String rank = KTText.s("codex.rank", "Tier %s of %s", selected.ordinal() + 1, TIERS.length);
        g.drawString(font, rank, dx + dw - 8 - font.width(rank), y, KTTheme.TEXT_DIM, false);

        y += 15;
        KTTheme.header(g, font, tx, y, tw, KTText.t("codex.harvest", "Habitat Harvest"));
        y += 12;
        ItemStack prod = selected.getProductionResult();
        g.renderItem(prod, tx, y);
        g.drawString(font, prod.getHoverName().getString() + " ×" + prod.getCount(), tx + 20, y + 1, 0xFFFCD34D, false);
        int ticks = selected.getBaseIntervalTicks();
        g.drawString(font, KTText.s("codex.interval", "every %s s (%s ticks)", String.format("%.1f", ticks / 20.0F), ticks),
                tx + 20, y + 10, KTTheme.TEXT_DIM, false);

        y += 24;
        KTTheme.header(g, font, tx, y, tw, KTText.t("codex.breeding", "Breeding"));
        y += 12;
        if (selected == ProductionTier.STANDARD) {
            y = wrap(g, font, KTText.t("codex.breeding.standard", "Wild village cats and newly tamed companions start here."), tx, y, tw, KTTheme.TEXT);
        } else {
            ProductionTier prev = TIERS[selected.ordinal() - 1];
            ItemStack a = prev.getProductionResult();
            g.renderItem(a, tx, y);
            g.drawString(font, "+", tx + 20, y + 4, KTTheme.TEXT, false);
            g.renderItem(a, tx + 28, y);
            g.drawString(font, "→", tx + 48, y + 4, KTTheme.TEXT, false);
            g.renderItem(prod, tx + 58, y);
            g.drawString(font, font.plainSubstrByWidth(KTText.s("codex.breeding.recipe", "two %s cats", prev.getDisplayName()), tw - 80),
                    tx + 78, y + 4, prev.getColorValue() | 0xFF000000, false);
            y += 20;
        }
        g.renderItem(new ItemStack(ModItems.BREEDING_CATNIP.get()), tx, y - 2);
        y = wrap(g, font, KTText.t("codex.breeding.item", "Breed with Breeding Catnip (or Spirit Catnip)."), tx + 20, y + 2, tw - 20, 0xFFA7F3D0) + 2;

        KTTheme.header(g, font, tx, y, tw, KTText.t("codex.nekomata", "Spirit Catnip & Nekomata"));
        y += 12;
        y = wrap(g, font, KTText.t("codex.nekomata.text",
                "Feed Spirit Catnip to either parent: the kitten is always a Nekomata, with two tails and double habitat yield."),
                tx, y, tw, 0xFF67E8F9);

        // Footnote beside the Close button, only when it fits under the text above.
        Component note = KTText.t("codex.note", "Coats are cosmetic skins; tiers only change through breeding.");
        int noteLines = font.split(note, tw - 70).size();
        int noteY = ly + lh - 6 - noteLines * 10;
        if (y + 2 <= noteY) wrap(g, font, note, tx, noteY, tw - 70, KTTheme.TEXT_DIM);
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Draws wrapped text; returns the y below it. */
    private static int wrap(GuiGraphics g, Font font, Component text, int x, int y, int w, int color) {
        List<FormattedCharSequence> lines = font.split(text, w);
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
