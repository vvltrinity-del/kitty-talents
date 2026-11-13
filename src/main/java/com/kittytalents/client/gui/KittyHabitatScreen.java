package com.kittytalents.client.gui;

import com.kittytalents.block.KittyHabitatBlockEntity;
import com.kittytalents.block.KittyHabitatBlockEntity.Status;
import com.kittytalents.client.gui.theme.KTButton;
import com.kittytalents.client.gui.theme.KTContainerScreen;
import com.kittytalents.client.gui.theme.KTTheme;
import com.kittytalents.client.gui.theme.KTText;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.inventory.KittyHabitatMenu;
import com.kittytalents.registry.ModEntities;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Kitty Production Habitat (Phase 6 theme): worker card with a portrait of the housed cat, upgrade and supply slots on
 * the left; live status, cycles, progress, Dismiss / Pause and outputs on the right. Live values come from the menu's
 * synced data, and the buttons go to the server through clickMenuButton.
 */
public class KittyHabitatScreen extends KTContainerScreen<KittyHabitatMenu> {
    private static final int LW = 112, RW = 100, PH = 102;

    private final KittyHabitatBlockEntity habitat;
    private KTButton pauseButton, dismissButton;
    private TalentedCat portraitCat;
    private CompoundTag portraitSource;

    public KittyHabitatScreen(KittyHabitatMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, KittyHabitatMenu.WIDTH, KittyHabitatMenu.HEIGHT);
        this.habitat = menu.getHabitat();
    }

    @Override
    protected Component bannerTitle() {
        return KTText.t("habitat.title", "KITTY PRODUCTION HABITAT");
    }

    private int rx() { return leftPos + 8 + LW + 4; }
    private int ry() { return topPos + 26; }

    @Override
    protected void init() {
        super.init();
        int bx = rx() + 5, bw = (RW - 12) / 2;
        dismissButton = addRenderableWidget(new KTButton(bx, ry() + 46, bw, 16, KTText.t("habitat.dismiss", "Dismiss"),
                b -> click(KittyHabitatMenu.BUTTON_DISMISS)).style(KTButton.Style.RED)
                .tip(KTText.t("habitat.dismiss.tip", "Release the housed cat beside the habitat")));
        pauseButton = addRenderableWidget(new KTButton(bx + bw + 2, ry() + 46, bw, 16, Component.empty(),
                b -> click(KittyHabitatMenu.BUTTON_PAUSE)).style(KTButton.Style.PURPLE).color(0xFFF3E8FF));
        updateButtons();
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private boolean hasWorker() {
        return habitat.getAssignedCatUUID() != null || habitat.hasStoredCat();
    }

    private void updateButtons() {
        if (pauseButton == null) return;
        pauseButton.setMessage(menu.isPaused() ? KTText.t("habitat.resume", "Resume") : KTText.t("habitat.pause", "Pause"));
        dismissButton.enabled(hasWorker());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    /** The housed cat rebuilt from the synced NBT (or the live linked worker, if loaded nearby). */
    private TalentedCat portrait() {
        if (minecraft == null || minecraft.level == null) return null;
        CompoundTag stored = habitat.getStoredCatTag();
        if (stored != null && !stored.isEmpty()) {
            if (portraitCat == null || portraitSource != stored) {
                portraitSource = stored;
                portraitCat = ModEntities.TALENTED_CAT.get().create(minecraft.level);
                if (portraitCat != null) {
                    CompoundTag copy = stored.copy();
                    copy.remove("UUID");
                    portraitCat.load(copy);
                    portraitCat.setOrderedToSit(false);
                }
            }
            return portraitCat;
        }
        if (habitat.getAssignedCatUUID() != null) {
            for (Entity e : minecraft.level.entitiesForRendering()) {
                if (e instanceof TalentedCat cat && habitat.getAssignedCatUUID().equals(cat.getUUID())) return cat;
            }
        }
        return null;
    }

    @Override
    protected void drawPanels(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Font font = this.font;
        int lx = leftPos + 8, ly = topPos + 26;

        // ---- Worker card
        KTTheme.inset(g, lx, ly, LW, PH);
        int textW = 64;
        if (hasWorker()) {
            int tierColor = habitat.getAssignedCatResourceTier().getColorValue() | 0xFF000000;
            g.drawString(font, fit(font, habitat.getAssignedCatName(), textW), lx + 6, ly + 6, KTTheme.TEXT_GOLD, true);
            g.drawString(font, fit(font, habitat.getAssignedCatVariant().getDisplayName(), textW), lx + 6, ly + 16, KTTheme.TEXT, false);
            g.drawString(font, "Lv. " + habitat.getAssignedCatLevel() + (habitat.isAssignedCatFemale() ? " ♀" : " ♂"),
                    lx + 6, ly + 26, habitat.isAssignedCatFemale() ? 0xFFF9A8D4 : 0xFF93C5FD, false);
            g.drawString(font, fit(font, habitat.getAssignedCatResourceTier().getDisplayName() + " tier", textW), lx + 6, ly + 36, tierColor, false);
        } else {
            g.drawString(font, KTText.s("habitat.no_worker", "No worker"), lx + 6, ly + 6, 0xFFF87171, true);
            List<FormattedCharSequence> hint = font.split(KTText.t("habitat.no_worker.hint", "Use a Work Clipboard on the cat, then on the habitat."), textW);
            for (int i = 0; i < Math.min(3, hint.size()); i++) g.drawString(font, hint.get(i), lx + 6, ly + 18 + i * 10, KTTheme.TEXT_DIM, false);
        }
        int px = lx + 72, py = ly + 5, pw = 34, ph = 39;
        KTTheme.portrait(g, px, py, pw, ph);
        TalentedCat cat = portrait();
        if (cat != null) {
            g.enableScissor(px + 4, py + 4, px + pw - 4, py + ph - 4);
            int footX = px + pw / 2, footY = py + ph - 7;
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, footX, footY, 30, (float) footX - mouseX, (float) (footY - 20) - mouseY, cat);
            g.disableScissor();
        } else {
            g.drawCenteredString(font, "?", px + pw / 2, py + ph / 2 - 4, KTTheme.TEXT_DIM);
        }
        KTTheme.header(g, font, lx + 6, ly + 47, LW - 12, KTText.t("habitat.upgrades", "Upgrades"));
        g.drawString(font, KTText.s("habitat.supplies", "Supplies"), lx + 48, ly + 84, KTTheme.TEXT_DIM, false);

        // ---- Status card
        int rx = rx(), ry = ry();
        KTTheme.inset(g, rx, ry, RW, PH);
        Status st = menu.getStatus();
        int stColor = switch (st) {
            case WORKING -> 0xFF6EE7A0;
            case PAUSED, PAUSED_REDSTONE -> 0xFFFCD34D;
            default -> 0xFFF87171;
        };
        String label = KTText.s("habitat.status", "Status:") + " ";
        g.drawString(font, label, rx + 6, ry + 6, KTTheme.TEXT_DIM, false);
        List<FormattedCharSequence> stLines = font.split(Component.literal(
                KTText.s("habitat.status." + st.name().toLowerCase(), KittyHabitatBlockEntity.statusText(st, habitat.getAssignedCatResourceTier()))),
                RW - 12 - font.width(label));
        for (int i = 0; i < Math.min(2, stLines.size()); i++) {
            g.drawString(font, stLines.get(i), rx + 6 + font.width(label), ry + 6 + i * 10, stColor, false);
        }
        g.drawString(font, KTText.s("habitat.cycles", "Cycles: %s", menu.getTotalCycles()), rx + 6, ry + 27, KTTheme.TEXT_DIM, false);
        float frac = menu.getProgress() / (float) menu.getMaxProgress();
        KTTheme.bar(g, rx + 6, ry + 39, RW - 28, 5, frac, st == Status.WORKING ? 0xFF22C55E : 0xFF6B6760);
        ItemStack product = hasWorker() ? habitat.getAssignedCatResourceTier().getProductionResult() : ItemStack.EMPTY;
        if (!product.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(rx + RW - 19, ry + 35, 0);
            g.pose().scale(0.75F, 0.75F, 1F);
            g.renderItem(product, 0, 0);
            g.pose().popPose();
        }
        // 5.0: trait, bond and neighbour modifiers. The combined speed sits after the cycle count; hover it (or the
        // worker card) for the list ("Shy: alone, +20%").
        List<String> mods = habitat.getModifierLines();
        if (!mods.isEmpty()) {
            int pct = Math.round((habitat.getSyncedSpeed() - 1.0F) * 100);
            String tag = (pct >= 0 ? "+" : "") + pct + "%";
            String cycles = KTText.s("habitat.cycles", "Cycles: %s", menu.getTotalCycles());
            g.drawString(font, tag, rx + 6 + font.width(cycles) + 6, ry + 27, pct >= 0 ? 0xFF86EFAC : 0xFFF87171, false);
            boolean overCycles = mouseX >= rx + 6 && mouseX < rx + RW - 6 && mouseY >= ry + 25 && mouseY < ry + 36;
            boolean overWorker = mouseX >= lx && mouseX < lx + LW && mouseY >= ly && mouseY < ly + 45;
            if (overCycles || overWorker) {
                java.util.List<Component> tip = new java.util.ArrayList<>();
                tip.add(KTText.t("habitat.mods", "Personality and bond").copy().withStyle(net.minecraft.ChatFormatting.GOLD));
                for (String m : mods) tip.add(Component.literal(m));
                g.renderComponentTooltip(font, tip, mouseX, mouseY);
            }
        }
        KTTheme.header(g, font, rx + 6, ry + 66, RW - 12, KTText.t("habitat.outputs", "Outputs"));
    }

    @Override
    protected void drawOverlay(GuiGraphics g, int mouseX, int mouseY) {
        // Faint placeholder in the empty food slot.
        var food = menu.slots.get(8);
        if (!food.hasItem()) {
            int fx = leftPos + food.x, fy = topPos + food.y;
            g.renderItem(new ItemStack(Items.COOKED_COD), fx, fy);
            g.fill(fx, fy, fx + 16, fy + 16, 0xB02A2A2E);
        }
    }

    private static String fit(Font font, String s, int w) {
        return font.width(s) <= w ? s : font.plainSubstrByWidth(s, w - font.width("…")) + "…";
    }
}
