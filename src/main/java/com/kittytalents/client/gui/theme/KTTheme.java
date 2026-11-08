package com.kittytalents.client.gui.theme;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Kitty Talents UI theme (Phase 6): Minecraft wood and leather. The frame, title strip and tabs are tiled from the
 * game's own planks (dark oak / spruce, so resource packs restyle them too), corners are iron rivets, and the inset
 * panels are stitched leather (our own seamless tile in the leather item's palette; vanilla has no leather block).
 * Buttons stay stone like the reference mockups. All drawing is plain GuiGraphics calls; nothing is cached.
 */
public final class KTTheme {
    private KTTheme() {}

    public static final ResourceLocation STONE = mc("stone");
    public static final ResourceLocation DARK_OAK = mc("dark_oak_planks");
    public static final ResourceLocation SPRUCE = mc("spruce_planks");
    public static final ResourceLocation LEATHER = new ResourceLocation("kittytalents", "textures/gui/leather.png");
    public static final int LEATHER_SIZE = 32;
    public static final int STITCH = 0xFFC9A26B;
    public static final ResourceLocation IRON = mc("iron_block");
    public static final ResourceLocation GOLD = mc("gold_block");
    public static final ResourceLocation AMETHYST = mc("amethyst_block");
    public static final ResourceLocation RED = mc("red_nether_bricks");

    // Text palette
    public static final int TEXT = 0xFFF2EEE6;
    public static final int TEXT_DIM = 0xFFA8A29E;
    public static final int TEXT_GOLD = 0xFFF0C75E;
    public static final int TEXT_DARK = 0xFF2B2006;
    public static final int GOLD_EDGE = 0xFFE0B04A;
    public static final int OUTLINE = 0xFF0B0B0C;

    private static ResourceLocation mc(String block) {
        return new ResourceLocation("minecraft", "textures/block/" + block + ".png");
    }

    /** Tile a 16x16 block texture over a rectangle (manual tiling: texture wrap mode is not guaranteed). */
    public static void tile(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h) {
        tile(g, tex, 16, x, y, w, h);
    }

    public static void tile(GuiGraphics g, ResourceLocation tex, int size, int x, int y, int w, int h) {
        for (int yy = y; yy < y + h; yy += size) {
            int bh = Math.min(size, y + h - yy);
            for (int xx = x; xx < x + w; xx += size) {
                int bw = Math.min(size, x + w - xx);
                g.blit(tex, xx, yy, 0, 0, bw, bh, size, size);
            }
        }
    }

    private static void bevel(GuiGraphics g, int x, int y, int w, int h, int light, int dark) {
        g.fill(x, y, x + w, y + 1, light);
        g.fill(x, y, x + 1, y + h, light);
        g.fill(x, y + h - 1, x + w, y + h, dark);
        g.fill(x + w - 1, y, x + w, y + h, dark);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.renderOutline(x, y, w, h, color);
    }

    /** The main dark-oak frame with iron corner rivets. */
    public static void frame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, OUTLINE);
        tile(g, DARK_OAK, x, y, w, h);
        g.fill(x, y, x + w, y + h, 0x30000000);
        bevel(g, x, y, w, h, 0x66FFFFFF, 0x88000000);
        bevel(g, x + 1, y + 1, w - 2, h - 2, 0x22FFFFFF, 0x44000000);
        rivet(g, x - 2, y - 2);
        rivet(g, x + w - 6, y - 2);
        rivet(g, x - 2, y + h - 6);
        rivet(g, x + w - 6, y + h - 6);
    }

    private static void rivet(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 8, y + 8, OUTLINE);
        tile(g, IRON, x + 1, y + 1, 6, 6);
        g.fill(x + 1, y + 1, x + 7, y + 7, 0x22000000);
        bevel(g, x + 1, y + 1, 6, 6, 0x88FFFFFF, 0x66000000);
    }

    /** A recessed stitched-leather sub-panel with small ornamental corners. */
    public static void inset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF1A120C);
        tile(g, LEATHER, LEATHER_SIZE, x, y, w, h);
        g.fill(x, y, x + w, y + h, 0x88140C08);
        bevel(g, x, y, w, h, 0x55000000, 0x22FFFFFF);
        stitch(g, x + 3, y + 3, w - 6, h - 6);
        int c = 0xFF8D8A84;
        corner(g, x + 2, y + 2, 1, 1, c);
        corner(g, x + w - 3, y + 2, -1, 1, c);
        corner(g, x + 2, y + h - 3, 1, -1, c);
        corner(g, x + w - 3, y + h - 3, -1, -1, c);
    }

    /** Dashed thread 3 px inside a leather panel's edge. */
    private static void stitch(GuiGraphics g, int x, int y, int w, int h) {
        for (int i = 0; i < w; i += 4) {
            g.fill(x + i, y, x + Math.min(i + 2, w), y + 1, STITCH);
            g.fill(x + i, y + h - 1, x + Math.min(i + 2, w), y + h, STITCH);
        }
        for (int i = 0; i < h; i += 4) {
            g.fill(x, y + i, x + 1, y + Math.min(i + 2, h), STITCH);
            g.fill(x + w - 1, y + i, x + w, y + Math.min(i + 2, h), STITCH);
        }
    }

    private static void corner(GuiGraphics g, int x, int y, int dx, int dy, int color) {
        for (int i = 0; i < 4; i++) {
            g.fill(x + i * dx, y, x + i * dx + 1, y + 1, color);
            g.fill(x, y + i * dy, x + 1, y + i * dy + 1, color);
        }
    }

    /** A highlighted bordered box inside an inset (description, mastery). */
    public static void box(GuiGraphics g, int x, int y, int w, int h, int border) {
        g.fill(x, y, x + w, y + h, 0x66000000);
        outline(g, x, y, w, h, border);
    }

    /** Title strip: "TALENTED CAT — Section" with a small chevron ornament. */
    public static void titleBar(GuiGraphics g, Font font, int x, int y, int w, int h, Component left) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, OUTLINE);
        tile(g, SPRUCE, x, y, w, h);
        g.fill(x, y, x + w, y + h, 0x99000000);
        bevel(g, x, y, w, h, 0x33FFFFFF, 0x66000000);
        g.drawString(font, left, x + 8, y + (h - 8) / 2, TEXT_GOLD, true);
        int lx = x + 10 + font.width(left);
        for (int i = 0; i < 3; i++) g.fill(lx + 4 + i * 4, y + h / 2 - 1, lx + 6 + i * 4, y + h / 2 + 1, 0xFF6B6760);
    }

    /** A folder tab above the frame. Active tabs are lighter and gold-edged. */
    public static void tab(GuiGraphics g, Font font, int x, int y, int w, int h, Component label, int color, boolean active, boolean hovered) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + (active ? 0 : 1), OUTLINE);
        tile(g, SPRUCE, x, y, w, h);
        g.fill(x, y, x + w, y + h, active ? 0x10000000 : (hovered ? 0x40000000 : 0x77000000));
        bevel(g, x, y, w, h, 0x44FFFFFF, active ? 0x00000000 : 0x66000000);
        if (active) {
            g.fill(x - 1, y - 1, x + w + 1, y, GOLD_EDGE);
            g.fill(x - 1, y - 1, x, y + h, GOLD_EDGE);
            g.fill(x + w, y - 1, x + w + 1, y + h, GOLD_EDGE);
        }
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2 + 1, active ? color : dim(color));
    }

    /** Rank pips: filled gold squares for trained ranks, dark for untrained. */
    public static void pips(GuiGraphics g, int x, int y, int filled, int max, int size, int gap) {
        for (int i = 0; i < max; i++) {
            int px = x + i * (size + gap);
            g.fill(px, y, px + size, y + size, OUTLINE);
            boolean on = i < filled;
            g.fill(px + 1, y + 1, px + size - 1, y + size - 1, on ? 0xFFE8B23A : 0xFF2E2C2A);
            if (on) g.fill(px + 1, y + 1, px + size - 1, y + 2, 0xFFFFE08A);
        }
    }

    /** An item/equipment slot (18x18). */
    public static void slot(GuiGraphics g, int x, int y, boolean highlight) {
        g.fill(x, y, x + 18, y + 18, OUTLINE);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF2A2A2E);
        bevel(g, x + 1, y + 1, 16, 16, 0xFF151517, 0xFF55555A);
        if (highlight) outline(g, x, y, 18, 18, GOLD_EDGE);
    }

    /** A small coloured label badge (e.g. PASSIVE / RADAR / SYSTEM). Returns its width. */
    public static int badge(GuiGraphics g, Font font, int x, int y, String text, int color) {
        int w = font.width(text) + 6;
        g.fill(x, y, x + w, y + 10, (color & 0x00FFFFFF) | 0x55000000);
        outline(g, x, y, w, 10, color);
        g.drawString(font, text, x + 3, y + 1, color, false);
        return w;
    }

    /** Filled circle (scanline fills). */
    public static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            g.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, color);
        }
    }

    /** Round portrait frame: outline, gold ring, dark leather-toned interior. */
    public static void roundFrame(GuiGraphics g, int cx, int cy, int r) {
        disc(g, cx, cy, r + 3, OUTLINE);
        disc(g, cx, cy, r + 2, GOLD_EDGE);
        disc(g, cx, cy, r, 0xFF1A120C);
        disc(g, cx, cy, r - 2, 0xFF2A1D14);
    }

    /** Square portrait frame: iron-rivet wood border around a dark backdrop. */
    public static void portrait(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, OUTLINE);
        tile(g, DARK_OAK, x, y, w, h);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, OUTLINE);
        g.fillGradient(x + 4, y + 4, x + w - 4, y + h - 4, 0xFF2A1D14, 0xFF120C08);
        rivet(g, x - 2, y - 2);
        rivet(g, x + w - 6, y - 2);
        rivet(g, x - 2, y + h - 6);
        rivet(g, x + w - 6, y + h - 6);
    }

    /** Horizontal bar (HP / XP / Energy). */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, float fraction, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, OUTLINE);
        g.fill(x, y, x + w, y + h, 0xFF2E2C2A);
        int fw = Math.round(w * Math.max(0F, Math.min(1F, fraction)));
        if (fw > 0) {
            g.fill(x, y, x + fw, y + h, color);
            g.fill(x, y, x + fw, y + 1, 0x55FFFFFF);
        }
    }

    /** Small gold section header with a rule to the right. */
    public static void header(GuiGraphics g, Font font, int x, int y, int w, Component text) {
        g.drawString(font, text, x, y, TEXT_GOLD, true);
        int lx = x + font.width(text) + 4;
        if (lx < x + w) g.fill(lx, y + 4, x + w, y + 5, 0x66E0B04A);
    }

    /** HUD panel: thin dark-oak border around stitched leather, lighter than the screen frame. */
    public static void hudPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, OUTLINE);
        tile(g, DARK_OAK, x, y, w, h);
        bevel(g, x, y, w, h, 0x55FFFFFF, 0x88000000);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF1A120C);
        tile(g, LEATHER, LEATHER_SIZE, x + 3, y + 3, w - 6, h - 6);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0x99140C08);
    }

    public static int dim(int color) {
        int r = ((color >> 16) & 0xFF) * 3 / 5, gr = ((color >> 8) & 0xFF) * 3 / 5, b = (color & 0xFF) * 3 / 5;
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }
}
