package com.kittytalents.client.gui.tab;

import com.kittytalents.client.gui.CatTalentScreen;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Function;
import java.util.function.Supplier;

/** Stats: two leather columns of icon rows (read-only lifetime records). */
public class StatsTab implements CatTab {
    private record Stat(String key, String name, Supplier<ItemStack> icon, int color, Function<TalentedCat, String> value) {}

    private static final Stat[] COMBAT = {
            new Stat("kills", "Total Kills", () -> new ItemStack(Items.IRON_SWORD), 0xFFF2EEE6, c -> String.valueOf(c.getTotalKills())),
            new Stat("hostile", "Hostile Kills", () -> new ItemStack(Items.ZOMBIE_HEAD), 0xFFF87171, c -> String.valueOf(c.getHostileKills())),
            new Stat("prey", "Prey Caught", () -> new ItemStack(Items.COD), 0xFFFBBF24, c -> String.valueOf(c.getPassiveKills())),
            new Stat("dealt", "Damage Dealt", () -> new ItemStack(Items.DIAMOND_SWORD), 0xFFFCA5A5, c -> String.valueOf((int) c.getDamageDealt())),
            new Stat("taken", "Damage Taken", () -> new ItemStack(Items.SHIELD), 0xFFFB7185, c -> String.valueOf((int) c.getDamageTaken())),
            new Stat("pounces", "Pounces", () -> new ItemStack(Items.RABBIT_FOOT), 0xFFC4B5FD, c -> String.valueOf(c.getPouncesPerformed())),
            new Stat("creepers", "Creepers Scared", () -> new ItemStack(Items.CREEPER_HEAD), 0xFF86EFAC, c -> String.valueOf(c.getCreepersScared())),
    };
    private static final Stat[] LIFE = {
            new Stat("distance", "Distance", () -> new ItemStack(Items.LEATHER_BOOTS), 0xFF93C5FD, c -> (int) c.getDistanceTraveled() + "m"),
            new Stat("food", "Food Eaten", () -> new ItemStack(Items.COOKED_SALMON), 0xFF6EE7A0, c -> String.valueOf(c.getFoodConsumed())),
            new Stat("mined", "Blocks Mined", () -> new ItemStack(Items.IRON_PICKAXE), 0xFFFCD34D, c -> String.valueOf(c.getBlocksMined())),
            new Stat("retrieved", "Items Fetched", () -> new ItemStack(Items.HOPPER), 0xFF7DD3FC, c -> String.valueOf(c.getItemsRetrieved())),
            new Stat("healing", "Healing Given", () -> new ItemStack(Items.GLISTERING_MELON_SLICE), 0xFFFDA4AF, c -> c.getHealingProvided() + " HP"),
            new Stat("petted", "Times Petted", () -> new ItemStack(Items.POPPY), 0xFFF9A8D4, c -> String.valueOf(c.getTimesPet())),
            new Stat("frenzy", "Frenzies", () -> new ItemStack(ModItems.CATNIP.get()), 0xFFD8B4FE, c -> String.valueOf(c.getFrenzyActivations())),
    };

    @Override
    public Component title(CatTalentScreen s) { return KTText.t("tab.stats.title", "Statistics"); }

    @Override
    public void init(CatTalentScreen s) {}

    @Override
    public void render(CatTalentScreen s, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Font font = s.font();
        int colW = (s.cw() - 6) / 2, h = s.ch() - 14;
        column(s, g, font, s.cx(), s.cy(), colW, h, KTText.t("stats.combat", "Combat"), COMBAT);
        column(s, g, font, s.cx() + colW + 6, s.cy(), colW, h, KTText.t("stats.life", "Life & Work"), LIFE);
        String note = KTText.s("stats.note", "Records are permanent · Sneak + right-click with Coal to cleanse");
        g.drawCenteredString(font, font.plainSubstrByWidth(note, s.cw()), s.cx() + s.cw() / 2, s.cy() + s.ch() - 9, KTTheme.TEXT_DIM);
    }

    private static void column(CatTalentScreen s, GuiGraphics g, Font font, int x, int y, int w, int h, Component title, Stat[] stats) {
        KTTheme.inset(g, x, y, w, h);
        KTTheme.header(g, font, x + 7, y + 6, w - 14, title);
        int rowH = Math.min(24, (h - 22) / stats.length);
        int ry = y + 18;
        for (int i = 0; i < stats.length; i++) {
            Stat st = stats[i];
            int yy = ry + i * rowH;
            if (i % 2 == 0) g.fill(x + 5, yy, x + w - 5, yy + rowH - 1, 0x22000000);
            int iy = yy + (rowH - 16) / 2;
            g.renderItem(st.icon().get(), x + 7, iy);
            g.drawString(font, KTText.s("stats." + st.key(), st.name()), x + 27, yy + (rowH - 8) / 2, KTTheme.TEXT, false);
            String v = st.value().apply(s.cat());
            g.drawString(font, v, x + w - 9 - font.width(v), yy + (rowH - 8) / 2, st.color(), true);
        }
    }
}
