package com.kittytalents.compat.jade;

import com.kittytalents.KittyTalents;
import com.kittytalents.block.*;
import com.kittytalents.compat.TacZCompat;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class KittyJadePlugin implements IWailaPlugin {
    public static final ResourceLocation CAT_INFO = new ResourceLocation(KittyTalents.MODID, "cat_info");
    public static final ResourceLocation AUTOMATION_INFO = new ResourceLocation(KittyTalents.MODID, "automation_info");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(new CatInfoProvider(), TalentedCat.class);
        AutomationBlockProvider blockProvider = new AutomationBlockProvider();
        registration.registerBlockComponent(blockProvider, KittyHabitatBlock.class);
        registration.registerBlockComponent(blockProvider, KittyWorkStationBlock.class);
        registration.registerBlockComponent(blockProvider, TreatOvenBlock.class);
    }

    public static class CatInfoProvider implements IEntityComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof TalentedCat cat) {
                tooltip.add(Component.literal("Lv." + cat.getCatLevel() + " [" + cat.getProductionTier().getDisplayName() + " Tier]")
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal("Mode: " + cat.getCatMode().name() + " | Hunger: " + cat.getHunger() + "/120")
                        .withStyle(ChatFormatting.AQUA));
                if (cat.getCatMode() == CatMode.WORK || cat.getCatJob() != com.kittytalents.work.CatJob.NONE) {
                    tooltip.add(Component.literal("Job: " + cat.getCatJob().getDisplayName() + " (" + cat.getWorkStatus() + ")")
                            .withStyle(cat.isClockedIn() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
                }
                ItemStack weapon = cat.getEquippedWeapon();
                if (!weapon.isEmpty()) {
                    if (TacZCompat.isGun(weapon)) {
                        int cur = TacZCompat.getAmmoCount(weapon);
                        int max = TacZCompat.getMaxAmmoCount(weapon);
                        tooltip.add(Component.literal("Gun: " + weapon.getHoverName().getString() + " [" + cur + "/" + max + "]")
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    } else {
                        tooltip.add(Component.literal("Weapon: " + weapon.getHoverName().getString())
                                .withStyle(ChatFormatting.WHITE));
                    }
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
            return CAT_INFO;
        }
    }

    public static class AutomationBlockProvider implements IBlockComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof KittyHabitatBlockEntity habitat) {
                if (habitat.getAssignedCatUUID() != null || habitat.hasStoredCat()) {
                    tooltip.add(Component.literal("Worker: " + habitat.getAssignedCatName() + " [" + habitat.getAssignedCatResourceTier().getDisplayName() + "]")
                            .withStyle(ChatFormatting.GREEN));
                    int pct = (habitat.getProgress() * 100) / Math.max(1, habitat.getMaxProgress());
                    tooltip.add(Component.literal("Status: " + habitat.getStatusMessage() + " (" + pct + "%)")
                            .withStyle(ChatFormatting.AQUA));
                } else {
                    tooltip.add(Component.literal("Idle - Assign Cat with Work Clipboard").withStyle(ChatFormatting.GRAY));
                }
            } else if (accessor.getBlockEntity() instanceof KittyWorkStationBlockEntity station) {
                int count = station.getBoundWorkerNames().size();
                tooltip.add(Component.literal("Bound Workers: " + count).withStyle(ChatFormatting.GOLD));
                for (String name : station.getBoundWorkerNames()) {
                    tooltip.add(Component.literal(" • " + name).withStyle(ChatFormatting.AQUA));
                }
            } else if (accessor.getBlockEntity() instanceof TreatOvenBlockEntity oven) {
                int pct = (oven.getCookProgress() * 100) / Math.max(1, oven.getMaxCookProgress());
                tooltip.add(Component.literal("Baking Progress: " + pct + "%").withStyle(ChatFormatting.GOLD));
                if (oven.getBurnTime() > 0) {
                    tooltip.add(Component.literal("Fuel Remaining: " + (oven.getBurnTime() / 20) + "s").withStyle(ChatFormatting.YELLOW));
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
            return AUTOMATION_INFO;
        }
    }
}
