package com.kittytalents.client.gui.tab;

import com.kittytalents.ability.CatAbilities;
import com.kittytalents.ability.CatAbility;
import com.kittytalents.client.gui.CatTalentScreen;
import com.kittytalents.client.gui.theme.KTButton;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.client.gui.theme.KTWidget;
import com.kittytalents.combat.CatRole;
import com.kittytalents.combat.CatRoleEngine;
import com.kittytalents.combat.FelineElement;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Combat: role and element on the left; the four Twine ability cards and presets on the right. */
public class CombatTab implements CatTab {
    private static final int LEFT_W = 210, CARD_H = 22;
    private static int presetHeaderY(CatTalentScreen s) { return s.cy() + 18 + 4 * (CARD_H + 2) + 3; }
    /** Every role (5.0: six were hidden, though the Field Guide listed all twelve). */
    private static final CatRole[] ROLES = { CatRole.DPS, CatRole.TANK, CatRole.HEALER, CatRole.MAGE, CatRole.SCOUT, CatRole.BRUISER,
            CatRole.ASSASSIN, CatRole.SUPPORT, CatRole.CONTROLLER, CatRole.BREAKER, CatRole.MOBILITY, CatRole.HYBRID };
    /** Rows of role chips: Auto plus the roles, four per row. */
    private static final int ROLE_ROWS = (ROLES.length + 1 + 3) / 4;
    private static final FelineElement[] ELEMENTS = { FelineElement.PHYSICAL, FelineElement.FIRE, FelineElement.FROST,
            FelineElement.ELECTRIC, FelineElement.POISON, FelineElement.SHADOW, FelineElement.ARCANE };
    private static final Map<String, ResourceLocation> ICON_CACHE = new HashMap<>();

    private record Card(String key, String fallback, Function<TalentedCat, String> get, BiConsumer<TalentedCat, String> set, boolean ultimate) {}

    private static final Card[] CARDS = {
            new Card("V", "feline_dash", TalentedCat::getAbilitySlot1, TalentedCat::setAbilitySlot1, false),
            new Card("G", "feline_hiss", TalentedCat::getAbilitySlot2, TalentedCat::setAbilitySlot2, false),
            new Card("B", "healing_purr", TalentedCat::getAbilitySlot3, TalentedCat::setAbilitySlot3, false),
            new Card("R", "catnip_frenzy", TalentedCat::getAbilityUltimate, TalentedCat::setAbilityUltimate, true),
    };

    private int hoveredCard = -1;

    @Override
    public Component title(CatTalentScreen s) { return KTText.t("tab.combat.title", "Combat"); }

    private int rx(CatTalentScreen s) { return s.cx() + LEFT_W + 6; }
    private int rw(CatTalentScreen s) { return s.cw() - LEFT_W - 6; }
    /** Role description lines shown under the role name (dropped on small GUIs). */
    private static int roleDescLines(CatTalentScreen s) { return s.ch() >= 200 ? 2 : 0; }
    private static int statsY(CatTalentScreen s) { return s.cy() + 20 + ROLE_ROWS * 18 + 11 + roleDescLines(s) * 10 + 2; }
    private static int elementHeaderY(CatTalentScreen s) { return statsY(s) + 22; }

    static int elementColor(FelineElement e) {
        return switch (e) {
            case FIRE -> 0xFFFB923C;
            case FROST -> 0xFF7DD3FC;
            case ELECTRIC -> 0xFFFDE047;
            case POISON -> 0xFF86EFAC;
            case SHADOW -> 0xFFA78BFA;
            case ARCANE -> 0xFFF0ABFC;
            default -> 0xFFD6D3D1;
        };
    }

    private static String elementDesc(FelineElement e) {
        return switch (e) {
            case FIRE -> KTText.s("combat.element.fire", "Ignites foes with lingering burn damage.");
            case FROST -> KTText.s("combat.element.frost", "Slows enemies and inflicts freezing.");
            case ELECTRIC -> KTText.s("combat.element.electric", "Chains lightning between nearby enemies.");
            case POISON -> KTText.s("combat.element.poison", "Lingering venom that ignores armor.");
            case SHADOW -> KTText.s("combat.element.shadow", "Wither decay and stronger ambushes.");
            case ARCANE -> KTText.s("combat.element.arcane", "Raw magic damage and faster energy recovery.");
            default -> KTText.s("combat.element.physical", "Pure kinetic strikes with strong stagger.");
        };
    }

