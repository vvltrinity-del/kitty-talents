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
    private final com.kittytalents.core.tuning.Tuning.Key energyCost;
    private final com.kittytalents.core.tuning.Tuning.Key cooldownTicks;
    private final FelineElement element;
    private final Category category;

    public CatAbility(String id, String displayName, String description, int energyCost, int cooldownTicks, FelineElement element, Category category) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.energyCost = com.kittytalents.core.tuning.Tuning.ability(id, "energy_cost", energyCost, "Energy cost");
        this.cooldownTicks = com.kittytalents.core.tuning.Tuning.ability(id, "cooldown_ticks", cooldownTicks, "Cooldown ticks");
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
        return energyCost.i();
    }

    public int getCooldownTicks() {
        return cooldownTicks.i();
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

        if (currentTime < lastCastTime + getCooldownTicks()) {
            return false; // Still on cooldown
        }

        // Warlock (5.0 promotion): abilities cost health instead of Energy.
        boolean warlock = cat.isPromoted() && cat.getCatRole() == com.kittytalents.combat.CatRole.MAGE;
        if (warlock ? cat.getHealth() <= Math.max(1.0F, getEnergyCost() / 5.0F) + 1.0F : FelineEnergyEngine.getEnergy(cat) < getEnergyCost()) {
            return false; // Not enough energy (or health)
        }

        if (execute(cat, player)) {
            cat.playAnimation(TalentedCat.ANIM_CAST);
            if (!com.kittytalents.combat.RolePromotions.payWithHealth(cat, getEnergyCost())) FelineEnergyEngine.consumeEnergy(cat, getEnergyCost());
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
