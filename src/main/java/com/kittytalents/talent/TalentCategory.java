package com.kittytalents.talent;

/**
 * Categorization of feline talents for Kitty Talents 3.0 (R8).
 */
public enum TalentCategory {
    COMBAT("Combat", "Offensive capabilities and weapon mastery"),
    DEFENSE("Defense", "Durability, shielding, and survival"),
    MOBILITY("Mobility", "Movement, traversal, and agility"),
    SUPPORT("Support", "Healing, buffs, and companion wellbeing"),
    UTILITY("Utility", "Work, exploration, gathering, and sensing"),
    DIRE("Dire", "Endgame awakened traits requiring Dire Treats");

    private final String displayName;
    private final String description;

    TalentCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
