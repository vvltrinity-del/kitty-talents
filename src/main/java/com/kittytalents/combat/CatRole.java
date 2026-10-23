package com.kittytalents.combat;

/**
 * Cat Combat Roles for Kitty Talents 3.0 (R3).
 * Emerging from talents, stats, abilities, and equipment.
 */
public enum CatRole {
    NONE("None", 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, "Balanced baseline companion with unspecialized capabilities."),
    DPS("DPS", 0.95f, 1.30f, 1.10f, 1.10f, 0.15f, 0.0f, "High offensive power specializing in direct burst and sustained damage."),
    ASSASSIN("Assassin", 0.75f, 1.45f, 1.25f, 1.20f, 0.30f, -0.10f, "Lethal burst and extreme agility, trading resilience for critical strikes."),
    TANK("Tank", 1.60f, 0.75f, 0.85f, 0.90f, 0.0f, 0.25f, "Colossal durability and defensive crowd control designed to protect allies."),
    BRUISER("Bruiser", 1.30f, 1.15f, 0.95f, 1.00f, 0.05f, 0.10f, "Formidable frontline combatant balancing heavy hits with sturdy defense."),
    SUPPORT("Support", 1.10f, 0.85f, 1.05f, 1.35f, 0.0f, 0.05f, "Enhances owner and allied companions through buffs, utility, and energy flow."),
    HEALER("Healer", 1.05f, 0.70f, 1.00f, 1.40f, 0.0f, 0.0f, "Specialized restorative guardian providing continuous health and cleanse."),
    CONTROLLER("Controller", 1.00f, 0.90f, 1.00f, 1.25f, 0.05f, 0.05f, "Disrupts and weakens enemy groups with stuns, slows, and debuffs."),
    BREAKER("Breaker", 1.20f, 1.25f, 0.90f, 1.00f, 0.10f, 0.10f, "Heavy armor-shattering specialist devastating shielded and armored foes."),
    SCOUT("Scout", 0.85f, 0.95f, 1.30f, 1.25f, 0.10f, 0.0f, "Rapid reconnaissance tracker revealing hidden hazards and retrieving items."),
    MOBILITY("Mobility", 0.90f, 1.05f, 1.35f, 1.30f, 0.10f, 0.0f, "Evasive acrobat capable of rapid 4-way dodging and aerial traversal."),
    HYBRID("Hybrid", 1.10f, 1.10f, 1.10f, 1.10f, 0.05f, 0.05f, "Versatile multi-disciplined companion excelling across multiple domains."),
    MAGE("Mage", 0.85f, 1.10f, 1.00f, 1.50f, 0.10f, 0.0f, "Arcane spellcaster unleashing devastating elemental abilities via Feline Energy.");

    private final String displayName;
    private final float hpMult;
    private final float dmgMult;
    private final float speedMult;
    private final float energyRegenMult;
    private final float critChanceBonus;
    private final float damageResistanceBonus;
    private final String description;

    CatRole(String displayName, float hpMult, float dmgMult, float speedMult,
            float energyRegenMult, float critChanceBonus, float damageResistanceBonus,
            String description) {
        this.displayName = displayName;
        this.hpMult = hpMult;
        this.dmgMult = dmgMult;
        this.speedMult = speedMult;
        this.energyRegenMult = energyRegenMult;
        this.critChanceBonus = critChanceBonus;
        this.damageResistanceBonus = damageResistanceBonus;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public float getHpMultiplier() { return hpMult; }
    public float getDamageMultiplier() { return dmgMult; }
    public float getSpeedMultiplier() { return speedMult; }
    public float getEnergyRegenMultiplier() { return energyRegenMult; }
    public float getCritChanceBonus() { return critChanceBonus; }
    public float getDamageResistanceBonus() { return damageResistanceBonus; }
    public String getDescription() { return description; }

    public static CatRole fromId(int id) {
        if (id < 0 || id >= values().length) return NONE;
        return values()[id];
    }

    public static CatRole fromString(String name) {
        if (name == null || name.trim().isEmpty()) return NONE;
        for (CatRole role : values()) {
            if (role.name().equalsIgnoreCase(name.trim()) || role.displayName.equalsIgnoreCase(name.trim())) {
                return role;
            }
        }
        return NONE;
    }
}
