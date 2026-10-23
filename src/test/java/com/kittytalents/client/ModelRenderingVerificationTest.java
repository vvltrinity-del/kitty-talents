package com.kittytalents.client;

import com.kittytalents.client.renderer.TalentedCatArmorModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Verification test suite for 3D Models, Rendering, Armor, and Cosmetic assets (R15, R16, R17, R18).
 */
public class ModelRenderingVerificationTest {

    public static void main(String[] args) throws Exception {
        System.out.println("Running Model and Rendering Verification Suite...");

        testArmorMeshHierarchy();
        testArmorTexturesExistAndValid();
        testLeatherDyeableArmorLayers();
        testSnowLeopardTextureIntegrity();
        testPantherTextureIntegrity();
        testBackpackTextureExists();

        System.out.println("ALL MODEL, RENDERING & COSMETICS TESTS PASSED SUCCESSFULLY!");
    }

    public static void testArmorMeshHierarchy() {
        MeshDefinition mesh = TalentedCatArmorModel.createArmorMesh(new CubeDeformation(0.2F));
        PartDefinition root = mesh.getRoot();

        // 1. Head Open Cowl + Tactical Goggles
        PartDefinition head = root.getChild("head");
        assertNotNull(head, "Head part definition is missing");

        // 2. Torso Barding Harness (CHEST) & Flank Guards (LEGS)
        PartDefinition body = root.getChild("body");
        assertNotNull(body, "Body part definition is missing");
        assertNotNull(body.getChild("barding"), "Barding chest harness missing");
        assertNotNull(body.getChild("flank_guards"), "Flank guards missing");

        // 3. Front Legs: Shoulder Guards (CHEST) & Paw Cuffs (FEET)
        PartDefinition leftFront = root.getChild("left_front_leg");
        PartDefinition rightFront = root.getChild("right_front_leg");
        assertNotNull(leftFront, "Left front leg missing");
        assertNotNull(rightFront, "Right front leg missing");
        assertNotNull(leftFront.getChild("shoulder_guard"), "Left front shoulder guard missing");
        assertNotNull(rightFront.getChild("shoulder_guard"), "Right front shoulder guard missing");
        assertNotNull(leftFront.getChild("paw_cuff"), "Left front paw cuff missing");
        assertNotNull(rightFront.getChild("paw_cuff"), "Right front paw cuff missing");

        // 4. Hind Legs: Thigh Guards (LEGS) & Paw Cuffs (FEET)
        PartDefinition leftHind = root.getChild("left_hind_leg");
        PartDefinition rightHind = root.getChild("right_hind_leg");
        assertNotNull(leftHind, "Left hind leg missing");
        assertNotNull(rightHind, "Right hind leg missing");
        assertNotNull(leftHind.getChild("thigh_guard"), "Left hind thigh guard missing");
        assertNotNull(rightHind.getChild("thigh_guard"), "Right hind thigh guard missing");
        assertNotNull(leftHind.getChild("paw_cuff"), "Left hind paw cuff missing");
        assertNotNull(rightHind.getChild("paw_cuff"), "Right hind paw cuff missing");

        System.out.println("✓ Armor mesh hierarchy verified (cowl, goggles, barding, flank guards, shoulders, paw cuffs).");
    }

    public static void testArmorTexturesExistAndValid() throws IOException {
        String[] armors = {
                "cat_armor_iron.png",
                "cat_armor_golden.png",
                "cat_armor_diamond.png",
                "cat_armor_netherite.png",
                "cat_armor_chainmail.png",
                "cat_armor_turtle.png",
                "cat_armor_leather.png",
                "cat_armor_leather_overlay.png"
        };
        File armorDir = new File("src/main/resources/assets/kittytalents/textures/models/armor");
        for (String name : armors) {
            File f = new File(armorDir, name);
            assertTrue(f.exists(), "Armor texture missing: " + name);
            BufferedImage img = ImageIO.read(f);
            assertNotNull(img, "Cannot read armor texture: " + name);
            assertEqual(64, img.getWidth());
            assertEqual(32, img.getHeight());
        }
        System.out.println("✓ All 8 armor texture files verified (64x32 dimensions).");
    }

