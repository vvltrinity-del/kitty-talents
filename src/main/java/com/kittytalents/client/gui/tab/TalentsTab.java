package com.kittytalents.client.gui.tab;

import com.kittytalents.client.gui.CatTalentScreen;
import com.kittytalents.client.gui.theme.KTButton;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.network.PacketRespecTalents;
import com.kittytalents.network.PacketTrainTalent;
import com.kittytalents.talent.Talent;
import com.kittytalents.talent.Talents;
import com.kittytalents.talent.effect.TalentEngine;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Talents: category chips, a scrollable list with rank pips, and a detail card with the mastery box and TRAIN. */
public class TalentsTab implements CatTab {
    private static final int LIST_W = 160;
    private static final int ROW_H = 22, ROW_GAP = 2;
    private static final String[] CATEGORY_KEYS = { "all", "combat", "defense", "mobility", "support", "utility", "dire" };
    private static final String[] CATEGORY_NAMES = { "All", "Combat", "Defense", "Mobility", "Support", "Utility", "Dire" };
    /** Chip i (after "All") shows talents of this declared category, so new and addon talents sort themselves. */
    private static final com.kittytalents.talent.TalentCategory[] CATEGORY_FILTER = { null,
            com.kittytalents.talent.TalentCategory.COMBAT, com.kittytalents.talent.TalentCategory.DEFENSE,
            com.kittytalents.talent.TalentCategory.MOBILITY, com.kittytalents.talent.TalentCategory.SUPPORT,
            com.kittytalents.talent.TalentCategory.UTILITY, com.kittytalents.talent.TalentCategory.DIRE };

    private final List<Talent> allTalents = new ArrayList<>();
    private final List<Talent> filtered = new ArrayList<>();
    private Talent selected = Talents.POUNCE;
    private int category;
    private int scroll;

    public TalentsTab() {
        for (Talent t : Talents.getAll()) {
            if (!t.isStandard() && t.isAvailable()) allTalents.add(t);
        }
        filter();
    }

    @Override
    public Component title(CatTalentScreen s) { return KTText.t("tab.talents.title", "Talents"); }

    // ------------------------------------------------------------------ filtering (unchanged rules)

    private void filter() {
        filtered.clear();
        var want = CATEGORY_FILTER[category];
        for (Talent t : allTalents) {
            if (want == null || t.getCategory() == want) filtered.add(t);
        }
        scroll = 0;
        if (!filtered.isEmpty() && !filtered.contains(selected)) selected = filtered.get(0);
    }

    // ------------------------------------------------------------------ layout helpers

    private int listY(CatTalentScreen s) { return s.cy() + 18; }
    private int listH(CatTalentScreen s) { return s.ch() - 18; }
    private int visibleRows(CatTalentScreen s) { return Math.max(1, (listH(s) - 10) / (ROW_H + ROW_GAP)); }
    private int maxScroll(CatTalentScreen s) { return Math.max(0, filtered.size() - visibleRows(s)); }
    private int dx(CatTalentScreen s) { return s.cx() + LIST_W + 6; }
    private int dw(CatTalentScreen s) { return s.cw() - LIST_W - 6; }

    private static boolean canTrain(TalentedCat cat, Talent t) {
        int lvl = cat.getTalentLevel(t);
        return lvl < t.getMaxLevel() && cat.getSkillPoints() >= cat.getCostForNextRank(t)
                && (!t.isDireTalent() || cat.isDireAwakened());
    }

    // ------------------------------------------------------------------ widgets

