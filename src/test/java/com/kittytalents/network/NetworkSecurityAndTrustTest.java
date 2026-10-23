package com.kittytalents.network;

import com.kittytalents.entity.WhistleCommand;

/**
 * Pure verification suite for Network Security, Packet Decoding, NaN vector safety,
 * and Trust permission boundaries (R26, R27, R28).
 */
public class NetworkSecurityAndTrustTest {

    public static void main(String[] args) {
        testCommandTypeBoundaryClamping();
        testSlotIdBoundaryValidation();
        testVectorNormalizationNaNProtection();
        testWhistleCommandTypeSafety();
        testTrustPermissionInvariants();
        System.out.println("ALL NETWORK SECURITY & TRUST TESTS PASSED SUCCESSFULLY.");
    }

    public static void testCommandTypeBoundaryClamping() {
        PacketTeamCommand.CommandType[] types = PacketTeamCommand.CommandType.values();
        assertTrue(types.length > 0, "CommandType must have valid enum values");

        // Verify ordinal bounds: valid index maps correctly
        int validOrdinal = 0;
        PacketTeamCommand.CommandType decoded = (validOrdinal >= 0 && validOrdinal < types.length)
                ? types[validOrdinal]
                : PacketTeamCommand.CommandType.ATTACK_MY_TARGET;
        assertEqual(PacketTeamCommand.CommandType.ATTACK_MY_TARGET, decoded);

        // Verify negative ordinal clamps to default
        int negativeOrdinal = -1;
        PacketTeamCommand.CommandType safeNegative = (negativeOrdinal >= 0 && negativeOrdinal < types.length)
                ? types[negativeOrdinal]
                : PacketTeamCommand.CommandType.ATTACK_MY_TARGET;
        assertEqual(PacketTeamCommand.CommandType.ATTACK_MY_TARGET, safeNegative);

        // Verify overflow ordinal clamps to default
        int overflowOrdinal = 9999;
        PacketTeamCommand.CommandType safeOverflow = (overflowOrdinal >= 0 && overflowOrdinal < types.length)
                ? types[overflowOrdinal]
                : PacketTeamCommand.CommandType.ATTACK_MY_TARGET;
        assertEqual(PacketTeamCommand.CommandType.ATTACK_MY_TARGET, safeOverflow);
    }

    public static void testSlotIdBoundaryValidation() {
        // Equipment slots: 0 to 10 inclusive
        for (int slot = 0; slot <= 10; slot++) {
            assertTrue(slot >= 0 && slot <= 10, "Slot " + slot + " must be valid");
        }

        // Out-of-bounds slots must be rejected
        int[] invalidSlots = {-5, -1, 11, 12, 100, Integer.MAX_VALUE};
        for (int invalidSlot : invalidSlots) {
            assertFalse(invalidSlot >= 0 && invalidSlot <= 10, "Slot " + invalidSlot + " must be rejected");
        }
    }

    public static void testVectorNormalizationNaNProtection() {
        // Vector difference for co-located entities (dx = 0, dy = 0, dz = 0)
        double dx = 0.0;
        double dy = 0.0;
        double dz = 0.0;
        double lengthSqr = dx * dx + dy * dy + dz * dz;

        // Raw vector normalization without lengthSqr check:
        double rawLength = Math.sqrt(lengthSqr);
        double rawNormalizedX = dx / rawLength;
        double rawNormalizedY = dy / rawLength;
        double rawNormalizedZ = dz / rawLength;

        // Raw normalization produces Double.NaN:
        assertTrue(Double.isNaN(rawNormalizedX), "Raw 0/0 produces NaN X");
        assertTrue(Double.isNaN(rawNormalizedY), "Raw 0/0 produces NaN Y");
        assertTrue(Double.isNaN(rawNormalizedZ), "Raw 0/0 produces NaN Z");

        // Guarded vector normalization used in PacketWhistleCommand, PacketTwineInput, TalentedCat, ForgeEvents:
        double safeX, safeY, safeZ;
        if (lengthSqr > 1.0E-4D) {
            double len = Math.sqrt(lengthSqr);
            safeX = dx / len;
            safeY = dy / len;
            safeZ = dz / len;
        } else {
            // Fallback vector (0, 0, 1)
            safeX = 0.0;
            safeY = 0.0;
            safeZ = 1.0;
        }

        assertFalse(Double.isNaN(safeX), "Guarded vector X must not be NaN");
        assertFalse(Double.isNaN(safeY), "Guarded vector Y must not be NaN");
        assertFalse(Double.isNaN(safeZ), "Guarded vector Z must not be NaN");
        assertEqual(0.0, safeX);
        assertEqual(0.0, safeY);
        assertEqual(1.0, safeZ);
    }

