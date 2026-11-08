package com.kittytalents.client.hud;

import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.combat.FelineStatusType;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Reads a cat's synced statuses (TalentedCat#getSyncedStatuses) and draws them as small icon badges. */
public final class StatusView {
    private StatusView() {}

    public record Entry(FelineStatusType type, int remainingTicks, int power) {}

    public static List<Entry> of(TalentedCat cat) {
        List<Entry> out = new ArrayList<>();
        CompoundTag tag = cat.getSyncedStatuses();
        if (tag == null || tag.isEmpty()) return out;
        long now = cat.level().getGameTime();
        for (String key : tag.getAllKeys()) {
            FelineStatusType type = FelineStatusType.fromString(key);
            if (type == null) continue;
            CompoundTag e = tag.getCompound(key);
            int left = (int) (e.getLong("End") - now);
            if (left > 0) out.add(new Entry(type, left, e.getInt("Power")));
        }
        // Debuffs first, then shortest remaining.
        out.sort((a, b) -> a.type().isBeneficial() != b.type().isBeneficial()
                ? (a.type().isBeneficial() ? 1 : -1) : Integer.compare(a.remainingTicks(), b.remainingTicks()));
        return out;
    }

    static int color(FelineStatusType t) {
        return switch (t) {
            case BURN -> 0xFFF97316;
            case FREEZE -> 0xFF7DD3FC;
            case SHOCK -> 0xFFFDE047;
            case POISON -> 0xFF86EFAC;
            case BLEED -> 0xFFDC2626;
            case MARKED -> 0xFFC084FC;
            case VULNERABLE -> 0xFFA78BFA;
            case WEAKNESS -> 0xFF9CA3AF;
            case HASTE -> 0xFF5EEAD4;
            case REGENERATION -> 0xFFF9A8D4;
            case FORTIFIED -> 0xFF93C5FD;
        };
    }

    /** Statuses with their own 16px icon in textures/gui/icons; the rest get a lettered swatch. */
    private static ResourceLocation icon(FelineStatusType t) {
        return switch (t) {
            case BURN, FREEZE, SHOCK, POISON, BLEED, MARKED, FORTIFIED ->
                    new ResourceLocation("kittytalents", "textures/gui/icons/status_" + t.name().toLowerCase() + ".png");
            default -> null;
        };
    }

    /** One badge: a size x size icon with an outline in the status colour; seconds left underneath when showTime. */
    public static int drawBadge(GuiGraphics g, Font font, int x, int y, int size, Entry e, boolean showTime) {
        int c = color(e.type());
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, e.type().isBeneficial() ? 0xFF14532D : 0xFF450A0A);
        g.fill(x, y, x + size, y + size, 0xCC000000);
        ResourceLocation tex = icon(e.type());
        if (tex != null) {
            g.blit(tex, x, y, 0, 0, size, size, size, size);
        } else {
            g.fill(x + 1, y + 1, x + size - 1, y + size - 1, (c & 0x00FFFFFF) | 0x88000000);
            g.drawCenteredString(font, e.type().name().substring(0, 1), x + size / 2, y + (size - 8) / 2 + 1, KTTheme.TEXT);
        }
        // Blink during the last 3 seconds.
        if (e.remainingTicks() < 60 && (e.remainingTicks() / 5) % 2 == 0) g.fill(x, y, x + size, y + size, 0x66000000);
        if (showTime) {
            String t = (e.remainingTicks() + 19) / 20 + "s";
            g.pose().pushPose();
            g.pose().translate(x + size / 2F, y + size + 1, 0);
            g.pose().scale(0.5F, 0.5F, 1F);
            g.drawCenteredString(font, t, 0, 0, c);
            g.pose().popPose();
        }
        return size + 3;
    }
}
