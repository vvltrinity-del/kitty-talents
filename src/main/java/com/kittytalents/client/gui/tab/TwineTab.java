package com.kittytalents.client.gui.tab;

import com.kittytalents.client.gui.CatTalentScreen;
import com.kittytalents.client.gui.theme.KTButton;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.network.PacketTwineAction;
import com.kittytalents.registry.ModItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Twine: the commence button, the control map and the cat body's built-in capabilities. */
public class TwineTab implements CatTab {
    private static final String[][] CONTROLS = {
            { "move", "W A S D", "Move, with step-assist" },
            { "jump", "Space", "High jump, wall moves" },
            { "attack", "L-Click", "Claw combo, bite finisher" },
            { "pounce", "R-Click", "Pounce / interact" },
            { "dodge", "Alt", "Dodge in any direction" },
            { "abilities", "V G B", "Slotted abilities 1-3" },
            { "ultimate", "R", "Ultimate (full meter)" },
            { "exit", "U", "Return to your body" },
    };

    private static final int BANNER_H = 46;

    @Override
    public Component title(CatTalentScreen s) { return KTText.t("tab.twine.title", "Soul Twining"); }

    @Override
    public void init(CatTalentScreen s) {
        int bw = 210;
        s.add(new KTButton(s.cx() + (s.cw() - bw) / 2, s.cy() + 7, bw, 20, KTText.t("twine.commence", "✦ COMMENCE TWINING ✦"), b -> {
            PacketHandler.sendToServer(new PacketTwineAction(s.cat().getId(), true));
            s.onClose();
        }).style(KTButton.Style.PURPLE).color(0xFFF3E8FF));
    }

    @Override
    public void render(CatTalentScreen s, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Font font = s.font();
        int x = s.cx(), y = s.cy(), w = s.cw(), h = s.ch();

        KTTheme.inset(g, x, y, w, BANNER_H);
        String intro = KTText.s("twine.intro", "Control your cat directly. Your body sleeps safely while you're away.");
        g.drawCenteredString(font, font.plainSubstrByWidth(intro, w - 12), x + w / 2, y + 32, KTTheme.TEXT_DIM);

        int py = y + BANNER_H + 6, ph = h - BANNER_H - 6;
        int colW = (w - 6) / 2;

        // Controls
        KTTheme.inset(g, x, py, colW, ph);
        KTTheme.header(g, font, x + 7, py + 6, colW - 14, KTText.t("twine.controls", "Controls"));
        int keyW = 0;
        for (String[] c : CONTROLS) keyW = Math.max(keyW, font.width(c[1]) + 8);
        int rowH = Math.min(16, (ph - 22) / CONTROLS.length);
        for (int i = 0; i < CONTROLS.length; i++) {
            String[] c = CONTROLS[i];
            int ry = py + 19 + i * rowH;
            int kx = x + 8;
            g.fill(kx - 1, ry - 1, kx + keyW + 1, ry + 11, KTTheme.OUTLINE);
            KTTheme.tile(g, KTTheme.STONE, kx, ry, keyW, 11);
            g.fill(kx, ry, kx + keyW, ry + 11, 0x55000000);
            g.drawCenteredString(font, c[1], kx + keyW / 2, ry + 2, KTTheme.TEXT);
            String d = KTText.s("twine.control." + c[0], c[2]);
            g.drawString(font, font.plainSubstrByWidth(d, colW - keyW - 22), kx + keyW + 6, ry + 2, KTTheme.TEXT, false);
        }

        // Capabilities
        int cx = x + colW + 6, cw = w - colW - 6;
        KTTheme.inset(g, cx, py, cw, ph);
        KTTheme.header(g, font, cx + 7, py + 6, cw - 14, KTText.t("twine.capabilities", "Cat Body"));
        String regen = String.valueOf((int) (s.cat().getCatRole().getEnergyRegenMultiplier() * 10));
        Object[][] caps = {
                { "PASSIVE", 0xFF6EE7A0, KTText.s("twine.cap.fall", "No fall damage"), KTText.s("twine.cap.fall.desc", "Cats always land on their feet.") },
                { "PASSIVE", 0xFF7DD3FC, KTText.s("twine.cap.crawl", "Squeeze through gaps"), KTText.s("twine.cap.crawl.desc", "Fit through 1-block crawlspaces.") },
                { "SENSE", 0xFFFBBF24, KTText.s("twine.cap.vision", "Low-light vision"), KTText.s("twine.cap.vision.desc", "Night vision and movement sense.") },
                { "ENERGY", 0xFFC084FC, KTText.s("twine.cap.energy", "Energy reserve"), KTText.s("twine.cap.energy.desc", "%s Energy, +%s per second.", s.cat().getMaxEnergy(), regen) },
        };
        int capH = Math.min(34, (ph - 22) / caps.length);
        for (int i = 0; i < caps.length; i++) {
            int ry = py + 19 + i * capH;
            int bw = KTTheme.badge(g, font, cx + 8, ry, (String) caps[i][0], (Integer) caps[i][1]);
            g.drawString(font, font.plainSubstrByWidth((String) caps[i][2], cw - bw - 22), cx + 12 + bw, ry + 1, KTTheme.TEXT, false);
            List<FormattedCharSequence> d = font.split(Component.literal((String) caps[i][3]), cw - 18);
            if (!d.isEmpty()) g.drawString(font, d.get(0), cx + 9, ry + 13, KTTheme.TEXT_DIM, false);
        }
    }
}
