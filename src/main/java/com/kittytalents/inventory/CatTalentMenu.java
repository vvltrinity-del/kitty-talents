package com.kittytalents.inventory;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CatTalentMenu extends AbstractContainerMenu {
    private final TalentedCat cat;

    public CatTalentMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getCatFromBuf(playerInventory.player.level(), extraData));
    }

    public CatTalentMenu(int containerId, Inventory playerInventory, TalentedCat cat) {
        super(ModMenus.CAT_TALENT_MENU.get(), containerId);
        this.cat = cat;
    }

    private static TalentedCat getCatFromBuf(Level level, FriendlyByteBuf buf) {
        if (buf != null) {
            int entityId = buf.readInt();
            Entity entity = level.getEntity(entityId);
            if (entity instanceof TalentedCat talentedCat) {
                return talentedCat;
            }
        }
        return null;
    }

    public TalentedCat getCat() {
        return cat;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return cat != null && cat.isAlive() && cat.distanceToSqr(player) < 64.0D;
    }
}
