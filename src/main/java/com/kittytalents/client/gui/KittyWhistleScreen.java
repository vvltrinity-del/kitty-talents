package com.kittytalents.client.gui;

import com.kittytalents.entity.WhistleCommand;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.network.PacketWhistleCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class KittyWhistleScreen extends Screen {
    private EditBox searchBox;
    private final List<WhistleCommand> filteredCommands = new ArrayList<>();

    public KittyWhistleScreen() {
        super(Component.literal("Kitty Whistle Command Center"));
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int centerX = this.width / 2;
        int topY = 32;

        this.searchBox = new EditBox(this.font, centerX - 100, topY, 200, 18, Component.literal("Search Commands"));
        this.searchBox.setResponder(this::onSearchChanged);
        this.addWidget(this.searchBox);

        updateFilteredCommands("");
    }

    private void onSearchChanged(String query) {
        updateFilteredCommands(query);
    }

    private void updateFilteredCommands(String query) {
        this.clearWidgets();
        this.addWidget(this.searchBox);

        filteredCommands.clear();
        String lower = query.toLowerCase(Locale.ROOT).trim();
        for (WhistleCommand cmd : WhistleCommand.values()) {
            if (lower.isEmpty() || cmd.getDisplayName().toLowerCase(Locale.ROOT).contains(lower)) {
                filteredCommands.add(cmd);
            }
        }

        int centerX = this.width / 2;
        int startY = 60;
        int cols = 3;
        int btnW = 120;
        int btnH = 20;
        int gap = 6;
        int totalW = (cols * btnW) + ((cols - 1) * gap);
        int startX = centerX - (totalW / 2);

        for (int i = 0; i < filteredCommands.size(); i++) {
            WhistleCommand cmd = filteredCommands.get(i);
            int col = i % cols;
            int row = i / cols;

            int btnX = startX + col * (btnW + gap);
            int btnY = startY + row * (btnH + gap);

            this.addRenderableWidget(Button.builder(Component.literal(cmd.getDisplayName()), b -> {
                PacketHandler.sendToServer(new PacketWhistleCommand(cmd));
                this.onClose();
            }).bounds(btnX, btnY, btnW, btnH).build());
        }

        // Close button at bottom
        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
                .bounds(centerX - 40, this.height - 28, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, centerX, 12, 0xFFE080);
        graphics.drawCenteredString(this.font, "Execute commands to all nearby companion cats simultaneously", centerX, 22, 0x888888);

        this.searchBox.render(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (keyCode == 256) { // Escape
                this.searchBox.setFocused(false);
                return true;
            }
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (this.searchBox != null && !this.searchBox.isMouseOver(mouseX, mouseY)) {
            this.searchBox.setFocused(false);
        }
        return handled;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
