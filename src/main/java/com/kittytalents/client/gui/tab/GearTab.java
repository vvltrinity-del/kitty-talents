package com.kittytalents.client.gui.tab;

import com.kittytalents.client.gui.CatTalentScreen;
import com.kittytalents.client.gui.theme.KTButton;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.client.gui.theme.KTWidget;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.network.PacketEquipCatItem;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Function;

/** Gear: framed cat in the middle, hand slots left, armor right, cosmetics along the bottom. */
public class GearTab implements CatTab {
    private static final int PW = 112;

    private record SlotDef(int id, String key, String name, ItemStack ghost, Function<TalentedCat, ItemStack> getter) {}

    private static final SlotDef[] LEFT = {
            new SlotDef(0, "weapon", "Weapon", new ItemStack(Items.IRON_SWORD), TalentedCat::getEquippedWeapon),
            new SlotDef(5, "accessory", "Accessory", ItemStack.EMPTY, TalentedCat::getEquippedAccessory),
            new SlotDef(6, "tool", "Tool", new ItemStack(Items.IRON_PICKAXE), TalentedCat::getEquippedTool),
    };
    private static final SlotDef[] RIGHT = {
            new SlotDef(1, "head", "Helmet", new ItemStack(Items.IRON_HELMET), c -> c.getArmorSlotItem(1)),
            new SlotDef(2, "chest", "Chest", new ItemStack(Items.IRON_CHESTPLATE), c -> c.getArmorSlotItem(2)),
            new SlotDef(3, "legs", "Legs", new ItemStack(Items.IRON_LEGGINGS), c -> c.getArmorSlotItem(3)),
            new SlotDef(4, "boots", "Boots", new ItemStack(Items.IRON_BOOTS), c -> c.getArmorSlotItem(4)),
    };
    private static final SlotDef[] COSMETIC = {
            new SlotDef(7, "cosmetic1", "Cosmetic 1", ItemStack.EMPTY, c -> c.getCosmeticSlot(0)),
            new SlotDef(8, "cosmetic2", "Cosmetic 2", ItemStack.EMPTY, c -> c.getCosmeticSlot(1)),
            new SlotDef(9, "cosmetic3", "Cosmetic 3", ItemStack.EMPTY, c -> c.getCosmeticSlot(2)),
    };

    @Override
    public Component title(CatTalentScreen s) { return KTText.t("tab.gear.title", "Equipment"); }

    private int portraitX(CatTalentScreen s) { return s.cx() + (s.cw() - PW) / 2; }
    private int portraitY(CatTalentScreen s) { return s.cy() + 8; }
    private int leftX(CatTalentScreen s) { return portraitX(s) - 30; }
    private int rightX(CatTalentScreen s) { return portraitX(s) + PW + 12; }
    private int ph(CatTalentScreen s) { return Math.min(128, s.ch() - 54); }
    private int cosmeticY(CatTalentScreen s) { return portraitY(s) + ph(s) + 18; }

    @Override
    public void init(CatTalentScreen s) {
        TalentedCat cat = s.cat();
        int py = portraitY(s);
        for (int i = 0; i < LEFT.length; i++) addSlot(s, cat, LEFT[i], leftX(s), py + 14 + i * 36);
        for (int i = 0; i < RIGHT.length; i++) addSlot(s, cat, RIGHT[i], rightX(s), py + 6 + i * 30);

        int cy = cosmeticY(s);
        int startX = s.cx() + s.cw() / 2 - (3 * 26 + 4 + 110) / 2;
        for (int i = 0; i < COSMETIC.length; i++) addSlot(s, cat, COSMETIC[i], startX + i * 26, cy);
        boolean visible = cat.isCosmeticsVisible();
        s.add(new KTButton(startX + 3 * 26 + 4, cy + 1, 110, 16,
                visible ? KTText.t("gear.cosmetics.shown", "Cosmetics: Shown") : KTText.t("gear.cosmetics.hidden", "Cosmetics: Hidden"),
                b -> {
                    PacketHandler.sendToServer(new PacketEquipCatItem(cat.getId(), 10));
                    cat.setCosmeticsVisible(!cat.isCosmeticsVisible());
                    s.rebuild();
                }).icon(new ItemStack(visible ? Items.ENDER_EYE : Items.ENDER_PEARL)).style(KTButton.Style.CHIP).selected(visible));
    }

