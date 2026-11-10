package com.kittytalents.client.debug;

import com.kittytalents.KittyTalents;
import com.kittytalents.entity.CatBreedingManager;
import com.kittytalents.entity.CatVariant;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talents;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Developer harness: renders a fixed matrix of cat looks (skins, armor, held items, cosmetics, poses)
 * offscreen into PNG strips, then quits the game. Inactive unless the JVM is started with
 * {@code -Dkittytalents.renderMatrix=<output dir>} (see the {@code runRenderMatrix} Gradle task).
 * Each PNG holds four views of one combination: front 3/4, side, back 3/4, and from above.
 */
@Mod.EventBusSubscriber(modid = KittyTalents.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RenderMatrix {
    private static final String OUTPUT = System.getProperty("kittytalents.renderMatrix");
    private static final int CELL = 320;
    private static final int SCALE = 170;
    /** yaw offset (degrees, added to 180 = facing the camera) and camera pitch (degrees). */
    private static final float[][] VIEWS = {{-35F, -15F}, {-90F, -4F}, {-145F, -15F}, {-35F, -55F}};
    private static final int WARMUP_TICKS = 60;

    private static int ticksInWorld;
    private static boolean done;

    private RenderMatrix() {}

    private record Combo(String name, Consumer<TalentedCat> setup) {}

    private static List<Combo> combos() {
        List<Combo> list = new ArrayList<>();
        list.add(new Combo("01_default", c -> {}));
        list.add(new Combo("02_default_sit", c -> c.setInSittingPose(true)));
        list.add(new Combo("10_armor_leather", c -> armor(c, Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS)));
        list.add(new Combo("11_armor_chainmail", c -> armor(c, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS)));
        list.add(new Combo("12_armor_iron", c -> armor(c, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS)));
        list.add(new Combo("13_armor_gold", c -> armor(c, Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS)));
        list.add(new Combo("14_armor_diamond", c -> armor(c, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS)));
        list.add(new Combo("15_armor_netherite", c -> armor(c, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS)));
        list.add(new Combo("16_armor_netherite_sit", c -> {
            armor(c, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);
            c.setInSittingPose(true);
        }));
        list.add(new Combo("17_armor_turtle_helmet", c -> c.setArmorSlotItemClient(1, new ItemStack(Items.TURTLE_HELMET))));
        list.add(new Combo("20_mouth_sword", c -> c.setEquippedWeaponClient(new ItemStack(Items.DIAMOND_SWORD))));
        list.add(new Combo("21_mouth_axe", c -> c.setEquippedToolClient(new ItemStack(Items.IRON_AXE))));
        list.add(new Combo("30_cape", c -> c.setCosmeticSlotClient(0, new ItemStack(ModItems.HERO_CAPE.get()))));
        list.add(new Combo("31_cape_sit", c -> {
            c.setCosmeticSlotClient(0, new ItemStack(ModItems.HERO_CAPE.get()));
            c.setInSittingPose(true);
        }));
        list.add(new Combo("32_cosmetics_three", c -> {
            c.setCosmeticSlotClient(0, new ItemStack(ModItems.CROWN.get()));
            c.setCosmeticSlotClient(1, new ItemStack(ModItems.CAT_SCARF.get()));
            c.setCosmeticSlotClient(2, new ItemStack(ModItems.BOWTIE.get()));
        }));
        // 4.0 Phase 2b: every accessory on its own, standing and sitting.
        String[][] acc = {{"top_hat"}, {"witch_hat"}, {"crown"}, {"halo"}, {"feline_goggles"}, {"phantom_mask"},
                {"bowtie"}, {"cat_scarf"}, {"hero_cape"}, {"angel_wings"}};
        for (int i = 0; i < acc.length; i++) {
            String id = acc[i][0];
            String num = String.format("%02d", 90 + i);
            list.add(new Combo("acc_" + num + "_" + id, c -> c.setCosmeticSlotClient(0,
                    new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("kittytalents", id))))));
            list.add(new Combo("acc_" + num + "_" + id + "_sit", c -> {
                c.setCosmeticSlotClient(0, new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("kittytalents", id))));
                c.setInSittingPose(true);
            }));
        }
        list.add(new Combo("acc_99f_angel_wings_flying", c -> {
            c.setCosmeticSlotClient(0, new ItemStack(ModItems.ANGEL_WINGS.get()));
            c.setOnGround(false);
            c.tickCount = 7;
        }));
        list.add(new Combo("61_sky_wings_flying", c -> {
            c.setTalentLevel(Talents.SKY_FELINE, 1);
            c.setOnGround(false);
        }));
        list.add(new Combo("40_skin_panther", c -> c.setCatVariant(CatVariant.PANTHER)));
        list.add(new Combo("41_skin_snow_leopard", c -> c.setCatVariant(CatVariant.SNOW_LEOPARD)));
        list.add(new Combo("42_skin_void_ender", c -> c.setSkin("kittytalents:void_ender")));
        list.add(new Combo("50_nekomata_stand", c -> c.setMutation(CatBreedingManager.MutationType.NEKOMATA)));
        list.add(new Combo("51_nekomata_sit", c -> {
            c.setMutation(CatBreedingManager.MutationType.NEKOMATA);
            c.setInSittingPose(true);
        }));
        list.add(new Combo("70_anim_stalk", c -> c.setPose(net.minecraft.world.entity.Pose.CROUCHING)));
        list.add(new Combo("71_anim_pounce_launch", c -> midAnim(c, TalentedCat.ANIM_POUNCE, 4)));
        list.add(new Combo("72_anim_pounce_land", c -> midAnim(c, TalentedCat.ANIM_POUNCE, 10)));
        list.add(new Combo("73_anim_dodge", c -> midAnim(c, TalentedCat.ANIM_DODGE, 4)));
        list.add(new Combo("74_anim_cast", c -> midAnim(c, TalentedCat.ANIM_CAST, 6)));
        list.add(new Combo("75_anim_groom", c -> { c.setInSittingPose(true); midAnim(c, TalentedCat.ANIM_GROOM, 20); }));
        list.add(new Combo("76_anim_sleep", RenderMatrix::lyingDown));
        list.add(new Combo("60_wings_pack", c -> {
            c.setTalentLevel(Talents.SKY_FELINE, 1);
            c.setTalentLevel(Talents.KITTY_PACK, 1);
        }));
        return list;
    }

    /** Freeze a one-shot animation {@code ticksIn} ticks after it started. */
    private static void midAnim(TalentedCat cat, byte anim, int ticksIn) {
        cat.handleEntityEvent(anim);
        cat.tickCount += ticksIn;
    }

    /** Fully curled up (vanilla lie-down amount is private and only advanced by Cat#tick). */
    private static void lyingDown(TalentedCat cat) {
        cat.setLying(true);
        cat.setInSittingPose(true);
        for (String name : new String[]{"lieDownAmount", "lieDownAmountO", "lieDownAmountTail", "lieDownAmountOTail"}) {
            try {
                java.lang.reflect.Field f = net.minecraft.world.entity.animal.Cat.class.getDeclaredField(name);
                f.setAccessible(true);
                f.setFloat(cat, 1.0F);
            } catch (ReflectiveOperationException e) {
                KittyTalents.LOGGER.warn("[RenderMatrix] could not set {}: {}", name, e.toString());
            }
        }
    }

    private static void armor(TalentedCat cat, Item head, Item chest, Item legs, Item feet) {
        Item[] items = {head, chest, legs, feet};
        for (int i = 0; i < 4; i++) {
            ItemStack stack = new ItemStack(items[i]);
            if (stack.getItem() instanceof DyeableLeatherItem dyeable) dyeable.setColor(stack, 0x3B6FD4);
            cat.setArmorSlotItemClient(i + 1, stack);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (OUTPUT == null || done || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (++ticksInWorld < WARMUP_TICKS) return;
        done = true;
        File dir = new File(OUTPUT);
        dir.mkdirs();
        int written = 0;
        try {
            for (Combo combo : combos()) {
                TalentedCat cat = ModEntities.TALENTED_CAT.get().create(mc.level);
                if (cat == null) throw new IllegalStateException("could not create TalentedCat");
                cat.setTame(true);
                cat.setVariant(net.minecraft.core.registries.BuiltInRegistries.CAT_VARIANT
                        .getOrThrow(net.minecraft.world.entity.animal.CatVariant.TABBY));
                cat.setOnGround(true); // offscreen cats never tick; without this they render as airborne
                combo.setup().accept(cat);
                renderStrip(mc, cat, new File(dir, combo.name() + ".png"));
                written++;
            }
            KittyTalents.LOGGER.info("[RenderMatrix] wrote {} images to {}", written, dir.getAbsolutePath());
        } catch (Throwable t) {
            KittyTalents.LOGGER.error("[RenderMatrix] failed after {} images", written, t);
        }
        mc.stop();
    }

    private static void renderStrip(Minecraft mc, TalentedCat cat, File out) throws Exception {
        int width = CELL * VIEWS.length;
        TextureTarget target = new TextureTarget(width, CELL, true, Minecraft.ON_OSX);
        target.setClearColor(0.72F, 0.76F, 0.80F, 1.0F);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);

        Matrix4f oldProjection = RenderSystem.getProjectionMatrix();
        VertexSorting oldSorting = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, width, CELL, 0, 1000F, 3000F), VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        modelView.translate(0, 0, -2000F);
        RenderSystem.applyModelViewMatrix();

        GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        for (int i = 0; i < VIEWS.length; i++) {
            float yaw = 180F + VIEWS[i][0];
            float pitch = VIEWS[i][1];
            cat.yBodyRot = cat.yBodyRotO = yaw;
            cat.setYRot(yaw);
            cat.yRotO = yaw;
            cat.yHeadRot = cat.yHeadRotO = yaw;
            cat.setXRot(0);
            Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf camera = new Quaternionf().rotateX(pitch * ((float) Math.PI / 180F));
            pose.mul(camera);
            InventoryScreen.renderEntityInInventory(graphics, CELL * i + CELL / 2, (int) (CELL * 0.68F), SCALE, pose, camera, cat);
        }
        graphics.flush();

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(oldProjection, oldSorting);

        try (NativeImage image = Screenshot.takeScreenshot(target)) {
            image.writeToFile(out);
        }
        target.destroyBuffers();
        mc.getMainRenderTarget().bindWrite(true);
    }
}