    static ResourceLocation abilityIcon(String id) {
        return ICON_CACHE.computeIfAbsent(id, k -> {
            ResourceLocation rl = new ResourceLocation("kittytalents", "textures/ability/" + k + ".png");
            return Minecraft.getInstance().getResourceManager().getResource(rl).isPresent() ? rl : HomeTab.icon("ultimate_star");
        });
    }

    @Override
    public void init(CatTalentScreen s) {
        TalentedCat cat = s.cat();
        int x = s.cx() + 6, y = s.cy(), w = LEFT_W - 12;

        // 5.0: a max-level cat can be promoted in its role (owner only).
        var self = net.minecraft.client.Minecraft.getInstance().player;
        if (cat.canPromoteRole() && self != null && self.getUUID().equals(cat.getOwnerUUID())) {
            Component promo = Component.translatable(com.kittytalents.combat.RolePromotions.key(cat.getCatRole()));
            s.add(new KTButton(x + w - 74, y + 2, 74, 13, KTText.t("combat.promote", "★ Promote"),
                    b -> com.kittytalents.network.PacketHandler.sendToServer(new com.kittytalents.network.PacketPromoteRole(cat.getId())))
                    .style(KTButton.Style.GOLD)
                    .tip(Component.empty().append(promo).append(": ").append(Component.translatable(com.kittytalents.combat.RolePromotions.key(cat.getCatRole()) + ".desc"))));
        }

        // Roles (4 per row: Auto + every role)
        int chipW = (w - 6) / 4;
        s.add(new KTButton(x, y + 18, chipW, 16, KTText.t("combat.auto", "Auto"), b -> {
            CatRoleEngine.autoUpdateRole(cat);
            s.rebuild();
        }).style(KTButton.Style.GOLD).tip(KTText.t("combat.auto.tip", "Pick the role that fits this cat's talents")));
        for (int i = 0; i < ROLES.length; i++) {
            CatRole r = ROLES[i];
            int slot = i + 1;
            s.add(new KTButton(x + (slot % 4) * (chipW + 2), y + 18 + (slot / 4) * 18, chipW, 16, Component.literal(r.getDisplayName()), b -> {
                cat.setCatRole(r);
                s.sendLoadout();
                s.rebuild();
            }).style(KTButton.Style.CHIP).selected(cat.getCatRole() == r).tip(Component.literal(r.getDescription())));
        }

        // Elements (4 per row)
        for (int i = 0; i < ELEMENTS.length; i++) {
            FelineElement el = ELEMENTS[i];
            boolean cur = cat.getPrimaryElement() == el;
            s.add(new KTButton(x + (i % 4) * (chipW + 2), elementHeaderY(s) + 12 + (i / 4) * 18, chipW, 16, Component.literal(cap(el.name())), b -> {
                cat.setPrimaryElement(el);
                s.sendLoadout();
                s.rebuild();
            }).style(KTButton.Style.CHIP).selected(cur).color(cur ? elementColor(el) : KTTheme.dim(elementColor(el))));
        }

        // Ability cards 2x2
        int rx = rx(s) + 6, rw = rw(s) - 12;
        int cardW = rw, cardH = CARD_H;
        for (int i = 0; i < CARDS.length; i++) {
            Card c = CARDS[i];
            int idx = i;
            int cx = rx, cy = y + 18 + i * (cardH + 2);
            s.add(new KTWidget(cx, cy, cardW, cardH, Component.literal(c.key()), (g, wx, wy, ww, wh, hover) -> {
                if (hover) hoveredCard = idx;
                paintCard(s, g, c, wx, wy, ww, wh, hover);
            }, () -> {
                cycle(cat, c);
                s.sendLoadout();
                s.rebuild();
            }));
        }

        // Presets 2x2
        int py = presetHeaderY(s) + 12;
        int presetW = (rw - 4) / 2;
        String[][] presets = {
                { "balanced", "Balanced", "feline_dash", "feline_hiss", "healing_purr", "catnip_frenzy" },
                { "berserker", "Berserker", "shadow_strike", "venom_fang", "inferno_spin", "apex_predator" },
                { "elemental", "Elemental", "volt_dash", "frost_nova", "tidal_surge", "soul_flame_barrage" },
                { "guardian", "Guardian", "fortress_guard", "feline_dash", "healing_purr", "celestial_purr" },
        };
        for (int i = 0; i < presets.length; i++) {
            String[] p = presets[i];
            s.add(new KTButton(rx + (i % 2) * (presetW + 4), py + (i / 2) * 18, presetW, 16, KTText.t("combat.preset." + p[0], p[1]), b -> {
                cat.setAbilitySlot1(p[2]);
                cat.setAbilitySlot2(p[3]);
                cat.setAbilitySlot3(p[4]);
                cat.setAbilityUltimate(p[5]);
                s.sendLoadout();
                s.rebuild();
            }).icon(abilityIcon(p[5])));
        }
    }

