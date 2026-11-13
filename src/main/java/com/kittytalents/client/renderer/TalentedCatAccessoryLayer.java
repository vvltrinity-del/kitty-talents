package com.kittytalents.client.renderer;

import com.kittytalents.client.model.AccessoryModel;
import com.kittytalents.client.model.KTCatModel;
import com.kittytalents.client.model.KTCatModel.Anchor;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.item.CatAccessoryItem;
import com.kittytalents.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Draws the functional accessory and up to three cosmetics as 3D models (4.0 Phase 2b). Each Kitty Talents accessory
 * has a model file under assets/kittytalents/models/accessories (see {@link AccessoryModel}); angel wings are drawn by
 * TalentedCatWingLayer. Items without a model (other mods' cosmetics) fall back to a small item render.
 */
public class TalentedCatAccessoryLayer extends RenderLayer<TalentedCat, KTCatModel> {
    /** Loaded on first use; this layer is rebuilt on every resource reload, so packs can replace models. */
    private final Map<String, Optional<AccessoryModel>> models = new HashMap<>();

    public TalentedCatAccessoryLayer(RenderLayerParent<TalentedCat, KTCatModel> parent) {
        super(parent);
    }

    private Optional<AccessoryModel> model(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || !key.getNamespace().equals("kittytalents")) return Optional.empty();
        return models.computeIfAbsent(key.getPath(), id -> Optional.ofNullable(
                Minecraft.getInstance().getResourceManager().getResource(
                        new ResourceLocation("kittytalents", "models/accessories/" + id + ".json")).isPresent()
                        ? AccessoryModel.load(id) : null));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TalentedCat cat, float limbSwing,
                       float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (cat.isInvisible()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() == cat && mc.options.getCameraType().isFirstPerson()) return;

        ItemStack accessory = cat.getEquippedAccessory();
        if (!accessory.isEmpty()) renderOne(poseStack, buffer, packedLight, cat, accessory, limbSwingAmount, ageInTicks);
        // 5.0 Captain: wears a crown unless it already has something on its head.
        if (cat.isCaptain() && !wearsHat(cat)) renderOne(poseStack, buffer, packedLight, cat, CAPTAIN_CROWN.get(), limbSwingAmount, ageInTicks);
        if (cat.isCosmeticsVisible()) {
            for (int i = 0; i < 3; i++) {
                ItemStack cosmetic = cat.getCosmeticSlot(i);
                if (!cosmetic.isEmpty()) renderOne(poseStack, buffer, packedLight, cat, cosmetic, limbSwingAmount, ageInTicks);
            }
        }
    }

    private static final java.util.function.Supplier<ItemStack> CAPTAIN_CROWN =
            com.google.common.base.Suppliers.memoize(() -> new ItemStack(ModItems.CROWN.get()));

    private static boolean wearsHat(TalentedCat cat) {
        if (isHat(cat.getEquippedAccessory())) return true;
        if (!cat.isCosmeticsVisible()) return false;
        for (int i = 0; i < 3; i++) if (isHat(cat.getCosmeticSlot(i))) return true;
        return false;
    }

    private static boolean isHat(ItemStack s) {
        return s.getItem() instanceof CatAccessoryItem acc && acc.getAccessoryType() == CatAccessoryItem.AccessoryType.HAT;
    }

    private void renderOne(PoseStack pose, MultiBufferSource buffer, int light, TalentedCat cat, ItemStack stack,
                           float limbSwingAmount, float age) {
        if (stack.is(ModItems.ANGEL_WINGS.get())) return; // TalentedCatWingLayer
        KTCatModel cat3d = this.getParentModel();
        pose.pushPose();
        Optional<AccessoryModel> model = model(stack);
        if (model.isPresent()) {
            AccessoryModel m = model.get();
            switch (m.mount) {
                case HEAD_TOP -> cat3d.translateToAnchor(Anchor.HEAD_TOP, pose);
                case HEAD -> cat3d.head.translateAndRotate(pose);
                case NECK -> cat3d.translateToAnchor(Anchor.NECK, pose);
                case BACK -> cat3d.translateToAnchor(Anchor.BACK, pose);
            }
            m.render(pose, buffer, light, cat, limbSwingAmount, age);
        } else {
            renderItemFallback(pose, buffer, light, cat, stack, cat3d);
        }
        pose.popPose();
    }

    /** Cosmetics from other mods: a small item render at a sensible spot for its type. */
    private void renderItemFallback(PoseStack pose, MultiBufferSource buffer, int light, TalentedCat cat, ItemStack stack, KTCatModel cat3d) {
        CatAccessoryItem.AccessoryType type = stack.getItem() instanceof CatAccessoryItem acc ? acc.getAccessoryType() : CatAccessoryItem.AccessoryType.HAT;
        switch (type) {
            case GLASSES -> { cat3d.head.translateAndRotate(pose); pose.translate(0.0D, -0.05D, -0.28D); pose.scale(0.4F, 0.4F, 0.4F); }
            case BOW -> { cat3d.translateToAnchor(Anchor.NECK, pose); pose.translate(0.0D, -0.03D, -0.06D); pose.scale(0.35F, 0.35F, 0.35F); }
            case CAPE -> { cat3d.translateToAnchor(Anchor.BACK, pose); pose.translate(0.0D, 0.3D, 0.02D); pose.scale(0.6F, 0.6F, 0.6F); }
            default -> { cat3d.translateToAnchor(Anchor.HEAD_TOP, pose); pose.translate(0.0D, -0.12D, 0.0D); pose.scale(0.5F, 0.5F, 0.5F); }
        }
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, pose, buffer, cat.level(), cat.getId());
    }
}
