package com.kittytalents.api.event;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when a Talented Cat produces or finds a reward item.
 * If canceled, the item will not be dropped/given to the cat.
 */
@Cancelable
public class CatRewardEvent extends Event {
    private final TalentedCat cat;
    private ItemStack reward;

    public CatRewardEvent(TalentedCat cat, ItemStack reward) {
        this.cat = cat;
        this.reward = reward;
    }

    public TalentedCat getCat() { return cat; }
    public ItemStack getReward() { return reward; }
    public void setReward(ItemStack stack) { this.reward = stack; }
}
