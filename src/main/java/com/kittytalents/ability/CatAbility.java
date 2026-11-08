package com.kittytalents.ability;

import com.kittytalents.combat.FelineElement;
import com.kittytalents.combat.FelineEnergyEngine;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Base class for all feline active skills, ultimates, and utilities in Kitty Talents 3.0 (R4).
 */
public abstract class CatAbility {

    public enum Category {
        SKILL,
        ULTIMATE,
        UTILITY,
        PASSIVE
    }

    private final String id;
    private final String displayName;
    private final String description;
    private final int energyCost;
    private final int cooldownTicks;
    private final FelineElement element;
    private final Category category;

    public CatAbility(String id, String displayName, String description, int energyCost, int cooldownTicks, FelineElement element, Category category) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.energyCost = energyCost;
        this.cooldownTicks = cooldownTicks;
        this.element = element;
        this.category = category;
    }

    public CatAbility(String id, int energyCost, int cooldownTicks, FelineElement element) {
        this(id, id.replace('_', ' '), "", energyCost, cooldownTicks, element, Category.SKILL);
    }

    public CatAbility(String id, int energyCost, int cooldownTicks) {
        this(id, energyCost, cooldownTicks, FelineElement.PHYSICAL);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getEnergyCost() {
        return energyCost;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public FelineElement getElement() {
        return element;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isUltimate() {
        return category == Category.ULTIMATE;
    }

    public float getAiPriority(TalentedCat cat) {
        return 1.0f;
    }

    /**
     * Checks energy and cooldown on the server and executes the ability.
     */
    public boolean tryCast(TalentedCat cat, Player player) {
        if (cat == null || cat.level().isClientSide || !cat.isAlive() || cat.isIncapacitated()) {
            return false;
        }

        long lastCastTime = cat.getAbilityCooldown(id);
        long currentTime = cat.level().getGameTime();

        if (currentTime < lastCastTime + cooldownTicks) {
            return false; // Still on cooldown
        }

        if (FelineEnergyEngine.getEnergy(cat) < energyCost) {
            return false; // Not enough energy
        }

        if (execute(cat, player)) {
            cat.playAnimation(TalentedCat.ANIM_CAST);
            FelineEnergyEngine.consumeEnergy(cat, energyCost);
            cat.setAbilityCooldown(id, currentTime);

            if (isUltimate()) {
                cat.playSound(com.kittytalents.registry.ModSounds.ABILITY_ULTIMATE.get(), 1.0F, 1.0F);
            } else {
                cat.playSound(com.kittytalents.registry.ModSounds.ABILITY_CAST.get(), 0.8F, 1.2F);
            }
            return true;
        }

        return false;
    }

    /**
     * Executes ability behavior. Returns true if successfully triggered.
     */
    protected abstract boolean execute(TalentedCat cat, Player player);
}
