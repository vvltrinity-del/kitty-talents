package com.kittytalents.client.gui.theme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Supplier;

/** A clickable widget whose look is a lambda (equipment slots, dye swatches, ability cards). */
public class KTWidget extends AbstractWidget {
    @FunctionalInterface
    public interface Painter { void paint(GuiGraphics g, int x, int y, int w, int h, boolean hovered); }

    private final Painter painter;
    private final Runnable onPress;
    private Supplier<List<FormattedCharSequence>> tooltip;

    public KTWidget(int x, int y, int w, int h, Component narration, Painter painter, Runnable onPress) {
        super(x, y, w, h, narration);
        this.painter = painter;
        this.onPress = onPress;
    }

    /** Tooltip computed when hovered (so item tooltips stay current). */
    public KTWidget hoverTip(Supplier<List<FormattedCharSequence>> lines) { this.tooltip = lines; return this; }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        painter.paint(g, getX(), getY(), width, height, isHovered());
        if (isHovered() && tooltip != null && net.minecraft.client.Minecraft.getInstance().screen != null) {
            List<FormattedCharSequence> lines = tooltip.get();
            if (lines != null && !lines.isEmpty()) {
                net.minecraft.client.Minecraft.getInstance().screen.setTooltipForNextRenderPass(lines);
            }
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (onPress != null) onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, getMessage());
    }
}
