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
    /** After the tab shots: one frame with the Twine HUD and companion cards drawn over the world. */
    private static final int HUD_STEP = GUI_SCALES.length * TABS;
    /** After the HUD: the other themed screens, each at both GUI scales. */
    private static final String[] EXTRAS = { "habitat", "codex", "whistle", "pack", "bowl", "station", "oven", "blocks", "blocks_back", "items", "style_skins" };
    private static final int LAST_STEP = HUD_STEP + EXTRAS.length * GUI_SCALES.length;

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
        File dir = new File(OUTPUT);
        dir.mkdirs();
        String file;
        if (step == HUD_STEP) {
            file = "hud.png";
        } else if (step > HUD_STEP) {
            int e = step - HUD_STEP - 1;
            file = "gui" + GUI_SCALES[e / EXTRAS.length] + "_" + EXTRAS[e % EXTRAS.length] + ".png";
        } else {
            file = "gui" + GUI_SCALES[step / TABS] + "_tab" + (step % TABS) + ".png";
        }
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(new File(dir, file));
        } catch (Exception e) {
            KittyTalents.LOGGER.error("[ScreenShots] failed", e);
        }
        step++;
        if (step > LAST_STEP) {
            done = true;
            KittyTalents.LOGGER.info("[ScreenShots] wrote {} images to {}", step, dir.getAbsolutePath());
            mc.setScreen(null);
            mc.stop();
            return;
        }
        open(mc);
    }

    /** During the HUD step, draw both HUDs for the fake cat as part of the normal GUI pass. */
    @SubscribeEvent
    public static void onRenderGui(net.minecraftforge.client.event.RenderGuiEvent.Post event) {
        if (OUTPUT == null || done || step != HUD_STEP || cat == null) return;
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        com.kittytalents.client.hud.TwineHud.draw(event.getGuiGraphics(), mc, cat, w, h);
        com.kittytalents.client.hud.CompanionHud.card(event.getGuiGraphics(), mc.font, w - 130, 6, cat);
    }

    private static void open(Minecraft mc) {
        frames = 0;
        if (step > HUD_STEP) {
            int e = step - HUD_STEP - 1;
            int scale = GUI_SCALES[e / EXTRAS.length];
            if (mc.options.guiScale().get() != scale) {
                mc.options.guiScale().set(scale);
                mc.resizeDisplay();
            }
            mc.setScreen(extraScreen(mc, EXTRAS[e % EXTRAS.length]));
            if (mc.screen instanceof CatTalentScreen screen) screen.showTab(3);
            return;
        }
        if (step == HUD_STEP) {
            mc.setScreen(null);
            mc.options.guiScale().set(3);
            mc.resizeDisplay();
            // Two statuses with time left, for the badges.
            net.minecraft.nbt.CompoundTag view = new net.minecraft.nbt.CompoundTag();
            long now = mc.level.getGameTime();
            for (String[] s : new String[][] { { "BURN", "200" }, { "HASTE", "50" } }) {
                net.minecraft.nbt.CompoundTag e = new net.minecraft.nbt.CompoundTag();
                e.putLong("End", now + Integer.parseInt(s[1]));
                e.putInt("Power", 1);
                view.put(s[0], e);
            }
            cat.setSyncedStatuses(view);
            cat.setHealth(cat.getMaxHealth() * 0.6F);
            return;
        }
        int scale = GUI_SCALES[step / TABS];
        if (mc.options.guiScale().get() != scale) {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
        }
        if (!(mc.screen instanceof CatTalentScreen)) {
            // A few items in the (client-side) inventory so the Gear tab's real slots have something to show.
            var inv = mc.player.getInventory();
            if (inv.isEmpty()) {
                inv.setItem(0, new ItemStack(Items.DIAMOND_SWORD));
                inv.setItem(1, new ItemStack(Items.IRON_HELMET));
                inv.setItem(2, new ItemStack(Items.COOKED_SALMON, 12));
                inv.setItem(9, new ItemStack(ModItems.TOP_HAT.get()));
                inv.setItem(10, new ItemStack(ModItems.CAT_COLLAR.get(), 3));
                inv.setItem(20, new ItemStack(Items.IRON_PICKAXE));
            }
            CatTalentMenu menu = new CatTalentMenu(0, mc.player.getInventory(), cat);
            mc.setScreen(new CatTalentScreen(menu, mc.player.getInventory(), Component.literal("Talented Cat")));
        }
        if (mc.screen instanceof CatTalentScreen screen) {
            screen.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            screen.showTab(step % TABS);
        }
    }

    /** Block and pack screens on client-only block entities (no world access), with sample contents. */
    private static net.minecraft.client.gui.screens.Screen extraScreen(Minecraft mc, String name) {
        var inv = mc.player.getInventory();
        var pos = net.minecraft.core.BlockPos.ZERO;
        switch (name) {
            case "habitat" -> {
                var be = new com.kittytalents.block.KittyHabitatBlockEntity(pos, com.kittytalents.registry.ModBlocks.KITTY_HABITAT.get().defaultBlockState());
                be.assignWorker(cat.getUUID(), "Hermes", com.kittytalents.entity.CatVariant.TABBY,
                        com.kittytalents.entity.ProductionTier.IRON, 70, false, "Nimble", "NONE");
                net.minecraft.nbt.CompoundTag t = be.saveWithoutMetadata();
                net.minecraft.nbt.CompoundTag catTag = cat.saveWithoutId(new net.minecraft.nbt.CompoundTag());
                t.put("StoredCat", catTag);
                t.putInt("TotalCycles", 1771);
                t.putInt("Progress", 60);
                be.load(t);
                be.getInventory().setStackInSlot(4, new ItemStack(Items.IRON_INGOT, 12));
                be.getInventory().setStackInSlot(0, new ItemStack(ModItems.UPGRADE_PRODUCTIVITY_1.get()));
                var menu = new com.kittytalents.inventory.KittyHabitatMenu(0, inv, be);
                return new com.kittytalents.client.gui.KittyHabitatScreen(menu, inv, Component.literal("Habitat"));
            }
            case "codex" -> { return new com.kittytalents.client.gui.VariantChartScreen(); }
            case "blocks" -> { return new BlockGallery(false); }
            case "blocks_back" -> { return new BlockGallery(true); }
            case "items" -> { return new ItemGallery(); }
            case "style_skins" -> {
                // The skin page with a few extra "pack" skins, so the list pages.
                if (com.kittytalents.skin.CatSkins.get("shotpack:skin_0") == null) {
                    var list = new java.util.ArrayList<>(com.kittytalents.skin.CatSkins.all());
                    for (int i = 0; i < 8; i++) {
                        list.add(new com.kittytalents.skin.CatSkins.Skin(new net.minecraft.resources.ResourceLocation("shotpack", "skin_" + i),
                                "Pack Skin " + i, com.kittytalents.skin.CatSkins.FALLBACK_TEXTURE, 900 + i, java.util.List.of()));
                    }
                    com.kittytalents.skin.CatSkins.set(list);
                }
                cat.setSkin("kittytalents:tiger");
                com.kittytalents.client.gui.tab.StyleTab.debugSub = 1;
                CatTalentMenu menu = new CatTalentMenu(0, inv, cat);
                return new CatTalentScreen(menu, inv, Component.literal("Talented Cat"));
            }
            case "whistle" -> { return new com.kittytalents.client.gui.KittyWhistleScreen(); }
            case "pack" -> {
                cat.setTalentLevel(Talents.KITTY_PACK, 5);
                cat.getCatPack().setItem(2, new ItemStack(Items.COOKED_SALMON, 9));
                var menu = new com.kittytalents.inventory.CatPackMenu(0, inv, cat);
                return new com.kittytalents.client.gui.CatPackScreen(menu, inv, Component.literal("Mittens's Pack"));
            }
            case "bowl" -> {
                var be = new com.kittytalents.block.CatFoodBowlBlockEntity(pos, com.kittytalents.registry.ModBlocks.CAT_FOOD_BOWL.get().defaultBlockState());
                be.getInventory().setStackInSlot(0, new ItemStack(Items.COOKED_COD, 16));
                var data = new net.minecraft.world.inventory.SimpleContainerData(3);
                data.set(0, 16);
                data.set(1, 2);
                var menu = new com.kittytalents.inventory.CatFoodBowlMenu(0, inv, be, data);
                return new com.kittytalents.client.gui.CatFoodBowlScreen(menu, inv, Component.literal("Cat Food Bowl"));
            }
            case "station" -> {
                var be = new com.kittytalents.block.KittyWorkStationBlockEntity(pos, com.kittytalents.registry.ModBlocks.KITTY_WORK_STATION.get().defaultBlockState());
                be.getInventory().setStackInSlot(0, new ItemStack(Items.WHEAT, 32));
                var menu = new com.kittytalents.inventory.KittyWorkStationMenu(0, inv, be);
                return new com.kittytalents.client.gui.KittyWorkStationScreen(menu, inv, Component.literal("Work Station"));
            }
            default -> {
                var be = new com.kittytalents.block.TreatOvenBlockEntity(pos, com.kittytalents.registry.ModBlocks.TREAT_OVEN.get().defaultBlockState());
                be.getInventory().setStackInSlot(0, new ItemStack(ModItems.CATNIP.get(), 4));
                var menu = new com.kittytalents.inventory.TreatOvenMenu(0, inv, be);
                return new com.kittytalents.client.gui.TreatOvenScreen(menu, inv, Component.literal("Treat Oven"));
            }
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