    @Override
    public void init(CatTalentScreen s) {
        TalentedCat cat = s.cat();
        int x = s.cx(), y = s.cy();

        // Category chips + Respec
        int respecW = 54;
        int chipW = (s.cw() - respecW - 4) / CATEGORY_KEYS.length - 2;
        for (int i = 0; i < CATEGORY_KEYS.length; i++) {
            final int idx = i;
            s.add(new KTButton(x + i * (chipW + 2), y, chipW, 14, KTText.t("talents.cat." + CATEGORY_KEYS[i], CATEGORY_NAMES[i]), b -> {
                category = idx;
                filter();
                s.rebuild();
            }).style(KTButton.Style.CHIP).selected(category == i));
        }
        // Training and respec are owner-only (CatAction.TRAIN / RESPEC); co-owners don't get the buttons.
        if (com.kittytalents.core.perm.CatPermissions.can(net.minecraft.client.Minecraft.getInstance().player, cat, com.kittytalents.core.perm.CatAction.RESPEC)) {
            s.add(new KTButton(x + s.cw() - respecW, y, respecW, 14, KTText.t("talents.respec", "Respec"),
                    b -> PacketHandler.sendToServer(new PacketRespecTalents(cat.getId())))
                    .style(KTButton.Style.RED).tip(KTText.t("talents.respec.tip", "Refund every trained rank")));
        }

        // List rows
        scroll = Math.min(scroll, maxScroll(s));
        int rowX = x + 5, rowW = LIST_W - 10 - 5, rowY = listY(s) + 5;
        int end = Math.min(filtered.size(), scroll + visibleRows(s));
        for (int i = scroll; i < end; i++) {
            Talent t = filtered.get(i);
            boolean locked = t.isDireTalent() && !cat.isDireAwakened();
            boolean maxed = cat.getTalentLevel(t) >= t.getMaxLevel();
            int color = locked ? 0xFF8B8580 : (maxed ? KTTheme.TEXT_GOLD : KTTheme.TEXT);
            s.add(new KTButton(rowX, rowY + (i - scroll) * (ROW_H + ROW_GAP), rowW, ROW_H, Component.literal(t.getDisplayName()), b -> {
                selected = t;
                s.rebuild();
            }).icon(t.getIcon()).textTop(3).selected(t == selected).color(color));
        }

        // Detail: TRAIN
        if (selected != null && com.kittytalents.core.perm.CatPermissions.can(net.minecraft.client.Minecraft.getInstance().player, cat, com.kittytalents.core.perm.CatAction.TRAIN)) {
            int lvl = cat.getTalentLevel(selected);
            boolean max = lvl >= selected.getMaxLevel();
            Component label = max ? KTText.t("talents.mastered", "MAXED") : KTText.t("talents.train", "TRAIN");
            int bw = 84, bh = 20;
            s.add(new KTButton(dx(s) + dw(s) - bw - 8, listY(s) + listH(s) - bh - 8, bw, bh, label, b -> {
                PacketHandler.sendToServer(new PacketTrainTalent(cat.getId(), selected.getId()));
                s.rebuild();
            }).style(KTButton.Style.GOLD).enabled(canTrain(cat, selected)));
        }
    }

    @Override
    public boolean mouseScrolled(CatTalentScreen s, double mouseX, double mouseY, double delta) {
        if (mouseX >= s.cx() && mouseX < s.cx() + LIST_W && mouseY >= listY(s) && mouseY < listY(s) + listH(s)) {
            int next = Math.max(0, Math.min(maxScroll(s), scroll - (int) Math.signum(delta)));
            if (next != scroll) {
                scroll = next;
                s.rebuild();
            }
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(CatTalentScreen s, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        TalentedCat cat = s.cat();
        Font font = s.font();

        // Skill points on the title strip's right end
        String sp = KTText.s("talents.sp", "%s SP", cat.getSkillPoints());
        int spX = s.titleX() + s.titleW() - font.width(sp) - 10;
        g.drawString(font, sp, spX, s.titleY() + 4, KTTheme.TEXT_GOLD, true);
        g.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE), spX - 18, s.titleY());

        // List panel + scrollbar
        int lx = s.cx(), ly = listY(s), lh = listH(s);
        KTTheme.inset(g, lx, ly, LIST_W, lh);
        if (maxScroll(s) > 0) {
            int trackX = lx + LIST_W - 8, trackY = ly + 5, trackH = lh - 10;
            g.fill(trackX, trackY, trackX + 3, trackY + trackH, 0x66000000);
            int thumbH = Math.max(12, trackH * visibleRows(s) / filtered.size());
            int thumbY = trackY + (trackH - thumbH) * scroll / maxScroll(s);
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, KTTheme.GOLD_EDGE);
        }
        if (filtered.isEmpty()) {
            g.drawCenteredString(font, KTText.t("talents.none", "No talents here"), lx + LIST_W / 2, ly + 14, KTTheme.TEXT_DIM);
        }

        // Detail panel
        int dx = dx(s), dw = dw(s);
        KTTheme.inset(g, dx, ly, dw, lh);
        if (selected == null) return;
        int lvl = cat.getTalentLevel(selected), max = selected.getMaxLevel();

