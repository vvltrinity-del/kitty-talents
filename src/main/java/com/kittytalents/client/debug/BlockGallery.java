package com.kittytalents.client.debug;

import com.kittytalents.block.CatFoodBowlBlock;
import com.kittytalents.block.CatTreeBlock;
import com.kittytalents.registry.ModBlocks;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;

/** Dev-only screen (ScreenShots harness): every Kitty Talents block model from the front and the back. */
public class BlockGallery extends Screen {
    private record Entry(String name, BlockState lower, BlockState upper) {}

    private final boolean back;

    public BlockGallery(boolean back) {
        super(Component.literal("Block gallery"));
        this.back = back;
    }

    private static List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        BlockState bowl = ModBlocks.CAT_FOOD_BOWL.get().defaultBlockState();
        list.add(new Entry("bowl (empty)", bowl, null));
        list.add(new Entry("bowl (full)", bowl.setValue(CatFoodBowlBlock.FOOD, 3), null));
        list.add(new Entry("bed", ModBlocks.CAT_BED.get().defaultBlockState(), null));
        list.add(new Entry("scratcher", ModBlocks.CAT_SCRATCHER.get().defaultBlockState(), null));
        list.add(new Entry("bath", ModBlocks.CAT_BATH.get().defaultBlockState(), null));
        list.add(new Entry("rice mill", ModBlocks.CAT_RICE_MILL.get().defaultBlockState(), null));
        list.add(new Entry("habitat", ModBlocks.KITTY_HABITAT.get().defaultBlockState(), null));
        list.add(new Entry("feeding station", ModBlocks.FEEDING_STATION.get().defaultBlockState(), null));
        list.add(new Entry("incubator", ModBlocks.KITTEN_INCUBATOR.get().defaultBlockState(), null));
        list.add(new Entry("work station", ModBlocks.KITTY_WORK_STATION.get().defaultBlockState(), null));
        list.add(new Entry("treat oven", ModBlocks.TREAT_OVEN.get().defaultBlockState(), null));
        BlockState tree = ModBlocks.CAT_TREE.get().defaultBlockState();
        list.add(new Entry("cat tree", tree, tree.setValue(CatTreeBlock.HALF, DoubleBlockHalf.UPPER)));
        return list;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF8FA3B8);
        List<Entry> list = entries();
        int cols = 6, cellW = width / cols, cellH = height / 2;
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = g.bufferSource();
        Lighting.setupFor3DItems();
        for (int i = 0; i < list.size(); i++) {
            Entry e = list.get(i);
            int cx = (i % cols) * cellW + cellW / 2, cy = (i / cols) * cellH + cellH / 2 + 8;
            float scale = Math.min(cellW, cellH) * (e.upper() == null ? 0.42F : 0.27F);
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(cx, cy, 150);
            pose.scale(scale, -scale, scale);
            pose.mulPose(Axis.XP.rotationDegrees(28));
            pose.mulPose(Axis.YP.rotationDegrees(back ? 25 : 205));
            pose.translate(-0.5, e.upper() == null ? -0.35 : -0.9, -0.5);
            mc.getBlockRenderer().renderSingleBlock(e.lower().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH),
                    pose, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY);
            if (e.upper() != null) {
                pose.translate(0, 1, 0);
                mc.getBlockRenderer().renderSingleBlock(e.upper(), pose, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY);
            }
            pose.popPose();
            buffers.endBatch();
            g.drawCenteredString(font, e.name() + (back ? " (back)" : ""), cx, (i / cols) * cellH + 6, 0xFFFFFFFF);
        }
        Lighting.setupForFlatItems();
    }
}
