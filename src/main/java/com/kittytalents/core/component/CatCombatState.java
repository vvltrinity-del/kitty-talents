package com.kittytalents.core.component;

import com.kittytalents.combat.CatRole;
import com.kittytalents.combat.FelineElement;
import com.kittytalents.core.persist.CatSchema;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Combat identity and loadout (saved) plus combat scratch state (transient, never saved).
 * Shared by AI, Command and Twine control so all three use the same cooldowns and timers.
 */
public final class CatCombatState {

    public static final String DEFAULT_SLOT_1 = "feline_dash";
    public static final String DEFAULT_SLOT_2 = "feline_hiss";
    public static final String DEFAULT_SLOT_3 = "healing_purr";
    public static final String DEFAULT_ULTIMATE = "catnip_frenzy";

    // ---- Saved ----
    private CatRole role = CatRole.NONE;
    private FelineElement primaryElement = FelineElement.PHYSICAL;
    private FelineElement secondaryElement = FelineElement.PHYSICAL;
    private String slot1 = DEFAULT_SLOT_1, slot2 = DEFAULT_SLOT_2, slot3 = DEFAULT_SLOT_3, ultimate = DEFAULT_ULTIMATE;
    /** Ability id (lower-case) -> game time of last cast. */
    private final Map<String, Long> cooldowns = new HashMap<>();
    /** Game time at which the Nine Lives instant revive is available again. */
    private long nineLivesReadyAt = 0L;

    // ---- Transient scratch state (was wrongly persisted in Forge data before schema 1) ----
    public long dodgeEnd, perfectDodgeEnd;
    public int comboStep;
    public long comboExpiry, lastAttackTime;
    public boolean pounceActive;
    public long pounceStart;
    public long lastTwinePounceTick;
    public int outOfCombatTicks = 100;

    public CatRole getRole() { return role; }
    public void setRole(CatRole role) { this.role = role == null ? CatRole.NONE : role; }
    public FelineElement getPrimaryElement() { return primaryElement; }
    public void setPrimaryElement(FelineElement e) { this.primaryElement = e == null ? FelineElement.PHYSICAL : e; }
    public FelineElement getSecondaryElement() { return secondaryElement; }
    public void setSecondaryElement(FelineElement e) { this.secondaryElement = e == null ? FelineElement.PHYSICAL : e; }

    public String getSlot1() { return slot1; }
    public String getSlot2() { return slot2; }
    public String getSlot3() { return slot3; }
    public String getUltimate() { return ultimate; }
    public void setSlot1(String id) { slot1 = orDefault(id, DEFAULT_SLOT_1); }
    public void setSlot2(String id) { slot2 = orDefault(id, DEFAULT_SLOT_2); }
    public void setSlot3(String id) { slot3 = orDefault(id, DEFAULT_SLOT_3); }
    public void setUltimate(String id) { ultimate = orDefault(id, DEFAULT_ULTIMATE); }

    public long getCooldown(String abilityId) {
        return abilityId == null ? 0L : cooldowns.getOrDefault(abilityId.toLowerCase(Locale.ROOT), 0L);
    }
    public void setCooldown(String abilityId, long gameTime) {
        if (abilityId != null) cooldowns.put(abilityId.toLowerCase(Locale.ROOT), gameTime);
    }

    public long getNineLivesReadyAt() { return nineLivesReadyAt; }
    public void setNineLivesReadyAt(long tick) { this.nineLivesReadyAt = tick; }

    private static String orDefault(String id, String def) { return id == null || id.isEmpty() ? def : id; }

    private static <E extends Enum<E>> E parse(Class<E> type, String name, E fallback) {
        try { return Enum.valueOf(type, name); } catch (IllegalArgumentException | NullPointerException e) { return fallback; }
    }

    public void save(CompoundTag tag) {
        tag.putString("CatRole", role.name());
        tag.putString("PrimaryElement", primaryElement.name());
        tag.putString("SecondaryElement", secondaryElement.name());
        tag.putString("AbilitySlot1", slot1);
        tag.putString("AbilitySlot2", slot2);
        tag.putString("AbilitySlot3", slot3);
        tag.putString("AbilityUltimate", ultimate);
        tag.putLong("NineLivesReadyAt", nineLivesReadyAt);
        if (!cooldowns.isEmpty()) {
            CompoundTag cds = new CompoundTag();
            cooldowns.forEach(cds::putLong);
            tag.put(CatSchema.KEY_ABILITY_COOLDOWNS, cds);
        }
    }

    public void load(CompoundTag tag) {
        if (tag.contains("CatRole")) role = parse(CatRole.class, tag.getString("CatRole"), CatRole.NONE);
        if (tag.contains("PrimaryElement")) primaryElement = parse(FelineElement.class, tag.getString("PrimaryElement"), FelineElement.PHYSICAL);
        if (tag.contains("SecondaryElement")) secondaryElement = parse(FelineElement.class, tag.getString("SecondaryElement"), FelineElement.PHYSICAL);
        if (tag.contains("AbilitySlot1")) setSlot1(tag.getString("AbilitySlot1"));
        if (tag.contains("AbilitySlot2")) setSlot2(tag.getString("AbilitySlot2"));
        if (tag.contains("AbilitySlot3")) setSlot3(tag.getString("AbilitySlot3"));
        if (tag.contains("AbilityUltimate")) setUltimate(tag.getString("AbilityUltimate"));
        nineLivesReadyAt = tag.getLong("NineLivesReadyAt");
        cooldowns.clear();
        CompoundTag cds = tag.getCompound(CatSchema.KEY_ABILITY_COOLDOWNS);
        for (String id : cds.getAllKeys()) cooldowns.put(id, cds.getLong(id));
    }
}
