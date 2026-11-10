package com.kittytalents.client.gui.tab;

import com.kittytalents.client.gui.CatTalentScreen;
import com.kittytalents.client.gui.theme.KTButton;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.core.gear.CatGearRules;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.inventory.CatTalentMenu;
import com.kittytalents.network.PacketEquipCatItem;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Gear (4.0 Phase 5): real slots. The cat's gear sits around its portrait (hands left, armor right, cosmetics below)
 * and the player's inventory is on the right, so items drag, shift-click and right-click like any container.
 */
public class GearTab implements CatTab {
    private static final String[] KEYS = { "weapon", "head", "chest", "legs", "boots", "accessory", "tool", "cosmetic1", "cosmetic2", "cosmetic3" };
    private static final String[] NAMES = { "Weapon", "Helmet", "Chest", "Legs", "Boots", "Accessory", "Tool", "Cosmetic 1", "Cosmetic 2", "Cosmetic 3" };
    private static final int INV_W = 162;

    private static ItemStack ghost(int slot) {
        return switch (slot) {
            case 0 -> new ItemStack(Items.IRON_SWORD);
            case 1 -> new ItemStack(Items.IRON_HELMET);
            case 2 -> new ItemStack(Items.IRON_CHESTPLATE);
            case 3 -> new ItemStack(Items.IRON_LEGGINGS);
            case 4 -> new ItemStack(Items.IRON_BOOTS);
            case 5 -> new ItemStack(ModItems.CAT_COLLAR.get());
            case 6 -> new ItemStack(Items.IRON_PICKAXE);
            default -> new ItemStack(ModItems.HERO_CAPE.get());
        };
    }

    @Override
    public Component title(CatTalentScreen s) { return KTText.t("tab.gear.title", "Equipment"); }

    // ---------------------------------------------------------------- layout

    private int invX(CatTalentScreen s) { return s.cx() + s.cw() - 10 - INV_W; }
    private int invY(CatTalentScreen s) { return s.cy() + 20; }
    private int gearX(CatTalentScreen s) { return s.cx() + 10; }
    private int portraitW(CatTalentScreen s) { return Math.max(48, Math.min(130, invX(s) - 14 - gearX(s) - 48)); }
    private int portraitX(CatTalentScreen s) { return gearX(s) + 24; }
    private int portraitY(CatTalentScreen s) { return s.cy() + 8; }
    private int portraitH(CatTalentScreen s) { return Math.max(72, Math.min(136, s.ch() - 46)); }
    private int armorX(CatTalentScreen s) { return portraitX(s) + portraitW(s) + 6; }
    private int cosmeticY(CatTalentScreen s) { return portraitY(s) + portraitH(s) + 8; }

    /** Top-left of the 18px frame of gear slot {@code i}. */
    private int[] gearFrame(CatTalentScreen s, int i) {
        int py = portraitY(s), ph = portraitH(s);
        return switch (i) {
            case 0, 5, 6 -> {
                int row = i == 0 ? 0 : i == 5 ? 1 : 2;
                int step = Math.min(36, (ph - 18) / 2);
                yield new int[]{gearX(s), py + (ph - 18 - 2 * step) / 2 + row * step};
            }
            case 1, 2, 3, 4 -> {
                int step = Math.min(28, (ph - 18) / 3);
                yield new int[]{armorX(s), py + (ph - 18 - 3 * step) / 2 + (i - 1) * step};
            }
            default -> new int[]{portraitX(s) + (i - 7) * 22, cosmeticY(s)};
        };
    }

    private int[] invFrame(CatTalentScreen s, int menuIndex) {
        int i = menuIndex - CatTalentMenu.INV_START;
        if (i < 27) return new int[]{invX(s) + (i % 9) * 18, invY(s) + (i / 9) * 18};
        return new int[]{invX(s) + (i - 27) * 18, invY(s) + 3 * 18 + 4};
    }

