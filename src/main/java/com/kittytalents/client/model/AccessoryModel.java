package com.kittytalents.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kittytalents.KittyTalents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import javax.annotation.Nullable;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A 3D accessory (4.0 Phase 2b) loaded from assets/kittytalents/models/accessories/&lt;id&gt;.json, which
 * tools/asset_gen/accessories_3d.py writes together with the matching texture. Resource packs can override both.
 * Parts keep their authored pose so animation can reset to it every frame ({@link #reset}).
 */
public final class AccessoryModel {
    /** Where on the cat the accessory sits. HEAD is the head bone itself (goggles, masks). */
    public enum Mount { HEAD_TOP, HEAD, NECK, BACK }

    public final String id;
    public final Mount mount;
    public final ResourceLocation texture;
    public final boolean emissive;
    public final ModelPart root;
    /** Every named part, for animation, with its authored pose. */
    public final Map<String, ModelPart> parts = new HashMap<>();
    private final Map<ModelPart, PartPose> rest = new HashMap<>();
    private final Map<ModelPart, Integer> depth = new HashMap<>();
    /** Rest-pose bounds in blocks (model space, +Y down): minX, minY, minZ, maxX, maxY, maxZ. Used to centre item icons. */
    public final float[] bounds = new float[6];

    private AccessoryModel(String id, Mount mount, ResourceLocation texture, boolean emissive, ModelPart root) {
        this.id = id;
        this.mount = mount;
        this.texture = texture;
        this.emissive = emissive;
        this.root = root;
    }

    /** Loads an accessory, or null (with a log line) if its file is missing or broken. */
    @Nullable
    public static AccessoryModel load(String id) {
        ResourceLocation file = new ResourceLocation(KittyTalents.MODID, "models/accessories/" + id + ".json");
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(file);
        if (res.isEmpty()) {
            KittyTalents.LOGGER.warn("[Accessories] missing {}", file);
            return null;
        }
        try (Reader reader = res.get().openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray size = json.getAsJsonArray("texture_size");
            MeshDefinition mesh = new MeshDefinition();
            List<String> names = new ArrayList<>();
            for (JsonElement p : json.getAsJsonArray("parts")) addPart(mesh.getRoot(), p.getAsJsonObject(), names);
            ModelPart baked = mesh.getRoot().bake(size.get(0).getAsInt(), size.get(1).getAsInt());
            AccessoryModel model = new AccessoryModel(id, Mount.valueOf(json.get("anchor").getAsString()),
                    new ResourceLocation(json.get("texture").getAsString()),
                    json.has("emissive") && json.get("emissive").getAsBoolean(), baked);
            model.index(baked, json.getAsJsonArray("parts"), 1);
            model.measure();
            return model;
        } catch (Exception e) {
            KittyTalents.LOGGER.error("[Accessories] could not load {}", file, e);
            return null;
        }
    }

    private static void addPart(PartDefinition parent, JsonObject p, List<String> names) {
        CubeListBuilder cubes = CubeListBuilder.create();
        for (JsonElement be : p.getAsJsonArray("boxes")) {
            JsonObject b = be.getAsJsonObject();
            JsonArray uv = b.getAsJsonArray("uv"), from = b.getAsJsonArray("from"), sz = b.getAsJsonArray("size");
            cubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt())
                    .addBox(from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
                            sz.get(0).getAsFloat(), sz.get(1).getAsFloat(), sz.get(2).getAsFloat(),
                            new CubeDeformation(b.get("inflate").getAsFloat()));
        }
        JsonArray pivot = p.getAsJsonArray("pivot"), rot = p.getAsJsonArray("rot");
        float rad = (float) (Math.PI / 180.0);
        String name = p.get("name").getAsString();
        names.add(name);
        PartDefinition def = parent.addOrReplaceChild(name, cubes, PartPose.offsetAndRotation(
                pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat(),
                rot.get(0).getAsFloat() * rad, rot.get(1).getAsFloat() * rad, rot.get(2).getAsFloat() * rad));
        for (JsonElement c : p.getAsJsonArray("children")) addPart(def, c.getAsJsonObject(), names);
    }

    /** Walks the JSON part tree alongside the baked parts, recording names, authored poses and depth. */
    private void index(ModelPart parent, JsonArray jsonParts, int d) {
        for (JsonElement e : jsonParts) {
            JsonObject p = e.getAsJsonObject();
            ModelPart child = parent.getChild(p.get("name").getAsString());
            parts.put(p.get("name").getAsString(), child);
            rest.put(child, child.storePose());
            depth.put(child, d);
            index(child, p.getAsJsonArray("children"), d + 1);
        }
    }

    private void measure() {
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        org.joml.Vector4f v = new org.joml.Vector4f();
        root.visit(new com.mojang.blaze3d.vertex.PoseStack(), (pose, path, index, cube) -> {
            for (int i = 0; i < 8; i++) {
                v.set(((i & 1) == 0 ? cube.minX : cube.maxX) / 16F, ((i & 2) == 0 ? cube.minY : cube.maxY) / 16F,
                        ((i & 4) == 0 ? cube.minZ : cube.maxZ) / 16F, 1F);
                pose.pose().transform(v);
                b[0] = Math.min(b[0], v.x); b[1] = Math.min(b[1], v.y); b[2] = Math.min(b[2], v.z);
                b[3] = Math.max(b[3], v.x); b[4] = Math.max(b[4], v.y); b[5] = Math.max(b[5], v.z);
            }
        });
        if (b[0] > b[3]) java.util.Arrays.fill(b, 0F);
        System.arraycopy(b, 0, bounds, 0, 6);
    }

    /** Puts every part back to its authored pose. */
    public void reset() {
        rest.forEach(ModelPart::loadPose);
    }

    /** How deep a part sits in the hierarchy (1 = top level), used to grow cloth sway down a chain. */
    public int depthOf(ModelPart part) {
        return depth.getOrDefault(part, 1);
    }

    /**
     * Animates and draws the accessory at the current pose (the caller has already moved to the mount point).
     * Cloth ("sway*") lifts behind a walking cat and drifts gently; "bob*" parts float and turn slowly;
     * wings ("flap_l"/"flap_r") rest folded and beat while the cat is in the air.
     */
    public void render(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light,
                       net.minecraft.world.entity.LivingEntity entity, float limbSwingAmount, float age) {
        reset();
        float walk = Math.min(1.0F, limbSwingAmount);
        boolean flying = !entity.onGround() && !entity.isInWater() && !entity.isPassenger();
        for (Map.Entry<String, ModelPart> e : parts.entrySet()) {
            String name = e.getKey();
            ModelPart part = e.getValue();
            int d = depthOf(part);
            if (name.startsWith("sway")) {
                part.xRot += walk * 0.22F + net.minecraft.util.Mth.sin(age * 0.09F + d) * 0.05F;
                part.zRot += net.minecraft.util.Mth.sin(age * 0.07F + d * 0.7F) * 0.025F * d;
            } else if (name.startsWith("bob")) {
                part.y += net.minecraft.util.Mth.sin(age * 0.1F) * 0.35F;
                part.yRot += age * 0.02F;
            } else if (name.startsWith("flap_")) {
                float side = name.endsWith("_l") ? -1.0F : 1.0F;
                // Rest pose (from the file) is folded back along the spine; in the air the wing swings out sideways
                // (z) and beats up and down around the spine (y).
                if (flying) {
                    part.zRot -= side * 1.0F;
                    part.yRot += side * net.minecraft.util.Mth.sin(age * 0.6F) * 0.6F;
                } else {
                    part.yRot += side * net.minecraft.util.Mth.sin(age * 0.06F) * 0.04F;
                }
            }
        }
        net.minecraft.client.renderer.RenderType type = emissive
                ? net.minecraft.client.renderer.RenderType.entityTranslucentEmissive(texture)
                : net.minecraft.client.renderer.RenderType.entityCutoutNoCull(texture);
        root.render(pose, buffers.getBuffer(type), emissive ? 0xF000F0 : light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
