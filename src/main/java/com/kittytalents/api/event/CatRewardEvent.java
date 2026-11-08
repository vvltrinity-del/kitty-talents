package com.kittytalents.api.event;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when a Talented Cat brings its owner an item (Mouser morning gifts). Change the reward, or cancel the event
 * when you delivered it yourself (e.g. straight into a backpack) so it is not dropped.
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