        int iy = ly + 8;
        KTTheme.slot(g, dx + 8, iy, true);
        g.renderItem(selected.getIcon(), dx + 9, iy + 1);
        g.drawString(font, selected.getDisplayName().toUpperCase(), dx + 32, iy + 1, KTTheme.TEXT_GOLD, true);
        String rank = KTText.s("talents.rank", "Rank %s / %s", lvl, max);
        g.drawString(font, rank, dx + 32, iy + 11, KTTheme.TEXT_DIM, false);
        int pipX = dx + 36 + font.width(rank);
        int pipSize = 7, pipGap = 2;
        if (pipX + max * (pipSize + pipGap) > dx + dw - 8) { pipSize = 5; pipGap = 1; }
        KTTheme.pips(g, pipX, iy + 11, lvl, max, pipSize, pipGap);
        if (selected.isDireTalent()) {
            KTTheme.badge(g, font, dx + dw - 8 - font.width("DIRE") - 6, iy, "DIRE", cat.isDireAwakened() ? 0xFFC084FC : 0xFF8B8580);
        }

        // Description box
        int boxX = dx + 8, boxW = dw - 16, by = iy + 26;
        List<FormattedCharSequence> desc = font.split(selected.getDescription(), boxW - 10);
        int descLines = Math.min(desc.size(), 5);
        KTTheme.box(g, boxX, by, boxW, descLines * 10 + 8, 0x665C4A3A);
        for (int i = 0; i < descLines; i++) g.drawString(font, desc.get(i), boxX + 5, by + 5 + i * 10, KTTheme.TEXT, false);

        // Mastery box
        boolean mastered = TalentEngine.isMastered(cat, selected);
        int my = by + descLines * 10 + 14;
        Component mtext = Component.translatable("talent.kittytalents." + selected.getId() + ".mastery");
        List<FormattedCharSequence> mlines = font.split(mtext, boxW - 10);
        int bottomLimit = ly + lh - 34; // keep clear of the TRAIN row
        int mLines = Math.max(1, Math.min(mlines.size(), (bottomLimit - my - 18) / 10));
        int mh = 16 + mLines * 10;
        KTTheme.box(g, boxX, my, boxW, mh, mastered ? KTTheme.GOLD_EDGE : 0xFF6B6760);
        Component mhead = Component.literal(mastered ? "★ " : "☆ ").append(Component.translatable("gui.kittytalents.mastery").getString().toUpperCase());
        g.drawString(font, mhead, boxX + 5, my + 4, mastered ? KTTheme.TEXT_GOLD : KTTheme.TEXT_DIM, true);
        if (!mastered) {
            String req = KTText.s("talents.mastery_req", "at rank %s", max);
            g.drawString(font, req, boxX + boxW - 5 - font.width(req), my + 4, KTTheme.TEXT_DIM, false);
        }
        for (int i = 0; i < mLines; i++) {
            g.drawString(font, mlines.get(i), boxX + 5, my + 15 + i * 10, mastered ? 0xFFF5DFA0 : 0xFF9C958C, false);
        }

        // Cost line next to TRAIN
        int costY = ly + lh - 21;
        if (lvl >= max) {
            g.drawString(font, KTText.s("talents.max", "Fully trained"), dx + 10, costY, 0xFF6EE7A0, false);
        } else if (!com.kittytalents.core.perm.CatPermissions.can(net.minecraft.client.Minecraft.getInstance().player, cat, com.kittytalents.core.perm.CatAction.TRAIN)) {
            g.drawString(font, KTText.s("talents.owner_only", "Only the owner can train this cat"), dx + 10, costY, KTTheme.TEXT_DIM, false);
        } else if (selected.isDireTalent() && !cat.isDireAwakened()) {
            g.drawString(font, KTText.s("talents.needs_dire", "Needs a Dire Treat"), dx + 10, costY, 0xFFC084FC, false);
        } else {
            int cost = cat.getCostForNextRank(selected);
            int col = cat.getSkillPoints() >= cost ? KTTheme.TEXT : 0xFFF87171;
            g.drawString(font, KTText.s("talents.cost", "Cost: %s SP", cost), dx + 10, costY, col, false);
        }
    }

    @Override
    public void renderOverlay(CatTalentScreen s, GuiGraphics g, int mouseX, int mouseY) {
        // Rank pips under each row's name
        TalentedCat cat = s.cat();
        int rowX = s.cx() + 5, rowY = listY(s) + 5;
        int end = Math.min(filtered.size(), scroll + visibleRows(s));
        for (int i = scroll; i < end; i++) {
            Talent t = filtered.get(i);
            int y = rowY + (i - scroll) * (ROW_H + ROW_GAP) + 13;
            int max = t.getMaxLevel();
            int size = max > 10 ? 3 : 4;
            KTTheme.pips(g, rowX + 22, y, cat.getTalentLevel(t), max, size, 1);
        }
    }
}
