package com.kittytalents.client.gui.theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Themed button: stone by default, gold/amethyst/red-brick materials for emphasis, optional icon and selection. */
public class KTButton extends Button {
    public enum Style { STONE, GOLD, PURPLE, RED, CHIP }

    private Style style = Style.STONE;
    private ItemStack icon = ItemStack.EMPTY;
    private ResourceLocation iconTexture;
    private boolean selected;
    private int textColor = -1;
    private boolean leftAligned;
    private int textTop = -1;

    public KTButton(int x, int y, int w, int h, Component label, OnPress onPress) {
        super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
    }

    public KTButton style(Style s) { this.style = s; return this; }
    public KTButton icon(ItemStack stack) { this.icon = stack == null ? ItemStack.EMPTY : stack; return this; }
    public KTButton icon(ResourceLocation texture16) { this.iconTexture = texture16; return this; }
    public KTButton selected(boolean value) { this.selected = value; return this; }
    public KTButton color(int argb) { this.textColor = argb; return this; }
    public KTButton left() { this.leftAligned = true; return this; }
    /** Draw the label at this offset from the top instead of centred (room for a second line drawn by the owner). */
    public KTButton textTop(int dy) { this.textTop = dy; this.leftAligned = true; return this; }
    public KTButton tip(Component text) { this.setTooltip(Tooltip.create(text)); return this; }
    public KTButton enabled(boolean value) { this.active = value; return this; }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = width, h = height;
        boolean hover = isHoveredOrFocused() && active;
        ResourceLocation tex = switch (style) {
            case GOLD -> KTTheme.GOLD;
            case PURPLE -> KTTheme.AMETHYST;
            case RED -> KTTheme.RED;
            default -> KTTheme.STONE;
        };
        int shade = switch (style) {
            case GOLD -> 0x55000000;
            case PURPLE -> 0x66000000;
            case RED -> 0x33000000;
            case CHIP -> selected ? 0x55000000 : 0x99000000;
            default -> 0x55000000;
        };
        if (!active) shade = 0xAA000000;
        else if (hover) shade = Math.max(0, ((shade >>> 24) - 0x30)) << 24;

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, KTTheme.OUTLINE);
        KTTheme.tile(g, tex, x, y, w, h);
        g.fill(x, y, x + w, y + h, shade);
        g.fill(x, y, x + w, y + 1, 0x55FFFFFF);
        g.fill(x, y, x + 1, y + h, 0x33FFFFFF);
        g.fill(x, y + h - 1, x + w, y + h, 0x77000000);
        g.fill(x + w - 1, y, x + w, y + h, 0x77000000);
        if (selected || (hover && style != Style.STONE)) KTTheme.outline(g, x - 1, y - 1, w + 2, h + 2, KTTheme.GOLD_EDGE);
        else if (hover) KTTheme.outline(g, x - 1, y - 1, w + 2, h + 2, 0xFFD8D4CC);

        int pad = style == Style.CHIP ? 2 : 4; // chips are narrow filter tabs: tighter padding fits "Defense"
        int textLeft = x + pad;
        if (!icon.isEmpty() || iconTexture != null) {
            int size = h >= 18 ? 16 : 12;
            int iy = textTop >= 0 ? y + 3 : y + (h - size) / 2;
            if (iconTexture != null) {
                g.blit(iconTexture, x + 3, iy, 0, 0, size, size, size, size);
            } else if (size == 16) {
                g.renderItem(icon, x + 3, iy);
            } else {
                g.pose().pushPose();
                g.pose().translate(x + 3, iy, 0);
                g.pose().scale(0.75F, 0.75F, 1.0F);
                g.renderItem(icon, 0, 0);
                g.pose().popPose();
            }
            textLeft = x + 3 + size + 3;
        }

        Font font = Minecraft.getInstance().font;
        int color = textColor != -1 ? textColor : (style == Style.GOLD ? KTTheme.TEXT_DARK : (selected ? KTTheme.TEXT_GOLD : KTTheme.TEXT));
        if (!active) color = KTTheme.dim(color);
        int avail = x + w - (pad - 1) - textLeft;
        Component msg = getMessage();
        String text = msg.getString();
        if (font.width(text) > avail) text = font.plainSubstrByWidth(text, Math.max(0, avail - font.width("…"))) + "…";
        int tx = leftAligned || textLeft != x + pad ? textLeft : x + (w - font.width(text)) / 2;
        if (!leftAligned && textLeft != x + pad) tx = textLeft + Math.max(0, (avail - font.width(text)) / 2);
        boolean shadow = style != Style.GOLD;
        g.drawString(font, Component.literal(text).withStyle(msg.getStyle()), tx, textTop >= 0 ? y + textTop : y + (h - 8) / 2, color, shadow);
    }
}
