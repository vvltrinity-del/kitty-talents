package com.kittytalents.client.gui.tab;

import com.kittytalents.client.gui.CatTalentScreen;
import com.kittytalents.client.gui.VariantChartScreen;
import com.kittytalents.client.gui.theme.KTButton;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.client.gui.theme.KTWidget;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.network.PacketChangeCatSkin;
import com.kittytalents.network.PacketChangeCollarColor;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.network.PacketRenameCat;
import com.kittytalents.registry.ModItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Style: sub-page buttons on the left, a round portrait with appearance info, and the collar / skin / variant pages. */
public class StyleTab implements CatTab {
    public static final String[] SKIN_NAMES = {
            "Tabby", "Tuxedo", "Red", "Siamese", "British Shorthair",
            "Calico", "Persian", "Ragdoll", "White", "Jellie",
            "All Black", "Tiger", "Snow Leopard", "Void Ender", "Golden Maneki", "Ghost Phantom", "Panther"
    };
    private static final String[] RANDOM_CAT_NAMES = {
            "Luna", "Milo", "Oliver", "Leo", "Bella", "Charlie", "Chloe", "Lucy", "Simba", "Lily",
            "Loki", "Shadow", "Cleo", "Jack", "Felix", "Nala", "Jasper", "Smokey", "Willow", "Boots"
    };
    private static final int LEFT_W = 104, TOP_H = 80;

    private int sub; // 0 accessories, 1 skins, 2 variants

    @Override
    public Component title(CatTalentScreen s) {
        return switch (sub) {
            case 1 -> KTText.t("tab.style.skins", "Style · Skins");
            case 2 -> KTText.t("tab.style.variants", "Style · Variants");
            default -> KTText.t("tab.style.accessories", "Style · Accessories");
        };
    }

    private int rx(CatTalentScreen s) { return s.cx() + LEFT_W + 6; }
    private int rw(CatTalentScreen s) { return s.cw() - LEFT_W - 6; }
    private int pageY(CatTalentScreen s) { return s.cy() + TOP_H + 6; }
    private int pageH(CatTalentScreen s) { return s.ch() - TOP_H - 6; }

    @Override
    public void init(CatTalentScreen s) {
        TalentedCat cat = s.cat();
        int x = s.cx() + 6, w = LEFT_W - 12, y = s.cy() + 8;
        String[] keys = { "accessories", "skins", "variants" };
        String[] names = { "Accessories", "Skins", "Variants" };
        ItemStack[] icons = { new ItemStack(ModItems.CAT_COLLAR.get()), new ItemStack(Items.PAINTING), new ItemStack(Items.BOOK) };
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            s.add(new KTButton(x, y + i * 24, w, 20, KTText.t("style." + keys[i], names[i]), b -> {
                sub = idx;
                s.rebuild();
            }).icon(icons[i]).left().selected(sub == i));
        }
        s.add(new KTButton(x, s.cy() + s.ch() - 26, w, 18, KTText.t("style.random_name", "Random Name"), b -> {
            String rand = RANDOM_CAT_NAMES[(int) (Math.random() * RANDOM_CAT_NAMES.length)];
            cat.setCustomName(Component.literal(rand));
            PacketHandler.sendToServer(new PacketRenameCat(cat.getId(), rand));
            s.rebuild();
        }).icon(new ItemStack(Items.NAME_TAG)));

