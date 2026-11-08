package com.kittytalents.core.component;

import net.minecraft.nbt.CompoundTag;

/** Lifetime counters shown on the Stats tab. Server-authoritative; NBT keys unchanged from 2.0. */
public final class CatStatistics {
    private int totalKills, hostileKills, passiveKills;
    private float damageDealt, damageTaken;
    private double distanceTraveled;
    private int foodConsumed, blocksMined, pouncesPerformed, itemsRetrieved, creepersScared;
    private int healingProvided, timesPet, frenzyActivations;

    public void recordKill(boolean hostile) {
        totalKills++;
        if (hostile) hostileKills++; else passiveKills++;
    }
    public void addDamageDealt(float amount) { damageDealt += amount; }
    public void addDamageTaken(float amount) { damageTaken += amount; }
    public void addDistance(double blocks) { distanceTraveled += blocks; }
    public void addFoodConsumed(int amount) { foodConsumed += amount; }
    public void incrementBlocksMined() { blocksMined++; }
    public void incrementPounces() { pouncesPerformed++; }
    public void incrementItemsRetrieved() { itemsRetrieved++; }
    public void incrementCreepersScared() { creepersScared++; }
    public void addHealingProvided(int amount) { healingProvided += amount; }
    public void incrementTimesPet() { timesPet++; }
    public void incrementFrenzyActivations() { frenzyActivations++; }

    public int getTotalKills() { return totalKills; }
    public int getHostileKills() { return hostileKills; }
    public int getPassiveKills() { return passiveKills; }
    public float getDamageDealt() { return damageDealt; }
    public float getDamageTaken() { return damageTaken; }
    public double getDistanceTraveled() { return distanceTraveled; }
    public int getFoodConsumed() { return foodConsumed; }
    public int getBlocksMined() { return blocksMined; }
    public int getPouncesPerformed() { return pouncesPerformed; }
    public int getItemsRetrieved() { return itemsRetrieved; }
    public int getCreepersScared() { return creepersScared; }
    public int getHealingProvided() { return healingProvided; }
    public int getTimesPet() { return timesPet; }
    public int getFrenzyActivations() { return frenzyActivations; }

    public void reset() {
        totalKills = hostileKills = passiveKills = 0;
        damageDealt = damageTaken = 0.0F;
        distanceTraveled = 0.0D;
        foodConsumed = blocksMined = pouncesPerformed = itemsRetrieved = creepersScared = 0;
        healingProvided = timesPet = frenzyActivations = 0;
    }

    public void save(CompoundTag tag) {
        tag.putInt("TotalKills", totalKills);
        tag.putInt("HostileKills", hostileKills);
        tag.putInt("PassiveKills", passiveKills);
        tag.putFloat("DamageDealt", damageDealt);
        tag.putFloat("DamageTaken", damageTaken);
        tag.putDouble("DistanceTraveled", distanceTraveled);
        tag.putInt("FoodConsumed", foodConsumed);
        tag.putInt("BlocksMined", blocksMined);
        tag.putInt("PouncesPerformed", pouncesPerformed);
        tag.putInt("ItemsRetrieved", itemsRetrieved);
        tag.putInt("CreepersScared", creepersScared);
        tag.putInt("HealingProvided", healingProvided);
        tag.putInt("TimesPet", timesPet);
        tag.putInt("FrenzyActivations", frenzyActivations);
    }

    public void load(CompoundTag tag) {
        totalKills = tag.getInt("TotalKills");
        hostileKills = tag.getInt("HostileKills");
        passiveKills = tag.getInt("PassiveKills");
        damageDealt = tag.getFloat("DamageDealt");
        damageTaken = tag.getFloat("DamageTaken");
        distanceTraveled = tag.getDouble("DistanceTraveled");
        foodConsumed = tag.getInt("FoodConsumed");
        blocksMined = tag.getInt("BlocksMined");
        pouncesPerformed = tag.getInt("PouncesPerformed");
        itemsRetrieved = tag.getInt("ItemsRetrieved");
        creepersScared = tag.getInt("CreepersScared");
        healingProvided = tag.getInt("HealingProvided");
        timesPet = tag.getInt("TimesPet");
        frenzyActivations = tag.getInt("FrenzyActivations");
    }
}
