package com.kittytalents.client.network;

import com.kittytalents.client.ClientTwineHandler;
import com.kittytalents.client.gui.CatTalentScreen;
import com.kittytalents.client.gui.VariantChartScreen;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Isolated client-side packet and screen handlers.
 * Ensures zero client class loading on dedicated server environments.
 */
public class ClientPacketHandlers {

    public static void handleSyncCatEquipment(int entityId, ItemStack weapon, ItemStack tool,
                                              ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack boots,
                                              ItemStack accessory, ItemStack cosmetic1, ItemStack cosmetic2, ItemStack cosmetic3,
                                              boolean cosmeticsVisible) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity instanceof TalentedCat cat) {
                cat.setEquippedWeaponClient(weapon);
                cat.setEquippedToolClient(tool);
                cat.setArmorSlotItemClient(1, helmet);
                cat.setArmorSlotItemClient(2, chest);
                cat.setArmorSlotItemClient(3, legs);
                cat.setArmorSlotItemClient(4, boots);
                cat.setEquippedAccessoryClient(accessory);
                cat.setCosmeticSlotClient(0, cosmetic1);
                cat.setCosmeticSlotClient(1, cosmetic2);
                cat.setCosmeticSlotClient(2, cosmetic3);
                cat.setCosmeticsVisible(cosmeticsVisible);

                if (mc.screen instanceof CatTalentScreen screen) {
                    screen.refreshEquipmentUi();
                }
            }
        }
    }

    public static void handleSyncCatEquipment(int entityId, ItemStack weapon, ItemStack tool,
                                              ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack boots,
                                              ItemStack accessory, List<ItemStack> cosmetics) {
        ItemStack c0 = (cosmetics != null && cosmetics.size() > 0) ? cosmetics.get(0) : ItemStack.EMPTY;
        ItemStack c1 = (cosmetics != null && cosmetics.size() > 1) ? cosmetics.get(1) : ItemStack.EMPTY;
        ItemStack c2 = (cosmetics != null && cosmetics.size() > 2) ? cosmetics.get(2) : ItemStack.EMPTY;
        handleSyncCatEquipment(entityId, weapon, tool, helmet, chest, legs, boots, accessory, c0, c1, c2, true);
    }

    public static void handleSyncTalents(int entityId, int skillPoints, Map<String, Integer> talentMap) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity instanceof TalentedCat cat) {
                cat.setSkillPoints(skillPoints);
                cat.setAllTalentLevels(talentMap);
                cat.updateAttributes();
                if (mc.screen instanceof CatTalentScreen screen && screen.getMenu().getCat() == cat) {
                    screen.refreshTalentUi();
                }
            }
        }
    }

    public static void handleSyncTrustedPlayers(int entityId, Map<UUID, String> players) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(entityId);
        if (e instanceof TalentedCat cat) {
            cat.setTrustedPlayers(players);
            if (mc.screen instanceof CatTalentScreen screen) {
                screen.refreshTrustUi();
            }
        }
    }

    public static void handleSyncTwine(int catEntityId, boolean twined) {
        ClientTwineHandler.handleSyncTwine(catEntityId, twined);
    }

    public static void openVariantChartScreen() {
        Minecraft.getInstance().setScreen(new VariantChartScreen());
    }
}
