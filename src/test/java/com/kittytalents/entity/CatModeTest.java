package com.kittytalents.entity;

import com.kittytalents.core.persist.CatSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatModeTest {

    @Test
    void exactlyTwelveModes() {
        assertEquals(12, CatMode.values().length);
    }

    @Test
    void everyLegacyNameMapsToALiveMode() {
        for (String legacy : CatSchema.LEGACY_MODE_ORDER) {
            CatMode m = CatMode.fromSavedName(legacy);
            assertNotNull(m, legacy);
        }
        assertEquals(CatMode.FOLLOW, CatMode.fromSavedName("ESCORT"));
        assertEquals(CatMode.FOLLOW, CatMode.fromSavedName("protect"));
        assertEquals(CatMode.FOLLOW, CatMode.fromSavedName("DEFENSIVE"));
        assertEquals(CatMode.FOLLOW, CatMode.fromSavedName("RETREAT"));
        assertEquals(CatMode.GUARD, CatMode.fromSavedName("PATROL"));
        assertEquals(CatMode.AGGRESSIVE, CatMode.fromSavedName("BERSERKER"));
        assertEquals(CatMode.WORK, CatMode.fromSavedName("MINE"));
        assertEquals(CatMode.WORK, CatMode.fromSavedName("FARM"));
        assertEquals(CatMode.WORK, CatMode.fromSavedName("FISH"));
    }

    @Test
    void currentNamesRoundTripAndUnknownIsSafe() {
        for (CatMode m : CatMode.values()) assertEquals(m, CatMode.fromSavedName(m.name()));
        assertEquals(CatMode.FOLLOW, CatMode.fromSavedName("NOT_A_MODE"));
        assertEquals(CatMode.FOLLOW, CatMode.fromSavedName(null));
        assertEquals(CatMode.DOCILE, CatMode.byId(-1));
        assertEquals(CatMode.DOCILE, CatMode.byId(999));
    }

    @Test
    void modeSemanticsAreConsistent() {
        assertFalse(CatMode.DOCILE.canAttack());
        assertFalse(CatMode.RETURN_HOME.canAttack());
        assertFalse(CatMode.AGGRESSIVE.retreatsWhenHurt());
        assertTrue(CatMode.STAY.usesAnchor() && CatMode.GUARD.usesAnchor());
        assertFalse(CatMode.FOLLOW.usesAnchor());
        assertTrue(CatMode.FOLLOW.followsOwner() && CatMode.TACTICAL.followsOwner() && CatMode.SUPPORT.followsOwner());
        for (CatMode m : CatMode.values()) {
            assertFalse(m.followsOwner() && m.usesAnchor(), m + " cannot both follow and hold an anchor");
        }
    }
}
