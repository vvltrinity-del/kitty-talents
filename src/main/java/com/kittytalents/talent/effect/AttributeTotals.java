package com.kittytalents.talent.effect;

/** Mutable sums that talents add to; TalentedCat#updateAttributes writes them to the real attributes. */
public final class AttributeTotals {
    public double maxHealth;
    public double movementSpeed;
    public double attackDamage;
    public double armor;
    public double armorToughness;
    public double knockbackResistance;

    public AttributeTotals(double maxHealth, double movementSpeed, double attackDamage, double armor) {
        this.maxHealth = maxHealth;
        this.movementSpeed = movementSpeed;
        this.attackDamage = attackDamage;
        this.armor = armor;
    }
}
