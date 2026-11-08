package com.kittytalents.client.debug;

import com.kittytalents.KittyTalents;
import com.kittytalents.client.gui.CatTalentScreen;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.inventory.CatTalentMenu;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talents;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.util.UUID;

/**
 * Developer harness: opens the companion screen on every tab, at two GUI scales, and saves window screenshots, then
 * quits. Inactive unless the JVM is started with {@code -Dkittytalents.screenShots=<output dir>} (Gradle task
 * {@code runScreenShots}). The cat is a client-only entity that is never added to the world; nothing is sent.
 */
@Mod.EventBusSubscriber(modid = KittyTalents.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScreenShots {
    private static final String OUTPUT = System.getProperty("kittytalents.screenShots");
    private static final int WARMUP_TICKS = 60, FRAMES_PER_SHOT = 6;
    private static final int[] GUI_SCALES = { 2, 3 };
    private static final int TABS = 7;

    private static int ticksInWorld;
    private static int step = -1, frames;
    private static TalentedCat cat;
    private static boolean done;

    private ScreenShots() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (OUTPUT == null || done || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (step < 0) {
            if (++ticksInWorld < WARMUP_TICKS) return;
            cat = makeCat(mc);
            step = 0;
            open(mc);
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (OUTPUT == null || done || step < 0 || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (++frames < FRAMES_PER_SHOT) return;
        int scale = GUI_SCALES[step / TABS], tab = step % TABS;
        File dir = new File(OUTPUT);
        dir.mkdirs();
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(new File(dir, "gui" + scale + "_tab" + tab + ".png"));
        } catch (Exception e) {
            KittyTalents.LOGGER.error("[ScreenShots] failed", e);
        }
        step++;
        if (step >= GUI_SCALES.length * TABS) {
            done = true;
            KittyTalents.LOGGER.info("[ScreenShots] wrote {} images to {}", step, dir.getAbsolutePath());
            mc.setScreen(null);
            mc.stop();
            return;
        }
        open(mc);
    }

    private static void open(Minecraft mc) {
        frames = 0;
        int scale = GUI_SCALES[step / TABS];
        if (mc.options.guiScale().get() != scale) {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
        }
        if (!(mc.screen instanceof CatTalentScreen)) {
            CatTalentMenu menu = new CatTalentMenu(0, mc.player.getInventory(), cat);
            mc.setScreen(new CatTalentScreen(menu, mc.player.getInventory(), Component.literal("Talented Cat")));
        }
        if (mc.screen instanceof CatTalentScreen screen) {
            screen.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            screen.showTab(step % TABS);
        }
    }

    private static TalentedCat makeCat(Minecraft mc) {
        TalentedCat c = ModEntities.TALENTED_CAT.get().create(mc.level);
        if (c == null) throw new IllegalStateException("could not create TalentedCat");
        c.setTame(true);
        c.setOwnerUUID(mc.player.getUUID());
        c.setVariant(net.minecraft.core.registries.BuiltInRegistries.CAT_VARIANT
                .getOrThrow(net.minecraft.world.entity.animal.CatVariant.TABBY));
        c.setCustomName(Component.literal("Mittens"));
        c.setCatLevel(14);
        c.setSkillPoints(9);
        c.setTalentLevel(Talents.POUNCE_STRIKE, 5);
        c.setTalentLevel(Talents.CAT_ARMOR, 2);
        c.setTalentLevel(Talents.KITTY_PACK, 1);
        c.addTrustedPlayer(UUID.randomUUID(), "Steve");
        c.addTrustedPlayer(UUID.randomUUID(), "Alex");
        c.setArmorSlotItemClient(1, new ItemStack(Items.IRON_HELMET));
        c.setArmorSlotItemClient(2, new ItemStack(Items.DIAMOND_CHESTPLATE));
        c.setEquippedWeaponClient(new ItemStack(Items.GOLDEN_SWORD));
        c.setCosmeticSlotClient(0, new ItemStack(ModItems.HERO_CAPE.get()));
        return c;
    }
}