    public static void testLeatherDyeableArmorLayers() throws IOException {
        File leatherBase = new File("src/main/resources/assets/kittytalents/textures/models/armor/cat_armor_leather.png");
        File leatherOverlay = new File("src/main/resources/assets/kittytalents/textures/models/armor/cat_armor_leather_overlay.png");

        BufferedImage baseImg = ImageIO.read(leatherBase);
        BufferedImage overlayImg = ImageIO.read(leatherOverlay);

        // Check base image is grayscale (R == G == B) for pure tinting
        int sampled = 0;
        for (int y = 0; y < baseImg.getHeight(); y++) {
            for (int x = 0; x < baseImg.getWidth(); x++) {
                int argb = baseImg.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha > 50) {
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    assertEqual(r, g);
                    assertEqual(g, b);
                    sampled++;
                }
            }
        }
        assertTrue(sampled > 100, "Base leather texture must contain solid grayscale pixels");

        // Check overlay has non-transparent buckles/straps
        int overlayPixels = 0;
        for (int y = 0; y < overlayImg.getHeight(); y++) {
            for (int x = 0; x < overlayImg.getWidth(); x++) {
                int alpha = (overlayImg.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > 50) overlayPixels++;
            }
        }
        assertTrue(overlayPixels > 50, "Overlay must contain visible buckle/strap detailing");

        System.out.println("✓ Dyeable leather armor base (grayscale) and overlay (buckles/straps) verified.");
    }

    public static void testSnowLeopardTextureIntegrity() throws IOException {
        File f = new File("src/main/resources/assets/kittytalents/textures/entity/cat/skin_12.png");
        assertTrue(f.exists(), "Snow leopard texture skin_12.png missing");
        BufferedImage img = ImageIO.read(f);

        // Check eye pixels: bright blue outer iris (X=5, 9), black pupil (X=6, 8) at Y=6
        int leftEye = img.getRGB(5, 6);
        int rightEye = img.getRGB(9, 6);
        int leftPupil = img.getRGB(6, 6);
        int rightPupil = img.getRGB(8, 6);

        int leftEyeBlue = leftEye & 0xFF;
        int rightEyeBlue = rightEye & 0xFF;
        assertTrue(leftEyeBlue > 200, "Left eye must be bright blue: " + Integer.toHexString(leftEye));
        assertTrue(rightEyeBlue > 200, "Right eye must be bright blue: " + Integer.toHexString(rightEye));

        int pupilB = leftPupil & 0xFF;
        assertTrue(pupilB < 50, "Pupil must be dark/black: " + Integer.toHexString(leftPupil));
        assertEqual(leftPupil, rightPupil);

        // Check ears at Y=10 have non-zero alpha
        int earAlpha = (img.getRGB(1, 10) >> 24) & 0xFF;
        assertTrue(earAlpha > 200, "Ear UV must have solid pixels, no transparent hole");

        System.out.println("✓ Snow Leopard texture verified (bright blue symmetrical eyes, pupils, solid ears).");
    }

    public static void testPantherTextureIntegrity() throws IOException {
        File f = new File("src/main/resources/assets/kittytalents/textures/entity/cat/panther.png");
        assertTrue(f.exists(), "Panther texture missing");
        BufferedImage img = ImageIO.read(f);

        // Check blue eyes at Y=6: X=5 and X=9
        int leftEye = img.getRGB(5, 6);
        int rightEye = img.getRGB(9, 6);
        int leftEyeB = leftEye & 0xFF;
        int rightEyeB = rightEye & 0xFF;
        assertTrue(leftEyeB > 180, "Panther left eye must be blue: " + Integer.toHexString(leftEye));
        assertTrue(rightEyeB > 180, "Panther right eye must be blue: " + Integer.toHexString(rightEye));

        // Check body backside at Y=1, X=27 has solid black coat
        int coat = img.getRGB(27, 1);
        int coatAlpha = (coat >> 24) & 0xFF;
        assertTrue(coatAlpha > 200, "Panther backside must have solid coat texture");

        System.out.println("✓ Panther texture verified (black coat with bright blue eyes and complete backside).");
    }

    public static void testBackpackTextureExists() throws IOException {
        File f = new File("src/main/resources/assets/kittytalents/textures/models/accessories/cat_backpack.png");
        assertTrue(f.exists(), "Backpack texture cat_backpack.png missing");
        BufferedImage img = ImageIO.read(f);
        assertEqual(32, img.getWidth());
        assertEqual(32, img.getHeight());

        System.out.println("✓ Cat backpack texture verified (32x32 dimensions).");
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError("Assertion Failed: " + msg);
    }

    private static void assertNotNull(Object obj, String msg) {
        if (obj == null) throw new AssertionError("Assertion Failed: " + msg);
    }

    private static void assertEqual(Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError("Assertion Failed! Expected: " + expected + ", Actual: " + actual);
    }
}