    private static String cap(String s) { return s.charAt(0) + s.substring(1).toLowerCase(); }

    private static String abilityId(TalentedCat cat, Card c) {
        String id = c.get().apply(cat);
        return id != null && !id.isEmpty() ? id : c.fallback();
    }

    private static void cycle(TalentedCat cat, Card c) {
        List<CatAbility> list = c.ultimate() ? CatAbilities.getUltimates() : CatAbilities.getSkills();
        if (list.isEmpty()) return;
        String cur = c.get().apply(cat);
        int idx = -1;
        if (cur != null && !cur.isEmpty()) {
            for (int i = 0; i < list.size(); i++) if (list.get(i).getId().equalsIgnoreCase(cur)) { idx = i; break; }
        }
        c.set().accept(cat, list.get((idx + 1) % list.size()).getId());
    }

    private static void paintCard(CatTalentScreen s, GuiGraphics g, Card c, int x, int y, int w, int h, boolean hover) {
        Font font = s.font();
        String id = abilityId(s.cat(), c);
        CatAbility ab = CatAbilities.get(id);
        int border = c.ultimate() ? KTTheme.GOLD_EDGE : (hover ? 0xFFD8D4CC : 0xFF6B6760);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, KTTheme.OUTLINE);
        g.fill(x, y, x + w, y + h, hover ? 0xCC2A2018 : 0xCC1A140F);
        KTTheme.outline(g, x, y, w, h, border);
        g.blit(abilityIcon(id), x + 3, y + 3, 0, 0, 16, 16, 16, 16);
        // Key badge
        int kx = x + 22;
        g.fill(kx, y + 6, kx + 10, y + 16, KTTheme.OUTLINE);
        g.fill(kx + 1, y + 7, kx + 9, y + 15, c.ultimate() ? 0xFFB7791F : 0xFF3F3B36);
        g.drawCenteredString(font, c.key(), kx + 5, y + 7, KTTheme.TEXT);
        int cost = ab != null ? ab.getEnergyCost() : 0;
        int cd = ab != null ? ab.getCooldownTicks() / 20 : 0;
        String stats = cost + "⚡ " + cd + "s";
        int statsX = x + w - 5 - font.width(stats);
        g.drawString(font, stats, statsX, y + 7, 0xFF7DD3FC, false);
        String name = ab != null ? ab.getDisplayName() : id.replace('_', ' ');
        int avail = statsX - 6 - (kx + 14);
        if (font.width(name) > avail) name = font.plainSubstrByWidth(name, avail - font.width("…")) + "…";
        g.drawString(font, name, kx + 14, y + 7, c.ultimate() ? KTTheme.TEXT_GOLD : KTTheme.TEXT, false);
        if (ab != null) g.fill(x + 3, y + h - 3, x + 19, y + h - 2, elementColor(ab.getElement()));
    }

    @Override
    public void render(CatTalentScreen s, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        TalentedCat cat = s.cat();
        Font font = s.font();
        int x = s.cx(), y = s.cy(), h = s.ch();
        hoveredCard = -1;

        // ---- Left: role & element
        KTTheme.inset(g, x, y, LEFT_W, h);
        KTTheme.header(g, font, x + 7, y + 6, LEFT_W - 14, KTText.t("combat.role", "Role"));
        CatRole role = cat.getCatRole();
        List<FormattedCharSequence> rd = font.split(Component.literal(role.getDescription()), LEFT_W - 16);
        int ty = y + 22 + ROLE_ROWS * 18;
        g.drawString(font, role.getDisplayName(), x + 8, ty, KTTheme.TEXT_GOLD, true);
        if (cat.isPromoted()) { // 5.0 promotion name after the role
            String promo = "★ " + Component.translatable(com.kittytalents.combat.RolePromotions.key(role)).getString();
            g.drawString(font, promo, x + 8 + font.width(role.getDisplayName()) + 6, ty, 0xFFF0C75E, true);
        }
        for (int i = 0; i < Math.min(roleDescLines(s), rd.size()); i++) g.drawString(font, rd.get(i), x + 8, ty + 11 + i * 10, KTTheme.TEXT_DIM, false);
        int sy = statsY(s);
        int sx = stat(g, font, x + 8, sy, "HP", role.getHpMultiplier(), 0xFF86EFAC);
        sx = stat(g, font, sx, sy, "DMG", role.getDamageMultiplier(), 0xFFFCA5A5);
        stat(g, font, sx, sy, "SPD", role.getSpeedMultiplier(), 0xFF7DD3FC);
        sx = stat(g, font, x + 8, sy + 10, "NRG", role.getEnergyRegenMultiplier(), 0xFFFDE047);
        g.drawString(font, "CRIT", sx, sy + 10, KTTheme.TEXT_DIM, false);
        g.drawString(font, String.format("+%.0f%%", role.getCritChanceBonus() * 100), sx + font.width("CRIT "), sy + 10, 0xFFF0ABFC, false);

        int ey = elementHeaderY(s);
        KTTheme.header(g, font, x + 7, ey, LEFT_W - 14, KTText.t("combat.element", "Element"));
        FelineElement el = cat.getPrimaryElement();
        List<FormattedCharSequence> ed = font.split(Component.literal(elementDesc(el)), LEFT_W - 16);
        // The element name only where it fits (all twelve roles take two more chip rows; 5.0).
        if (ey + 59 <= y + h - 3) g.drawString(font, cap(el.name()), x + 8, ey + 50, elementColor(el), true);
        int edLines = Math.max(0, (y + h - 4 - (ey + 61)) / 10);
        for (int i = 0; i < Math.min(edLines, ed.size()); i++) g.drawString(font, ed.get(i), x + 8, ey + 61 + i * 10, KTTheme.TEXT_DIM, false);

        // ---- Right: loadout
        int rx = rx(s), rw = rw(s);
        KTTheme.inset(g, rx, y, rw, h);
        KTTheme.header(g, font, rx + 7, y + 6, rw - 14, KTText.t("combat.loadout", "Twine Loadout"));
        KTTheme.header(g, font, rx + 7, presetHeaderY(s), rw - 14, KTText.t("combat.presets", "Presets"));
    }

    private static int stat(GuiGraphics g, Font font, int x, int y, String label, float mult, int color) {
        g.drawString(font, label, x, y, KTTheme.TEXT_DIM, false);
        String v = String.format("%.0f%%", mult * 100);
        int vx = x + font.width(label + " ");
        g.drawString(font, v, vx, y, color, false);
        return vx + font.width(v) + 8;
    }

    @Override
    public void renderOverlay(CatTalentScreen s, GuiGraphics g, int mouseX, int mouseY) {
        // Description of the hovered card (or a hint) under the presets
        Font font = s.font();
        int rx = rx(s) + 7, rw = rw(s) - 14;
        int y = presetHeaderY(s) + 12 + 2 * 18 + 3;
        int maxLines = Math.max(0, (s.cy() + s.ch() - 6 - y) / 10);
        Component text;
        if (hoveredCard >= 0) {
            CatAbility ab = CatAbilities.get(abilityId(s.cat(), CARDS[hoveredCard]));
            text = ab == null ? Component.empty() : Component.literal(ab.getDisplayName() + ": ").withStyle(s2 -> s2.withColor(KTTheme.TEXT_GOLD & 0xFFFFFF))
                    .append(Component.literal(ab.getDescription()).withStyle(s2 -> s2.withColor(KTTheme.TEXT & 0xFFFFFF)));
            if (ab != null) {
                for (Component v : com.kittytalents.core.tuning.Tuning.describeAll(com.kittytalents.core.tuning.Tuning.ABILITIES, ab.getId(),
                        java.util.Set.of("energy_cost", "cooldown_ticks"))) {
                    text = text.copy().append(Component.literal("  · ").withStyle(s2 -> s2.withColor(0x7DD3FC))).append(v.copy().withStyle(s2 -> s2.withColor(0x7DD3FC)));
                }
            }
        } else {
            text = KTText.t("combat.hint", "Click a card to cycle its ability. Keys work while Twined.");
        }
        List<FormattedCharSequence> lines = font.split(text, rw);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            g.drawString(font, lines.get(i), rx, y + i * 10, hoveredCard >= 0 ? KTTheme.TEXT : KTTheme.TEXT_DIM, false);
        }
    }
}
