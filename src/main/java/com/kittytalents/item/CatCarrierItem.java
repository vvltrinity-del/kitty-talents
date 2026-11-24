package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CatCarrierItem extends Item {
    public CatCarrierItem(Properties properties) {
        super(properties);
    }

    public boolean hasStoredCat(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("StoredCat");
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!hasStoredCat(stack) && target instanceof TalentedCat cat && com.kittytalents.core.perm.CatPermissions.can(player, cat, com.kittytalents.core.perm.CatAction.CARE)) {
            Level level = target.level();
            if (!level.isClientSide) {
                CompoundTag catTag = new CompoundTag();
                cat.saveWithoutId(catTag);

                stack.getOrCreateTag().put("StoredCat", catTag);
                stack.getTag().putString("CatName", cat.getName().getString());
                stack.getTag().putInt("CatLevel", cat.getCatLevel());

                cat.discard();

                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.displayClientMessage(Component.literal("Safely secured " + cat.getName().getString() + " inside carrier!").withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();

        if (hasStoredCat(stack)) {
            if (!level.isClientSide) {
                CompoundTag catTag = stack.getTag().getCompound("StoredCat");
                TalentedCat cat = ModEntities.TALENTED_CAT.get().create(level);
                if (cat != null) {
                    cat.load(catTag);
                    cat.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player != null ? player.getYRot() : 0, 0);
                    level.addFreshEntity(cat);

                    stack.getTag().remove("StoredCat");
                    stack.getTag().remove("CatName");
                    stack.getTag().remove("CatLevel");

                    level.playSound(null, pos, SoundEvents.CAT_PURREOW, SoundSource.PLAYERS, 1.0F, 1.0F);
                    if (player != null) {
                        player.displayClientMessage(Component.literal("Released " + cat.getName().getString() + " from carrier.").withStyle(ChatFormatting.YELLOW), true);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (hasStoredCat(stack)) {
            String name = stack.getTag().getString("CatName");
            int lvl = stack.getTag().getInt("CatLevel");
            tooltip.add(Component.literal("Contains: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(name).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" (Lvl " + lvl + ")").withStyle(ChatFormatting.YELLOW)));
            tooltip.add(Component.literal("Right-Click on ground to release").withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.literal("Empty Carrier").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("Right-Click your cat to safely secure it").withStyle(ChatFormatting.GRAY));
        }
    }
}
