package com.kittytalents.item;

import com.kittytalents.block.KittyHabitatBlockEntity;
import com.kittytalents.entity.CatVariant;
import com.kittytalents.entity.ProductionTier;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class KittyWorkClipboardItem extends Item {
    public KittyWorkClipboardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TalentedCat cat && (cat.isOwnedBy(player) || cat.isTrusted(player))) {
            if (!player.level().isClientSide) {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putUUID("CatUUID", cat.getUUID());
                tag.putString("CatName", cat.getName().getString());
                tag.putString("CatVariant", cat.getCatVariant().getId());
                tag.putString("CatTier", cat.getProductionTier().getId());
                tag.putInt("CatLevel", cat.getCatLevel());
                tag.putBoolean("CatFemale", cat.isFemale());
                tag.putString("CatRole", cat.getCatVariant().getTraitDescription());

                cat.playSound(SoundEvents.VILLAGER_WORK_LIBRARIAN, 1.0F, 1.2F);
                if (cat.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            cat.getX(), cat.getY() + 0.6D, cat.getZ(),
                            5, 0.2D, 0.2D, 0.2D, 0.05D);
                }

                player.displayClientMessage(Component.literal("Selected Worker: ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(cat.getName().getString() + " [" + cat.getProductionTier().getDisplayName() + " Tier - " + cat.getCatVariant().getDisplayName() + "]").withStyle(ChatFormatting.GREEN)), true);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getTag();

        if (level.getBlockEntity(pos) instanceof KittyHabitatBlockEntity habitat) {
            if (tag != null && (tag.hasUUID("CatUUID") || tag.contains("CatUUID"))) {
                if (!level.isClientSide) {
                    UUID catUUID = tag.hasUUID("CatUUID") ? tag.getUUID("CatUUID") : UUID.fromString(tag.getString("CatUUID"));
                    String catName = tag.getString("CatName");
                    CatVariant variant = CatVariant.fromId(tag.getString("CatVariant"));
                    ProductionTier tier = tag.contains("CatTier") ?
                            ProductionTier.fromId(tag.getString("CatTier")) : ProductionTier.STANDARD;
                    int catLvl = tag.getInt("CatLevel");
                    boolean female = tag.getBoolean("CatFemale");
                    String role = tag.getString("CatRole");

                    if (level instanceof ServerLevel sl) {
                        Entity e = sl.getEntity(catUUID);
                        if (e instanceof TalentedCat activeCat) {
                            catName = activeCat.getName().getString();
                            variant = activeCat.getCatVariant();
                            tier = activeCat.getProductionTier();
                            catLvl = activeCat.getCatLevel();
                            female = activeCat.isFemale();
                        }
                    }

                    habitat.assignWorker(catUUID, catName, variant, tier, catLvl, female, role);
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.BLOCKS, 1.0F, 1.5F);
                    if (player != null) {
                        player.displayClientMessage(Component.literal("Worker " + catName + " assigned to Production Habitat").withStyle(ChatFormatting.GREEN), true);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            } else if (player != null && !level.isClientSide) {
                player.displayClientMessage(Component.literal("No cat recorded on clipboard! Right-click an owned Talented Cat first.").withStyle(ChatFormatting.RED), true);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && (tag.hasUUID("CatUUID") || tag.contains("CatUUID"))) {
            tooltip.add(Component.literal("=== SELECTED CAT WORKER ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltip.add(Component.literal("Name: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(tag.getString("CatName")).withStyle(ChatFormatting.WHITE)));
            
            CatVariant variant = CatVariant.fromId(tag.getString("CatVariant"));
            tooltip.add(Component.literal("Breed: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(variant.getDisplayName()).withStyle(ChatFormatting.AQUA)));

            ProductionTier tier = tag.contains("CatTier") ? ProductionTier.fromId(tag.getString("CatTier")) : ProductionTier.STANDARD;
            tooltip.add(Component.literal("Production Tier: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(tier.getDisplayName()).withStyle(tier.getFormatting())));

            tooltip.add(Component.literal("Level: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(tag.getInt("CatLevel"))).withStyle(ChatFormatting.YELLOW)));
            tooltip.add(Component.literal("Gender: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(tag.getBoolean("CatFemale") ? "Female ♀" : "Male ♂").withStyle(tag.getBoolean("CatFemale") ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.BLUE)));
            tooltip.add(Component.literal("Worker Role: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(tag.getString("CatRole")).withStyle(ChatFormatting.GREEN)));
            tooltip.add(Component.literal("Right-click a Production Habitat to assign.").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("Kitty Work Clipboard").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("Right-click an owned Talented Cat to select worker.").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Right-click a Production Habitat to assign worker.").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
