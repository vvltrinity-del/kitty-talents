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
        boolean qualified = job.canCatPerform(cat);
        Component jobLabel = qualified
                ? KTText.t("home.job", "Job: %s", job.getDisplayName())
                : KTText.t("home.job_unqualified", "Job: %s (Unqualified)", job.getDisplayName());
        int jobColor = qualified ? KTTheme.TEXT_GOLD : 0xFF9CA3AF;
        s.add(new KTButton(x, y, w - clockW - 2, 16, jobLabel, b -> cycleJob(s))
                .icon(job.getIcon()).color(jobColor).tip(buildJobTooltip(cat, job)));
        boolean clockedIn = cat.isClockedIn();
        s.add(new KTButton(x + w - clockW, y, clockW, 16,
                clockedIn ? KTText.t("home.clocked_in", "Clocked In") : KTText.t("home.clocked_out", "Clocked Out"),
                b -> PacketHandler.sendToServer(new PacketSetCatJob(cat.getId(), cat.getCatJob(), !cat.isClockedIn()))).style(KTButton.Style.CHIP).selected(clockedIn).color(clockedIn ? 0xFF6EE7A0 : 0xFFF87171));

        // Actions: Sit/Stand, Pack, Whistle, TWINE
        y += 22;
        int bw = (w - 6) / 4;
        boolean sitting = cat.isOrderedToSit();
        s.add(new KTButton(x, y, bw, 18, sitting ? KTText.t("home.stand", "Stand") : KTText.t("home.sit", "Sit"), b -> {
            CatMode m = cat.isOrderedToSit() ? CatMode.FOLLOW : CatMode.DOCILE;
            PacketHandler.sendToServer(new PacketChangeCatMode(cat.getId(), m.ordinal()));
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
        var me = Minecraft.getInstance().player;
        if (!com.kittytalents.core.perm.CatPermissions.can(me, cat, com.kittytalents.core.perm.CatAction.RENAME)) {
            // Co-owner: renaming and managing co-owners are the owner's; they may only leave.
            if (me != null && cat.isTrusted(me)) {
                s.add(new KTButton(x, y, 90, 16, KTText.t("home.trust.leave", "Leave co-owners"),
                        b -> PacketHandler.sendToServer(new PacketManageTrust(cat.getId(), 1, me.getUUID().toString())))
                        .style(KTButton.Style.CHIP).color(0xFFFCA5A5)
                        .tip(KTText.t("home.trust.leave.tip", "Stop being a co-owner of this cat")));
            }
            return;
        }
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
        if (me != null && me.getUUID().equals(cat.getOwnerUUID())) {
            int chipX = x;
            for (Map.Entry<UUID, String> e : cat.getTrustedPlayers().entrySet()) {
                String pname = e.getValue().isEmpty() ? "Player" : e.getValue();
                UUID id = e.getKey();
                // Role chip (click to change what this co-owner may do), then a small remove button
                com.kittytalents.core.perm.TrustRole role = cat.getTrustRole(id);
                String label = pname + ": " + role.title().getString();
                int cw = Math.min(110, font.width(label) + 8);
                if (chipX + cw + 15 > x + w) break;
                s.add(new KTButton(chipX, y, cw, 14, Component.literal(label),
                        b -> PacketHandler.sendToServer(new PacketManageTrust(cat.getId(), PacketManageTrust.SET_ROLE,
                                id + "|" + role.next().name())))
                        .style(KTButton.Style.CHIP).color(role == com.kittytalents.core.perm.TrustRole.FULL ? 0xFF86EFAC
                                : role == com.kittytalents.core.perm.TrustRole.HANDLER ? 0xFF7DD3FC : 0xFFFDE68A)
                        .tip(Component.empty().append(role.text()).append("\n")
                                .append(KTText.t("home.trust.role_tip", "Click to change to %s", role.next().title().getString()))));
                chipX += cw + 1;
                s.add(new KTButton(chipX, y, 12, 14, Component.literal("✕"),
                        b -> PacketHandler.sendToServer(new PacketManageTrust(cat.getId(), 1, id.toString())))
                        .style(KTButton.Style.CHIP).color(0xFFFCA5A5)
                        .tip(KTText.t("home.trust.remove", "Remove %s as co-owner", pname)));
                chipX += 12 + 4;
            }
        }
    }

    // Requests only: the screen rebuilds when the server's synced state arrives (CatTalentScreen#containerTick),
    // so a rejected change never shows as applied.
    private static void setMode(CatTalentScreen s, CatMode mode) {
        PacketHandler.sendToServer(new PacketChangeCatMode(s.cat().getId(), mode.ordinal()));
    }

    private static void cycleJob(CatTalentScreen s) {
        TalentedCat cat = s.cat();
        CatJob[] jobs = CatJob.values();
        CatJob cur = cat.getCatJob();
        boolean shift = net.minecraft.client.gui.screens.Screen.hasShiftDown();
        CatJob next = cur;
        for (int i = 1; i <= jobs.length; i++) {
            CatJob candidate = jobs[(cur.ordinal() + i) % jobs.length];
            if (shift || candidate.canCatPerform(cat)) {
                next = candidate;
                break;
            }
        }
        // Picking a job clocks the cat in (and so puts it in WORK mode, server side).
        PacketHandler.sendToServer(new PacketSetCatJob(cat.getId(), next, next != CatJob.NONE || cat.isClockedIn()));
    }

    private static Component buildJobTooltip(TalentedCat cat, CatJob job) {
        if (job == CatJob.NONE) {
            return Component.literal(job.getDescription() + "\n§7Click to cycle qualified jobs.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("§6").append(job.getDisplayName()).append("§r\n");
        sb.append(job.getDescription()).append("\n\n");
        sb.append("§eRequirements:§r\n");
        if (job.getMinCatLevel() > 1) {
            boolean ok = cat.getCatLevel() >= job.getMinCatLevel();
            sb.append(ok ? " §a✔ " : " §c✕ ").append("Cat Level ").append(job.getMinCatLevel()).append("§r\n");
        }
        if (job.getRequiredTalent() != null) {
            boolean ok = cat.getTalentLevel(job.getRequiredTalent()) >= job.getMinTalentRank();
            sb.append(ok ? " §a✔ " : " §c✕ ").append(job.getRequiredTalent().getDisplayName())
              .append(" Rank ").append(job.getMinTalentRank()).append("§r\n");
        }
        if (job.requiresTacZ()) {
            boolean ok = job.isAvailable();
            sb.append(ok ? " §a✔ " : " §c✕ ").append("TACZ Gun Mod§r\n");
        }
        boolean ok = job.canCatPerform(cat);
        sb.append("\n");
        if (!ok) {
            sb.append("§c✕ Unqualified for this job§r\n");
        } else if (cat.getHunger() <= 20) {
            sb.append("§6⚠ Too hungry to work (Hunger ≤ 20)§r\n");
        } else {
            sb.append("§a✔ Qualified to work§r\n");
        }
        sb.append("§7Click to cycle qualified jobs (Shift-click: all)");
        return Component.literal(sb.toString());
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
        int fx = x + 10, fy = y + 20, fw = PORTRAIT_W - 20, fh = Math.max(48, h - 116);
        KTTheme.portrait(g, fx, fy, fw, fh);
        s.drawCat(g, fx + 4, fy + 4, fw - 8, fh - 8, mouseX, mouseY);

        // Level badge over the frame's top edge
        String lvl = (cat.isDireAwakened() ? "✦ " : "") + KTText.s("home.level_short", "Lv %s", cat.getCatLevel());
        var stage = cat.getGrowthStage();
        if (stage != com.kittytalents.core.living.GrowthStage.ADULT) lvl = stage.title().getString() + " · " + lvl;
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

        // Bond bar (progress to the next bond level) with the bond level and personality under it; hover for details
        barY += 14;
        int bond = cat.getBondLevel(), pts = cat.getBondPoints();
        int lo = com.kittytalents.core.living.CatBond.pointsFor(bond), hi = com.kittytalents.core.living.CatBond.pointsFor(bond + 1);
        float frac = bond >= com.kittytalents.core.living.CatBond.MAX_LEVEL ? 1F : (pts - lo) / (float) Math.max(1, hi - lo);
        g.drawString(font, "❤", x + 9, barY - 2, 0xFFF472B6, false);
        KTTheme.bar(g, barX, barY, barW, 5, frac, 0xFFEC4899);
        String bondText = KTText.s("home.bond", "Bond %s", bond);
        g.drawString(font, bondText, barX, barY + 7, KTTheme.TEXT_DIM, false);
        var trait = cat.getTrait();
        if (trait != com.kittytalents.core.living.CatTrait.NONE) {
            String t = trait.title().getString();
            g.drawString(font, t, barX + barW - font.width(t), barY + 7, trait.color, false);
        }
        livingX = x + 6; livingY = barY - 4; livingW = PORTRAIT_W - 12; livingH = 22;

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
        renderWorkStatusBadge(g, font, rx + rw - 6, cy + 4, cat);
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

    private int livingX, livingY, livingW, livingH;

    /** Hovering the bond row: personality (bonus and quirk), bond progress and perks, growth stage. */
    @Override
    public void renderOverlay(CatTalentScreen s, GuiGraphics g, int mouseX, int mouseY) {
        if (livingW == 0 || mouseX < livingX || mouseX >= livingX + livingW || mouseY < livingY || mouseY >= livingY + livingH) return;
        TalentedCat cat = s.cat();
        java.util.List<Component> lines = new java.util.ArrayList<>();
        var trait = cat.getTrait();
        if (trait != com.kittytalents.core.living.CatTrait.NONE) {
            lines.add(KTText.t("home.personality", "Personality: %s", trait.title().getString()).copy().withStyle(st -> st.withColor(trait.color & 0xFFFFFF)));
            lines.add(Component.literal(" + ").append(trait.bonusText()).withStyle(net.minecraft.ChatFormatting.GREEN));
            lines.add(Component.literal(" ~ ").append(trait.quirkText()).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        var stage = cat.getGrowthStage();
        if (stage != com.kittytalents.core.living.GrowthStage.ADULT) {
            lines.add(KTText.t("home.growing", "%s: still growing. Food and treats help.", stage.title().getString()).copy().withStyle(net.minecraft.ChatFormatting.AQUA));
        }
        int bond = cat.getBondLevel();
        String progress = bond >= com.kittytalents.core.living.CatBond.MAX_LEVEL ? KTText.s("home.bond_max", "max")
                : cat.getBondPoints() + " / " + com.kittytalents.core.living.CatBond.pointsFor(bond + 1);
        lines.add(KTText.t("home.bond_line", "Bond level %s (%s)", bond, progress).copy().withStyle(st -> st.withColor(0xF472B6)));
        lines.add(KTText.t("home.bond_how", "Grows with time together, petting, feeding, play and fighting side by side.").copy().withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        for (var perk : com.kittytalents.core.living.CatBond.Perk.values()) {
            boolean has = bond >= perk.level;
            lines.add(Component.literal((has ? "✔ " : "✖ ") + perk.level + "  ").append(perk.title()).append(": ").append(perk.text())
                    .withStyle(has ? net.minecraft.ChatFormatting.WHITE : net.minecraft.ChatFormatting.DARK_GRAY));
        }
        g.renderComponentTooltip(s.font(), lines, mouseX, mouseY);
    }

    private static void info(GuiGraphics g, Font font, int x, int y, String label, String value, int color, int maxW) {
        String l = label + ": ";
        g.drawString(font, l, x, y, KTTheme.TEXT_DIM, false);
        int lw = font.width(l);
        g.drawString(font, font.plainSubstrByWidth(value, Math.max(10, maxW - lw)), x + lw, y, color, false);
    }

    private static void renderWorkStatusBadge(GuiGraphics g, Font font, int rightX, int y, TalentedCat cat) {
        CatJob job = cat.getCatJob();
        if (job == CatJob.NONE && !cat.isClockedIn() && cat.getCatMode() != CatMode.WORK) return;

        String text;
        int fg;
        int bg;
        int border;

        if (job != CatJob.NONE && !job.canCatPerform(cat)) {
            text = KTText.s("home.work_status.unqualified", "Unqualified");
            fg = 0xFFFCA5A5;
            bg = 0x44EF4444;
            border = 0xFFEF4444;
        } else if (cat.getHunger() <= 20) {
            text = KTText.s("home.work_status.too_hungry", "Too Hungry");
            fg = 0xFFFCD34D;
            bg = 0x44F59E0B;
            border = 0xFFF59E0B;
        } else if (cat.isClockedIn() || cat.getCatMode() == CatMode.WORK) {
            String raw = cat.getWorkStatus();
            text = (raw == null || raw.isBlank()) ? KTText.s("home.work_status.working", "Working") : raw;
            if (text.contains(" (")) text = text.substring(0, text.indexOf(" ("));
            fg = 0xFF86EFAC;
            bg = 0x4422C55E;
            border = 0xFF22C55E;
        } else {
            return;
        }

        int textW = font.width(text);
        int bw = textW + 8;
        int bx = rightX - bw;
        g.fill(bx, y, bx + bw, y + 12, bg);
        g.renderOutline(bx, y, bw, 12, border);
        g.drawString(font, text, bx + 4, y + 2, fg, false);
    }

    static net.minecraft.resources.ResourceLocation icon(String name) {
        return new net.minecraft.resources.ResourceLocation("kittytalents", "textures/gui/icons/" + name + ".png");
    }
}