    private static void addSlot(CatTalentScreen s, TalentedCat cat, SlotDef def, int x, int y) {
        Component label = KTText.t("gear.slot." + def.key(), def.name());
        ItemStack ghost = def.ghost().isEmpty() ? new ItemStack(def.id() == 5 ? ModItems.CAT_COLLAR.get() : ModItems.HERO_CAPE.get()) : def.ghost();
        s.add(new KTWidget(x, y, 18, 18, label, (g, wx, wy, w, h, hover) -> {
            ItemStack stack = def.getter().apply(cat);
            KTTheme.slot(g, wx, wy, hover);
            if (!stack.isEmpty()) {
                g.renderItem(stack, wx + 1, wy + 1);
                g.renderItemDecorations(s.font(), stack, wx + 1, wy + 1);
            } else {
                g.renderItem(ghost, wx + 1, wy + 1);
                g.fill(wx + 1, wy + 1, wx + 17, wy + 17, 0xB02A2A2E);
            }
            if (hover) g.fill(wx + 1, wy + 1, wx + 17, wy + 17, 0x40FFFFFF);
        }, () -> PacketHandler.sendToServer(new PacketEquipCatItem(cat.getId(), def.id(), Screen.hasShiftDown())))
                .hoverTip(() -> {
                    ItemStack stack = def.getter().apply(cat);
                    if (!stack.isEmpty()) {
                        List<FormattedCharSequence> lines = new java.util.ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), stack)
                                .stream().map(Component::getVisualOrderText).toList());
                        lines.add(KTText.t("gear.unequip", "Shift-click to unequip").withStyle(net.minecraft.ChatFormatting.GRAY).getVisualOrderText());
                        return lines;
                    }
                    return List.<FormattedCharSequence>of(label.getVisualOrderText(),
                            KTText.t("gear.empty", "Empty - click with an item in hand").withStyle(net.minecraft.ChatFormatting.GRAY).getVisualOrderText());
                }));
    }

    @Override
    public void render(CatTalentScreen s, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        TalentedCat cat = s.cat();
        Font font = s.font();
        KTTheme.inset(g, s.cx(), s.cy(), s.cw(), s.ch());

        int px = portraitX(s), py = portraitY(s);
        int ph = ph(s);
        KTTheme.portrait(g, px, py, PW, ph);
        s.drawCat(g, px + 4, py + 4, PW - 8, ph - 8, mouseX, mouseY);

        // Slot labels: left column right-aligned before the slot, right column after it
        for (int i = 0; i < LEFT.length; i++) {
            String l = KTText.s("gear.slot." + LEFT[i].key(), LEFT[i].name());
            int y = py + 14 + i * 36 + 5;
            g.drawString(font, l, leftX(s) - 5 - font.width(l), y, KTTheme.TEXT_DIM, false);
        }
        for (int i = 0; i < RIGHT.length; i++) {
            int y = py + 6 + i * 30 + 5;
            g.drawString(font, KTText.s("gear.slot." + RIGHT[i].key(), RIGHT[i].name()), rightX(s) + 23, y, KTTheme.TEXT_DIM, false);
        }

        // Cosmetics header, with the combat summary on its right end
        int cy = cosmeticY(s);
        String summary = KTText.s("gear.summary", "Armor %s · Damage %s", cat.getArmorValue(),
                String.format("%.1f", cat.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        int sumX = s.cx() + s.cw() - 9 - font.width(summary);
        KTTheme.header(g, font, s.cx() + 8, cy - 12, sumX - 6 - (s.cx() + 8), KTText.t("gear.cosmetics", "Cosmetics"));
        g.drawString(font, summary, sumX, cy - 12, 0xFFCBD5E1, false);
    }
}
