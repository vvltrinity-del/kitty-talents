package com.kittytalents.client.hud;

import com.kittytalents.ability.CatAbilities;
import com.kittytalents.ability.CatAbility;
import com.kittytalents.client.ClientTwineHandler;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.combat.CatRole;
import com.kittytalents.compat.tacz.TacZCompat;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HUD while Twined (Phase 6): a wood-and-leather card in the top-left with name, level and role, HP / Energy /
 * Ultimate bars, the four ability slots with cooldown sweeps, and status badges underneath. Registered as a GUI
 * overlay above the hotbar, so F1 and other HUD-hiding screens hide it too.
 */
public final class TwineHud implements IGuiOverlay {
    public static final TwineHud INSTANCE = new TwineHud();
    private static final int X = 6, Y = 6, W = 158, H = 77, SLOT = 24;
    private static final Map<String, ResourceLocation> ICONS = new HashMap<>();

    private TwineHud() {}

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        if (!ClientTwineHandler.isTwined()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) return;
        Entity entity = mc.level.getEntity(ClientTwineHandler.activeTwinedCatId);
        if (!(entity instanceof TalentedCat cat)) return;
        draw(g, mc, cat, screenWidth, screenHeight);
    }

    /** Draws the HUD for a cat (also used by the screenshot harness). */
    public static void draw(GuiGraphics g, Minecraft mc, TalentedCat cat, int screenWidth, int screenHeight) {
        Font font = mc.font;
        long now = cat.level().getGameTime();

        if (cat.hasEquippedGun() && mc.options.getCameraType().isFirstPerson()) {
            gunCrosshair(g, screenWidth / 2, screenHeight / 2, cat, mc);
        }

        KTTheme.hudPanel(g, X, Y, W, H);

        // Header: name + level, role badge on the right
        boolean hasRole = cat.getCatRole() != CatRole.NONE;
        String role = cat.getCatRole().getDisplayName().toUpperCase();
        int roleW = hasRole ? font.width(role) + 6 : 0;
        String lvl = " Lv" + cat.getCatLevel();
        int nameMax = W - 12 - roleW - font.width(lvl) - 4;
        String name = cat.getName().getString();
        if (font.width(name) > nameMax) name = font.plainSubstrByWidth(name, nameMax - font.width("…")) + "…";
        g.drawString(font, name, X + 6, Y + 6, cat.getProductionTier().getColorValue() | 0xFF000000, true);
        g.drawString(font, lvl, X + 6 + font.width(name), Y + 6, KTTheme.TEXT_DIM, false);
        if (hasRole) KTTheme.badge(g, font, X + W - 6 - roleW, Y + 5, role, roleColor(cat.getCatRole()));

        // Bars
        float hp = cat.getHealth() / Math.max(1F, cat.getMaxHealth());
        int hpColor = hp > 0.5F ? 0xFF22C55E : (hp > 0.25F ? 0xFFF59E0B : 0xFFDC2626);
        bar(g, font, Y + 19, "hp_heart", hp, hpColor, String.format("%.0f", cat.getHealth()));
        int maxEnergy = Math.max(1, cat.getMaxEnergy());
        bar(g, font, Y + 29, "energy_bolt", cat.getEnergy() / (float) maxEnergy, 0xFF0EA5E9, String.valueOf(cat.getEnergy()));
        String ultId = cat.getAbilityUltimate();
        float ult = cooldownProgress(cat, ultId, now);
        boolean ultReady = ult >= 1F;
        bar(g, font, Y + 39, "ultimate_star", ult, ultReady ? 0xFFE8B23A : 0xFF9333EA,
                ultReady ? KTText.s("hud.ready", "READY") : (int) (ult * 100) + "%");

        // Ability slots
        String ultKey = cat.hasEquippedGun() ? "Sh+R" : "R";
        String[][] slots = { { "V", cat.getAbilitySlot1() }, { "G", cat.getAbilitySlot2() }, { "B", cat.getAbilitySlot3() }, { ultKey, ultId } };
        int sx = X + (W - 4 * SLOT - 3 * 6) / 2, sy = Y + 49;
        for (int i = 0; i < 4; i++) slot(g, font, sx + i * (SLOT + 6), sy, slots[i][0], slots[i][1], cat, now, i == 3);

        // Statuses under the panel, then the controls hint
        int below = Y + H + 4;
        List<StatusView.Entry> statuses = StatusView.of(cat);
        if (!statuses.isEmpty()) {
            int bx = X + 1;
            for (StatusView.Entry e : statuses) {
                if (bx + 12 > X + W) break;
                bx += StatusView.drawBadge(g, font, bx, below, 12, e, true) + 1;
            }
            below += 20;
        }
        footer(g, font, cat, below);
    }

    private static void bar(GuiGraphics g, Font font, int y, String icon, float frac, int color, String value) {
        g.blit(new ResourceLocation("kittytalents", "textures/gui/icons/" + icon + ".png"), X + 6, y - 1, 0, 0, 8, 8, 8, 8);
        int bx = X + 18, bw = W - 18 - 6 - 36;
        KTTheme.bar(g, bx, y + 1, bw, 4, frac, color);
        g.drawString(font, value, X + W - 6 - font.width(value), y - 1, KTTheme.TEXT, false);
    }

    /** 0..1 cooldown progress for an ability (1 = ready). */
    private static float cooldownProgress(TalentedCat cat, String id, long now) {
        CatAbility ab = CatAbilities.get(id);
        long last = cat.getAbilityCooldown(id);
        long cd = ab != null ? ab.getCooldownTicks() : 40;
        if (last <= 0 || cd <= 0 || now >= last + cd) return 1F;
        return Math.max(0F, (now - last) / (float) cd);
    }

    private static void slot(GuiGraphics g, Font font, int x, int y, String key, String id, TalentedCat cat, long now, boolean ult) {
        CatAbility ab = CatAbilities.get(id);
        float progress = cooldownProgress(cat, id, now);
        boolean ready = progress >= 1F;
        boolean affordable = ab == null || cat.getEnergy() >= ab.getEnergyCost();

        g.fill(x - 1, y - 1, x + SLOT + 1, y + SLOT + 1, KTTheme.OUTLINE);
        g.fill(x, y, x + SLOT, y + SLOT, 0xFF201812);
        if (id != null && !id.isEmpty()) g.blit(icon(id), x + 4, y + 4, 0, 0, 16, 16, 16, 16);
        if (!ready) {
            int h = Math.round(SLOT * (1F - progress));
            g.fill(x, y, x + SLOT, y + h, 0xAA000000);
            long last = cat.getAbilityCooldown(id);
            long cd = ab != null ? ab.getCooldownTicks() : 40;
            float secs = Math.max(0, (last + cd - now) / 20F);
            String t = secs >= 10 ? String.valueOf((int) secs) : String.format("%.1f", secs);
            g.drawCenteredString(font, t, x + SLOT / 2, y + 8, KTTheme.TEXT);
        } else if (!affordable) {
            g.fill(x, y, x + SLOT, y + SLOT, 0x66450A0A);
        }
        int border = ult ? (ready ? KTTheme.GOLD_EDGE : 0xFF8A6A2A) : (ready && affordable ? 0xFF8D8A84 : 0xFF4A4642);
        if (ult && ready && (now / 10) % 2 == 0) border = 0xFFFFE08A;
        KTTheme.outline(g, x - 1, y - 1, SLOT + 2, SLOT + 2, border);
        // Key letter in the bottom-right corner
        int kw = font.width(key) + 3;
        g.fill(x + SLOT - kw, y + SLOT - 9, x + SLOT, y + SLOT, 0xDD000000);
        g.drawString(font, key, x + SLOT - kw + 2, y + SLOT - 8, ult ? KTTheme.TEXT_GOLD : KTTheme.TEXT, false);
    }

    private static ResourceLocation icon(String id) {
        return ICONS.computeIfAbsent(id, k -> {
            ResourceLocation rl = new ResourceLocation("kittytalents", "textures/ability/" + k + ".png");
            return Minecraft.getInstance().getResourceManager().getResource(rl).isPresent() ? rl
                    : new ResourceLocation("kittytalents", "textures/gui/icons/ultimate_star.png");
        });
    }

    private static void footer(GuiGraphics g, Font font, TalentedCat cat, int y) {
        String text;
        int color;
        if (cat.hasEquippedGun()) {
            ItemStack gun = cat.getEquippedWeapon();
            boolean reloading = TacZCompat.isReloading(cat);
            text = reloading ? KTText.s("hud.gun.reloading", "%s · reloading…", gun.getHoverName().getString())
                    : KTText.s("hud.gun", "%s · %s/%s · [R] Reload", gun.getHoverName().getString(),
                    TacZCompat.getAmmoCount(gun), TacZCompat.getMaxAmmoCount(gun));
            color = reloading ? 0xFFF59E0B : 0xFF7DD3FC;
        } else {
            text = KTText.s("hud.twine.controls", "[Alt] Dodge · [X] Synergy · [U] Untwine");
            color = 0xCCC9C2B8;
        }
        g.drawString(font, text, X, y, color, true);
    }

    private static void gunCrosshair(GuiGraphics g, int cx, int cy, TalentedCat cat, Minecraft mc) {
        boolean aiming = mc.options.keyUse.isDown();
        boolean reloading = TacZCompat.isReloading(cat);
        int gap = aiming ? 3 : 6, len = aiming ? 4 : 5;
        int color = reloading ? 0xCCF59E0B : (aiming ? 0xEE22C55E : 0xDD38BDF8);
        g.fill(cx - gap - len, cy, cx - gap, cy + 1, color);
        g.fill(cx + gap + 1, cy, cx + gap + 1 + len, cy + 1, color);
        g.fill(cx, cy - gap - len, cx + 1, cy - gap, color);
        g.fill(cx, cy + gap + 1, cx + 1, cy + gap + 1 + len, color);
    }

    static int roleColor(CatRole role) {
        if (role == null) return 0xFFA8A29E;
        return switch (role) {
            case DPS, ASSASSIN -> 0xFFF87171;
            case TANK, BRUISER -> 0xFF60A5FA;
            case HEALER, SUPPORT -> 0xFF34D399;
            case MAGE -> 0xFFC084FC;
            case SCOUT, MOBILITY -> 0xFFFBBF24;
            case CONTROLLER, BREAKER -> 0xFFF472B6;
            case HYBRID -> 0xFF818CF8;
            default -> 0xFFA8A29E;
        };
    }
}
