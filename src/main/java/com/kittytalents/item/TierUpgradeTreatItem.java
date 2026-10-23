package com.kittytalents.item;

import com.kittytalents.entity.ProductionTier;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Craftable consumable treat that directly upgrades an owned Talented Cat's
 * Production Tier to the next level when fed, while preserving the cat's visual breed identity.
 */
public class TierUpgradeTreatItem extends Item {
    private final ProductionTier prerequisiteTier;
    private final ProductionTier targetTier;

    public TierUpgradeTreatItem(Properties properties, ProductionTier prerequisiteTier, ProductionTier targetTier, Rarity rarity) {
        super(properties.stacksTo(16).rarity(rarity));
        this.prerequisiteTier = prerequisiteTier;
        this.targetTier = targetTier;
    }

    public ProductionTier getPrerequisiteTier() {
        return prerequisiteTier;
    }

    public ProductionTier getTargetTier() {
        return targetTier;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TalentedCat cat && (cat.isOwnedBy(player) || cat.isTrusted(player.getUUID()))) {
            if (!cat.isIncapacitated()) {
                if (cat.getProductionTier() == this.prerequisiteTier) {
                    if (!player.level().isClientSide) {
                        cat.setProductionTier(this.targetTier);
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }

                        cat.playSound(SoundEvents.PLAYER_LEVELUP, 0.9F, 1.4F);
                        cat.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);

                        if (cat.level() instanceof ServerLevel serverLevel) {
                            spawnTierParticles(serverLevel, cat, targetTier);
                        }

                        player.displayClientMessage(
                                Component.literal("✦ Upgraded ").withStyle(ChatFormatting.GOLD)
                                        .append(Component.literal(cat.getName().getString()).withStyle(ChatFormatting.WHITE))
                                        .append(Component.literal(" to ").withStyle(ChatFormatting.GOLD))
                                        .append(Component.literal(targetTier.getDisplayName() + " Production Tier!").withStyle(targetTier.getFormatting()))
                                        .append(Component.literal(" ✦").withStyle(ChatFormatting.GOLD)),
                                true
                        );
                    }
                    return InteractionResult.sidedSuccess(player.level().isClientSide);
                } else if (cat.getProductionTier().ordinal() >= this.targetTier.ordinal()) {
                    if (!player.level().isClientSide) {
                        player.displayClientMessage(
                                Component.literal(cat.getName().getString() + " is already at " + cat.getProductionTier().getDisplayName() + " Tier or higher!").withStyle(ChatFormatting.YELLOW),
                                true
                        );
                    }
                    return InteractionResult.sidedSuccess(player.level().isClientSide);
                } else {
                    if (!player.level().isClientSide) {
                        player.displayClientMessage(
                                Component.literal(cat.getName().getString() + " must reach " + prerequisiteTier.getDisplayName() + " Tier before using this treat!").withStyle(ChatFormatting.RED),
                                true
                        );
                    }
                    return InteractionResult.sidedSuccess(player.level().isClientSide);
                }
            }
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    private void spawnTierParticles(ServerLevel level, TalentedCat cat, ProductionTier tier) {
        ParticleOptions particle = switch (tier) {
            case COAL -> ParticleTypes.FLAME;
            case COPPER -> ParticleTypes.SCRAPE;
            case IRON -> ParticleTypes.CRIT;
            case GOLD -> ParticleTypes.TOTEM_OF_UNDYING;
            case LAPIS -> ParticleTypes.ENCHANT;
            case REDSTONE -> ParticleTypes.ELECTRIC_SPARK;
            case EMERALD -> ParticleTypes.HAPPY_VILLAGER;
            case DIAMOND -> ParticleTypes.END_ROD;
            case NETHERITE -> ParticleTypes.SOUL_FIRE_FLAME;
            default -> ParticleTypes.HEART;
        };

        for (int i = 0; i < 20; i++) {
            double ox = (level.random.nextDouble() - 0.5D) * 0.8D;
            double oy = level.random.nextDouble() * 0.8D;
            double oz = (level.random.nextDouble() - 0.5D) * 0.8D;
            level.sendParticles(particle, cat.getX() + ox, cat.getY() + oy, cat.getZ() + oz, 1, 0, 0.05D, 0, 0.02D);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Production Tier Catalyst").withStyle(targetTier.getFormatting(), ChatFormatting.BOLD));
        tooltip.add(Component.literal("Prerequisite: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(prerequisiteTier.getDisplayName() + " Tier").withStyle(prerequisiteTier.getFormatting())));
        tooltip.add(Component.literal("Upgrades to: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(targetTier.getDisplayName() + " Tier").withStyle(targetTier.getFormatting())));
        tooltip.add(Component.literal("Right-click an owned Talented Cat to upgrade.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