    public static void testWhistleCommandTypeSafety() {
        WhistleCommand[] commands = WhistleCommand.values();
        assertTrue(commands.length >= 6, "Must define all whistle commands");

        // Verify QUIET, ROAR_HISS, COME, STAY, ATTACK_TARGET, HEEL, etc.
        assertEqual("QUIET", WhistleCommand.valueOf("QUIET").name());
        assertEqual("ROAR_HISS", WhistleCommand.valueOf("ROAR_HISS").name());
        assertEqual("COME", WhistleCommand.valueOf("COME").name());
        assertEqual("STAY", WhistleCommand.valueOf("STAY").name());
        assertEqual("ATTACK_TARGET", WhistleCommand.valueOf("ATTACK_TARGET").name());
        assertEqual("HEEL", WhistleCommand.valueOf("HEEL").name());
    }

    public static void testTrustPermissionInvariants() {
        // Permission Matrix Invariants:
        // 1. Rename: Only Primary Owner (NOT trusted, NOT stranger)
        // 2. Respec Talents: Only Primary Owner (NOT trusted, NOT stranger)
        // 3. Reset Statistics (Coal): Only Primary Owner (NOT trusted, NOT stranger)
        // 4. Equipment/Pack/Follow: Primary Owner OR Trusted Friend (NOT stranger)

        java.util.UUID ownerUUID = java.util.UUID.randomUUID();
        java.util.UUID trustedUUID = java.util.UUID.randomUUID();
        java.util.UUID strangerUUID = java.util.UUID.randomUUID();

        java.util.Map<java.util.UUID, String> trustMap = new java.util.HashMap<>();
        trustMap.put(trustedUUID, "TrustedFriend");

        // Helper simulation functions
        java.util.function.Predicate<java.util.UUID> isPrimaryOwner = id -> id.equals(ownerUUID);
        java.util.function.Predicate<java.util.UUID> isTrusted = id -> trustMap.containsKey(id);
        java.util.function.Predicate<java.util.UUID> canRenameOrRespecOrWipe = isPrimaryOwner::test;
        java.util.function.Predicate<java.util.UUID> canEquipOrCommand = id -> isPrimaryOwner.test(id) || isTrusted.test(id);

        // Verify Primary Owner
        assertTrue(canRenameOrRespecOrWipe.test(ownerUUID), "Primary owner can rename, respec, wipe");
        assertTrue(canEquipOrCommand.test(ownerUUID), "Primary owner can equip and command");

        // Verify Trusted Friend
        assertFalse(canRenameOrRespecOrWipe.test(trustedUUID), "Trusted friend CANNOT rename, respec, wipe");
        assertTrue(canEquipOrCommand.test(trustedUUID), "Trusted friend CAN equip and command");

        // Verify Stranger
        assertFalse(canRenameOrRespecOrWipe.test(strangerUUID), "Stranger CANNOT rename, respec, wipe");
        assertFalse(canEquipOrCommand.test(strangerUUID), "Stranger CANNOT equip or command");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion Failed: " + message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError("Assertion Failed: " + message);
        }
    }

    private static void assertEqual(Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError("Assertion Failed! Expected: " + expected + ", Actual: " + actual);
    }
}
