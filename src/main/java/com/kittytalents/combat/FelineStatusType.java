package com.kittytalents.combat;

/**
 * Status effect types supported by Kitty Talents 3.0 (R7).
 */
public enum FelineStatusType {
    BURN("Burn", false, 400, 3, "Deals ongoing fire damage, extinguished by water."),
    POISON("Poison", false, 400, 3, "Deals nature damage over time that ignores armor."),
    BLEED("Bleed", false, 300, 3, "Laceration causing damage over time; deals double damage when moving."),
    FREEZE("Freeze", false, 200, 3, "Drastically reduces movement speed and chills the target."),
    SHOCK("Shock", false, 160, 2, "Periodically discharges electric arcs that micro-stun the target."),
    MARKED("Marked", false, 300, 1, "Exposes vulnerabilities, increasing all incoming damage by 30%."),
    VULNERABLE("Vulnerable", false, 240, 2, "Weakens physical defenses, increasing damage taken by 25%."),
    WEAKNESS("Weakness", false, 300, 2, "Reduces outgoing attack damage by 30%."),
    HASTE("Haste", true, 300, 2, "Increases movement speed and attack recovery speed."),
    REGENERATION("Regeneration", true, 300, 3, "Continuously heals health over time."),
    FORTIFIED("Fortified", true, 300, 2, "Hardens resolve, reducing all incoming damage by 30%.");

    private final String displayName;
    private final boolean beneficial;
    private final int maxDurationTicks;
    private final int maxPower;
    private final String description;

    FelineStatusType(String displayName, boolean beneficial, int maxDurationTicks, int maxPower, String description) {
        this.displayName = displayName;
        this.beneficial = beneficial;
        this.maxDurationTicks = maxDurationTicks;
        this.maxPower = maxPower;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public boolean isBeneficial() { return beneficial; }
    public int getMaxDurationTicks() { return maxDurationTicks; }
    public int getMaxPower() { return maxPower; }
    public String getDescription() { return description; }

    public static FelineStatusType fromString(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        for (FelineStatusType type : values()) {
            if (type.name().equalsIgnoreCase(name.trim()) || type.displayName.equalsIgnoreCase(name.trim())) {
                return type;
            }
        }
        return null;
    }
}
