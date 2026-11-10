package com.kittytalents.client.gui;

import com.kittytalents.client.gui.theme.KTButton;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.network.PacketConductCat;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.network.PacketSyncConductList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProducerStickScreen extends Screen {
    private final List<PacketSyncConductList.ConductCatDto> allCats;
    private final List<PacketSyncConductList.ConductCatDto> filtered = new ArrayList<>();
    private boolean toBedMode;
    private EditBox searchBox;
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private int px, py, pw, ph;

    private static final int ROW_HEIGHT = 22;
    private static final int VISIBLE_ROWS = 6;

    public ProducerStickScreen(List<PacketSyncConductList.ConductCatDto> cats, boolean defaultToBed) {
        super(KTText.t("producer_stick.title", "PRODUCER STICK"));
        this.allCats = new ArrayList<>(cats);
        this.toBedMode = defaultToBed;
    }

    @Override
    protected void init() {
        pw = Math.min(340, width - 20);
        ph = 230;
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        String query = searchBox != null ? searchBox.getValue() : "";
        searchBox = new EditBox(font, px + 16, py + 36, pw - 32, 14, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search cats...").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setValue(query);
        searchBox.setResponder(q -> {
            selectedIndex = 0;
            scrollOffset = 0;
            updateFiltered();
        });

        rebuild();
    }

    private void updateFiltered() {
        filtered.clear();
        String q = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";
        for (PacketSyncConductList.ConductCatDto c : allCats) {
            if (q.isEmpty() || c.name.toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(c);
            }
        }
        if (selectedIndex >= filtered.size()) selectedIndex = Math.max(0, filtered.size() - 1);
    }

    private void rebuild() {
        clearWidgets();
        addRenderableWidget(searchBox);

        // Mode buttons: To Self / To Bed
        int modeBtnW = (pw - 36) / 2;
        addRenderableWidget(new KTButton(px + 16, py + 54, modeBtnW, 16, Component.literal("To Self"), b -> {
            toBedMode = false;
            rebuild();
        }).style(KTButton.Style.CHIP).selected(!toBedMode).color(!toBedMode ? 0xFF818CF8 : 0xFF9CA3AF));

        addRenderableWidget(new KTButton(px + 20 + modeBtnW, py + 54, modeBtnW, 16, Component.literal("To Bed"), b -> {
            toBedMode = true;
            rebuild();
        }).style(KTButton.Style.CHIP).selected(toBedMode).color(toBedMode ? 0xFFF472B6 : 0xFF9CA3AF));

        // Conduct Button
        addRenderableWidget(new KTButton(px + 16, py + ph - 26, pw - 32, 18, Component.literal("Conduct [Enter]"), b -> {
            conductSelected();
        }).style(KTButton.Style.PURPLE));

        updateFiltered();
    }

    private void conductSelected() {
        if (filtered.isEmpty() || selectedIndex < 0 || selectedIndex >= filtered.size()) return;
        PacketSyncConductList.ConductCatDto selected = filtered.get(selectedIndex);
        PacketConductCat.Action action = toBedMode ? PacketConductCat.Action.TO_BED : PacketConductCat.Action.TO_SELF;
        PacketHandler.sendToServer(new PacketConductCat(selected.catUUID, action));
        this.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            conductSelected();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            if (selectedIndex > 0) {
                selectedIndex--;
                if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            if (selectedIndex < filtered.size() - 1) {
                selectedIndex++;
                if (selectedIndex >= scrollOffset + VISIBLE_ROWS) scrollOffset = selectedIndex - VISIBLE_ROWS + 1;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0 && scrollOffset > 0) {
            scrollOffset--;
            return true;
        } else if (delta < 0 && scrollOffset + VISIBLE_ROWS < filtered.size()) {
            scrollOffset++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listY = py + 74;
        if (mouseX >= px + 16 && mouseX <= px + pw - 16 && mouseY >= listY && mouseY <= listY + (VISIBLE_ROWS * ROW_HEIGHT)) {
            int clickedRow = (int) ((mouseY - listY) / ROW_HEIGHT);
            int targetIdx = scrollOffset + clickedRow;
            if (targetIdx >= 0 && targetIdx < filtered.size()) {
                selectedIndex = targetIdx;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        renderBackground(g);
        KTTheme.frame(g, px, py, pw, ph);
        KTTheme.titleBar(g, font, px, py, pw, 24, Component.literal("PRODUCER STICK"));

        super.render(g, mouseX, mouseY, partialTicks);

        int listY = py + 74;
        int listW = pw - 32;

        if (filtered.isEmpty()) {
            g.drawCenteredString(font, Component.literal("No cats registered in this dimension"), px + pw / 2, listY + 30, 0xFFA0A0A0);
        } else {
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int catIdx = scrollOffset + i;
                if (catIdx >= filtered.size()) break;

                PacketSyncConductList.ConductCatDto cat = filtered.get(catIdx);
                int rowY = listY + (i * ROW_HEIGHT);
                boolean isSelected = (catIdx == selectedIndex);

                int bg = isSelected ? 0x55818CF8 : 0x22FFFFFF;
                int border = isSelected ? 0xFF818CF8 : 0x33FFFFFF;
                g.fill(px + 16, rowY, px + 16 + listW, rowY + ROW_HEIGHT - 2, bg);
                g.renderOutline(px + 16, rowY, listW, ROW_HEIGHT - 2, border);

                int textCol = isSelected ? 0xFFFFFFFF : 0xFFE0E0E0;
                g.drawString(font, cat.name, px + 22, rowY + 3, textCol);

                String subText;
                if (toBedMode) {
                    subText = cat.bedPos != null ? "Bed: " + cat.bedPos.getX() + ", " + cat.bedPos.getY() + ", " + cat.bedPos.getZ() : "No bed set";
                } else {
                    subText = cat.pos != null ? "Pos: " + cat.pos.getX() + ", " + cat.pos.getY() + ", " + cat.pos.getZ() : "Dimension: " + cat.dimension;
                }
                g.drawString(font, subText, px + 22, rowY + 12, 0xFFAAAAAA);
            }
        }
    }
}