        int px = rx(s) + 6, py = pageY(s) + 18, pw = rw(s) - 12;
        if (sub == 0) {
            DyeColor[] colors = DyeColor.values();
            int cell = Math.min(22, (pw) / 8);
            for (int c = 0; c < colors.length; c++) {
                DyeColor dye = colors[c];
                int sx = px + (c % 8) * cell, sy = py + (c / 8) * cell;
                float[] rgb = dye.getTextureDiffuseColors();
                int color = 0xFF000000 | ((int) (rgb[0] * 255) << 16) | ((int) (rgb[1] * 255) << 8) | (int) (rgb[2] * 255);
                s.add(new KTWidget(sx, sy, cell - 4, cell - 4, Component.literal(dye.getName()), (g, wx, wy, ww, wh, hover) -> {
                    boolean current = cat.getCollarColor() == dye;
                    g.fill(wx - 1, wy - 1, wx + ww + 1, wy + wh + 1, current ? KTTheme.GOLD_EDGE : KTTheme.OUTLINE);
                    g.fill(wx, wy, wx + ww, wy + wh, color);
                    g.fill(wx, wy, wx + ww, wy + 1, 0x55FFFFFF);
                    g.fill(wx, wy + wh - 1, wx + ww, wy + wh, 0x55000000);
                    if (hover) KTTheme.outline(g, wx - 1, wy - 1, ww + 2, wh + 2, 0xFFFFFFFF);
                }, () -> {
                    cat.setCollarColor(dye);
                    PacketHandler.sendToServer(new PacketChangeCollarColor(cat.getId(), dye.getId()));
                }).hoverTip(() -> List.<FormattedCharSequence>of(Component.translatable("color.minecraft." + dye.getName()).getVisualOrderText())));
            }
        } else if (sub == 1) {
            int cols = 3, bw = (pw - (cols - 1) * 3) / cols;
            for (int i = 0; i < SKIN_NAMES.length; i++) {
                final int id = i;
                s.add(new KTButton(px + (i % cols) * (bw + 3), pageY(s) + 6 + (i / cols) * 18, bw, 16,
                        KTText.t("style.skin." + i, SKIN_NAMES[i]), b -> {
                    cat.setSkinId(id);
                    PacketHandler.sendToServer(new PacketChangeCatSkin(cat.getId(), id));
                    s.rebuild();
                }).style(KTButton.Style.CHIP).selected(cat.getSkinId() == i));
            }
        } else {
            s.add(new KTButton(px, pageY(s) + pageH(s) - 28, pw, 20, KTText.t("style.codex", "Open Breeding Codex"),
                    b -> s.mc().setScreen(new VariantChartScreen()))
                    .style(KTButton.Style.GOLD).icon(new ItemStack(Items.WRITABLE_BOOK)));
        }
    }

    @Override
    public void render(CatTalentScreen s, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        TalentedCat cat = s.cat();
        Font font = s.font();
        KTTheme.inset(g, s.cx(), s.cy(), LEFT_W, s.ch());

        // Top card: round portrait + appearance
        int rx = rx(s), rw = rw(s), y = s.cy();
        KTTheme.inset(g, rx, y, rw, TOP_H);
        int r = 32, ccx = rx + 8 + r + 3, ccy = y + TOP_H / 2;
        KTTheme.roundFrame(g, ccx, ccy, r);
        g.enableScissor(ccx - r + 4, ccy - r + 4, ccx + r - 4, ccy + r - 2);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, ccx, ccy + r - 12, 48, (float) ccx - mouseX, (float) (ccy - 10) - mouseY, cat);
        g.disableScissor();

        int ix = ccx + r + 12, iy = y + 9, maxW = rx + rw - 8 - ix;
        int tierColor = cat.getProductionTier().getColorValue() | 0xFF000000;
        g.drawString(font, font.plainSubstrByWidth(cat.getName().getString(), maxW), ix, iy, tierColor, true);
        line(g, font, ix, iy + 13, KTText.s("style.breed", "Breed"), cat.getCatVariant().getDisplayName(),
                cat.getCatVariant().getRarity().getColor() | 0xFF000000, maxW);
        line(g, font, ix, iy + 24, KTText.s("style.tier", "Tier"), cat.getProductionTier().getDisplayName(), tierColor, maxW);
        line(g, font, ix, iy + 35, KTText.s("style.skin", "Skin"),
                cat.getSkinId() < SKIN_NAMES.length ? KTText.s("style.skin." + cat.getSkinId(), SKIN_NAMES[cat.getSkinId()]) : "Custom",
                KTTheme.TEXT, maxW);
        line(g, font, ix, iy + 46, KTText.s("style.gender", "Gender"), cat.isFemale() ? "♀ Female" : "♂ Male",
                cat.isFemale() ? 0xFFF472B6 : 0xFF60A5FA, maxW);
        g.drawString(font, font.plainSubstrByWidth(KTText.s("style.gender_hint", "(change with a Gender Ribbon)"), maxW),
                ix, iy + 57, 0xFF8B8580, false);

        // Page card
        int py = pageY(s), ph = pageH(s);
        KTTheme.inset(g, rx, py, rw, ph);
        if (sub == 0) {
            KTTheme.header(g, font, rx + 6, py + 6, rw - 12, KTText.t("style.collar", "Collar Colour"));
            int cell = Math.min(22, (rw - 12) / 8);
            int hy = py + 18 + 2 * cell + 4;
            g.drawString(font, font.plainSubstrByWidth(KTText.s("style.collar_hint", "Cosmetic items go in the Gear tab's cosmetic slots."), rw - 14),
                    rx + 7, hy, KTTheme.TEXT_DIM, false);
        } else if (sub == 2) {
            KTTheme.header(g, font, rx + 6, py + 6, rw - 12, KTText.t("style.variant", "Variant"));
            g.drawString(font, cat.getCatVariant().getDisplayName(), rx + 8, py + 20, cat.getCatVariant().getRarity().getColor() | 0xFF000000, true);
            List<FormattedCharSequence> lines = font.split(KTText.t("style.variant_hint",
                    "The codex lists every breed and its rarity, plus the tier evolution and breeding paths."), rw - 16);
            for (int i = 0; i < Math.min(4, lines.size()); i++) g.drawString(font, lines.get(i), rx + 8, py + 34 + i * 10, KTTheme.TEXT_DIM, false);
        }
    }

    private static void line(GuiGraphics g, Font font, int x, int y, String label, String value, int color, int maxW) {
        String l = label + ": ";
        g.drawString(font, l, x, y, KTTheme.TEXT_DIM, false);
        g.drawString(font, font.plainSubstrByWidth(value, Math.max(10, maxW - font.width(l))), x + font.width(l), y, color, false);
    }
}
