package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CatTreatItem extends Item {
    private final int minLevel;
    private final int maxLevel;

    public CatTreatItem(Properties properties, int minLevel, int maxLevel) {
        super(properties);
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    public boolean canLevelUp(TalentedCat cat) {
        int level = cat.getCatLevel();
        return level >= minLevel && level <= maxLevel;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.kittytalents.treat_levels", minLevel, maxLevel).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
