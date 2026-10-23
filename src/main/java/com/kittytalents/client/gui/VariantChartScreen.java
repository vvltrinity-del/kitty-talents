package com.kittytalents.client.gui;

import com.kittytalents.entity.ProductionTier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class VariantChartScreen extends Screen {
    private final List<ProductionTier> tiers = new ArrayList<>();
    private final List<ProductionTier> filteredTiers = new ArrayList<>();
    private ProductionTier selectedTier = ProductionTier.STANDARD;
    private int scrollOffset = 0;
    private EditBox searchBox;

    public VariantChartScreen() {
        super(Component.literal("Feline Tier Evolution & Breeding Codex"));
        for (ProductionTier t : ProductionTier.values()) {
            tiers.add(t);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int panelWidth = Math.min(520, this.width - 40);
        int panelHeight = Math.min(300, this.height - 40);
        int startX = (this.width - panelWidth) / 2;
        int startY = (this.height - panelHeight) / 2;

        this.searchBox = new EditBox(this.font, startX + 16, startY + 28, 160, 16, Component.literal("Search"));
        this.searchBox.setResponder(this::updateSearch);
        this.addWidget(this.searchBox);

        // Close Button
        this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose())
                .bounds(startX + panelWidth - 70, startY + panelHeight - 26, 60, 18).build());

        updateSearch(searchBox.getValue());
    }

    private void updateSearch(String query) {
        filteredTiers.clear();
        String lower = query.toLowerCase().trim();
        for (ProductionTier t : tiers) {
            if (lower.isEmpty() || t.getDisplayName().toLowerCase().contains(lower) || t.getId().contains(lower)) {
                filteredTiers.add(t);
            }
        }
        scrollOffset = 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (delta < 0) {
            int maxScroll = Math.max(0, filteredTiers.size() - 8);
            scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelWidth = Math.min(520, this.width - 40);
        int startX = (this.width - panelWidth) / 2;
        int startY = (this.height - Math.min(300, this.height - 40)) / 2;

        int listX = startX + 16;
        int listY = startY + 48;

        for (int i = 0; i < 8; i++) {
            int idx = scrollOffset + i;
            if (idx >= filteredTiers.size()) break;
            int itemY = listY + i * 24;
            if (mouseX >= listX && mouseX <= listX + 160 && mouseY >= itemY && mouseY <= itemY + 22) {
                this.selectedTier = filteredTiers.get(idx);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int panelWidth = Math.min(520, this.width - 40);
        int panelHeight = Math.min(300, this.height - 40);
        int startX = (this.width - panelWidth) / 2;
        int startY = (this.height - panelHeight) / 2;

        // Dark translucent panel
        graphics.fill(startX, startY, startX + panelWidth, startY + panelHeight, 0xEE111827);
        graphics.fill(startX + 1, startY + 1, startX + panelWidth - 1, startY + 22, 0xFF1F2937);

        // Header Title
        graphics.drawString(this.font, "FELINE TIER EVOLUTION & BREEDING CODEX", startX + 16, startY + 7, 0xFFF59E0B, false);

        // Search Box render
        if (searchBox != null) searchBox.render(graphics, mouseX, mouseY, partialTick);

        // Left List: Tiers
        int listX = startX + 16;
        int listY = startY + 48;
        for (int i = 0; i < 8; i++) {
            int idx = scrollOffset + i;
            if (idx >= filteredTiers.size()) break;

            ProductionTier t = filteredTiers.get(idx);
            boolean isSelected = (t == selectedTier);
            int itemY = listY + i * 24;

            graphics.fill(listX, itemY, listX + 160, itemY + 22, isSelected ? 0xFF374151 : 0xFF1F2937);
            // Color indicator dot
            graphics.fill(listX + 4, itemY + 6, listX + 14, itemY + 16, 0xFF000000 | t.getColorValue());

            graphics.drawString(this.font, t.getDisplayName() + " Tier", listX + 18, itemY + 7, 0xFF000000 | t.getColorValue(), false);
        }

        // Right Detail Card
        if (selectedTier != null) {
            int detailX = startX + 190;
            int detailY = startY + 30;
            int detailW = panelWidth - 206;
            int detailH = panelHeight - 65;

            graphics.fill(detailX, detailY, detailX + detailW, detailY + detailH, 0xAA1F2937);

            // Tier Name & Badge
            graphics.drawString(this.font, selectedTier.getDisplayName().toUpperCase() + " TIER", detailX + 12, detailY + 10, 0xFF000000 | selectedTier.getColorValue(), false);
            graphics.drawString(this.font, "Rank: Tier " + (selectedTier.ordinal() + 1) + " of 10", detailX + 12, detailY + 24, 0xFF9CA3AF, false);

            // Production yield
            graphics.drawString(this.font, "=== SIMULATED HABITAT HARVEST ===", detailX + 12, detailY + 44, 0xFFE5E7EB, false);
            ItemStack prod = selectedTier.getProductionResult();
            graphics.renderItem(prod, detailX + 12, detailY + 58);
            graphics.drawString(this.font, prod.getHoverName().getString() + " (x" + prod.getCount() + ")", detailX + 34, detailY + 62, 0xFFFBBF24, false);
            graphics.drawString(this.font, "Generation Interval: " + selectedTier.getBaseIntervalTicks() + " ticks", detailX + 12, detailY + 78, 0xFF9CA3AF, false);

            // Guaranteed Breeding Evolution
            graphics.drawString(this.font, "=== GUARANTEED BREEDING EVOLUTION ===", detailX + 12, detailY + 98, 0xFFE5E7EB, false);

            String formula;
            if (selectedTier == ProductionTier.STANDARD) {
                formula = "Wild Village Cats / Tamed Companions (Starting Tier)";
            } else {
                ProductionTier prev = ProductionTier.values()[selectedTier.ordinal() - 1];
                formula = prev.getDisplayName() + " Tier  +  " + prev.getDisplayName() + " Tier";
            }

            graphics.drawString(this.font, "Parent Recipe: " + formula, detailX + 12, detailY + 114, 0xFFA7F3D0, false);
            graphics.drawString(this.font, "Breeding Item: Breeding Catnip (or Spirit Catnip)", detailX + 12, detailY + 128, 0xFFFCD34D, false);

            // Mutation Notice
            graphics.drawString(this.font, "=== SPIRIT CATNIP & NEKOMATA ===", detailX + 12, detailY + 148, 0xFFE5E7EB, false);
            graphics.drawString(this.font, "✦ Feeding Spirit Catnip to either parent guarantees", detailX + 12, detailY + 162, 0xFF67E8F9, false);
            graphics.drawString(this.font, "  a NEKOMATA mutation offspring (dual tails & 2x yield)!", detailX + 12, detailY + 174, 0xFF67E8F9, false);

            graphics.drawString(this.font, "Note: Coat breeds are cosmetic skins; tiers evolve through breeding!", detailX + 12, detailY + 194, 0xFF9CA3AF, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
