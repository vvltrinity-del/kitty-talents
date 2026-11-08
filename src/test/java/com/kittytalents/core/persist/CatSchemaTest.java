package com.kittytalents.core.persist;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatSchemaTest {

    /** A tag shaped like a 1.0 / 2.0 / partial-3.0 save. */
    private static CompoundTag legacyRoot(int modeOrdinal) {
        CompoundTag root = new CompoundTag();
        root.putInt("CatLevel", 42);
        root.putInt(CatSchema.KEY_LEGACY_MODE_ORDINAL, modeOrdinal);
        return root;
    }

    @Test
    void legacyModeOrdinalBecomesName() {
        for (int i = 0; i < CatSchema.LEGACY_MODE_ORDER.size(); i++) {
            CompoundTag root = legacyRoot(i);
            CatSchema.migrate(root, new CompoundTag());
            assertEquals(CatSchema.LEGACY_MODE_ORDER.get(i), root.getString(CatSchema.KEY_MODE_NAME));
        }
    }

    @Test
    void frozenLegacyOrderMatchesHistoricEnum() {
        // Spot-check ordinals that appear in real saves; this list must never be edited.
        assertEquals("DOCILE", CatSchema.LEGACY_MODE_ORDER.get(0));
        assertEquals("PATROL", CatSchema.LEGACY_MODE_ORDER.get(8));
        assertEquals("WORK", CatSchema.LEGACY_MODE_ORDER.get(20));
        assertEquals(21, CatSchema.LEGACY_MODE_ORDER.size());
    }

    @Test
    void outOfRangeOrdinalFallsBackToDocile() {
        CompoundTag root = legacyRoot(99);
        CatSchema.migrate(root, new CompoundTag());
        assertEquals("DOCILE", root.getString(CatSchema.KEY_MODE_NAME));
    }

    @Test
    void forgeDataMovesIntoRootAndTransientKeysAreDropped() {
        CompoundTag root = legacyRoot(1);
        CompoundTag forge = new CompoundTag();
        forge.putLong("cooldown_pounce", 1234L);
        forge.putLong("cooldown_feline_hiss", 99L);
        forge.putBoolean("SpiritCatnipUsed", true);
        forge.putString("MutationType", "NEKOMATA");
        forge.putInt("KT_CombatXP", 50);
        forge.putInt("KT_ComboStep", 2);
        forge.putBoolean("kt_ai_initialized", true);
        forge.putString("SomeOtherModsData", "keep me");

        assertEquals(0, CatSchema.migrate(root, forge));

        CompoundTag cds = root.getCompound(CatSchema.KEY_ABILITY_COOLDOWNS);
        assertEquals(1234L, cds.getLong("pounce"));
        assertEquals(99L, cds.getLong("feline_hiss"));
        assertTrue(root.getBoolean(CatSchema.KEY_SPIRIT_PRIMED));
        assertEquals("NEKOMATA", root.getString(CatSchema.KEY_MUTATION));
        assertEquals(CatSchema.CURRENT, root.getInt(CatSchema.KEY_SCHEMA));

        assertEquals(1, forge.size(), "only foreign data should remain: " + forge);
        assertEquals("keep me", forge.getString("SomeOtherModsData"));
    }

    @Test
    void rootMutationWinsOverForgeData() {
        CompoundTag root = legacyRoot(0);
        root.putString(CatSchema.KEY_MUTATION, "NONE");
        CompoundTag forge = new CompoundTag();
        forge.putString("MutationType", "NEKOMATA");
        CatSchema.migrate(root, forge);
        assertEquals("NONE", root.getString(CatSchema.KEY_MUTATION));
    }

    @Test
    void currentVersionIsIdempotent() {
        CompoundTag root = new CompoundTag();
        root.putInt(CatSchema.KEY_SCHEMA, CatSchema.CURRENT);
        root.putString(CatSchema.KEY_MODE_NAME, "GUARD");
        root.putInt(CatSchema.KEY_LEGACY_MODE_ORDINAL, 0); // stale leftover must not override the name
        CompoundTag before = root.copy();

        assertEquals(CatSchema.CURRENT, CatSchema.migrate(root, new CompoundTag()));
        assertEquals(before, root);
    }
}
