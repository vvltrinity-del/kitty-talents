package com.kittytalents.client.hud;

import com.kittytalents.client.twine.ClientTwineState;
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

    private static List<TalentedCat> cached = List.of();
    private static long cachedTick = Long.MIN_VALUE;

    /** Cats that get a card, nearest first. Recomputed once per game tick, not every frame. */
    public static List<TalentedCat> shownCats(Minecraft mc) {
        long now = mc.level.getGameTime();
        if (now != cachedTick) {
            cachedTick = now;
            cached = findCats(mc);
        }
        return cached;
    }

    private static List<TalentedCat> findCats(Minecraft mc) {
        LocalPlayer me = mc.player;
        double range = KittyTalentsClientConfig.COMPANION_HUD_RANGE.get();
        List<TalentedCat> near = mc.level.getEntitiesOfClass(TalentedCat.class, me.getBoundingBox().inflate(range),
                cat -> cat.isAlive() && cat.isTame());
        // Friends (optional): players you share a cat with, either way round.
        java.util.Set<java.util.UUID> friends = new java.util.HashSet<>();
        if (KittyTalentsClientConfig.SHOW_FRIEND_CATS.get()) {
            for (TalentedCat cat : near) {
                if (me.getUUID().equals(cat.getOwnerUUID())) friends.addAll(cat.getTrustedPlayers().keySet());
                else if (cat.isTrusted(me) && cat.getOwnerUUID() != null) friends.add(cat.getOwnerUUID());
            }
        }
        return near.stream().filter(cat ->
                        (me.getUUID().equals(cat.getOwnerUUID()) || cat.isTrusted(me) || friends.contains(cat.getOwnerUUID()))
                                && (cat.getCatMode().followsOwner() || cat.isIncapacitated()
                                    || cat.getHealth() < cat.getMaxHealth() * 0.5F))
                .sorted(Comparator.comparingDouble(me::distanceToSqr))
                .limit(KittyTalentsClientConfig.COMPANION_HUD_MAX.get())
                .toList();
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui || ClientTwineState.isTwined()) return;
        if (!KittyTalentsClientConfig.SHOW_COMPANION_HUD.get()) return;
        // Managed: fills and text batch into a few draws instead of one draw call each.
        g.drawManaged(() -> {
            int y = Y;
            for (TalentedCat cat : shownCats(mc)) {
                y += card(g, mc.font, X, y, cat) + 4;
            }
        });
    }

    /** The owner's name when the cat is not yours (a co-owned or a friend's cat), else null. */
    @javax.annotation.Nullable
    public static String otherOwner(TalentedCat cat) {
        Minecraft mc = Minecraft.getInstance();
        java.util.UUID owner = cat.getOwnerUUID();
        if (owner == null || mc.player == null || owner.equals(mc.player.getUUID())) return null;
        var info = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(owner) : null;
        return info != null ? info.getProfile().getName() : KTText.s("hud.someone", "someone");
    }

    /** Draws one card. @return its height (cats that are not yours get an extra line naming the owner). */
    public static int card(GuiGraphics g, Font font, int x, int y, TalentedCat cat) {
        String other = otherOwner(cat);
        int h = other != null ? H + 9 : H;
        KTTheme.hudPanel(g, x, y, W, h);
        if (other != null) {
            String line = KTText.s("hud.owned_by", "%s's cat", other);
            g.drawString(font, font.plainSubstrByWidth(line, W - 10), x + 5, y + H - 3, 0xFF7DD3FC, false);
        }
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
        return h;
    }
}
