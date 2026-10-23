package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class FelineLocatorOrbItem extends Item {
    public FelineLocatorOrbItem(Properties properties) {
        super(properties);
    }

    public static void bindCat(ItemStack stack, TalentedCat cat) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID("BoundCatUUID", cat.getUUID());
        tag.putString("BoundCatName", cat.getName().getString());
        tag.putString("OrbColor", cat.getCollarColor().getName());
    }

    public static boolean isBound(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID("BoundCatUUID");
    }

    @Nullable
    public static UUID getBoundUUID(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID("BoundCatUUID") ? stack.getTag().getUUID("BoundCatUUID") : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(this, 30);
        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.3F);

        if (!level.isClientSide) {
            UUID boundId = getBoundUUID(held);
            String name = held.hasTag() && held.getTag().contains("BoundCatName") ? held.getTag().getString("BoundCatName") : "Companion";

            if (boundId == null) {
                player.sendSystemMessage(Component.literal("Orb is empty. Right-click your cat with an Empty Locator Orb to bind it!").withStyle(ChatFormatting.GRAY));
                return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
            }

            player.sendSystemMessage(Component.literal("=== [ LOCATOR ORB: " + name.toUpperCase() + " ] ===").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

            TalentedCat cat = null;
            if (level instanceof ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(boundId);
                if (entity instanceof TalentedCat talentedCat) {
                    cat = talentedCat;
                } else if (serverLevel.getServer() != null) {
                    for (ServerLevel sl : serverLevel.getServer().getAllLevels()) {
                        if (sl != serverLevel) {
                            Entity other = sl.getEntity(boundId);
                            if (other instanceof TalentedCat tc) {
                                cat = tc;
                                break;
                            }
                        }
                    }
                }
            }

            if (cat != null) {
                boolean sameDim = cat.level() == level;
                String status = cat.isIncapacitated() ? (ChatFormatting.RED + "[DOWNED]") :
                        (cat.isOrderedToSit() ? (ChatFormatting.YELLOW + "[SITTING]") : (ChatFormatting.GREEN + "[ACTIVE]"));
                String dimName = cat.level().dimension().location().getPath();
                String distStr = sameDim ? String.format("%.0f blocks", Math.sqrt(cat.distanceToSqr(player))) : "Different Dimension";

                player.sendSystemMessage(Component.literal("Status: ").withStyle(ChatFormatting.GRAY).append(Component.literal(status)));
                player.sendSystemMessage(Component.literal(String.format("Distance: %s | Dimension: %s", distStr, dimName)).withStyle(ChatFormatting.AQUA));
                player.sendSystemMessage(Component.literal(String.format("Coordinates: X: %.0f, Y: %.0f, Z: %.0f", cat.getX(), cat.getY(), cat.getZ())).withStyle(ChatFormatting.YELLOW));
                player.sendSystemMessage(Component.literal(String.format("Health: %d/%d HP | Hunger: %d/120", (int) cat.getHealth(), (int) cat.getMaxHealth(), cat.getHunger())).withStyle(ChatFormatting.GREEN));
                player.sendSystemMessage(Component.literal("Mode: " + cat.getCatMode().name()).withStyle(ChatFormatting.GOLD));
            } else {
                player.sendSystemMessage(Component.literal(name + " is currently offline or in an unloaded chunk/dimension.").withStyle(ChatFormatting.YELLOW));
            }

            player.sendSystemMessage(Component.literal("=======================================").withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isBound(stack)) {
            String name = stack.getTag().getString("BoundCatName");
            tooltip.add(Component.literal("Bound to: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(name).withStyle(ChatFormatting.LIGHT_PURPLE)));
            tooltip.add(Component.literal("Right-click to track long-range coordinates and vital stats.").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("Unbound. Right-click your cat to bind.").withStyle(ChatFormatting.GRAY));
        }
    }
}
