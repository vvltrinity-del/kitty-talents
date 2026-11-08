package com.kittytalents.client.gui.tab;

import com.kittytalents.client.ClientModEvents;
import com.kittytalents.client.gui.CatTalentScreen;
import com.kittytalents.client.gui.theme.KTButton;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.config.KittyTalentsConfig;
import com.kittytalents.core.progression.CatProgression;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.network.*;
import com.kittytalents.registry.ModItems;
import com.kittytalents.work.CatJob;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.UUID;

/** Home: portrait with level/HP/XP on the left; info, behaviour, actions, name and co-owners on the right. */
public class HomeTab implements CatTab {
    private static final int PORTRAIT_W = 126;

    @Override
    public Component title(CatTalentScreen s) { return KTText.t("tab.home.title", "Home"); }

    private int rx(CatTalentScreen s) { return s.cx() + PORTRAIT_W + 6; }
    private int rw(CatTalentScreen s) { return s.cw() - PORTRAIT_W - 6; }
    /** Small GUI (e.g. scale 3 at 720p): name and co-owner fields share a row and the info card shrinks. */
    private static boolean compact(CatTalentScreen s) { return s.ch() < 206; }
    private static int infoH(CatTalentScreen s) { return compact(s) ? 40 : 50; }

    @Override
    public void init(CatTalentScreen s) {
        TalentedCat cat = s.cat();
        int x = rx(s) + 6, w = rw(s) - 12;
        int y = s.cy() + infoH(s) + 6 + 14; // inside the controls panel, under its header

        // Mode row: [◀][ mode ][▶][≡]
        int modeW = w - 3 * 20 - 6;
        s.add(new KTButton(x, y, 20, 16, Component.literal("◀"), b -> setMode(s, cat.getCatMode().previous())));
        s.add(new KTButton(x + 22, y, modeW, 16, cat.getCatMode().getDisplayName(), b -> setMode(s, cat.getCatMode().next()))
                .icon(new ItemStack(ModItems.KITTY_WHISTLE.get())).color(KTTheme.TEXT_GOLD)
                .tip(KTText.t("home.mode.tip", "Behaviour mode (click to cycle)")));
        s.add(new KTButton(x + 24 + modeW, y, 20, 16, Component.literal("▶"), b -> setMode(s, cat.getCatMode().next())));
        s.add(new KTButton(x + 46 + modeW, y, 20, 16, Component.literal("≡"), b -> ClientModEvents.openWhistleScreen())
                .tip(KTText.t("home.modes.tip", "Open the mode wheel")));

        // Job row: [job ▸][clocked]
        y += 20;
        CatJob job = cat.getCatJob();
        int clockW = 70;
        s.add(new KTButton(x, y, w - clockW - 2, 16, KTText.t("home.job", "Job: %s", job.getDisplayName()), b -> cycleJob(s))
                .icon(job.getIcon()).color(KTTheme.TEXT_GOLD).tip(Component.literal(job.getDescription())));
        boolean clockedIn = cat.isClockedIn();
        s.add(new KTButton(x + w - clockW, y, clockW, 16,
                clockedIn ? KTText.t("home.clocked_in", "Clocked In") : KTText.t("home.clocked_out", "Clocked Out"),
                b -> {
                    boolean next = !cat.isClockedIn();
                    cat.setClockedIn(next);
                    PacketHandler.sendToServer(new PacketSetCatJob(cat.getId(), cat.getCatJob(), next));
                    s.rebuild();
                }).style(KTButton.Style.CHIP).selected(clockedIn).color(clockedIn ? 0xFF6EE7A0 : 0xFFF87171));

        // Actions: Sit/Stand, Pack, Whistle, TWINE
        y += 22;
        int bw = (w - 6) / 4;
        boolean sitting = cat.isOrderedToSit();
        s.add(new KTButton(x, y, bw, 18, sitting ? KTText.t("home.stand", "Stand") : KTText.t("home.sit", "Sit"), b -> {
            CatMode m = cat.isOrderedToSit() ? CatMode.FOLLOW : CatMode.DOCILE;
            cat.setCatMode(m);
            PacketHandler.sendToServer(new PacketChangeCatMode(cat.getId(), m.ordinal()));
            s.rebuild();
        }).icon(new ItemStack(Items.RED_BED)));
        s.add(new KTButton(x + bw + 2, y, bw, 18, KTText.t("home.pack", "Pack"),
                b -> PacketHandler.sendToServer(new PacketOpenCatPack(cat.getId())))
                .icon(new ItemStack(Items.BUNDLE)).tip(KTText.t("home.pack.tip", "Open the Kitty Pack")));
        s.add(new KTButton(x + 2 * (bw + 2), y, bw, 18, KTText.t("home.whistle", "Whistle"), b -> ClientModEvents.openWhistleScreen())
                .icon(new ItemStack(ModItems.KITTY_WHISTLE.get())));
        s.add(new KTButton(x + 3 * (bw + 2), y, w - 3 * (bw + 2), 18, KTText.t("home.twine", "TWINE"), b -> {
            PacketHandler.sendToServer(new PacketTwineAction(cat.getId(), true));
            s.onClose();
        }).style(KTButton.Style.PURPLE).color(0xFFE9D5FF).tip(KTText.t("home.twine.tip", "Become your cat")));

        Font font = s.font();
        boolean compact = compact(s);
        // Name + co-owner fields: stacked under their own header, or side by side when space is short
        y += compact ? 22 : 34;
        int fieldW = compact ? (w - 4) / 2 - 50 : w - 58;
        EditBox name = new EditBox(font, x + 1, y + 1, fieldW - 2, 14, KTText.t("home.name", "Name"));
        name.setMaxLength(32);
        name.setValue(cat.hasCustomName() ? cat.getCustomName().getString() : cat.getName().getString());
        s.setNameBox(name);
        s.add(new KTButton(x + fieldW + 2, y, compact ? 46 : 56, 16, KTText.t("home.rename", "Rename"), b -> s.submitName()));

        int tx = x;
        if (compact) tx = x + w - fieldW - 48; else y += 20;
        EditBox trust = new EditBox(font, tx + 1, y + 1, fieldW - 2, 14, KTText.t("home.trust", "Trust"));
        trust.setMaxLength(16);
        trust.setHint(KTText.t("home.trust.hint", "Add co-owner...").withStyle(ChatFormatting.DARK_GRAY));
        s.setTrustBox(trust);
        s.add(new KTButton(tx + fieldW + 2, y, compact ? 46 : 56, 16, KTText.t("home.trust.add", "+ Trust"), b -> s.submitTrust())
                .tip(KTText.t("home.trust.tip", "Co-owners must be online players")));

        // Owner-only removal chips: "✕ name"
        y += 20;
        var me = Minecraft.getInstance().player;
        if (me != null && me.getUUID().equals(cat.getOwnerUUID())) {
            int chipX = x;
            for (Map.Entry<UUID, String> e : cat.getTrustedPlayers().entrySet()) {
                String pname = e.getValue().isEmpty() ? "Player" : e.getValue();
                int cw = Math.min(90, font.width("✕ " + pname) + 10);
                if (chipX + cw > x + w) break;
                UUID id = e.getKey();
                s.add(new KTButton(chipX, y, cw, 14, Component.literal("✕ " + pname),
                        b -> PacketHandler.sendToServer(new PacketManageTrust(cat.getId(), 1, id.toString())))
                        .style(KTButton.Style.CHIP).color(0xFFFCA5A5)
                        .tip(KTText.t("home.trust.remove", "Remove %s as co-owner", pname)));
                chipX += cw + 3;
            }
        }
    }

