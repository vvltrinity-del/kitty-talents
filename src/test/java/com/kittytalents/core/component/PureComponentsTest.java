package com.kittytalents.core.component;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PureComponentsTest {

    @Test
    void talentLevelsKeepExplicitZeroDistinctFromMissing() {
        CatTalentLevels t = new CatTalentLevels();
        t.set("mouser", 0);
        assertEquals(0, t.getOrDefault("mouser", 5), "explicit 0 must not fall back to the default");
        assertEquals(5, t.getOrDefault("fisher_cat", 5), "missing uses the default");
        t.set("pounce", -3);
        assertEquals(0, t.getOrDefault("pounce", 9), "negative ranks clamp to 0");
    }

    @Test
    void talentLevelsRoundTripAndViewIsReadOnly() {
        CatTalentLevels t = new CatTalentLevels();
        t.set("pounce", 4);
        t.set("mouser", 0);
        CompoundTag tag = new CompoundTag();
        t.save(tag);

        CatTalentLevels loaded = new CatTalentLevels();
        loaded.load(tag);
        assertEquals(Map.of("pounce", 4, "mouser", 0), loaded.view());
        assertThrows(UnsupportedOperationException.class, () -> loaded.view().put("x", 1));
    }

    @Test
    void talentLevelsLoadWithoutKeyKeepsCurrentState() {
        CatTalentLevels t = new CatTalentLevels();
        t.set("pounce", 2);
        t.load(new CompoundTag()); // old saves without a Talents tag must not wipe ranks
        assertEquals(2, t.getOrDefault("pounce", 0));
    }

    @Test
    void trustRoundTripPreservesOrderAndNames() {
        CatTrust trust = new CatTrust();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        trust.add(a, "Alice");
        trust.add(b, null);
        CompoundTag tag = new CompoundTag();
        trust.save(tag);

        CatTrust loaded = new CatTrust();
        loaded.load(tag);
        assertTrue(loaded.contains(a) && loaded.contains(b));
        assertEquals("Alice", loaded.view().get(a));
        assertEquals("", loaded.view().get(b), "null names are stored as empty");
        assertEquals(java.util.List.of(a, b), java.util.List.copyOf(loaded.view().keySet()));
        assertFalse(loaded.contains(null));
        assertThrows(UnsupportedOperationException.class, () -> loaded.view().clear());
    }
}
