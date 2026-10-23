package com.kittytalents.client.gui;

import com.kittytalents.block.KittyHabitatBlockEntity;
import com.kittytalents.inventory.KittyHabitatMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class KittyHabitatScreen extends AbstractContainerScreen<KittyHabitatMenu> {
    private final KittyHabitatBlockEntity habitat;

    public KittyHabitatScreen(KittyHabitatMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.habitat = menu.getHabitat();
        this.imageWidth = 200;
        this.imageHeight = 216;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        // Control buttons
        this.addRenderableWidget(Button.builder(Component.literal("Dismiss Worker"), button -> {
            habitat.removeWorker();
            button.active = false;
        }).bounds(this.leftPos + 104, this.topPos + 58, 86, 16).build());

        this.addRenderableWidget(Button.builder(Component.literal(habitat.isPaused() ? "Resume" : "Pause"), button -> {
            habitat.setPaused(!habitat.isPaused());
            button.setMessage(Component.literal(habitat.isPaused() ? "Resume" : "Pause"));
        }).bounds(this.leftPos + 104, this.topPos + 76, 86, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Dark translucent panel
        int x = this.leftPos;
        int y = this.topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xDD15151C);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 19, 0xFF2A2A38);

        // Header
        graphics.drawString(this.font, "Kitty Production Habitat", x + 10, y + 6, 0xFFFACC15, false);

        // Worker Panel Box (Left)
        graphics.fill(x + 10, y + 24, x + 96, y + 84, 0x8820202A);

        if (habitat.getAssignedCatUUID() != null) {
            graphics.drawString(this.font, habitat.getAssignedCatName(), x + 14, y + 28, 0xFF55FFFF, false);
            graphics.drawString(this.font, habitat.getAssignedCatVariant().getDisplayName(), x + 14, y + 40, 0xFFAAAAAA, false);
            graphics.drawString(this.font, "Lv." + habitat.getAssignedCatLevel() + " | " + (habitat.isAssignedCatFemale() ? "♀" : "♂"), x + 14, y + 52, 0xFFE0E0E0, false);
            graphics.drawString(this.font, habitat.getAssignedCatRole(), x + 14, y + 64, 0xFF55FF55, false);
        } else {
            graphics.drawString(this.font, "No Worker", x + 18, y + 40, 0xFFFF5555, false);
            graphics.drawString(this.font, "Use Clipboard", x + 14, y + 54, 0xFF888888, false);
        }

        // Status line
        graphics.drawString(this.font, "Status: " + habitat.getStatusMessage(), x + 104, y + 28, 0xFFE5E7EB, false);
        graphics.drawString(this.font, "Cycles: " + habitat.getTotalCycles(), x + 104, y + 40, 0xFF9CA3AF, false);

        // Progress Bar
        int progress = habitat.getProgress();
        int maxProgress = Math.max(1, habitat.getMaxProgress());
        int barWidth = 84;
        int filled = (progress * barWidth) / maxProgress;
        graphics.fill(x + 104, y + 50, x + 104 + barWidth, y + 54, 0xFF374151);
        graphics.fill(x + 104, y + 50, x + 104 + filled, y + 54, 0xFF10B981);

        // Labels for slots
        graphics.drawString(this.font, "Upgrades", x + 16, y + 84, 0xFF9CA3AF, false);
        graphics.drawString(this.font, "Outputs", x + 108, y + 84, 0xFF9CA3AF, false);

        // Draw slot outlines for Upgrades (0..3)
        for (int i = 0; i < 4; i++) {
            int slotX = x + 15 + i * 20;
            int slotY = y + 93;
            graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF2D3748);
        }

        // Draw slot outlines for Outputs (4..7)
        for (int i = 0; i < 4; i++) {
            int slotX = x + 107 + i * 20;
            int slotY = y + 93;
            graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF2D3748);
        }

        // Draw slot outlines for Food (8) & Catalyst (9)
        graphics.fill(x + 55, y + 65, x + 73, y + 83, 0xFF1E3A5F);
        graphics.fill(x + 75, y + 65, x + 93, y + 83, 0xFF3B2854);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