    private static void setMode(CatTalentScreen s, CatMode mode) {
        TalentedCat cat = s.cat();
        cat.setCatMode(mode);
        PacketHandler.sendToServer(new PacketChangeCatMode(cat.getId(), mode.ordinal()));
        s.rebuild();
    }

    private static void cycleJob(CatTalentScreen s) {
        TalentedCat cat = s.cat();
        CatJob[] jobs = CatJob.values();
        CatJob next = jobs[(cat.getCatJob().ordinal() + 1) % jobs.length];
        cat.setCatJob(next);
        if (next != CatJob.NONE) {
            cat.setCatMode(CatMode.WORK);
            cat.setClockedIn(true);
        }
        PacketHandler.sendToServer(new PacketSetCatJob(cat.getId(), next, cat.isClockedIn()));
        s.rebuild();
    }

    @Override
    public void render(CatTalentScreen s, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        TalentedCat cat = s.cat();
        Font font = s.font();
        int x = s.cx(), y = s.cy(), h = s.ch();

        // ---- Left: portrait card
        KTTheme.inset(g, x, y, PORTRAIT_W, h);
        int tierColor = cat.getProductionTier().getColorValue() | 0xFF000000;
        String name = cat.getName().getString();
        g.drawCenteredString(font, font.plainSubstrByWidth(name, PORTRAIT_W - 12), x + PORTRAIT_W / 2, y + 7, tierColor);
        int fx = x + 10, fy = y + 20, fw = PORTRAIT_W - 20, fh = Math.max(56, h - 94);
        KTTheme.portrait(g, fx, fy, fw, fh);
        s.drawCat(g, fx + 4, fy + 4, fw - 8, fh - 8, mouseX, mouseY);

        // Level badge over the frame's top edge
        String lvl = (cat.isDireAwakened() ? "✦ " : "") + KTText.s("home.level_short", "Lv %s", cat.getCatLevel());
        int bw = font.width(lvl) + 8;
        int bx = x + (PORTRAIT_W - bw) / 2, by = fy + fh - 6;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 12, KTTheme.OUTLINE);
        KTTheme.tile(g, KTTheme.GOLD, bx, by, bw, 12);
        g.fill(bx, by, bx + bw, by + 12, 0x33000000);
        g.drawString(font, lvl, bx + 4, by + 2, KTTheme.TEXT_DARK, false);