    @Override
    public void init(CatTalentScreen s) {
        TalentedCat cat = s.cat();
        CatTalentMenu menu = s.getMenu();
        for (int i = 0; i < CatTalentMenu.END; i++) {
            int[] f = i < CatTalentMenu.GEAR ? gearFrame(s, i) : invFrame(s, i);
            menu.place(i, f[0] + 1, f[1] + 1);
        }
        boolean visible = cat.isCosmeticsVisible();
        int[] last = gearFrame(s, 9);
        s.add(new KTButton(last[0] + 24, last[1], 18, 18, Component.empty(),
                b -> PacketHandler.sendToServer(new PacketEquipCatItem(cat.getId(), PacketEquipCatItem.TOGGLE_COSMETICS)))
                .icon(new ItemStack(visible ? Items.ENDER_EYE : Items.ENDER_PEARL)).style(KTButton.Style.CHIP).selected(visible)
                .tip(visible ? KTText.t("gear.cosmetics.shown", "Cosmetics: Shown") : KTText.t("gear.cosmetics.hidden", "Cosmetics: Hidden")));
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void render(CatTalentScreen s, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        TalentedCat cat = s.cat();
        Font font = s.font();
        KTTheme.inset(g, s.cx(), s.cy(), s.cw(), s.ch());

        int px = portraitX(s), py = portraitY(s), pw = portraitW(s), ph = portraitH(s);
        KTTheme.portrait(g, px, py, pw, ph);
        s.drawCat(g, px + 4, py + 4, pw - 8, ph - 8, mouseX, mouseY);

        // Gear slot frames, with a dimmed ghost of what goes there when empty
        for (int i = 0; i < CatTalentMenu.GEAR; i++) {
            int[] f = gearFrame(s, i);
            boolean hover = mouseX >= f[0] && mouseX < f[0] + 18 && mouseY >= f[1] && mouseY < f[1] + 18;
            KTTheme.slot(g, f[0], f[1], hover);
            if (CatGearRules.get(cat, i).isEmpty()) {
                g.renderItem(ghost(i), f[0] + 1, f[1] + 1);
                g.fill(f[0] + 1, f[1] + 1, f[0] + 17, f[1] + 17, 0xB02A2A2E);
            }
        }

        // Player inventory
        int ix = invX(s), iy = invY(s);
        KTTheme.header(g, font, ix, iy - 12, INV_W, KTText.t("gear.inventory", "Your inventory"));
        for (int i = CatTalentMenu.INV_START; i < CatTalentMenu.END; i++) {
            int[] f = invFrame(s, i);
            KTTheme.slot(g, f[0], f[1], false);
        }

        // Combat summary and a hint under the inventory
        int ty = iy + 4 * 18 + 10;
        String summary = KTText.s("gear.summary", "Armor %s · Damage %s", cat.getArmorValue(),
                String.format("%.1f", cat.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        g.drawString(font, summary, ix, ty, 0xFFCBD5E1, false);
        String hint = s.getMenu().canEquip()
                ? KTText.s("gear.hint", "Drag items onto the cat. Shift-click moves them.")
                : KTText.s("gear.hint_view", "Only the owner can change this cat's gear.");
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(hint), INV_W);
        for (int i = 0; i < lines.size() && ty + 12 + i * 10 < s.cy() + s.ch() - 6; i++) {
            g.drawString(font, lines.get(i), ix, ty + 12 + i * 10, KTTheme.TEXT_DIM, false);
        }
    }

    /** Empty gear slot: what goes there. Holding an item the slot refuses: why, in red. */
    @Override
    public void renderOverlay(CatTalentScreen s, GuiGraphics g, int mouseX, int mouseY) {
        Slot hovered = s.hoveredSlot();
        if (!(hovered instanceof CatTalentMenu.GearSlot gs)) return;
        int id = gs.gearSlot();
        ItemStack carried = s.getMenu().getCarried();
        List<Component> lines = new ArrayList<>();
        if (!carried.isEmpty() && !gs.mayPlace(carried)) {
            Component why = !s.getMenu().canEquip()
                    ? KTText.t("gear.hint_view", "Only the owner can change this cat's gear.")
                    : CatGearRules.refusal(s.cat(), id, carried);
            if (why == null) return;
            lines.add(KTText.t("gear.slot." + KEYS[id], NAMES[id]));
            lines.add(why.copy().withStyle(ChatFormatting.RED));
        } else if (carried.isEmpty() && !gs.hasItem()) {
            lines.add(KTText.t("gear.slot." + KEYS[id], NAMES[id]));
            lines.add(KTText.t("gear.empty", "Empty - drag an item here").withStyle(ChatFormatting.GRAY));
        } else {
            return;
        }
        g.renderComponentTooltip(s.font(), lines, mouseX, mouseY);
    }
}
