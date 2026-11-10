package com.kittytalents.client.renderer;

import com.kittytalents.client.model.AccessoryModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Draws an accessory item as its real 3D model (4.0 Phase 2b) in inventories, hands, item frames and on the ground.
 * The item model is "builtin/entity" with display transforms; this renderer only centres and sizes the model.
 */
public class AccessoryItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static AccessoryItemRenderer instance;
    private final Map<String, Optional<AccessoryModel>> models = new HashMap<>();

    private AccessoryItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static AccessoryItemRenderer get() {
        if (instance == null) instance = new AccessoryItemRenderer();
        return instance;
    }

    @Override
    public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
        models.clear();
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) return;
        Optional<AccessoryModel> model = models.computeIfAbsent(key.getPath(), id -> Optional.ofNullable(AccessoryModel.load(id)));
        if (model.isEmpty()) return;
        AccessoryModel m = model.get();
        pose.pushPose();
        // Entity models are authored upside down (+Y is down): flip into item space, turn neck/back pieces to face
        // the viewer, then centre the model's own bounds in the 1x1x1 item box and size its longest side to fit.
        float[] b = m.bounds;
        float dx = b[3] - b[0], dy = b[4] - b[1], dz = b[5] - b[2];
        float s = Math.min(1.8F, 0.85F / Math.max(0.05F, Math.max(dx, Math.max(dy, dz))));
        pose.translate(0.5D, 0.5D, 0.5D);
        pose.scale(s, -s, -s);
        if (m.mount == AccessoryModel.Mount.NECK || m.mount == AccessoryModel.Mount.BACK) pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.translate(-(b[0] + b[3]) / 2F, -(b[1] + b[4]) / 2F, -(b[2] + b[5]) / 2F);
        m.reset();
        RenderType type = m.emissive ? RenderType.entityTranslucentEmissive(m.texture) : RenderType.entityCutoutNoCull(m.texture);
        m.root.render(pose, buffers.getBuffer(type), m.emissive ? 0xF000F0 : light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }
}
