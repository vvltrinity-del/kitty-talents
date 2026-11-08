package com.kittytalents.client.hud;

import com.kittytalents.client.ClientTwineHandler;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.config.KittyTalentsClientConfig;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Comparator;
import java.util.List;

/**
 * Companion cards (Phase 6): a small wood-and-leather card per cat of yours that is following you, or is hurt or
 * downed, within range. Name, level, HP bar and status badges. Hidden while Twined (the Twine HUD takes over) and
 * configurable in kittytalents-client.toml.
 */
public final class CompanionHud implements IGuiOverlay {
    public static final CompanionHud INSTANCE = new CompanionHud();
    private static final int X = 6, Y = 6, W = 118, H = 28;

    private CompanionHud() {}

    /** Cats that get a card, nearest first. */
    public static List<TalentedCat> shownCats(Minecraft mc) {
        LocalPlayer me = mc.player;
        double range = KittyTalentsClientConfig.COMPANION_HUD_RANGE.get();
        return mc.level.getEntitiesOfClass(TalentedCat.class, me.getBoundingBox().inflate(range), cat ->
                        cat.isAlive() && cat.isTame()
                                && (me.getUUID().equals(cat.getOwnerUUID()) || cat.isTrusted(me))
                                && (cat.getCatMode().followsOwner() || cat.isIncapacitated()
                                    || cat.getHealth() < cat.getMaxHealth() * 0.5F))
                .stream()
                .sorted(Comparator.comparingDouble(me::distanceToSqr))
                .limit(KittyTalentsClientConfig.COMPANION_HUD_MAX.get())
                .toList();
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui || ClientTwineHandler.isTwined()) return;
        if (!KittyTalentsClientConfig.SHOW_COMPANION_HUD.get()) return;
        int y = Y;
        for (TalentedCat cat : shownCats(mc)) {
            card(g, mc.font, X, y, cat);
            y += H + 4;
        }
    }

    public static void card(GuiGraphics g, Font font, int x, int y, TalentedCat cat) {
        KTTheme.hudPanel(g, x, y, W, H);
        String lvl = "Lv" + cat.getCatLevel();
        int lvlW = font.width(lvl);
        String name = cat.getName().getString();
        int nameMax = W - 12 - lvlW - 4;
        if (font.width(name) > nameMax) name = font.plainSubstrByWidth(name, nameMax - font.width("…")) + "…";
        g.drawString(font, name, x + 5, y + 5, cat.getProductionTier().getColorValue() | 0xFF000000, true);
        g.drawString(font, lvl, x + W - 5 - lvlW, y + 5, KTTheme.TEXT_DIM, false);

        int by = y + 17;
        if (cat.isIncapacitated()) {
            g.drawString(font, KTText.s("hud.downed", "DOWNED"), x + 5, by - 2, 0xFFF87171, true);
        } else {
            float hp = cat.getHealth() / Math.max(1F, cat.getMaxHealth());
            int color = hp > 0.5F ? 0xFF22C55E : (hp > 0.25F ? 0xFFF59E0B : 0xFFDC2626);
            List<StatusView.Entry> statuses = StatusView.of(cat);
            int shown = Math.min(3, statuses.size());
            int barW = W - 10 - shown * 10;
            KTTheme.bar(g, x + 5, by, barW, 4, hp, color);
            int sx = x + 5 + barW + 3;
            for (int i = 0; i < shown; i++) sx += StatusView.drawBadge(g, font, sx, by - 2, 8, statuses.get(i), false) - 1;
        }
    }
}
