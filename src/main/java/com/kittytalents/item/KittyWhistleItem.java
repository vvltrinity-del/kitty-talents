package com.kittytalents.item;

import com.kittytalents.config.KittyTalentsConfig;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import com.kittytalents.entity.WhistleCommand;

import java.util.List;

public class KittyWhistleItem extends Item {
    public KittyWhistleItem(Properties properties) {
        super(properties);
    }

    public WhistleCommand getCommand(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("WhistleCmd")) {
            int idx = stack.getTag().getInt("WhistleCmd");
            if (idx >= 0 && idx < WhistleCommand.values().length) {
                return WhistleCommand.values()[idx];
            }
        }
        return WhistleCommand.HEEL;
    }

    public void setCommand(ItemStack stack, WhistleCommand cmd) {
        stack.getOrCreateTag().putInt("WhistleCmd", cmd.ordinal());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                        net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> com.kittytalents.client.ClientModEvents.openWhistleScreen()
                );
            }
            return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
        }

        // Blow Whistle!
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_FLUTE.get(), SoundSource.PLAYERS, 1.0F, 1.8F);

        if (!level.isClientSide) {
            int radius = KittyTalentsConfig.COMMON.whistleRadius.get();
            List<TalentedCat> cats = level.getEntitiesOfClass(
                    TalentedCat.class,
                    new AABB(player.blockPosition()).inflate(radius),
                    c -> c.isOwnedBy(player) && !c.isIncapacitated()
            );

            WhistleCommand cmd = getCommand(held);
            int count = 0;

            for (TalentedCat cat : cats) {
                switch (cmd) {
                    case HEEL -> {
                        cat.setTarget(null);
                        cat.setOrderedToSit(false);
                        cat.setCatMode(CatMode.FOLLOW);
                        if (cat.distanceToSqr(player) > 64.0D) {
                            cat.teleportTo(player.getX(), player.getY(), player.getZ());
                        } else {
                            cat.getNavigation().moveTo(player, 1.4D);
                        }
                    }
                    case STAY -> {
                        cat.setTarget(null);
                        cat.setCatMode(CatMode.DOCILE);
                        cat.setOrderedToSit(true);
                    }
                    case FOLLOW -> {
                        cat.setOrderedToSit(false);
                        cat.setCatMode(CatMode.FOLLOW);
                    }
                    case GUARD -> {
                        cat.setOrderedToSit(false);
                        cat.setCatMode(CatMode.GUARD);
                    }
                    case HUNT -> {
                        cat.setOrderedToSit(false);
                        cat.setCatMode(CatMode.HUNTING);
                    }
                    case AGGRESSIVE -> {
                        cat.setOrderedToSit(false);
                        cat.setCatMode(CatMode.AGGRESSIVE);
                    }
                    case RETURN_HOME -> {
                        BlockPos home = cat.getHomeBedPos();
                        if (home != null) {
                            cat.setTarget(null);
                            cat.setOrderedToSit(false);
                            cat.setCatMode(CatMode.WANDER);
                            cat.teleportTo(home.getX() + 0.5D, home.getY() + 0.5D, home.getZ() + 0.5D);
                        }
                    }
                    case STOP_ATTACK -> {
                        cat.setTarget(null);
                        cat.getNavigation().stop();
                    }
                    case QUIET -> {
                        cat.setQuietUntil(cat.tickCount + 6000); // 5 mins silence
                    }
                }
                count++;
            }

            player.displayClientMessage(Component.literal("Whistle: " + cmd.getTitle() + " (" + count + " cats commanded)").withStyle(ChatFormatting.GREEN), true);
        }

        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        WhistleCommand cmd = getCommand(stack);
        tooltip.add(Component.literal("Mode: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(cmd.getTitle()).withStyle(ChatFormatting.YELLOW)));
        tooltip.add(Component.literal(cmd.getDesc()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Right-Click: Blow Whistle").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Sneak + Right-Click: Change Command").withStyle(ChatFormatting.DARK_AQUA));
    }
}