        // HP / XP / Energy bars
        int barX = x + 22, barW = PORTRAIT_W - 32, barY = fy + fh + 12;
        g.blit(icon("hp_heart"), x + 8, barY - 3, 0, 0, 10, 10, 10, 10);
        KTTheme.bar(g, barX, barY, barW, 5, cat.getHealth() / Math.max(1F, cat.getMaxHealth()), 0xFFDC2626);
        g.drawString(font, String.format("%.0f/%.0f", cat.getHealth(), cat.getMaxHealth()), barX, barY + 7, KTTheme.TEXT_DIM, false);

        barY += 22;
        int maxLvl = KittyTalentsConfig.COMMON.maxCatLevel.get();
        boolean maxed = cat.getCatLevel() >= maxLvl;
        int need = maxed ? 1 : CatProgression.xpForNextLevel(cat.getCatLevel());
        g.blit(icon("ultimate_star"), x + 8, barY - 3, 0, 0, 10, 10, 10, 10);
        KTTheme.bar(g, barX, barY, barW, 5, maxed ? 1F : cat.getCatXp() / (float) Math.max(1, need), 0xFF65A30D);
        g.drawString(font, maxed ? KTText.s("home.xp_max", "XP max") : KTText.s("home.xp", "XP %s/%s", cat.getCatXp(), need),
                barX, barY + 7, KTTheme.TEXT_DIM, false);

        barY += 22;
        g.blit(icon("energy_bolt"), x + 8, barY - 3, 0, 0, 10, 10, 10, 10);
        KTTheme.bar(g, barX, barY, barW, 5, cat.getEnergy() / (float) Math.max(1, cat.getMaxEnergy()), 0xFF0EA5E9);

        // ---- Right top: info card (two columns)
        int rx = rx(s), rw = rw(s);
        int ih = infoH(s), step = compact(s) ? 11 : 13;
        KTTheme.inset(g, rx, y, rw, ih);
        int c1 = rx + 8, c2 = rx + rw / 2 + 4, ly = y + (compact(s) ? 5 : 7);
        String owner = cat.getOwner() != null ? cat.getOwner().getName().getString() : (cat.isTame() ? "You" : "Untamed");
        info(g, font, c1, ly, KTText.s("home.owner", "Owner"), owner, KTTheme.TEXT, rw / 2 - 12);
        info(g, font, c2, ly, KTText.s("home.points", "Skill pts"), String.valueOf(cat.getSkillPoints()), KTTheme.TEXT_GOLD, rw / 2 - 12);
        ly += step;
        info(g, font, c1, ly, KTText.s("home.breed", "Breed"), cat.getCatVariant().getDisplayName(),
                cat.getCatVariant().getRarity().getColor() | 0xFF000000, rw / 2 - 12);
        info(g, font, c2, ly, KTText.s("home.tier", "Tier"), cat.getProductionTier().getDisplayName(), tierColor, rw / 2 - 12);
        ly += step;
        info(g, font, c1, ly, KTText.s("home.gender", "Gender"), cat.isFemale() ? "♀ Female" : "♂ Male",
                cat.isFemale() ? 0xFFF472B6 : 0xFF60A5FA, rw / 2 - 12);
        info(g, font, c2, ly, KTText.s("home.hunger", "Hunger"), cat.getHunger() + "/120", 0xFF6EE7A0, rw / 2 - 12);

        // ---- Right bottom: controls card
        int cy = y + ih + 6;
        KTTheme.inset(g, rx, cy, rw, h - ih - 6);
        KTTheme.header(g, font, rx + 6, cy + 4, rw - 12, KTText.t("home.behaviour", "Behaviour & Actions"));
        // Section label above the name/trust rows
        int labelY = cy + 78;
        if (!compact(s)) KTTheme.header(g, font, rx + 6, labelY, rw - 12, KTText.t("home.identity", "Name & Co-owners"));
        var me = Minecraft.getInstance().player;
        boolean isOwner = me != null && me.getUUID().equals(cat.getOwnerUUID());
        if (!isOwner) {
            Map<UUID, String> trusted = cat.getTrustedPlayers();
            String list = trusted.isEmpty() ? KTText.s("home.trusted_none", "None") : String.join(", ", trusted.values());
            g.drawString(font, font.plainSubstrByWidth(KTText.s("home.trusted", "Trusted: %s", list), rw - 14),
                    rx + 7, (compact(s) ? cy + 101 : cy + 133), 0xFF7DD3FC, false);
        } else if (cat.getTrustedPlayers().isEmpty()) {
            g.drawString(font, KTText.s("home.trusted_hint", "No co-owners yet"), rx + 7, (compact(s) ? cy + 101 : cy + 133), KTTheme.TEXT_DIM, false);
        }
    }

    private static void info(GuiGraphics g, Font font, int x, int y, String label, String value, int color, int maxW) {
        String l = label + ": ";
        g.drawString(font, l, x, y, KTTheme.TEXT_DIM, false);
        int lw = font.width(l);
        g.drawString(font, font.plainSubstrByWidth(value, Math.max(10, maxW - lw)), x + lw, y, color, false);
    }

    static net.minecraft.resources.ResourceLocation icon(String name) {
        return new net.minecraft.resources.ResourceLocation("kittytalents", "textures/gui/icons/" + name + ".png");
    }
}
